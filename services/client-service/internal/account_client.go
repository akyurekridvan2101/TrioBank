package internal

import (
	"bytes"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
)

// AccountClient - Account Service ile iletişim için HTTP client
type AccountClient struct {
	BaseURL    string
	HTTPClient *http.Client
}

// CreateAccountRequest - Yeni hesap oluşturma isteği
type CreateAccountRequest struct {
	CustomerID  string `json:"customerId"`
	ProductCode string `json:"productCode"`
	Currency    string `json:"currency"`
}

// AccountResponse - Account Service'den dönen hesap bilgisi
type AccountResponse struct {
	ID            string `json:"id"`
	CustomerID    string `json:"customerId"`
	AccountNumber string `json:"accountNumber"`
	ProductCode   string `json:"productCode"`
	Status        string `json:"status"`
}

// NewAccountClient - Yeni AccountClient oluşturur
func NewAccountClient(baseURL string, client *http.Client) *AccountClient {
	return &AccountClient{
		BaseURL:    baseURL,
		HTTPClient: client,
	}
}

// CreateAccount - Account Service'de yeni hesap oluşturur
func (ac *AccountClient) CreateAccount(req CreateAccountRequest) (*AccountResponse, error) {
	body, err := json.Marshal(req)
	if err != nil {
		return nil, fmt.Errorf("failed to marshal request: %w", err)
	}

	log.Printf("Creating account in Account Service: customerID=%s, productCode=%s, currency=%s",
		req.CustomerID, req.ProductCode, req.Currency)

	resp, err := ac.HTTPClient.Post(
		ac.BaseURL+"/v1/accounts",
		"application/json",
		bytes.NewBuffer(body),
	)
	if err != nil {
		return nil, fmt.Errorf("failed to call account service: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK && resp.StatusCode != http.StatusCreated {
		return nil, fmt.Errorf("account service returned status %d", resp.StatusCode)
	}

	var account AccountResponse
	if err := json.NewDecoder(resp.Body).Decode(&account); err != nil {
		return nil, fmt.Errorf("failed to decode response: %w", err)
	}

	log.Printf("Account created successfully: id=%s, accountNumber=%s", account.ID, account.AccountNumber)
	return &account, nil
}

// GetAccountsByCustomerID - Müşterinin hesaplarını getirir
func (ac *AccountClient) GetAccountsByCustomerID(customerID string) ([]AccountResponse, error) {
	url := fmt.Sprintf("%s/v1/accounts?customerId=%s", ac.BaseURL, customerID)
	
	resp, err := ac.HTTPClient.Get(url)
	if err != nil {
		return nil, fmt.Errorf("failed to get accounts: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("account service returned status %d", resp.StatusCode)
	}

	var accounts []AccountResponse
	if err := json.NewDecoder(resp.Body).Decode(&accounts); err != nil {
		return nil, fmt.Errorf("failed to decode accounts: %w", err)
	}

	return accounts, nil
}

// UpdateAccountStatusRequest - Hesap durumu güncelleme isteği
type UpdateAccountStatusRequest struct {
	Status string `json:"status"`
	Reason string `json:"reason"`
}

// CloseAccount - Hesabı kapatır (CLOSED durumuna alır)
func (ac *AccountClient) CloseAccount(accountID, reason string) error {
	reqBody := UpdateAccountStatusRequest{
		Status: "CLOSED",
		Reason: reason,
	}

	body, err := json.Marshal(reqBody)
	if err != nil {
		return fmt.Errorf("failed to marshal request: %w", err)
	}

	url := fmt.Sprintf("%s/v1/accounts/%s/status", ac.BaseURL, accountID)
	req, err := http.NewRequest(http.MethodPatch, url, bytes.NewBuffer(body))
	if err != nil {
		return fmt.Errorf("failed to create request: %w", err)
	}
	req.Header.Set("Content-Type", "application/json")

	resp, err := ac.HTTPClient.Do(req)
	if err != nil {
		return fmt.Errorf("failed to close account: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK && resp.StatusCode != http.StatusNoContent {
		return fmt.Errorf("account service returned status %d", resp.StatusCode)
	}

	log.Printf("Account %s closed successfully", accountID)
	return nil
}
