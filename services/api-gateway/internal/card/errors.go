package card

import "fmt"

// ==========================================
// CARD SERVICE - ERROR HANDLING
// ==========================================
// Card servisten dönen hataların type-safe şekilde handle edilmesi için

// CardError - Card servisi hata tipi
type CardError struct {
	Code       string `json:"code"`
	Message    string `json:"message"`
	StatusCode int    `json:"status"`
}

// Error - error interface implementasyonu
func (e *CardError) Error() string {
	return fmt.Sprintf("[Card %d] %s: %s", e.StatusCode, e.Code, e.Message)
}

// NewCardError - Yeni card error oluşturur
func NewCardError(statusCode int, code, message string) *CardError {
	return &CardError{
		StatusCode: statusCode,
		Code:       code,
		Message:    message,
	}
}

// IsNotFound - 404 hatası mı kontrol eder
func (e *CardError) IsNotFound() bool {
	return e.StatusCode == 404
}

// IsServerError - 5xx hatası mı kontrol eder
func (e *CardError) IsServerError() bool {
	return e.StatusCode >= 500
}

// IsBadRequest - 400 hatası mı kontrol eder
func (e *CardError) IsBadRequest() bool {
	return e.StatusCode == 400
}
