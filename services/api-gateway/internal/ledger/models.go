package ledger

import "time"

// ==========================================
// LEDGER SERVICE - RESPONSE MODELS
// ==========================================
// Ledger servisten dönen response'ların Go struct karşılıkları
// OpenAPI spesifikasyonuna göre tanımlanmıştır

// BalanceResponse - Hesap bakiye bilgileri
// GET /api/v1/ledger/balances/{accountId} endpoint response'u
type BalanceResponse struct {
	AccountID        string    `json:"accountId"`
	Balance          float64   `json:"balance"`
	BlockedAmount    float64   `json:"blockedAmount"`
	AvailableBalance float64   `json:"availableBalance"` // EN ÖNEMLİ - UI'da bunu göster
	PendingDebits    float64   `json:"pendingDebits"`
	PendingCredits   float64   `json:"pendingCredits"`
	Currency         string    `json:"currency"`
	LastUpdatedAt    time.Time `json:"lastUpdatedAt"`
	LastEntryID      string    `json:"lastEntryId"`
	Version          int64     `json:"version"`
}

// StatementResponse - Hesap ekstresi response'u
// GET /api/v1/ledger/accounts/{accountId}/statement endpoint response'u
type StatementResponse struct {
	AccountID      string             `json:"accountId"`
	Currency       string             `json:"currency"`
	Period         *PeriodInfo        `json:"period,omitempty"`
	OpeningBalance float64            `json:"openingBalance"`
	ClosingBalance float64            `json:"closingBalance"`
	TotalDebits    float64            `json:"totalDebits"`
	TotalCredits   float64            `json:"totalCredits"`
	NetChange      float64            `json:"netChange"`
	EntryCount     int                `json:"entryCount"`
	Entries        []StatementEntry   `json:"entries"`
	Pagination     PaginationMetadata `json:"pagination"`
}

// StatementEntry - Ekstredeki tek bir işlem kaydı
type StatementEntry struct {
	EntryID         string    `json:"entryId"`
	TransactionID   string    `json:"transactionId"`
	Date            string    `json:"date"` // YYYY-MM-DD formatında
	TransactionTime time.Time `json:"transactionTime"`
	EntryType       string    `json:"entryType"` // "DEBIT" veya "CREDIT"
	Amount          float64   `json:"amount"`
	RunningBalance  float64   `json:"runningBalance"`
	Description     string    `json:"description"`
	ReferenceNumber string    `json:"referenceNumber"`
}

// PeriodInfo - Ekstre dönem bilgisi
type PeriodInfo struct {
	StartDate string `json:"startDate"` // YYYY-MM-DD
	EndDate   string `json:"endDate"`   // YYYY-MM-DD
}

// PaginationMetadata - Sayfalama bilgileri
type PaginationMetadata struct {
	Page          int   `json:"page"`
	Size          int   `json:"size"`
	TotalElements int64 `json:"totalElements"`
	TotalPages    int   `json:"totalPages"`
}

// StatementOptions - Statement endpoint için query parametreleri
type StatementOptions struct {
	StartDate             *time.Time // YYYY-MM-DD formatına çevrilecek
	EndDate               *time.Time // YYYY-MM-DD formatına çevrilecek
	Type                  string     // "DEBIT" veya "CREDIT"
	Keyword               string     // Açıklama/referans araması
	Page                  int        // Sayfa numarası (0-indexed)
	Size                  int        // Sayfa boyutu (default: 20, max: 100)
	IncludeRunningBalance bool       // Running balance hesaplansın mı? (default: true)
}

// NewStatementOptions - Default statement options oluşturur
func NewStatementOptions() *StatementOptions {
	return &StatementOptions{
		Page:                  0,
		Size:                  20,
		IncludeRunningBalance: true,
	}
}
