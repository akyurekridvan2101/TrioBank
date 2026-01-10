package card

import "time"

// ==========================================
// CARD SERVICE - RESPONSE MODELS
// ==========================================
// Card servisten dönen response'ların Go struct karşılıkları
// OpenAPI spesifikasyonuna göre tanımlanmıştır

// CardResponse - Genel kart response (polymorphic)
// GET /v1/cards endpoint response'u
type CardResponse struct {
	ID              string     `json:"id"`
	CardType        string     `json:"cardType"` // DEBIT | VIRTUAL
	Number          string     `json:"number"`   // Masked for DEBIT, full for VIRTUAL
	CardholderName  string     `json:"cardholderName"`
	ExpiryMonth     int        `json:"expiryMonth"`
	ExpiryYear      int        `json:"expiryYear"`
	CardBrand       string     `json:"cardBrand"` // VISA | MASTERCARD
	Status          string     `json:"status"`    // ACTIVE | BLOCKED | EXPIRED
	AccountID       string     `json:"accountId"`
	CreatedAt       time.Time  `json:"createdAt"`
	BlockedAt       *time.Time `json:"blockedAt,omitempty"`
	BlockReason     string     `json:"blockReason,omitempty"`
	
	// DEBIT-specific fields (only if cardType=DEBIT)
	DailyWithdrawalLimit *float64 `json:"dailyWithdrawalLimit,omitempty"`
	AtmEnabled           *bool    `json:"atmEnabled,omitempty"`
	
	// VIRTUAL-specific fields (only if cardType=VIRTUAL)
	CVV                  string     `json:"cvv,omitempty"`
	OnlineOnly           *bool      `json:"onlineOnly,omitempty"`
	SingleUseExpiresAt   *time.Time `json:"singleUseExpiresAt,omitempty"`
	UsageRestriction     string     `json:"usageRestriction,omitempty"`
}

// DebitCardResponse - Banka kartı response
// POST /v1/cards/debit endpoint response'u
type DebitCardResponse struct {
	ID                   string     `json:"id"`
	CardType             string     `json:"cardType"` // "DEBIT"
	Number               string     `json:"number"`   // Masked: **** **** **** 1234
	CardholderName       string     `json:"cardholderName"`
	ExpiryMonth          int        `json:"expiryMonth"`
	ExpiryYear           int        `json:"expiryYear"`
	CardBrand            string     `json:"cardBrand"`
	Status               string     `json:"status"`
	AccountID            string     `json:"accountId"`
	CreatedAt            time.Time  `json:"createdAt"`
	BlockedAt            *time.Time `json:"blockedAt,omitempty"`
	BlockReason          string     `json:"blockReason,omitempty"`
	DailyWithdrawalLimit float64    `json:"dailyWithdrawalLimit"`
	AtmEnabled           bool       `json:"atmEnabled"`
}

// VirtualCardResponse - Sanal kart response
// POST /v1/cards/virtual endpoint response'u
type VirtualCardResponse struct {
	ID                 string     `json:"id"`
	CardType           string     `json:"cardType"` // "VIRTUAL"
	Number             string     `json:"number"`   // Full: 5123 4567 8901 2345
	CVV                string     `json:"cvv"`      // CVV visible for virtual
	CardholderName     string     `json:"cardholderName"`
	ExpiryMonth        int        `json:"expiryMonth"`
	ExpiryYear         int        `json:"expiryYear"`
	CardBrand          string     `json:"cardBrand"`
	Status             string     `json:"status"`
	AccountID          string     `json:"accountId"`
	CreatedAt          time.Time  `json:"createdAt"`
	BlockedAt          *time.Time `json:"blockedAt,omitempty"`
	BlockReason        string     `json:"blockReason,omitempty"`
	OnlineOnly         bool       `json:"onlineOnly"`
	SingleUseExpiresAt *time.Time `json:"singleUseExpiresAt,omitempty"`
	UsageRestriction   string     `json:"usageRestriction,omitempty"`
}

// IssueDebitCardRequest - Banka kartı çıkarma request'i
// POST /v1/cards/debit endpoint request body
type IssueDebitCardRequest struct {
	AccountID            string   `json:"accountId"`
	CardholderName       string   `json:"cardholderName"`
	DailyWithdrawalLimit *float64 `json:"dailyWithdrawalLimit,omitempty"`
	AtmEnabled           *bool    `json:"atmEnabled,omitempty"`
	PIN                  string   `json:"pin,omitempty"` // 4-digit PIN
}

// IssueVirtualCardRequest - Sanal kart çıkarma request'i
// POST /v1/cards/virtual endpoint request body
type IssueVirtualCardRequest struct {
	AccountID              string `json:"accountId"`
	CardholderName         string `json:"cardholderName"`
	OnlineOnly             *bool  `json:"onlineOnly,omitempty"`
	SingleUse              *bool  `json:"singleUse,omitempty"`
	SingleUseValidityHours *int   `json:"singleUseValidityHours,omitempty"`
	UsageRestriction       string `json:"usageRestriction,omitempty"`
}

// CreditCardResponse - Kredi kartı response
// POST /v1/cards/credit endpoint response'u
type CreditCardResponse struct {
	ID              string     `json:"id"`
	CardType        string     `json:"cardType"` // "CREDIT"
	Number          string     `json:"number"`   // Masked: **** **** **** 1234
	CardholderName  string     `json:"cardholderName"`
	ExpiryMonth     int        `json:"expiryMonth"`
	ExpiryYear      int        `json:"expiryYear"`
	CardBrand       string     `json:"cardBrand"`
	Status          string     `json:"status"`
	AccountID       string     `json:"accountId"`
	CreatedAt       time.Time  `json:"createdAt"`
	BlockedAt       *time.Time `json:"blockedAt,omitempty"`
	BlockReason     string     `json:"blockReason,omitempty"`
	CreditLimit     float64    `json:"creditLimit"`
	AvailableCredit float64    `json:"availableCredit"`
	InterestRate    float64    `json:"interestRate"`
	StatementDay    int        `json:"statementDay"`
	PaymentDueDay   int        `json:"paymentDueDay"`
}

// IssueCreditCardRequest - Kredi kartı çıkarma request'i
// POST /v1/cards/credit endpoint request body
type IssueCreditCardRequest struct {
	AccountID     string   `json:"accountId"`
	CardholderName string   `json:"cardholderName"`
	CreditLimit    float64  `json:"creditLimit"`
	InterestRate   *float64 `json:"interestRate,omitempty"`
	StatementDay   *int     `json:"statementDay,omitempty"`
	PaymentDueDay  *int     `json:"paymentDueDay,omitempty"`
}

// AuthorizationRequest - İşlem yetkilendirme request'i
// POST /v1/cards/{id}/authorize endpoint request body
type AuthorizationRequest struct {
	Amount           float64 `json:"amount"`
	Currency         string  `json:"currency"`
	MerchantID       string  `json:"merchantId,omitempty"`
	MerchantName     string  `json:"merchantName,omitempty"`
	MerchantCategory string  `json:"merchantCategory,omitempty"`
	TransactionType  string  `json:"transactionType"` // PURCHASE, WITHDRAWAL, REFUND
	Channel          string  `json:"channel,omitempty"` // POS, ATM, ONLINE, MOBILE
}

// AuthorizationResponse - İşlem yetkilendirme response'u
// POST /v1/cards/{id}/authorize endpoint response'u
type AuthorizationResponse struct {
	Authorized          bool    `json:"authorized"`
	AccountID           string  `json:"accountId,omitempty"`
	CardType            string  `json:"cardType,omitempty"`
	CardStatus          string  `json:"cardStatus,omitempty"`
	RemainingDailyLimit *float64 `json:"remainingDailyLimit,omitempty"`
	DeclineReason       string  `json:"declineReason,omitempty"`
	Message             string  `json:"message,omitempty"`
}

// CardListOptions - GET /v1/cards için query parametreleri
type CardListOptions struct {
	CustomerID string   // Opsiyonel
	AccountID  string   // Opsiyonel
	CardTypes  []string // Opsiyonel (DEBIT, VIRTUAL, CREDIT)
}
