package card

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
// UNIT TESTS - CARD CLIENT
// ==========================================

// TestIssueDebitCard - Banka kartı çıkarma testi
func TestIssueDebitCard(t *testing.T) {
	// Mock HTTP server
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			t.Errorf("Expected POST method, got %s", r.Method)
		}

		if r.URL.Path != "/v1/cards/debit" {
			t.Errorf("Expected path /v1/cards/debit, got %s", r.URL.Path)
		}

		// Decode request
		var req IssueDebitCardRequest
		json.NewDecoder(r.Body).Decode(&req)

		if req.AccountID != "acc-123" {
			t.Errorf("Expected accountId acc-123, got %s", req.AccountID)
		}

		// Response
		response := DebitCardResponse{
			ID:             "card-new",
			CardType:       "DEBIT",
			Number:         "**** **** **** 1234",
			CardholderName: req.CardholderName,
			ExpiryMonth:    12,
			ExpiryYear:     2028,
			CardBrand:      "VISA",
			Status:         "ACTIVE",
			AccountID:      req.AccountID,
			CreatedAt:      time.Now(),
			AtmEnabled:     true,
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
	req := &IssueDebitCardRequest{
		AccountID:      "acc-123",
		CardholderName: "AHMET YILMAZ",
	}
	card, err := client.IssueDebitCard(ctx, req)

	if err != nil {
		t.Fatalf("Expected no error, got %v", err)
	}

	if card.ID != "card-new" {
		t.Errorf("Expected ID card-new, got %s", card.ID)
	}

	if card.CardType != "DEBIT" {
		t.Errorf("Expected cardType DEBIT, got %s", card.CardType)
	}

	if card.Status != "ACTIVE" {
		t.Errorf("Expected status ACTIVE, got %s", card.Status)
	}
}

// TestIssueVirtualCard - Sanal kart çıkarma testi
func TestIssueVirtualCard(t *testing.T) {
	// Mock HTTP server
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/v1/cards/virtual" {
			t.Errorf("Expected path /v1/cards/virtual, got %s", r.URL.Path)
		}

		// Response
		response := VirtualCardResponse{
			ID:             "card-virtual",
			CardType:       "VIRTUAL",
			Number:         "5123 4567 8901 2345",
			CVV:            "123",
			CardholderName: "AHMET YILMAZ",
			ExpiryMonth:    12,
			ExpiryYear:     2025,
			CardBrand:      "MASTERCARD",
			Status:         "ACTIVE",
			AccountID:      "acc-123",
			CreatedAt:      time.Now(),
			OnlineOnly:     true,
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
	req := &IssueVirtualCardRequest{
		AccountID:      "acc-123",
		CardholderName: "AHMET YILMAZ",
	}
	card, err := client.IssueVirtualCard(ctx, req)

	if err != nil {
		t.Fatalf("Expected no error, got %v", err)
	}

	if card.CardType != "VIRTUAL" {
		t.Errorf("Expected cardType VIRTUAL, got %s", card.CardType)
	}

	if card.CVV != "123" {
		t.Errorf("Expected CVV 123, got %s", card.CVV)
	}
}

// TestGetCards - Kart listesi testi
func TestGetCards(t *testing.T) {
	// Mock HTTP server
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/v1/cards" {
			t.Errorf("Expected path /v1/cards, got %s", r.URL.Path)
		}

		// Check query parameter
		customerID := r.URL.Query().Get("customerId")
		if customerID != "cust-123" {
			t.Errorf("Expected customerId cust-123, got %s", customerID)
		}

		response := []CardResponse{
			{
				ID:             "card-1",
				CardType:       "DEBIT",
				Number:         "**** **** **** 1234",
				CardholderName: "AHMET YILMAZ",
				Status:         "ACTIVE",
				AccountID:      "acc-123",
				CreatedAt:      time.Now(),
			},
			{
				ID:             "card-2",
				CardType:       "VIRTUAL",
				Number:         "5123 4567 8901 2345",
				CardholderName: "AHMET YILMAZ",
				Status:         "ACTIVE",
				AccountID:      "acc-123",
				CreatedAt:      time.Now(),
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
	opts := &CardListOptions{
		CustomerID: "cust-123",
	}
	cards, err := client.GetCards(ctx, opts)

	if err != nil {
		t.Fatalf("Expected no error, got %v", err)
	}

	if len(cards) != 2 {
		t.Fatalf("Expected 2 cards, got %d", len(cards))
	}

	if cards[0].CardType != "DEBIT" {
		t.Errorf("Expected first card to be DEBIT, got %s", cards[0].CardType)
	}

	if cards[1].CardType != "VIRTUAL" {
		t.Errorf("Expected second card to be VIRTUAL, got %s", cards[1].CardType)
	}
}

// TestGetCard_NotFound - 404 error testi
func TestGetCard_NotFound(t *testing.T) {
	// Mock HTTP server - 404 döndür
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusNotFound)
		json.NewEncoder(w).Encode(map[string]string{
			"error": "Card not found",
		})
	}))
	defer server.Close()

	// Test client
	redisClient := redis.NewClient(&redis.Options{Addr: "localhost:6379", DB: 1})
	client := NewClient(server.URL, redisClient)

	// Test
	ctx := context.Background()
	_, err := client.GetCard(ctx, "non-existent")

	if err == nil {
		t.Fatal("Expected error, got nil")
	}

	cardErr, ok := err.(*CardError)
	if !ok {
		t.Fatalf("Expected CardError, got %T", err)
	}

	if !cardErr.IsNotFound() {
		t.Errorf("Expected IsNotFound to be true")
	}
}

// TestBlockCard - Kart bloke etme testi
func TestBlockCard(t *testing.T) {
	// Mock HTTP server
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPatch {
			t.Errorf("Expected PATCH method, got %s", r.Method)
		}

		if r.URL.Query().Get("reason") != "LOST" {
			t.Errorf("Expected reason LOST, got %s", r.URL.Query().Get("reason"))
		}

		w.WriteHeader(http.StatusOK)
	}))
	defer server.Close()

	// Test client
	redisClient := redis.NewClient(&redis.Options{Addr: "localhost:6379", DB: 1})
	client := NewClient(server.URL, redisClient)

	// Test
	ctx := context.Background()
	err := client.BlockCard(ctx, "card-123", "LOST")

	if err != nil {
		t.Fatalf("Expected no error, got %v", err)
	}
}

// TestActivateCard - Kart aktif etme testi
func TestActivateCard(t *testing.T) {
	// Mock HTTP server
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPatch {
			t.Errorf("Expected PATCH method, got %s", r.Method)
		}

		w.WriteHeader(http.StatusOK)
	}))
	defer server.Close()

	// Test client
	redisClient := redis.NewClient(&redis.Options{Addr: "localhost:6379", DB: 1})
	client := NewClient(server.URL, redisClient)

	// Test
	ctx := context.Background()
	err := client.ActivateCard(ctx, "card-123")

	if err != nil {
		t.Fatalf("Expected no error, got %v", err)
	}
}

// TestCardError - Error type testi
func TestCardError(t *testing.T) {
	err := NewCardError(404, "NOT_FOUND", "Card not found")

	if !err.IsNotFound() {
		t.Errorf("Expected IsNotFound to be true")
	}

	if err.IsServerError() {
		t.Errorf("Expected IsServerError to be false")
	}

	expectedMsg := "[Card 404] NOT_FOUND: Card not found"
	if err.Error() != expectedMsg {
		t.Errorf("Expected error message '%s', got '%s'", expectedMsg, err.Error())
		}
	}
}
