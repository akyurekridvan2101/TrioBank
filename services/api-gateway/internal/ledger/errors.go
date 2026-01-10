package ledger

import "fmt"

// ==========================================
// LEDGER SERVICE - ERROR HANDLING
// ==========================================
// Ledger servisten dönen hataların type-safe şekilde handle edilmesi için

// LedgerError - Ledger servisi hata tipi
type LedgerError struct {
	Code       string `json:"code"`
	Message    string `json:"message"`
	StatusCode int    `json:"status"`
}

// Error - error interface implementasyonu
func (e *LedgerError) Error() string {
	return fmt.Sprintf("[Ledger %d] %s: %s", e.StatusCode, e.Code, e.Message)
}

// NewLedgerError - Yeni ledger error oluşturur
func NewLedgerError(statusCode int, code, message string) *LedgerError {
	return &LedgerError{
		StatusCode: statusCode,
		Code:       code,
		Message:    message,
	}
}

// IsAccountNotFound - 404 hatası mı kontrol eder
func (e *LedgerError) IsAccountNotFound() bool {
	return e.StatusCode == 404
}

// IsServerError - 5xx hatası mı kontrol eder
func (e *LedgerError) IsServerError() bool {
	return e.StatusCode >= 500
}

// IsBadRequest - 400 hatası mı kontrol eder
func (e *LedgerError) IsBadRequest() bool {
	return e.StatusCode == 400
}
