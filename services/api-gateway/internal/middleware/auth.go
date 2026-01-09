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
// Bu middleware, protected endpoint'lere gelen isteklerde
// kullanıcının geçerli bir token'a sahip olup olmadığını kontrol eder.
//
// AKIŞ:
// 1. Request gelir → Authorization header'ı var mı?
// 2. Header "Bearer <token>" formatında mı?
// 3. Token geçerli mi? (Auth service'e sorulur)
// 4. Geçerliyse → user_id context'e eklenir, request devam eder
// 5. Geçersizse → 401 Unauthorized döner
// ==========================================

// contextKey - Context'e değer eklerken kullanılan özel tip
// String yerine özel tip kullanmak Go'da best practice'tir
type contextKey string

// UserIDKey - User ID'yi context'te saklamak için anahtar
const UserIDKey contextKey = "user_id"

// AuthMiddleware - Token doğrulama için kullanılan struct
type AuthMiddleware struct {
	AuthClient *auth.AuthsClient // Auth service ile iletişim kuran client
}

// NewAuthMiddleware - Yeni AuthMiddleware oluşturur
func NewAuthMiddleware(client *auth.AuthsClient) *AuthMiddleware {
	return &AuthMiddleware{
		AuthClient: client,
	}
}

// RequireAuth - Protected endpoint'ler için middleware fonksiyonu
//
// Kullanım:
//
//	mux.Handle("/protected", authMiddleware.RequireAuth(myHandler))
func (am *AuthMiddleware) RequireAuth(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {

		// ADIM 1: Authorization header'ı al
		authHeader := r.Header.Get("Authorization")

		// Header yoksa → 401 dön
		if authHeader == "" {
			http.Error(w, "Authorization header gerekli", http.StatusUnauthorized)
			return
		}

		// ADIM 2: "Bearer " ile başlıyor mu kontrol et
		if !strings.HasPrefix(authHeader, "Bearer ") {
			http.Error(w, "Geçersiz token formatı", http.StatusUnauthorized)
			return
		}

		// ADIM 3: "Bearer " kısmını çıkar, sadece token'ı al
		token := strings.TrimPrefix(authHeader, "Bearer ")

		// ADIM 4: Token'ı auth service'den doğrula
		userId, err := am.AuthClient.ValidateToken(r.Context(), token)
		if err != nil {
			// Token geçersiz veya süresi dolmuş
			http.Error(w, "Geçersiz veya süresi dolmuş token", http.StatusUnauthorized)
			return
		}

		// ADIM 5: User ID'yi context'e ekle
		// Bu sayede sonraki handler'lar user_id'ye erişebilir
		ctx := context.WithValue(r.Context(), UserIDKey, userId)

		// ADIM 6: X-User-ID header'ı downstream servislere forward et
		r.Header.Set("X-User-ID", userId)

		// ADIM 7: Sonraki handler'a devam et
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}
