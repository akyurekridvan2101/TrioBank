package internal

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/TrioBank/triobank-platform/microservices/client-service/config"
	"github.com/google/uuid"
	_ "github.com/microsoft/go-mssqldb" // MSSQL driver
)

type DataBaseSql struct {
	DB *sql.DB
}

func StartSqlDb() (*sql.DB, error) {
	username := config.GetEnv("SQL_USERNAME")
	password := config.GetEnv("SQL_PASSWORD")
	address := config.GetEnv("SQL_ADDRESS")
	dbName := config.GetEnv("SQL_DATABASE_NAME")

	// First connect to master database to create client_db if it doesn't exist
	masterConnString := fmt.Sprintf("sqlserver://%s:%s@%s?database=master", username, password, address)
	masterDB, err := sql.Open("sqlserver", masterConnString)
	if err != nil {
		return nil, fmt.Errorf("failed to connect to master database: %w", err)
	}
	defer masterDB.Close()

	// Check if database exists, create if not
	createDBQuery := fmt.Sprintf(`
		IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = '%s')
		BEGIN
			CREATE DATABASE [%s]
		END
	`, dbName, dbName)

	_, err = masterDB.Exec(createDBQuery)
	if err != nil {
		return nil, fmt.Errorf("failed to create database: %w", err)
	}

	// Now connect to the client database
	connString := fmt.Sprintf("sqlserver://%s:%s@%s?database=%s", username, password, address, dbName)
	db, err := sql.Open("sqlserver", connString)
	if err != nil {
		return nil, err
	}

	db.SetMaxOpenConns(25)
	db.SetMaxIdleConns(25)
	db.SetConnMaxLifetime(5 * time.Minute)

	err = db.Ping()
	if err != nil {
		return nil, err
	}
	return db, nil
}

// InitializeDatabase creates tables if they don't exist
func InitializeDatabase(db *sql.DB) error {
	ctx := context.Background()

	// Create clients table with status column
	createTableQuery := `
	IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='clients' AND xtype='U')
	BEGIN
		CREATE TABLE clients (
			id VARCHAR(36) PRIMARY KEY,
			user_id VARCHAR(36) NOT NULL,
			tc_no VARCHAR(11) NOT NULL,
			first_name NVARCHAR(100) NOT NULL,
			last_name NVARCHAR(100) NOT NULL,
			email VARCHAR(255) NOT NULL,
			gsm VARCHAR(20),
			birth_date DATE NOT NULL,
			address_street NVARCHAR(255),
			address_city NVARCHAR(100),
			address_district NVARCHAR(100),
			address_postal_code VARCHAR(10),
			status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
			created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
			updated_at DATETIME2,
			CONSTRAINT CK_tc_no_length CHECK (LEN(tc_no) = 11)
		);
		
		CREATE INDEX IX_clients_user_id ON clients(user_id);
		CREATE INDEX IX_clients_tc_no ON clients(tc_no);
		CREATE INDEX IX_clients_email ON clients(email);
		CREATE INDEX IX_clients_status ON clients(status);
	END
	
	-- Add status column if it doesn't exist (for existing databases)
	IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'clients' AND COLUMN_NAME = 'status')
	BEGIN
		ALTER TABLE clients ADD status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
	END
	
	-- Add updated_at column if it doesn't exist
	IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'clients' AND COLUMN_NAME = 'updated_at')
	BEGIN
		ALTER TABLE clients ADD updated_at DATETIME2;
	END
	`

	_, err := db.ExecContext(ctx, createTableQuery)
	return err
}

func (db *DataBaseSql) CreateClient(ctx context.Context, client *Client) error {
	query := `
	INSERT INTO clients (
		id, user_id, tc_no, first_name, last_name,
		email, gsm, birth_date,
		address_street, address_city, address_district, address_postal_code,
		status, created_at
	) VALUES (
		@p1, @p2, @p3, @p4, @p5,
		@p6, @p7, @p8,
		@p9, @p10, @p11, @p12,
		@p13, @p14
	)`

	// Set default status to ACTIVE
	if client.Status == "" {
		client.Status = ClientStatusActive
	}

	_, err := db.DB.ExecContext(ctx, query,
		client.ID.String(),
		client.UserID.String(),
		client.TCNo,
		client.FirstName,
		client.LastName,
		client.Email,
		client.GSM,
		client.BirthDate,
		client.Address.Street,
		client.Address.City,
		client.Address.District,
		client.Address.PostalCode,
		string(client.Status),
		client.CreatedAt,
	)
	return err
}

func (db *DataBaseSql) GetClientByID(ctx context.Context, id uuid.UUID) (*Client, error) {
	query := `
		SELECT 
			id, user_id, tc_no, first_name, last_name,
			email, gsm, birth_date,
			ISNULL(address_street, ''), ISNULL(address_city, ''), 
			ISNULL(address_district, ''), ISNULL(address_postal_code, ''),
			created_at
		FROM clients 
		WHERE id = @p1`

	var client Client
	var idStr, userIDStr string
	err := db.DB.QueryRowContext(ctx, query, id.String()).Scan(
		&idStr,
		&userIDStr,
		&client.TCNo,
		&client.FirstName,
		&client.LastName,
		&client.Email,
		&client.GSM,
		&client.BirthDate,
		&client.Address.Street,
		&client.Address.City,
		&client.Address.District,
		&client.Address.PostalCode,
		&client.CreatedAt,
	)
	if errors.Is(err, sql.ErrNoRows) {
		return nil, ErrClientNotFound
	}
	if err != nil {
		return nil, err
	}
	client.ID, _ = uuid.Parse(idStr)
	client.UserID, _ = uuid.Parse(userIDStr)
	return &client, nil
}

func (db *DataBaseSql) GetClientByUserID(ctx context.Context, userID uuid.UUID) (*Client, error) {
	query := `
		SELECT 
			id, user_id, tc_no, first_name, last_name,
			email, gsm, birth_date,
			ISNULL(address_street, ''), ISNULL(address_city, ''), 
			ISNULL(address_district, ''), ISNULL(address_postal_code, ''),
			created_at
		FROM clients 
		WHERE user_id = @p1
	`

	var client Client
	var idStr, userIDStr string
	err := db.DB.QueryRowContext(ctx, query, userID.String()).Scan(
		&idStr,
		&userIDStr,
		&client.TCNo,
		&client.FirstName,
		&client.LastName,
		&client.Email,
		&client.GSM,
		&client.BirthDate,
		&client.Address.Street,
		&client.Address.City,
		&client.Address.District,
		&client.Address.PostalCode,
		&client.CreatedAt,
	)

	if errors.Is(err, sql.ErrNoRows) {
		return nil, ErrClientNotFound
	}
	if err != nil {
		return nil, err
	}
	client.ID, _ = uuid.Parse(idStr)
	client.UserID, _ = uuid.Parse(userIDStr)
	return &client, nil
}

func (db *DataBaseSql) GetClientByTCNo(ctx context.Context, tcNo string) (*Client, error) {
	query := `
		SELECT
			id, user_id, tc_no, first_name, last_name,
			email, gsm, birth_date,
			ISNULL(address_street, ''), ISNULL(address_city, ''), 
			ISNULL(address_district, ''), ISNULL(address_postal_code, ''),
			created_at
		FROM clients
		WHERE tc_no = @p1
	`

	var client Client
	var idStr, userIDStr string
	err := db.DB.QueryRowContext(ctx, query, tcNo).Scan(
		&idStr,
		&userIDStr,
		&client.TCNo,
		&client.FirstName,
		&client.LastName,
		&client.Email,
		&client.GSM,
		&client.BirthDate,
		&client.Address.Street,
		&client.Address.City,
		&client.Address.District,
		&client.Address.PostalCode,
		&client.CreatedAt,
	)
	if errors.Is(err, sql.ErrNoRows) {
		return nil, ErrClientNotFound
	}
	if err != nil {
		return nil, err
	}
	client.ID, _ = uuid.Parse(idStr)
	client.UserID, _ = uuid.Parse(userIDStr)
	return &client, nil
}

func (db *DataBaseSql) UpdateClient(ctx context.Context, id uuid.UUID, req UpdateClient) (*Client, error) {
	// Build dynamic UPDATE query to only include fields that are being updated
	// This prevents nil pointers from accidentally overwriting existing database values
	updates := []string{}
	args := []interface{}{id.String()} // First arg is always ID (@p1)
	argIndex := 2

	// Only add email to update if it's explicitly provided
	if req.Email != nil {
		updates = append(updates, fmt.Sprintf("email = @p%d", argIndex))
		args = append(args, *req.Email)
		argIndex++
	}

	// Only add GSM to update if it's explicitly provided
	if req.GSM != nil {
		updates = append(updates, fmt.Sprintf("gsm = @p%d", argIndex))
		args = append(args, *req.GSM)
		argIndex++
	}

	// Only add address fields if address is provided
	if req.Address != nil {
		updates = append(updates, fmt.Sprintf("address_street = @p%d", argIndex))
		args = append(args, req.Address.Street)
		argIndex++

		updates = append(updates, fmt.Sprintf("address_city = @p%d", argIndex))
		args = append(args, req.Address.City)
		argIndex++

		updates = append(updates, fmt.Sprintf("address_district = @p%d", argIndex))
		args = append(args, req.Address.District)
		argIndex++

		updates = append(updates, fmt.Sprintf("address_postal_code = @p%d", argIndex))
		args = append(args, req.Address.PostalCode)
		argIndex++
	}

	// If no fields to update, return error
	if len(updates) == 0 {
		return nil, errors.New("no fields to update")
	}

	// Always update the updated_at timestamp
	updates = append(updates, "updated_at = GETDATE()")

	// Build final query
	query := fmt.Sprintf(`
		UPDATE clients SET %s
		WHERE id = @p1
	`, strings.Join(updates, ", "))

	_, err := db.DB.ExecContext(ctx, query, args...)
	if err != nil {
		return nil, err
	}

	return db.GetClientByID(ctx, id)
}

func (db *DataBaseSql) DeleteClient(ctx context.Context, id uuid.UUID) error {
	query := `DELETE FROM clients WHERE id = @p1`

	result, err := db.DB.ExecContext(ctx, query, id.String())
	if err != nil {
		return err
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		return err
	}
	if rowsAffected == 0 {
		return ErrClientNotFound
	}

	return nil
}

func (db *DataBaseSql) DeleteClientByUserID(ctx context.Context, userID uuid.UUID) error {
	query := `DELETE FROM clients WHERE user_id = @p1`

	result, err := db.DB.ExecContext(ctx, query, userID.String())
	if err != nil {
		return err
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		return err
	}
	if rowsAffected == 0 {
		return ErrClientNotFound
	}

	return nil
}

// PassivizeClientByUserID sets client status to PASSIVE (soft delete)
func (db *DataBaseSql) PassivizeClientByUserID(ctx context.Context, userID uuid.UUID) error {
	query := `UPDATE clients SET status = @p1, updated_at = @p2 WHERE user_id = @p3 AND status = 'ACTIVE'`

	now := time.Now().UTC()
	result, err := db.DB.ExecContext(ctx, query, string(ClientStatusPassive), now, userID.String())
	if err != nil {
		return err
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		return err
	}
	if rowsAffected == 0 {
		return ErrClientNotFound
	}

	return nil
}

func (db *DataBaseSql) ListClients(ctx context.Context, req PaginationRequest) ([]ClientSummary, int, error) {
	countQuery := `SELECT COUNT(*) FROM clients`

	var total int
	if err := db.DB.QueryRowContext(ctx, countQuery).Scan(&total); err != nil {
		return nil, 0, err
	}

	offset := (req.Page - 1) * req.Limit

	dataQuery := `
		SELECT id, first_name, last_name, email
		FROM clients 
		ORDER BY created_at DESC 
		OFFSET @p1 ROWS FETCH NEXT @p2 ROWS ONLY
	`

	rows, err := db.DB.QueryContext(ctx, dataQuery, offset, req.Limit)
	if err != nil {
		return nil, 0, err
	}
	defer rows.Close()

	var clients []ClientSummary
	for rows.Next() {
		var c ClientSummary
		var idStr string
		if err := rows.Scan(&idStr, &c.FirstName, &c.LastName, &c.Email); err != nil {
			return nil, 0, err
		}
		c.ID, _ = uuid.Parse(idStr)
		clients = append(clients, c)
	}

	return clients, total, nil
}

func (db *DataBaseSql) IsClientExistByTCNo(ctx context.Context, tcNo string) (bool, error) {
	query := `SELECT CASE WHEN EXISTS(SELECT 1 FROM clients WHERE tc_no = @p1) THEN 1 ELSE 0 END`

	var exists bool
	err := db.DB.QueryRowContext(ctx, query, tcNo).Scan(&exists)
	return exists, err
}

func (db *DataBaseSql) IsClientExistByUserID(ctx context.Context, userID uuid.UUID) (bool, error) {
	query := `SELECT CASE WHEN EXISTS(SELECT 1 FROM clients WHERE user_id = @p1) THEN 1 ELSE 0 END`

	var exists bool
	err := db.DB.QueryRowContext(ctx, query, userID.String()).Scan(&exists)
	return exists, err
}
