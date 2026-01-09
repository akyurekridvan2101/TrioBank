package main

import (
	"log"
	"net/http"
	"time"

	"github.com/TrioBank/triobank-platform/microservices/api-gateway/config"
	"github.com/TrioBank/triobank-platform/microservices/api-gateway/internal/auth"
	"github.com/TrioBank/triobank-platform/microservices/api-gateway/internal/cache"
	"github.com/TrioBank/triobank-platform/microservices/api-gateway/internal/middleware"
	"github.com/TrioBank/triobank-platform/microservices/api-gateway/internal/proxy"
	httpSwagger "github.com/swaggo/http-swagger"

	_ "github.com/TrioBank/triobank-platform/microservices/api-gateway/docs" // Auto-generated docs
)

// @title TrioBank API Gateway
// @version 1.0
// @description API Gateway for TrioBank microservices platform. Provides unified entry point with rate limiting, authentication, and request routing.
// @termsOfService http://triobank.com/terms/

// @contact.name API Support
// @contact.url http://www.triobank.com/support
// @contact.email support@triobank.com

// @license.name MIT
// @license.url https://opensource.org/licenses/MIT

// @host localhost:3000
// @BasePath /
// @schemes http https

// @securityDefinitions.apikey BearerAuth
// @in header
// @name Authorization
// @description Type "Bearer" followed by a space and JWT token

func init() {
	config.LoadEnv()
}

func main() {

	// ========================================
	// ADIM 1: BAĞIMLILIKLARI BAŞLAT
	// ========================================

	redisClient := cache.StartRedis()
	log.Println("Redis connected")

	authClient := auth.NewAuthClient(config.GetEnv("AUTH_SERVICE_URL"), redisClient)

	serviceProxy := proxy.NewServiceProxy()
	log.Println("✅ Proxy ready")

	// ========================================
	// ADIM 2: MIDDLEWARE'LERİ OLUŞTUR
	// ========================================

	rateLimiter := middleware.NewRateLimiter(redisClient)
	authMiddleware := middleware.NewAuthMiddleware(authClient)

	// ========================================
	// ADIM 3: ROUTE'LARI TANIMLA
	// ========================================

	// ------------------------------------------
	// PUBLIC ENDPOINTS (Herkese açık) - /api prefix
	// ------------------------------------------

	http.Handle("/api/auth/login",
		rateLimiter.IPRateLimit(time.Minute, 20)(serviceProxy.ProxyToAuth()))

	http.Handle("/api/auth/login/confirm",
		rateLimiter.IPRateLimit(time.Minute, 20)(serviceProxy.ProxyToAuth()))

	http.Handle("/api/auth/register",
		rateLimiter.IPRateLimit(time.Minute, 30)(serviceProxy.ProxyToAuth()))

	http.Handle("/api/auth/register/confirm",
		rateLimiter.IPRateLimit(time.Minute, 20)(serviceProxy.ProxyToAuth()))

	// Password reset endpoints (public, no auth required)
	http.Handle("/api/auth/forgot-password/initiate",
		rateLimiter.IPRateLimit(time.Minute, 10)(serviceProxy.ProxyToAuth()))

	http.Handle("/api/auth/forgot-password/verify-code",
		rateLimiter.IPRateLimit(time.Minute, 20)(serviceProxy.ProxyToAuth()))

	http.Handle("/api/auth/forgot-password/reset",
		rateLimiter.IPRateLimit(time.Minute, 10)(serviceProxy.ProxyToAuth()))

	http.Handle("/api/auth/logout", serviceProxy.ProxyToAuth())
	http.Handle("/api/auth/refresh", serviceProxy.ProxyToAuth())
	http.Handle("/api/auth/validation", serviceProxy.ProxyToAuth())
	http.Handle("/api/auth/me", serviceProxy.ProxyToAuth())

	// ------------------------------------------
	// PROTECTED ENDPOINTS (Giriş gerekli) - /api prefix
	// ------------------------------------------

	http.Handle("/api/auth/password-change",
		authMiddleware.RequireAuth(
			rateLimiter.UserRateLimit(time.Minute, 100)(
				serviceProxy.ProxyToAuth())))

	http.Handle("/api/auth/delete-account",
		authMiddleware.RequireAuth(
			rateLimiter.UserRateLimit(time.Minute, 100)(
				serviceProxy.ProxyToAuth())))

	http.Handle("/api/auth/user/update",
		authMiddleware.RequireAuth(
			rateLimiter.UserRateLimit(time.Minute, 50)(
				serviceProxy.ProxyToAuth())))

	// ------------------------------------------
	// CLIENT SERVICE ENDPOINTS (Giriş gerekli) - /api prefix
	// ------------------------------------------

	// GET /api/clients - Client listesi
	// http.Handle("/api/clients",
	// 	authMiddleware.RequireAuth(
	// 		rateLimiter.UserRateLimit(time.Minute, 100)(
	// 			serviceProxy.ProxyToClient())))

	// GET/PUT/DELETE /api/clients/{id}
	http.Handle("/api/clients/",
		authMiddleware.RequireAuth(
			rateLimiter.UserRateLimit(time.Minute, 100)(
				serviceProxy.ProxyToClient())))

	// ------------------------------------------
	// LEDGER SERVICE ENDPOINTS (Giriş gerekli)
	// ------------------------------------------

	// GET /api/v1/ledger/accounts/{accountId}/statement - Hesap hareketleri
	http.Handle("/api/v1/ledger/accounts/",
		authMiddleware.RequireAuth(
			rateLimiter.UserRateLimit(time.Minute, 100)(
				serviceProxy.ProxyToLedger())))

	// GET /api/v1/ledger/balances/{accountId} - Bakiye sorgulama
	http.Handle("/api/v1/ledger/balances/",
		authMiddleware.RequireAuth(
			rateLimiter.UserRateLimit(time.Minute, 100)(
				serviceProxy.ProxyToLedger())))

	// ------------------------------------------
	// ACCOUNT SERVICE ENDPOINTS (Giriş gerekli) - /api prefix
	// ------------------------------------------

	// GET /api/v1/accounts?customerId=X - Müşteri hesapları listesi
	http.Handle("/api/v1/accounts",
		authMiddleware.RequireAuth(
			rateLimiter.UserRateLimit(time.Minute, 100)(
				serviceProxy.ProxyToAccount())))

	// GET /api/v1/accounts/{id} - Hesap detayı
	http.Handle("/api/v1/accounts/",
		authMiddleware.RequireAuth(
			rateLimiter.UserRateLimit(time.Minute, 100)(
				serviceProxy.ProxyToAccount())))

	// ------------------------------------------
	// PRODUCT ENDPOINTS (REMOVED)
	// ------------------------------------------

	// ------------------------------------------
	// CARD SERVICE ENDPOINTS (Giriş gerekli) - /api prefix
	// ------------------------------------------

	// POST /api/v1/cards/debit - Banka kartı çıkar
	http.Handle("/api/v1/cards/debit",
		authMiddleware.RequireAuth(
			rateLimiter.UserRateLimit(time.Minute, 100)(
				serviceProxy.ProxyToCard())))

	// POST /api/v1/cards/virtual - Sanal kart çıkar
	http.Handle("/api/v1/cards/virtual",
		authMiddleware.RequireAuth(
			rateLimiter.UserRateLimit(time.Minute, 100)(
				serviceProxy.ProxyToCard())))

	// POST /api/v1/cards/credit - Kredi kartı çıkar
	http.Handle("/api/v1/cards/credit",
		authMiddleware.RequireAuth(
			rateLimiter.UserRateLimit(time.Minute, 100)(
				serviceProxy.ProxyToCard())))

	// GET /api/v1/cards - Kart listesi (query: customerId, accountId, cardType)
	http.Handle("/api/v1/cards",
		authMiddleware.RequireAuth(
			rateLimiter.UserRateLimit(time.Minute, 100)(
				serviceProxy.ProxyToCard())))

	// GET/PATCH/POST /api/v1/cards/{id} - Kart detayı, block, activate, authorize, validate-pin
	http.Handle("/api/v1/cards/",
		authMiddleware.RequireAuth(
			rateLimiter.UserRateLimit(time.Minute, 100)(
				serviceProxy.ProxyToCard())))

	// ------------------------------------------
	// TRANSACTION SERVICE ENDPOINTS (Giriş gerekli) - /api prefix
	// ------------------------------------------

	// POST /api/v1/transactions/transfer - Para transferi
	http.Handle("/api/v1/transactions/transfer",
		authMiddleware.RequireAuth(
			rateLimiter.UserRateLimit(time.Minute, 100)(
				serviceProxy.ProxyToTransaction())))

	// GET /api/v1/transactions - Transfer listesi
	http.Handle("/api/v1/transactions",
		authMiddleware.RequireAuth(
			rateLimiter.UserRateLimit(time.Minute, 50)(
				serviceProxy.ProxyToTransaction())))

	// GET /api/v1/transactions/ - Transfer detayı, status, idempotency
	// /api/v1/transactions/{id}
	// /api/v1/transactions/by-idempotency-key/{key}
	// /api/v1/transactions/by-reference/{ref}
	http.Handle("/api/v1/transactions/",
		authMiddleware.RequireAuth(
			rateLimiter.UserRateLimit(time.Minute, 100)(
				serviceProxy.ProxyToTransaction())))

	// ------------------------------------------
	// SWAGGER UI ENDPOINTS
	// ------------------------------------------

	// Swagger UI - Spring Boot style URL
	http.HandleFunc("/swagger-ui.html", func(w http.ResponseWriter, r *http.Request) {
		http.Redirect(w, r, "/swagger/index.html", http.StatusMovedPermanently)
	})

	// Swagger UI - Standard swagger path
	http.Handle("/swagger/", httpSwagger.WrapHandler)

	// ------------------------------------------
	// HEALTH CHECK
	// ------------------------------------------

	http.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte(`{"status":"healthy"}`))
	})

	http.HandleFunc("/api/health", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte(`{"status":"healthy"}`))
	})

	// 404 Handler - Route bulunamazsa
	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		// /api/* path'leri için özel 404 mesajı
		if len(r.URL.Path) >= 4 && r.URL.Path[:4] == "/api" {
			log.Printf("[404] API route not found: %s %s", r.Method, r.URL.Path)
			w.WriteHeader(http.StatusNotFound)
			w.Write([]byte(`{"error":"API endpoint not found"}`))
			return
		}
		// Diğer path'ler için genel 404
		log.Printf("[404] Route not found: %s %s", r.Method, r.URL.Path)
		w.WriteHeader(http.StatusNotFound)
		w.Write([]byte("404 page not found"))
	})

	// ========================================
	// ADIM 4: SERVER'I BAŞLAT
	// ========================================

	port := config.GetEnv("API_GATEWAY_PORT")
	if port == "" {
		port = "3000"
	}

	log.Printf("API Gateway is starting: http://localhost:%s", port)

	// Wrap default mux with CORS
	err := http.ListenAndServe(":"+port, middleware.CORSMiddleware(http.DefaultServeMux))
	if err != nil {
		log.Fatal("Server error:", err)
	}
}
