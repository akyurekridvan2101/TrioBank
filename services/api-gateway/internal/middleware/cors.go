package middleware

import (
	"net/http"
)

// CORSMiddleware - CORS headers for same-origin architecture
// Cloudflare Workers/Pages arkasında same-origin kullanıldığı için
// CORS ayarları basitleştirildi, ancak yine de güvenli tutuldu
func CORSMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		origin := r.Header.Get("Origin")

		// Same-origin mimari: Origin varsa onu kullan, yoksa * kullan
		// Cloudflare Workers arkasında same-origin olacak, ama yine de CORS header'ları gerekli
		if origin != "" {
			w.Header().Set("Access-Control-Allow-Origin", origin)
			w.Header().Set("Access-Control-Allow-Credentials", "true")
		} else {
			// Non-browser requests için (no Origin header)
			w.Header().Set("Access-Control-Allow-Origin", "*")
		}

		w.Header().Set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS")
		w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-User-ID")
		w.Header().Set("Access-Control-Max-Age", "86400") // 24 saat cache

		// Handle preflight requests
		if r.Method == "OPTIONS" {
			w.WriteHeader(http.StatusNoContent)
			return
		}

		next.ServeHTTP(w, r)
	})
}
