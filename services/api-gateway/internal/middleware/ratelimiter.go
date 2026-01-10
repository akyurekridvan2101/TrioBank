package middleware

import (
	"log"
	"net"
	"net/http"
	"time"

	"github.com/TrioBank/triobank-platform/microservices/api-gateway/internal/cache"
)

// ==========================================
// RATE LIMITER MIDDLEWARE
// ==========================================
// Bu middleware, belirli bir süre içinde aynı kaynaktan
// gelen istek sayısını sınırlar.
//
// İKİ TİP VAR:
// 1. IPRateLimit   → Public endpoint'ler için (login, register)
//                    Aynı IP'den dakikada max 10 istek
//
// 2. UserRateLimit → Protected endpoint'ler için (password-change)
//                    Aynı kullanıcıdan dakikada max 100 istek
//
// NASIL ÇALIŞIR?
// Redis'te bir key oluşturulur: "gateway:ratelimit:ip:192.168.1.1"
// Bu key belirli süre (örn: 1 dakika) boyunca yaşar.
// Key varsa → zaten istek yapılmış, 429 dön
// Key yoksa → yeni key oluştur, isteğe izin ver
// ==========================================

// RateLimiter - Rate limiting için kullanılan struct
type RateLimiter struct {
	Redis cache.RedisI // Redis client (limit kontrolü için)
}

// NewRateLimiter - Yeni RateLimiter oluşturur
func NewRateLimiter(redis cache.RedisI) *RateLimiter {
	return &RateLimiter{
		Redis: redis,
	}
}

// IPRateLimit - IP bazlı rate limiting (PUBLIC endpoint'ler için)
//
// Kullanım:
//
//	mux.Handle("/auth/login", rateLimiter.IPRateLimit(time.Minute, 10)(loginHandler))
func (rl *RateLimiter) IPRateLimit(duration time.Duration, maxRequests int64) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {

			// ADIM 1: İstemcinin IP adresini al (port olmadan)
			ip, _, err := net.SplitHostPort(r.RemoteAddr)
			if err != nil {
				// Port yoksa (nadir), direkt kullan
				ip = r.RemoteAddr
			}

			// ADIM 2: Bu IP için limit kontrolü yap
			// Counter-based: Her istek counter'ı artırır
			allowed, err := rl.Redis.SetAndControlLimitForPublic(r.Context(), ip, duration, maxRequests)
			
			// Redis hatası varsa logla ama isteğe izin ver (fail-open)
			if err != nil {
				log.Printf("[RATE_LIMITER] Redis error for IP %s: %v", ip, err)
				// Redis down olsa bile isteğe izin ver (availability > rate limiting)
			}

			// ADIM 3: Limit aşıldıysa 429 dön
			if !allowed {
				w.Header().Set("Retry-After", "60") // 60 saniye bekle
				http.Error(w, "Çok fazla istek, lütfen bekleyin", http.StatusTooManyRequests)
				return
			}

			// ADIM 4: Limit aşılmadı, isteğe devam et
			next.ServeHTTP(w, r)
		})
	}
}

// UserRateLimit - User ID bazlı rate limiting (PROTECTED endpoint'ler için)
//
// NOT: Bu middleware AuthMiddleware'den SONRA çalışmalı!
// Çünkü user_id'yi context'ten alır.
//
// Kullanım:
//
//	mux.Handle("/auth/password-change",
//	    authMiddleware.RequireAuth(
//	        rateLimiter.UserRateLimit(time.Minute, 100)(handler)))
func (rl *RateLimiter) UserRateLimit(duration time.Duration, maxRequests int64) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {

			// ADIM 1: Context'ten user_id al
			// Bu değer AuthMiddleware tarafından eklendi
			userId, ok := r.Context().Value(UserIDKey).(string)

			// User ID yoksa → Auth middleware çalışmamış demek
			if !ok || userId == "" {
				http.Error(w, "Kullanıcı doğrulanmamış", http.StatusUnauthorized)
				return
			}

			// ADIM 2: Bu kullanıcı için limit kontrolü yap
			allowed, _ := rl.Redis.SetAndControlLimitForProtected(r.Context(), userId, duration, maxRequests)

			// ADIM 3: Limit aşıldıysa 429 dön
			if !allowed {
				w.Header().Set("Retry-After", "60")
				http.Error(w, "Çok fazla istek, lütfen bekleyin", http.StatusTooManyRequests)
				return
			}

			// ADIM 4: İsteğe devam et
			next.ServeHTTP(w, r)
		})
	}
}

