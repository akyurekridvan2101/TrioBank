package account

import "fmt"

// ==========================================
// ACCOUNT SERVICE - ERROR HANDLING
// ==========================================
// Account servisten dönen hataların type-safe şekilde handle edilmesi için

// AccountError - Account servisi hata tipi
type AccountError struct {
	Code       string `json:"code"`
	Message    string `json:"message"`
	StatusCode int    `json:"status"`
}

// Error - error interface implementasyonu
func (e *AccountError) Error() string {
	return fmt.Sprintf("[Account %d] %s: %s", e.StatusCode, e.Code, e.Message)
}

// NewAccountError - Yeni account error oluşturur
func NewAccountError(statusCode int, code, message string) *AccountError {
	return &AccountError{
		StatusCode: statusCode,
		Code:       code,
		Message:    message,
	}
}

// IsNotFound - 404 hatası mı kontrol eder
func (e *AccountError) IsNotFound() bool {
	return e.StatusCode == 404
}

// IsServerError - 5xx hatası mı kontrol eder
func (e *AccountError) IsServerError() bool {
	return e.StatusCode >= 500
}

// IsBadRequest - 400 hatası mı kontrol eder
func (e *AccountError) IsBadRequest() bool {
	return e.StatusCode == 400
}
