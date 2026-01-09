package internal

import (
	"context"
	"net/http"
	"time"
)

const (
	// RequestTimeout is the default timeout for all requests
	RequestTimeout = 10 * time.Second
	
	// XUserIDHeader is the header name for user identification from API Gateway
	XUserIDHeader = "X-User-ID"
)

// AuthMiddleware checks for X-User-ID header from API Gateway
// This middleware should be applied to protected endpoints
func AuthMiddleware(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		userID := r.Header.Get(XUserIDHeader)
		if userID == "" {
			respondError(w, http.StatusUnauthorized, "X-User-ID header is required")
			return
		}
		
		// Add user ID to context for downstream use
		ctx := context.WithValue(r.Context(), XUserIDHeader, userID)
		next.ServeHTTP(w, r.WithContext(ctx))
	}
}

// TimeoutMiddleware adds a timeout to the request context
func TimeoutMiddleware(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		ctx, cancel := context.WithTimeout(r.Context(), RequestTimeout)
		defer cancel()
		
		next.ServeHTTP(w, r.WithContext(ctx))
	}
}

// CombinedMiddleware applies both auth and timeout middleware
func CombinedMiddleware(next http.HandlerFunc) http.HandlerFunc {
	return TimeoutMiddleware(AuthMiddleware(next))
}

// PublicMiddleware applies only timeout (no auth required)
func PublicMiddleware(next http.HandlerFunc) http.HandlerFunc {
	return TimeoutMiddleware(next)
}

// GetUserIDFromContext extracts user ID from request context
func GetUserIDFromContext(ctx context.Context) string {
	if userID, ok := ctx.Value(XUserIDHeader).(string); ok {
		return userID
	}
	return ""
}
