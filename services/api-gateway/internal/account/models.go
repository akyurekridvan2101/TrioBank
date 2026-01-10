package account

import "time"

// ==========================================
// ACCOUNT SERVICE - RESPONSE MODELS
// ==========================================
// Account servisten dönen response'ların Go struct karşılıkları
// OpenAPI spesifikasyonuna göre tanımlanmıştır

// AccountResponse - Hesap bilgileri
// GET /v1/accounts ve diğer endpoint'lerin response'u
type AccountResponse struct {
	ID             string                 `json:"id"`
	CustomerID     string                 `json:"customerId"`
	AccountNumber  string                 `json:"accountNumber"` // IBAN
	ProductCode    string                 `json:"productCode"`
	Status         string                 `json:"status"` // ACTIVE | CLOSED
	Configurations map[string]interface{} `json:"configurations"`
	CreatedAt      time.Time              `json:"createdAt"`
	UpdatedAt      *time.Time             `json:"updatedAt,omitempty"`
}

// CreateAccountRequest - Yeni hesap açma request'i
// POST /v1/accounts endpoint request body
type CreateAccountRequest struct {
	CustomerID     string                 `json:"customerId"`
	ProductCode    string                 `json:"productCode"`
	Currency       string                 `json:"currency"`
	Configurations map[string]interface{} `json:"configurations,omitempty"`
}

// UpdateStatusRequest - Hesap durumunu değiştirme request'i
// PATCH /v1/accounts/{id}/status endpoint request body
type UpdateStatusRequest struct {
	Status string `json:"status"` // ACTIVE | CLOSED
	Reason string `json:"reason,omitempty"`
}

// UpdateConfigurationRequest - Hesap konfigürasyonunu güncelleme
// PATCH /v1/accounts/{id}/configuration endpoint request body
type UpdateConfigurationRequest struct {
	Configurations map[string]interface{} `json:"configurations"`
}

// ProductDefinition - Product bilgisi
// GET /v1/products endpoint response'u
type ProductDefinition struct {
	Code                 string                 `json:"code"`
	Name                 string                 `json:"name"`
	Category             string                 `json:"category"` // CHECKING | SAVINGS | CREDIT | LOAN
	Features             map[string]interface{} `json:"features"`
	DefaultConfiguration map[string]interface{} `json:"defaultConfiguration"`
	CreatedAt            time.Time              `json:"createdAt"`
	UpdatedAt            *time.Time             `json:"updatedAt,omitempty"`
	Active               bool                   `json:"active"`
}

// AccountListOptions - GET /v1/accounts için query parametreleri
type AccountListOptions struct {
	CustomerID string   // Zorunlu
	Statuses   []string // Opsiyonel (ACTIVE, CLOSED)
}
