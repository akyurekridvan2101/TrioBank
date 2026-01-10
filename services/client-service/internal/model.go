package internal

import (
	"context"
	"net/http"
	"time"

	"github.com/google/uuid"
)

// ClientStatus represents the status of a client
type ClientStatus string

const (
	ClientStatusActive  ClientStatus = "ACTIVE"
	ClientStatusPassive ClientStatus = "PASSIVE"
)

type Client struct {
	ID        uuid.UUID    `json:"id" db:"id"`
	UserID    uuid.UUID    `json:"user_id" db:"user_id"`
	TCNo      string       `json:"tc_no" db:"tc_no"`
	FirstName string       `json:"first_name" db:"first_name"`
	LastName  string       `json:"last_name" db:"last_name"`
	Email     string       `json:"email" db:"email"`
	GSM       string       `json:"gsm" db:"gsm"`
	BirthDate time.Time    `json:"birth_date" db:"birth_date"`
	Address   Address      `json:"address"`
	Status    ClientStatus `json:"status" db:"status"`
	CreatedAt time.Time    `json:"created_at" db:"created_at"`
	UpdatedAt *time.Time   `json:"updated_at,omitempty" db:"updated_at"`
}

type Address struct {
	Street     string `json:"street" db:"address_street"`
	City       string `json:"city" db:"address_city"`
	District   string `json:"district" db:"address_district"`
	PostalCode string `json:"postal_code" db:"address_postal_code"`
}

type PaginationRequest struct {
	Page  int `json:"page"`
	Limit int `json:"limit"`
}

type ClientSummary struct {
	ID        uuid.UUID `json:"id"`
	FirstName string    `json:"first_name"`
	LastName  string    `json:"last_name"`
	Email     string    `json:"email"`
}

type UpdateClient struct {
	Email   *string  `json:"email,omitempty"`
	GSM     *string  `json:"gsm,omitempty"`
	Address *Address `json:"address,omitempty"`
}

type Repo struct {
	Db            DataBaseI
	Client        *http.Client
	AccountClient *AccountClient
}

type DataBaseI interface {
	CreateClient(ctx context.Context, client *Client) error
	GetClientByID(ctx context.Context, id uuid.UUID) (*Client, error)
	GetClientByUserID(ctx context.Context, userID uuid.UUID) (*Client, error)
	GetClientByTCNo(ctx context.Context, tcNo string) (*Client, error)
	UpdateClient(ctx context.Context, id uuid.UUID, req UpdateClient) (*Client, error)
	DeleteClient(ctx context.Context, id uuid.UUID) error
	DeleteClientByUserID(ctx context.Context, userID uuid.UUID) error
	PassivizeClientByUserID(ctx context.Context, userID uuid.UUID) error
	ListClients(ctx context.Context, req PaginationRequest) ([]ClientSummary, int, error)
	IsClientExistByTCNo(ctx context.Context, tcNo string) (bool, error)
	IsClientExistByUserID(ctx context.Context, userID uuid.UUID) (bool, error)
}
