package main

import (
	"context"
	"database/sql"
	"fmt"
	"log"

	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
)

// MongoDB'ye Users ekleme
func insertUsers(db *mongo.Database, batch []UserData) error {
	collection := db.Collection("Users")
	
	// Batch insert için documents hazırla
	documents := make([]interface{}, len(batch))
	for i, user := range batch {
		documents[i] = bson.M{
			"_id":            user.MongoID,
			"uuid":           user.UserUUID,
			"tc":             user.TCNo,
			"email":          user.Email,
			"tel":            user.Phone,
			"hashedPassword": user.HashedPassword,
			"createdAt":      user.CreatedAt,
		}
	}
	
	// Batch insert
	_, err := collection.InsertMany(context.Background(), documents)
	if err != nil {
		return fmt.Errorf("failed to insert users: %w", err)
	}
	
	return nil
}

// MSSQL'e Clients ekleme
func insertClients(db *sql.DB, batch []UserData) error {
	ctx := context.Background()
	
	// Transaction başlat
	tx, err := db.BeginTx(ctx, nil)
	if err != nil {
		return fmt.Errorf("failed to begin transaction: %w", err)
	}
	defer tx.Rollback()
	
	// Prepared statement
	stmt, err := tx.PrepareContext(ctx, `
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
		)
	`)
	if err != nil {
		return fmt.Errorf("failed to prepare statement: %w", err)
	}
	defer stmt.Close()
	
	// Batch insert
	for _, user := range batch {
		_, err := stmt.ExecContext(ctx,
			user.ClientID.String(),
			user.UserUUID,
			user.TCNo,
			user.FirstName,
			user.LastName,
			user.Email,
			user.Phone,
			user.BirthDate,
			user.Address.Street,
			user.Address.City,
			user.Address.District,
			user.Address.PostalCode,
			"ACTIVE",
			user.CreatedAt,
		)
		if err != nil {
			return fmt.Errorf("failed to insert client %s: %w", user.UserUUID, err)
		}
	}
	
	// Commit
	if err := tx.Commit(); err != nil {
		return fmt.Errorf("failed to commit transaction: %w", err)
	}
	
	return nil
}

// MSSQL'e Accounts ekleme
func insertAccounts(db *sql.DB, batch []UserData) error {
	ctx := context.Background()
	
	tx, err := db.BeginTx(ctx, nil)
	if err != nil {
		return fmt.Errorf("failed to begin transaction: %w", err)
	}
	defer tx.Rollback()
	
	stmt, err := tx.PrepareContext(ctx, `
		INSERT INTO accounts (
			id, customer_id, account_number, product_code,
			status, currency, configurations, created_at, updated_at
		) VALUES (
			@p1, @p2, @p3, @p4, @p5, @p6, @p7, @p8, @p9
		)
	`)
	if err != nil {
		return fmt.Errorf("failed to prepare statement: %w", err)
	}
	defer stmt.Close()
	
	for _, user := range batch {
		for _, account := range user.Accounts {
			_, err := stmt.ExecContext(ctx,
				account.ID.String(),
				account.CustomerID,
				account.AccountNumber,
				account.ProductCode,
				account.Status,
				account.Currency,
				"{}",
				account.CreatedAt,
				account.UpdatedAt,
			)
			if err != nil {
				return fmt.Errorf("failed to insert account %s: %w", account.ID, err)
			}
		}
	}
	
	if err := tx.Commit(); err != nil {
		return fmt.Errorf("failed to commit transaction: %w", err)
	}
	
	return nil
}

// MSSQL'e Cards ekleme
func insertCards(db *sql.DB, batch []UserData) error {
	ctx := context.Background()
	
	tx, err := db.BeginTx(ctx, nil)
	if err != nil {
		return fmt.Errorf("failed to begin transaction: %w", err)
	}
	defer tx.Rollback()
	
	stmt, err := tx.PrepareContext(ctx, `
		INSERT INTO cards (
			id, card_type, card_number, masked_number, cvv,
			cardholder_name, expiry_month, expiry_year, card_brand,
			status, account_id, daily_withdrawal_limit, atm_enabled,
			online_only, created_at
		) VALUES (
			@p1, @p2, @p3, @p4, @p5,
			@p6, @p7, @p8, @p9,
			@p10, @p11, @p12, @p13,
			@p14, @p15
		)
	`)
	if err != nil {
		return fmt.Errorf("failed to prepare statement: %w", err)
	}
	defer stmt.Close()
	
	for _, user := range batch {
		for _, card := range user.Cards {
			_, err := stmt.ExecContext(ctx,
				card.ID.String(),
				card.CardType,
				card.CardNumber,
				card.MaskedNumber,
				card.CVV,
				card.CardholderName,
				card.ExpiryMonth,
				card.ExpiryYear,
				card.CardBrand,
				card.Status,
				card.AccountID.String(),
				card.DailyWithdrawalLimit,
				card.ATMEnabled,
				card.OnlineOnly,
				card.CreatedAt,
			)
			if err != nil {
				return fmt.Errorf("failed to insert card %s: %w", card.ID, err)
			}
		}
	}
	
	if err := tx.Commit(); err != nil {
		return fmt.Errorf("failed to commit transaction: %w", err)
	}
	
	return nil
}

// MSSQL'e Transactions ekleme
func insertTransactions(db *sql.DB, batch []UserData) error {
	ctx := context.Background()
	
	tx, err := db.BeginTx(ctx, nil)
	if err != nil {
		return fmt.Errorf("failed to begin transaction: %w", err)
	}
	defer tx.Rollback()
	
	stmt, err := tx.PrepareContext(ctx, `
		INSERT INTO transactions (
			id, type, amount, currency,
			from_account_id, to_account_id, status,
			description, created_at
		) VALUES (
			@p1, @p2, @p3, @p4, @p5, @p6, @p7, @p8, @p9
		)
	`)
	if err != nil {
		return fmt.Errorf("failed to prepare statement: %w", err)
	}
	defer stmt.Close()
	
	for _, user := range batch {
		for _, tx := range user.Transactions {
			var fromAccID, toAccID interface{}
			if tx.FromAccountID != nil {
				fromAccID = tx.FromAccountID.String()
			}
			if tx.ToAccountID != nil {
				toAccID = tx.ToAccountID.String()
			}
			
			_, err := stmt.ExecContext(ctx,
				tx.ID.String(),
				tx.Type,
				tx.Amount,
				tx.Currency,
				fromAccID,
				toAccID,
				tx.Status,
				tx.Description,
				tx.CreatedAt,
			)
			if err != nil {
				return fmt.Errorf("failed to insert transaction %s: %w", tx.ID, err)
			}
		}
	}
	
	if err := tx.Commit(); err != nil {
		return fmt.Errorf("failed to commit transaction: %w", err)
	}
	
	return nil
}

// MSSQL'e Ledger Entries ekleme
func insertLedgerEntries(db *sql.DB, batch []UserData) error {
	ctx := context.Background()
	
	tx, err := db.BeginTx(ctx, nil)
	if err != nil {
		return fmt.Errorf("failed to begin transaction: %w", err)
	}
	defer tx.Rollback()
	
	stmt, err := tx.PrepareContext(ctx, `
		INSERT INTO ledger_entries (
			id, transaction_id, account_id, type,
			amount, currency, balance, created_at
		) VALUES (
			@p1, @p2, @p3, @p4, @p5, @p6, @p7, @p8
		)
	`)
	if err != nil {
		return fmt.Errorf("failed to prepare statement: %w", err)
	}
	defer stmt.Close()
	
	for _, user := range batch {
		for _, entry := range user.LedgerEntries {
			_, err := stmt.ExecContext(ctx,
				entry.ID.String(),
				entry.TransactionID.String(),
				entry.AccountID.String(),
				entry.Type,
				entry.Amount,
				entry.Currency,
				entry.Balance,
				entry.CreatedAt,
			)
			if err != nil {
				log.Printf("Warning: failed to insert ledger entry %s: %v", entry.ID, err)
				// Ledger hatalarında devam et
			}
		}
	}
	
	if err := tx.Commit(); err != nil {
		return fmt.Errorf("failed to commit transaction: %w", err)
	}
	
	return nil
}
