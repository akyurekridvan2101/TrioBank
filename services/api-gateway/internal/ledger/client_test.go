package ledger

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/redis/go-redis/v9"
)

// ==========================================
// UNIT TESTS - LEDGER CLIENT
// ==========================================

// setupTestClient - Test için client oluşturur
func setupTestClient() (*Client, *redis.Client) {
	// Test Redis client (mock olarak kullanılabilir)
	redisClient := redis.NewClient(&redis.Options{
		Addr: "localhost:6379",
		DB:   1, // Test database
	})

	client := NewClient("http://test-ledger", redisClient)
	return client, redisClient
}

// TestGetBalance - Balance endpoint testi
func TestGetBalance(t *testing.T) {
	// Mock HTTP server
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/api/v1/ledger/balances/acc-123" {
			t.Errorf("Expected path /api/v1/ledger/balances/acc-123, got %s", r.URL.Path)
		}

		response := BalanceResponse{
			AccountID:        "acc-123",
			Balance:          15000.00,
			BlockedAmount:    2000.00,
			AvailableBalance: 13000.00,
			Currency:         "TRY",
			LastUpdatedAt:    time.Now(),
			Version:          1,
		}

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(response)
	}))
	defer server.Close()

	// Test client
	redisClient := redis.NewClient(&redis.Options{Addr: "localhost:6379", DB: 1})
	client := NewClient(server.URL, redisClient)

	// Test
	ctx := context.Background()
	balance, err := client.GetBalance(ctx, "acc-123")

	if err != nil {
		t.Fatalf("Expected no error, got %v", err)
	}

	if balance.AccountID != "acc-123" {
		t.Errorf("Expected accountId acc-123, got %s", balance.AccountID)
	}

	if balance.AvailableBalance != 13000.00 {
		t.Errorf("Expected availableBalance 13000.00, got %f", balance.AvailableBalance)
	}
}

// TestGetBalance_NotFound - 404 error testi
func TestGetBalance_NotFound(t *testing.T) {
	// Mock HTTP server - 404 döndür
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{
			"error": "Account not found",
		})
	}))
	defer server.Close()

	// Test client
	redisClient := redis.NewClient(&redis.Options{Addr: "localhost:6379", DB: 1})
	client := NewClient(server.URL, redisClient)

	// Test
	ctx := context.Background()
	_, err := client.GetBalance(ctx, "non-existent")

	if err == nil {
		t.Fatal("Expected error, got nil")
	}

	ledgerErr, ok := err.(*LedgerError)
	if !ok {
		t.Fatalf("Expected LedgerError, got %T", err)
	}

	if !ledgerErr.IsAccountNotFound() {
		t.Errorf("Expected IsAccountNotFound to be true")
	}
}

// TestGetStatement - Statement endpoint testi
func TestGetStatement(t *testing.T) {
	// Mock HTTP server
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// Check query parameters
		if r.URL.Query().Get("page") != "0" {
			t.Errorf("Expected page=0, got %s", r.URL.Query().Get("page"))
		}

		response := StatementResponse{
			AccountID:      "acc-123",
			Currency:       "TRY",
			OpeningBalance: 10000.00,
			ClosingBalance: 15000.00,
			TotalDebits:    5000.00,
			TotalCredits:   10000.00,
			NetChange:      5000.00,
			EntryCount:     2,
			Entries: []StatementEntry{
				{
					EntryID:        "entry-1",
					TransactionID:  "txn-1",
					Date:           "2025-12-21",
					EntryType:      "CREDIT",
					Amount:         5000.00,
					RunningBalance: 15000.00,
					Description:    "Maaş",
				},
			},
			Pagination: PaginationMetadata{
				Page:          0,
				Size:          20,
				TotalElements: 2,
				TotalPages:    1,
			},
		}

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(response)
	}))
	defer server.Close()

	// Test client
	redisClient := redis.NewClient(&redis.Options{Addr: "localhost:6379", DB: 1})
	client := NewClient(server.URL, redisClient)

	// Test
	ctx := context.Background()
	opts := NewStatementOptions()
	statement, err := client.GetStatement(ctx, "acc-123", opts)

	if err != nil {
		t.Fatalf("Expected no error, got %v", err)
	}

	if statement.AccountID != "acc-123" {
		t.Errorf("Expected accountId acc-123, got %s", statement.AccountID)
	}

	if len(statement.Entries) != 1 {
		t.Errorf("Expected 1 entry, got %d", len(statement.Entries))
	}

	if statement.NetChange != 5000.00 {
		t.Errorf("Expected netChange 5000.00, got %f", statement.NetChange)
	}
}

// TestStatementOptions - Default options testi
func TestStatementOptions(t *testing.T) {
	opts := NewStatementOptions()

	if opts.Page != 0 {
		t.Errorf("Expected default page 0, got %d", opts.Page)
	}

	if opts.Size != 20 {
		t.Errorf("Expected default size 20, got %d", opts.Size)
	}

	if !opts.IncludeRunningBalance {
		t.Errorf("Expected default includeRunningBalance true")
	}
}

// TestLedgerError - Error type testi
func TestLedgerError(t *testing.T) {
	err := NewLedgerError(404, "ACCOUNT_NOT_FOUND", "Account not found")

	if !err.IsAccountNotFound() {
		t.Errorf("Expected IsAccountNotFound to be true")
	}

	if err.IsServerError() {
		t.Errorf("Expected IsServerError to be false")
	}

	expectedMsg := "[Ledger 404] ACCOUNT_NOT_FOUND: Account not found"
	if err.Error() != expectedMsg {
		t.Errorf("Expected error message '%s', got '%s'", expectedMsg, err.Error())
	}
}

// TestLedgerError_ServerError - 500 error testi
func TestLedgerError_ServerError(t *testing.T) {
	err := NewLedgerError(500, "INTERNAL_ERROR", "Server error")

	if !err.IsServerError() {
		t.Errorf("Expected IsServerError to be true")
	}

	if err.IsAccountNotFound() {
		t.Errorf("Expected IsAccountNotFound to be false")
	}
}
