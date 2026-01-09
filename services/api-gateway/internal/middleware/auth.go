package middleware

import (
	"context"
	"net/http"
	"strings"

	"github.com/TrioBank/triobank-platform/microservices/api-gateway/internal/auth"
)

// ==========================================
// AUTH MIDDLEWARE
// ==========================================
// Korumalı endpointler için token doğrulama mekanizması.
// Header'dan token'ı alır, Auth servisine sorar, geçerliyse UserID'yi işler.
// ==========================================

// Context key tipi (String yerine type kullanmak daha güvenli)
type contextKey string

// User ID'yi context'te saklamak için anahtar
const UserIDKey contextKey = "user_id"

// AuthMiddleware struct
type AuthMiddleware struct {
	AuthClient *auth.AuthsClient // Auth servis client'ı
}

// NewAuthMiddleware constructor
func NewAuthMiddleware(client *auth.AuthsClient) *AuthMiddleware {
	return &AuthMiddleware{
		AuthClient: client,
	}
}

// RequireAuth: Endpoint koruma middleware'i
func (am *AuthMiddleware) RequireAuth(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {

		// 1. Authorization header kontrolü
		authHeader := r.Header.Get("Authorization")

		// Header yoksa 401
		if authHeader == "" {
			http.Error(w, "Authorization header gerekli", http.StatusUnauthorized)
			return
		}

		// 2. Format kontrolü (Bearer)
		if !strings.HasPrefix(authHeader, "Bearer ") {
			http.Error(w, "Geçersiz token formatı", http.StatusUnauthorized)
			return
		}

		// 3. Token'ı ayıkla
		token := strings.TrimPrefix(authHeader, "Bearer ")

		// 4. Doğrulama (Auth Service)
		userId, err := am.AuthClient.ValidateToken(r.Context(), token)
		if err != nil {
			// Token geçersiz veya süresi dolmuş
			http.Error(w, "Geçersiz veya süresi dolmuş token", http.StatusUnauthorized)
			return
		}

		// 5. UserID'yi context'e at
		ctx := context.WithValue(r.Context(), UserIDKey, userId)

		// 6. X-User-ID header'ı ekle (diğer servisler için)
		r.Header.Set("X-User-ID", userId)

		// 7. Devam et
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}
