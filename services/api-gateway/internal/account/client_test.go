package account

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
// UNIT TESTS - ACCOUNT CLIENT
// ==========================================

// TestGetCustomerAccounts - Account list testi
func TestGetCustomerAccounts(t *testing.T) {
	// Mock HTTP server
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/v1/accounts" {
			t.Errorf("Expected path /v1/accounts, got %s", r.URL.Path)
		}

		// Check query parameter
		customerID := r.URL.Query().Get("customerId")
		if customerID != "cust-123" {
			t.Errorf("Expected customerId cust-123, got %s", customerID)
		}

		response := []AccountResponse{
			{
				ID:            "acc-1",
				CustomerID:    "cust-123",
				AccountNumber: "TR330006100519786457841326",
				ProductCode:   "CHECKING_TRY",
				Status:        "ACTIVE",
				Configurations: map[string]interface{}{
					"dailyTransactionLimit": 50000.0,
					"emailNotifications":    true,
				},
				CreatedAt: time.Now(),
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
	opts := &AccountListOptions{
		CustomerID: "cust-123",
	}
	accounts, err := client.GetCustomerAccounts(ctx, opts)

	if err != nil {
		t.Fatalf("Expected no error, got %v", err)
	}

	if len(accounts) != 1 {
		t.Fatalf("Expected 1 account, got %d", len(accounts))
	}

	if accounts[0].ID != "acc-1" {
		t.Errorf("Expected ID acc-1, got %s", accounts[0].ID)
	}

	if accounts[0].Status != "ACTIVE" {
		t.Errorf("Expected status ACTIVE, got %s", accounts[0].Status)
	}
}

// TestGetCustomerAccounts_EmptyList - Boş liste testi
func TestGetCustomerAccounts_EmptyList(t *testing.T) {
	// Mock HTTP server - boş array döndür
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode([]AccountResponse{})
	}))
	defer server.Close()

	// Test client
	redisClient := redis.NewClient(&redis.Options{Addr: "localhost:6379", DB: 1})
	client := NewClient(server.URL, redisClient)

	// Test
	ctx := context.Background()
	opts := &AccountListOptions{
		CustomerID: "cust-123",
	}
	accounts, err := client.GetCustomerAccounts(ctx, opts)

	if err != nil {
		t.Fatalf("Expected no error, got %v", err)
	}

	if len(accounts) != 0 {
		t.Errorf("Expected empty list, got %d accounts", len(accounts))
	}
}

// TestCreateAccount - Hesap oluşturma testi
func TestCreateAccount(t *testing.T) {
	// Mock HTTP server
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			t.Errorf("Expected POST method, got %s", r.Method)
		}

		// Decode request
		var req CreateAccountRequest
		json.NewDecoder(r.Body).Decode(&req)

		if req.CustomerID != "cust-123" {
			t.Errorf("Expected customerId cust-123, got %s", req.CustomerID)
		}

		if req.ProductCode != "CHECKING_TRY" {
			t.Errorf("Expected productCode CHECKING_TRY, got %s", req.ProductCode)
		}

		// Response
		response := AccountResponse{
			ID:            "acc-new",
			CustomerID:    req.CustomerID,
			AccountNumber: "TR330006100519786457841328",
			ProductCode:   req.ProductCode,
			Status:        "ACTIVE",
			Configurations: map[string]interface{}{
				"dailyTransactionLimit": 50000.0,
			},
			CreatedAt: time.Now(),
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
	req := &CreateAccountRequest{
		CustomerID:  "cust-123",
		ProductCode: "CHECKING_TRY",
		Currency:    "TRY",
	}
	account, err := client.CreateAccount(ctx, req)

	if err != nil {
		t.Fatalf("Expected no error, got %v", err)
	}

	if account.ID != "acc-new" {
		t.Errorf("Expected ID acc-new, got %s", account.ID)
	}

	if account.Status != "ACTIVE" {
		t.Errorf("Expected status ACTIVE, got %s", account.Status)
	}
}

// TestGetAccount - Hesap detayı testi
func TestGetAccount(t *testing.T) {
	// Mock HTTP server
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/v1/accounts/acc-123" {
			t.Errorf("Expected path /v1/accounts/acc-123, got %s", r.URL.Path)
		}

		response := AccountResponse{
			ID:            "acc-123",
			CustomerID:    "cust-123",
			AccountNumber: "TR330006100519786457841326",
			ProductCode:   "CHECKING_TRY",
			Status:        "ACTIVE",
			CreatedAt:     time.Now(),
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
	account, err := client.GetAccount(ctx, "acc-123")

	if err != nil {
		t.Fatalf("Expected no error, got %v", err)
	}

	if account.ID != "acc-123" {
		t.Errorf("Expected ID acc-123, got %s", account.ID)
	}
}

// TestGetAccount_NotFound - 404 error testi
func TestGetAccount_NotFound(t *testing.T) {
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
	_, err := client.GetAccount(ctx, "non-existent")

	if err == nil {
		t.Fatal("Expected error, got nil")
	}

	accountErr, ok := err.(*AccountError)
	if !ok {
		t.Fatalf("Expected AccountError, got %T", err)
	}

	if !accountErr.IsNotFound() {
		t.Errorf("Expected IsNotFound to be true")
	}
}

// TestUpdateStatus - Status güncelleme testi
func TestUpdateStatus(t *testing.T) {
	// Mock HTTP server
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPatch {
			t.Errorf("Expected PATCH method, got %s", r.Method)
		}

		if r.URL.Path != "/v1/accounts/acc-123/status" {
			t.Errorf("Expected path /v1/accounts/acc-123/status, got %s", r.URL.Path)
		}

		// Decode request
		var req UpdateStatusRequest
		json.NewDecoder(r.Body).Decode(&req)

		if req.Status != "CLOSED" {
			t.Errorf("Expected status CLOSED, got %s", req.Status)
		}

		w.WriteHeader(http.StatusOK)
	}))
	defer server.Close()

	// Test client
	redisClient := redis.NewClient(&redis.Options{Addr: "localhost:6379", DB: 1})
	client := NewClient(server.URL, redisClient)

	// Test
	ctx := context.Background()
	req := &UpdateStatusRequest{
		Status: "CLOSED",
		Reason: "Test",
	}
	err := client.UpdateStatus(ctx, "acc-123", req)

	if err != nil {
		t.Fatalf("Expected no error, got %v", err)
	}
}

// TestGetProducts - ProductList testi
func TestGetProducts(t *testing.T) {
	// Mock HTTP server
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/v1/products" {
			t.Errorf("Expected path /v1/products, got %s", r.URL.Path)
		}

		response := []ProductDefinition{
			{
				Code:     "CHECKING_TRY",
				Name:     "Vadesiz Hesap",
				Category: "CHECKING",
				Active:   true,
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
	products, err := client.GetProducts(ctx)

	if err != nil {
		t.Fatalf("Expected no error, got %v", err)
	}

	if len(products) != 1 {
		t.Fatalf("Expected 1 product, got %d", len(products))
	}

	if products[0].Code != "CHECKING_TRY" {
		t.Errorf("Expected code CHECKING_TRY, got %s", products[0].Code)
	}
}

// TestAccountError - Error type testi
func TestAccountError(t *testing.T) {
	err := NewAccountError(404, "NOT_FOUND", "Account not found")

	if !err.IsNotFound() {
		t.Errorf("Expected IsNotFound to be true")
	}

	if err.IsServerError() {
		t.Errorf("Expected IsServerError to be false")
	}

	expectedMsg := "[Account 404] NOT_FOUND: Account not found"
	if err.Error() != expectedMsg {
		t.Errorf("Expected error message '%s', got '%s'", expectedMsg, err.Error())
	}
}
