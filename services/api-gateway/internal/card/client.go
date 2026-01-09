package card

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"strings"
	"time"

	"github.com/redis/go-redis/v9"
)

// ==========================================
// CARD SERVICE - CLIENT
// ==========================================
// Card servisine HTTP istekleri yapar
// - Redis caching (card listesi için)
// - Error handling
// - Support for both DEBIT and VIRTUAL cards

// Client - Card servis client'ı
type Client struct {
	baseURL    string
	httpClient *http.Client
	redis      *redis.Client
}

// NewClient - Yeni card client oluşturur
func NewClient(baseURL string, redisClient *redis.Client) *Client {
	return &Client{
		baseURL: baseURL,
		httpClient: &http.Client{
			Timeout: 10 * time.Second,
			Transport: &http.Transport{
				MaxIdleConns:        100,
				MaxIdleConnsPerHost: 10,
				IdleConnTimeout:     90 * time.Second,
			},
		},
		redis: redisClient,
	}
}

// ==========================================
// POST /v1/cards/debit - ISSUE DEBIT CARD
// ==========================================

// IssueDebitCard - Banka kartı çıkar
func (c *Client) IssueDebitCard(ctx context.Context, req *IssueDebitCardRequest) (*DebitCardResponse, error) {
	body, err := json.Marshal(req)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal request: %w", err)
	}

	httpReq, err := http.NewRequestWithContext(
		ctx,
		http.MethodPost,
		fmt.Sprintf("%s/v1/cards/debit", c.baseURL),
		bytes.NewReader(body),
	)
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	httpReq.Header.Set("Content-Type", "application/json")

	resp, err := c.httpClient.Do(httpReq)
	if err != nil {
		return nil, fmt.Errorf("request failed: %w", err)
	}
	defer resp.Body.Close()

	// Handle errors
	if resp.StatusCode != http.StatusOK && resp.StatusCode != http.StatusCreated {
		return nil, c.handleErrorResponse(resp)
	}

	var card DebitCardResponse
	if err := json.NewDecoder(resp.Body).Decode(&card); err != nil {
		return nil, fmt.Errorf("failed to decode response: %w", err)
	}

	return &card, nil
}

// ==========================================
// POST /v1/cards/virtual - ISSUE VIRTUAL CARD
// ==========================================

// IssueVirtualCard - Sanal kart çıkar
func (c *Client) IssueVirtualCard(ctx context.Context, req *IssueVirtualCardRequest) (*VirtualCardResponse, error) {
	body, err := json.Marshal(req)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal request: %w", err)
	}

	httpReq, err := http.NewRequestWithContext(
		ctx,
		http.MethodPost,
		fmt.Sprintf("%s/v1/cards/virtual", c.baseURL),
		bytes.NewReader(body),
	)
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	httpReq.Header.Set("Content-Type", "application/json")

	resp, err := c.httpClient.Do(httpReq)
	if err != nil {
		return nil, fmt.Errorf("request failed: %w", err)
	}
	defer resp.Body.Close()

	// Handle errors
	if resp.StatusCode != http.StatusOK && resp.StatusCode != http.StatusCreated {
		return nil, c.handleErrorResponse(resp)
	}

	var card VirtualCardResponse
	if err := json.NewDecoder(resp.Body).Decode(&card); err != nil {
		return nil, fmt.Errorf("failed to decode response: %w", err)
	}

	return &card, nil
}

// ==========================================
// GET /v1/cards - GET CARD LIST
// ==========================================

// GetCardsWithCache - Kart listesini getir (cache ile)
func (c *Client) GetCardsWithCache(ctx context.Context, opts *CardListOptions) ([]CardResponse, error) {
	// Cache key oluştur
	cacheKey := c.buildCardListCacheKey(opts)

	// Try cache first
	cached, err := c.redis.Get(ctx, cacheKey).Result()
	if err == nil {
		var cards []CardResponse
		if err := json.Unmarshal([]byte(cached), &cards); err == nil {
			return cards, nil
		}
	}

	// Cache miss - fetch from service
	cards, err := c.GetCards(ctx, opts)
	if err != nil {
		return nil, err
	}

	// Cache for 30 seconds
	data, _ := json.Marshal(cards)
	c.redis.Set(ctx, cacheKey, data, 30*time.Second)

	return cards, nil
}

// GetCards - Kart listesini getir (cache kullanma)
func (c *Client) GetCards(ctx context.Context, opts *CardListOptions) ([]CardResponse, error) {
	urlStr := fmt.Sprintf("%s/v1/cards", c.baseURL)

	req, err := http.NewRequestWithContext(ctx, http.MethodGet, urlStr, nil)
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	req.Header.Set("Content-Type", "application/json")

	// Query parameters
	q := url.Values{}
	if opts.CustomerID != "" {
		q.Set("customerId", opts.CustomerID)
	}
	if opts.AccountID != "" {
		q.Set("accountId", opts.AccountID)
	}
	if len(opts.CardTypes) > 0 {
		q.Set("cardType", strings.Join(opts.CardTypes, ","))
	}

	req.URL.RawQuery = q.Encode()

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return nil, fmt.Errorf("request failed: %w", err)
	}
	defer resp.Body.Close()

	// Handle errors
	if resp.StatusCode != http.StatusOK {
		return nil, c.handleErrorResponse(resp)
	}

	var cards []CardResponse
	if err := json.NewDecoder(resp.Body).Decode(&cards); err != nil {
		return nil, fmt.Errorf("failed to decode response: %w", err)
	}

	return cards, nil
}

// buildCardListCacheKey - Cache key oluştur
func (c *Client) buildCardListCacheKey(opts *CardListOptions) string {
	parts := []string{"card:list"}

	if opts.CustomerID != "" {
		parts = append(parts, fmt.Sprintf("cust:%s", opts.CustomerID))
	}
	if opts.AccountID != "" {
		parts = append(parts, fmt.Sprintf("acc:%s", opts.AccountID))
	}
	if len(opts.CardTypes) > 0 {
		parts = append(parts, fmt.Sprintf("types:%s", strings.Join(opts.CardTypes, ",")))
	}

	return strings.Join(parts, ":")
}

// ==========================================
// GET /v1/cards/{id} - GET CARD DETAILS
// ==========================================

// GetCard - Kart detaylarını getir
func (c *Client) GetCard(ctx context.Context, cardID string) (*CardResponse, error) {
	urlStr := fmt.Sprintf("%s/v1/cards/%s", c.baseURL, cardID)

	req, err := http.NewRequestWithContext(ctx, http.MethodGet, urlStr, nil)
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	req.Header.Set("Content-Type", "application/json")

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return nil, fmt.Errorf("request failed: %w", err)
	}
	defer resp.Body.Close()

	// Handle errors
	if resp.StatusCode != http.StatusOK {
		return nil, c.handleErrorResponse(resp)
	}

	var card CardResponse
	if err := json.NewDecoder(resp.Body).Decode(&card); err != nil {
		return nil, fmt.Errorf("failed to decode response: %w", err)
	}

	return &card, nil
}

// ==========================================
// PATCH /v1/cards/{id}/block - BLOCK CARD
// ==========================================

// BlockCard - Kartı bloke et
func (c *Client) BlockCard(ctx context.Context, cardID string, reason string) error {
	urlStr := fmt.Sprintf("%s/v1/cards/%s/block?reason=%s", c.baseURL, cardID, url.QueryEscape(reason))

	req, err := http.NewRequestWithContext(ctx, http.MethodPatch, urlStr, nil)
	if err != nil {
		return fmt.Errorf("failed to create request: %w", err)
	}

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return fmt.Errorf("request failed: %w", err)
	}
	defer resp.Body.Close()

	// Handle errors
	if resp.StatusCode != http.StatusOK && resp.StatusCode != http.StatusNoContent {
		return c.handleErrorResponse(resp)
	}

	return nil
}

// ==========================================
// PATCH /v1/cards/{id}/activate - ACTIVATE CARD
// ==========================================

// ActivateCard - Kartı aktif et
func (c *Client) ActivateCard(ctx context.Context, cardID string) error {
	urlStr := fmt.Sprintf("%s/v1/cards/%s/activate", c.baseURL, cardID)

	req, err := http.NewRequestWithContext(ctx, http.MethodPatch, urlStr, nil)
	if err != nil {
		return fmt.Errorf("failed to create request: %w", err)
	}

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return fmt.Errorf("request failed: %w", err)
	}
	defer resp.Body.Close()

	// Handle errors
	if resp.StatusCode != http.StatusOK && resp.StatusCode != http.StatusNoContent {
		return c.handleErrorResponse(resp)
	}

	return nil
}

// ==========================================
// POST /v1/cards/credit - ISSUE CREDIT CARD
// ==========================================

// IssueCreditCard - Kredi kartı çıkar
func (c *Client) IssueCreditCard(ctx context.Context, req *IssueCreditCardRequest) (*CreditCardResponse, error) {
	body, err := json.Marshal(req)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal request: %w", err)
	}

	httpReq, err := http.NewRequestWithContext(
		ctx,
		http.MethodPost,
		fmt.Sprintf("%s/v1/cards/credit", c.baseURL),
		bytes.NewReader(body),
	)
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	httpReq.Header.Set("Content-Type", "application/json")

	resp, err := c.httpClient.Do(httpReq)
	if err != nil {
		return nil, fmt.Errorf("request failed: %w", err)
	}
	defer resp.Body.Close()

	// Handle errors
	if resp.StatusCode != http.StatusOK && resp.StatusCode != http.StatusCreated {
		return nil, c.handleErrorResponse(resp)
	}

	var card CreditCardResponse
	if err := json.NewDecoder(resp.Body).Decode(&card); err != nil {
		return nil, fmt.Errorf("failed to decode response: %w", err)
	}

	return &card, nil
}

// ==========================================
// POST /v1/cards/{id}/authorize - AUTHORIZE TRANSACTION
// ==========================================

// AuthorizeTransaction - İşlem için kart yetkilendirmesi
func (c *Client) AuthorizeTransaction(ctx context.Context, cardID string, req *AuthorizationRequest) (*AuthorizationResponse, error) {
	body, err := json.Marshal(req)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal request: %w", err)
	}

	httpReq, err := http.NewRequestWithContext(
		ctx,
		http.MethodPost,
		fmt.Sprintf("%s/v1/cards/%s/authorize", c.baseURL, cardID),
		bytes.NewReader(body),
	)
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	httpReq.Header.Set("Content-Type", "application/json")

	resp, err := c.httpClient.Do(httpReq)
	if err != nil {
		return nil, fmt.Errorf("request failed: %w", err)
	}
	defer resp.Body.Close()

	// Handle errors
	if resp.StatusCode != http.StatusOK && resp.StatusCode != http.StatusForbidden {
		return nil, c.handleErrorResponse(resp)
	}

	var authResp AuthorizationResponse
	if err := json.NewDecoder(resp.Body).Decode(&authResp); err != nil {
		return nil, fmt.Errorf("failed to decode response: %w", err)
	}

	return &authResp, nil
}

// ==========================================
// POST /v1/cards/{id}/validate-pin - VALIDATE PIN
// ==========================================

// ValidatePIN - Kart PIN'ini doğrula
func (c *Client) ValidatePIN(ctx context.Context, cardID string, pin string) (bool, error) {
	urlStr := fmt.Sprintf("%s/v1/cards/%s/validate-pin?pin=%s", c.baseURL, cardID, url.QueryEscape(pin))

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, urlStr, nil)
	if err != nil {
		return false, fmt.Errorf("failed to create request: %w", err)
	}

	req.Header.Set("Content-Type", "application/json")

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return false, fmt.Errorf("request failed: %w", err)
	}
	defer resp.Body.Close()

	// Handle errors
	if resp.StatusCode != http.StatusOK {
		return false, c.handleErrorResponse(resp)
	}

	var isValid bool
	if err := json.NewDecoder(resp.Body).Decode(&isValid); err != nil {
		return false, fmt.Errorf("failed to decode response: %w", err)
	}

	return isValid, nil
}

// ==========================================
// ERROR HANDLING
// ==========================================

// handleErrorResponse - HTTP error response'ları CardError'a çevirir
func (c *Client) handleErrorResponse(resp *http.Response) error {
	switch resp.StatusCode {
	case http.StatusNotFound:
		return NewCardError(404, "NOT_FOUND", "Card or resource not found")
	case http.StatusBadRequest:
		return NewCardError(400, "BAD_REQUEST", "Invalid request parameters")
	case http.StatusInternalServerError:
		return NewCardError(500, "INTERNAL_ERROR", "Card service error")
	case http.StatusServiceUnavailable:
		return NewCardError(503, "SERVICE_UNAVAILABLE", "Card service temporarily unavailable")
	default:
		return NewCardError(resp.StatusCode, "UNKNOWN_ERROR", fmt.Sprintf("Unexpected status: %d", resp.StatusCode))
	}
}
