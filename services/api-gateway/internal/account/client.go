package account

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
// ACCOUNT SERVICE - CLIENT
// ==========================================
// Account servisine HTTP istekleri yapar
// - Redis caching (account listesi için)
// - Retry mekanizması
// - Error handling

// Client - Account servis client'ı
type Client struct {
	baseURL    string
	httpClient *http.Client
	redis      *redis.Client
}

// NewClient - Yeni account client oluşturur
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
// GET /v1/accounts - ACCOUNT LIST
// ==========================================

// GetCustomerAccountsWithCache - Müşteri hesaplarını listele (cache ile)
func (c *Client) GetCustomerAccountsWithCache(ctx context.Context, opts *AccountListOptions) ([]AccountResponse, error) {
	// Cache key oluştur
	statuses := "all"
	if len(opts.Statuses) > 0 {
		statuses = strings.Join(opts.Statuses, ",")
	}
	cacheKey := fmt.Sprintf("account:list:%s:%s", opts.CustomerID, statuses)

	// Try cache first
	cached, err := c.redis.Get(ctx, cacheKey).Result()
	if err == nil {
		var accounts []AccountResponse
		if err := json.Unmarshal([]byte(cached), &accounts); err == nil {
			return accounts, nil
		}
	}

	// Cache miss - fetch from service
	accounts, err := c.GetCustomerAccounts(ctx, opts)
	if err != nil {
		return nil, err
	}

	// Cache for 30 seconds
	data, _ := json.Marshal(accounts)
	c.redis.Set(ctx, cacheKey, data, 30*time.Second)

	return accounts, nil
}

// GetCustomerAccounts - Müşteri hesaplarını listele (cache kullanma)
func (c *Client) GetCustomerAccounts(ctx context.Context, opts *AccountListOptions) ([]AccountResponse, error) {
	urlStr := fmt.Sprintf("%s/v1/accounts", c.baseURL)

	req, err := http.NewRequestWithContext(ctx, http.MethodGet, urlStr, nil)
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	req.Header.Set("Content-Type", "application/json")

	// Query parameters
	q := url.Values{}
	q.Set("customerId", opts.CustomerID)

	if len(opts.Statuses) > 0 {
		q.Set("status", strings.Join(opts.Statuses, ","))
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

	var accounts []AccountResponse
	if err := json.NewDecoder(resp.Body).Decode(&accounts); err != nil {
		return nil, fmt.Errorf("failed to decode response: %w", err)
	}

	return accounts, nil
}

// ==========================================
// POST /v1/accounts - CREATE ACCOUNT
// ==========================================

// CreateAccount - Yeni hesap aç
func (c *Client) CreateAccount(ctx context.Context, req *CreateAccountRequest) (*AccountResponse, error) {
	body, err := json.Marshal(req)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal request: %w", err)
	}

	httpReq, err := http.NewRequestWithContext(
		ctx,
		http.MethodPost,
		fmt.Sprintf("%s/v1/accounts", c.baseURL),
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
	// Account Service returns 201 Created for POST requests
	if resp.StatusCode != http.StatusOK && resp.StatusCode != http.StatusCreated {
		return nil, c.handleErrorResponse(resp)
	}

	var account AccountResponse
	if err := json.NewDecoder(resp.Body).Decode(&account); err != nil {
		return nil, fmt.Errorf("failed to decode response: %w", err)
	}

	return &account, nil
}

// ==========================================
// GET /v1/accounts/{id} - GET ACCOUNT
// ==========================================

// GetAccount - Hesap detaylarını getir
func (c *Client) GetAccount(ctx context.Context, accountID string) (*AccountResponse, error) {
	urlStr := fmt.Sprintf("%s/v1/accounts/%s", c.baseURL, accountID)

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

	var account AccountResponse
	if err := json.NewDecoder(resp.Body).Decode(&account); err != nil {
		return nil, fmt.Errorf("failed to decode response: %w", err)
	}

	return &account, nil
}

// ==========================================
// PATCH /v1/accounts/{id}/status - UPDATE STATUS
// ==========================================

// UpdateStatus - Hesap durumunu değiştir
func (c *Client) UpdateStatus(ctx context.Context, accountID string, req *UpdateStatusRequest) error {
	body, err := json.Marshal(req)
	if err != nil {
		return fmt.Errorf("failed to marshal request: %w", err)
	}

	httpReq, err := http.NewRequestWithContext(
		ctx,
		http.MethodPatch,
		fmt.Sprintf("%s/v1/accounts/%s/status", c.baseURL, accountID),
		bytes.NewReader(body),
	)
	if err != nil {
		return fmt.Errorf("failed to create request: %w", err)
	}

	httpReq.Header.Set("Content-Type", "application/json")

	resp, err := c.httpClient.Do(httpReq)
	if err != nil {
		return fmt.Errorf("request failed: %w", err)
	}
	defer resp.Body.Close()

	// Handle errors
	if resp.StatusCode != http.StatusOK {
		return c.handleErrorResponse(resp)
	}

	return nil
}

// ==========================================
// PATCH /v1/accounts/{id}/configuration - UPDATE CONFIGURATION
// ==========================================

// UpdateConfiguration - Hesap konfigürasyonunu güncelle
func (c *Client) UpdateConfiguration(ctx context.Context, accountID string, req *UpdateConfigurationRequest) error {
	body, err := json.Marshal(req)
	if err != nil {
		return fmt.Errorf("failed to marshal request: %w", err)
	}

	httpReq, err := http.NewRequestWithContext(
		ctx,
		http.MethodPatch,
		fmt.Sprintf("%s/v1/accounts/%s/configuration", c.baseURL, accountID),
		bytes.NewReader(body),
	)
	if err != nil {
		return fmt.Errorf("failed to create request: %w", err)
	}

	httpReq.Header.Set("Content-Type", "application/json")

	resp, err := c.httpClient.Do(httpReq)
	if err != nil {
		return fmt.Errorf("request failed: %w", err)
	}
	defer resp.Body.Close()

	// Handle errors
	if resp.StatusCode != http.StatusOK {
		return c.handleErrorResponse(resp)
	}

	return nil
}

// ==========================================
// GET /v1/products - GET PRODUCTS
// ==========================================

// GetProducts - Tüm product'ları listele
func (c *Client) GetProducts(ctx context.Context) ([]ProductDefinition, error) {
	urlStr := fmt.Sprintf("%s/v1/products", c.baseURL)

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

	var products []ProductDefinition
	if err := json.NewDecoder(resp.Body).Decode(&products); err != nil {
		return nil, fmt.Errorf("failed to decode response: %w", err)
	}

	return products, nil
}

// GetProduct - Belirli bir product'ı getir
func (c *Client) GetProduct(ctx context.Context, code string) (*ProductDefinition, error) {
	urlStr := fmt.Sprintf("%s/v1/products/%s", c.baseURL, code)

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

	var product ProductDefinition
	if err := json.NewDecoder(resp.Body).Decode(&product); err != nil {
		return nil, fmt.Errorf("failed to decode response: %w", err)
	}

	return &product, nil
}

// ==========================================
// ERROR HANDLING
// ==========================================

// handleErrorResponse - HTTP error response'ları AccountError'a çevirir
func (c *Client) handleErrorResponse(resp *http.Response) error {
	switch resp.StatusCode {
	case http.StatusNotFound:
		return NewAccountError(404, "NOT_FOUND", "Account or resource not found")
	case http.StatusBadRequest:
		return NewAccountError(400, "BAD_REQUEST", "Invalid request parameters")
	case http.StatusInternalServerError:
		return NewAccountError(500, "INTERNAL_ERROR", "Account service error")
	case http.StatusServiceUnavailable:
		return NewAccountError(503, "SERVICE_UNAVAILABLE", "Account service temporarily unavailable")
	default:
		return NewAccountError(resp.StatusCode, "UNKNOWN_ERROR", fmt.Sprintf("Unexpected status: %d", resp.StatusCode))
	}
}
