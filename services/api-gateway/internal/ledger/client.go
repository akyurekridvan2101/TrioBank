package ledger

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"strconv"
	"time"

	"github.com/redis/go-redis/v9"
)

// ==========================================
// LEDGER SERVICE - CLIENT
// ==========================================
// Ledger servisine HTTP istekleri yapar
// - Redis caching
// - Retry mekanizması
// - Error handling

// Client - Ledger servis client'ı
type Client struct {
	baseURL    string
	httpClient *http.Client
	redis      *redis.Client
}

// NewClient - Yeni ledger client oluşturur
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
// BALANCE ENDPOINT
// ==========================================

// GetBalanceWithCache - Bakiye sorgula (cache ile)
// Cache miss olursa retry mekanizması ile servisden çeker
func (c *Client) GetBalanceWithCache(ctx context.Context, accountID string) (*BalanceResponse, error) {
	cacheKey := fmt.Sprintf("ledger:balance:%s", accountID)

	// Try cache first
	cached, err := c.redis.Get(ctx, cacheKey).Result()
	if err == nil {
		var balance BalanceResponse
		if err := json.Unmarshal([]byte(cached), &balance); err == nil {
			return &balance, nil
		}
	}

	// Cache miss - fetch from service with retry
	balance, err := c.GetBalanceWithRetry(ctx, accountID)
	if err != nil {
		return nil, err
	}

	// Cache for 30 seconds
	data, _ := json.Marshal(balance)
	c.redis.Set(ctx, cacheKey, data, 30*time.Second)

	return balance, nil
}

// GetBalanceWithRetry - Retry mekanizması ile bakiye sorgula
func (c *Client) GetBalanceWithRetry(ctx context.Context, accountID string) (*BalanceResponse, error) {
	var balance *BalanceResponse
	var err error

	backoff := time.Second
	maxRetries := 3

	for attempt := 0; attempt < maxRetries; attempt++ {
		balance, err = c.GetBalance(ctx, accountID)
		if err == nil {
			return balance, nil
		}

		// Don't retry on 404 (account not found) or 400 (bad request)
		if ledgerErr, ok := err.(*LedgerError); ok {
			if ledgerErr.IsAccountNotFound() || ledgerErr.IsBadRequest() {
				return nil, err
			}
		}

		// Retry with exponential backoff
		if attempt < maxRetries-1 {
			time.Sleep(backoff)
			backoff *= 2 // 1s, 2s, 4s
		}
	}

	return nil, fmt.Errorf("max retries exceeded: %w", err)
}

// GetBalance - Bakiye sorgula (cache kullanma, retry yapma)
func (c *Client) GetBalance(ctx context.Context, accountID string) (*BalanceResponse, error) {
	url := fmt.Sprintf("%s/api/v1/ledger/balances/%s", c.baseURL, accountID)

	req, err := http.NewRequestWithContext(ctx, http.MethodGet, url, nil)
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

	var balance BalanceResponse
	if err := json.NewDecoder(resp.Body).Decode(&balance); err != nil {
		return nil, fmt.Errorf("failed to decode response: %w", err)
	}

	return &balance, nil
}

// ==========================================
// STATEMENT ENDPOINT
// ==========================================

// GetStatement - Hesap ekstresi getir
// NOT: Statement'lar cache edilmez (sürekli değişiyor)
func (c *Client) GetStatement(ctx context.Context, accountID string, opts *StatementOptions) (*StatementResponse, error) {
	if opts == nil {
		opts = NewStatementOptions()
	}

	urlStr := fmt.Sprintf("%s/api/v1/ledger/accounts/%s/statement", c.baseURL, accountID)

	req, err := http.NewRequestWithContext(ctx, http.MethodGet, urlStr, nil)
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	req.Header.Set("Content-Type", "application/json")

	// Build query parameters
	q := url.Values{}

	if opts.StartDate != nil {
		q.Set("startDate", opts.StartDate.Format("2006-01-02")) // YYYY-MM-DD
	}
	if opts.EndDate != nil {
		q.Set("endDate", opts.EndDate.Format("2006-01-02"))
	}
	if opts.Type != "" {
		q.Set("type", opts.Type)
	}
	if opts.Keyword != "" {
		q.Set("keyword", opts.Keyword)
	}
	q.Set("page", strconv.Itoa(opts.Page))
	q.Set("size", strconv.Itoa(opts.Size))
	q.Set("includeRunningBalance", strconv.FormatBool(opts.IncludeRunningBalance))

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

	var statement StatementResponse
	if err := json.NewDecoder(resp.Body).Decode(&statement); err != nil {
		return nil, fmt.Errorf("failed to decode response: %w", err)
	}

	return &statement, nil
}

// ==========================================
// ERROR HANDLING
// ==========================================

// handleErrorResponse - HTTP error response'ları LedgerError'a çevirir
func (c *Client) handleErrorResponse(resp *http.Response) error {
	switch resp.StatusCode {
	case http.StatusNotFound:
		return NewLedgerError(404, "ACCOUNT_NOT_FOUND", "Account not found in ledger")
	case http.StatusBadRequest:
		return NewLedgerError(400, "BAD_REQUEST", "Invalid request parameters")
	case http.StatusInternalServerError:
		return NewLedgerError(500, "INTERNAL_ERROR", "Ledger service error")
	case http.StatusServiceUnavailable:
		return NewLedgerError(503, "SERVICE_UNAVAILABLE", "Ledger service temporarily unavailable")
	default:
		return NewLedgerError(resp.StatusCode, "UNKNOWN_ERROR", fmt.Sprintf("Unexpected status: %d", resp.StatusCode))
	}
}
