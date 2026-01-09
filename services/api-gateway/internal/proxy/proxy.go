package proxy

import (
	"log"
	"net/http"
	"net/http/httputil"
	"net/url"
	"time"

	"github.com/TrioBank/triobank-platform/microservices/api-gateway/config"
)

// ==========================================
// REVERSE PROXY
// ==========================================
// Bu modül, gelen istekleri arka plandaki servislere yönlendirir.
//
// REVERSE PROXY NEDİR?
// Client → API Gateway → Auth Service / Client Service
//
// Client doğrudan backend servislerine istek atmaz.
// API Gateway arada durur ve isteği yönlendirir.
//
// AVANTAJLARI:
// - Tek giriş noktası (single entry point)
// - Rate limiting, auth kontrolü gateway'de yapılır
// - Backend servisleri dış dünyadan gizlenir
// ==========================================

// ServiceProxy - Servislere istek yönlendiren struct
type ServiceProxy struct {
	AuthProxy    *httputil.ReverseProxy // Auth service'e yönlendiren proxy
	ClientProxy  *httputil.ReverseProxy // Client service'e yönlendiren proxy
	LedgerProxy  *httputil.ReverseProxy // Ledger service'e yönlendiren proxy
	AccountProxy *httputil.ReverseProxy // Account service'e yönlendiren proxy
	CardProxy    *httputil.ReverseProxy // Card service'e yönlendiren proxy
	TransactionProxy *httputil.ReverseProxy // Transaction service'e yönlendiren proxy
}

// NewServiceProxy - Yeni proxy oluşturur
func NewServiceProxy() *ServiceProxy {

	// Auth Service Proxy
	authURLString := config.GetEnv("AUTH_SERVICE_URL")
	authURL, _ := url.Parse(authURLString)
	authProxy := httputil.NewSingleHostReverseProxy(authURL)
	authProxy.ErrorHandler = func(w http.ResponseWriter, r *http.Request, err error) {
		http.Error(w, "Auth servisi şu an erişilemez", http.StatusBadGateway)
	}

	// Client Service Proxy
	clientURLString := config.GetEnv("CLIENT_SERVICE_URL")
	clientURL, _ := url.Parse(clientURLString)
	clientProxy := httputil.NewSingleHostReverseProxy(clientURL)
	clientProxy.ErrorHandler = func(w http.ResponseWriter, r *http.Request, err error) {
		http.Error(w, "Client servisi şu an erişilemez", http.StatusBadGateway)
	}

	// Ledger Service Proxy
	ledgerURLString := config.GetEnv("LEDGER_SERVICE_URL")
	ledgerURL, _ := url.Parse(ledgerURLString)
	ledgerProxy := httputil.NewSingleHostReverseProxy(ledgerURL)
	ledgerProxy.ErrorHandler = func(w http.ResponseWriter, r *http.Request, err error) {
		http.Error(w, "Ledger servisi şu an erişilemez", http.StatusBadGateway)
	}

	// Account Service Proxy
	accountURLString := config.GetEnv("ACCOUNT_SERVICE_URL")
	accountURL, _ := url.Parse(accountURLString)
	accountProxy := httputil.NewSingleHostReverseProxy(accountURL)
	accountProxy.ErrorHandler = func(w http.ResponseWriter, r *http.Request, err error) {
		http.Error(w, "Account servisi şu an erişilemez", http.StatusBadGateway)
	}

	// Card Service Proxy
	cardURLString := config.GetEnv("CARD_SERVICE_URL")
	cardURL, _ := url.Parse(cardURLString)
	cardProxy := httputil.NewSingleHostReverseProxy(cardURL)
	cardProxy.ErrorHandler = func(w http.ResponseWriter, r *http.Request, err error) {
		http.Error(w, "Card servisi şu an erişilemez", http.StatusBadGateway)
	}

	// Transaction Service Proxy
	transactionURLString := config.GetEnv("TRANSACTION_SERVICE_URL")
	if transactionURLString == "" {
		transactionURLString = "http://transaction-service:8084" // default fallback
	}
	transactionURL, _ := url.Parse(transactionURLString)
	transactionProxy := httputil.NewSingleHostReverseProxy(transactionURL)
	transactionProxy.ErrorHandler = func(w http.ResponseWriter, r *http.Request, err error) {
		http.Error(w, "Transaction servisi şu an erişilemez", http.StatusBadGateway)
	}

	return &ServiceProxy{
		AuthProxy:    authProxy,
		ClientProxy:  clientProxy,
		LedgerProxy:  ledgerProxy,
		AccountProxy: accountProxy,
		CardProxy:    cardProxy,
		TransactionProxy: transactionProxy,
	}
}

// ProxyToAuth - İsteği auth service'e yönlendirir
// /api prefix'ini kaldırarak backend servise gönderir
func (sp *ServiceProxy) ProxyToAuth() http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()
		
		// /api prefix'ini kaldır (backend servisler /api beklemiyor)
		originalPath := r.URL.Path
		if len(originalPath) >= 4 && originalPath[:4] == "/api" {
			r.URL.Path = originalPath[4:] // /api/auth/login -> /auth/login
			if r.URL.Path == "" {
				r.URL.Path = "/"
			}
		}
		
		// Log request
		log.Printf("[AUTH] %s %s -> %s from %s", r.Method, originalPath, r.URL.Path, r.RemoteAddr)
		
		// Proxy işlemi
		sp.AuthProxy.ServeHTTP(w, r)
		
		// Log response time
		duration := time.Since(start)
		log.Printf("[AUTH] Completed in %v", duration)
	})
}

// ProxyToClient - İsteği client service'e yönlendirir
// /api prefix'ini kaldırarak backend servise gönderir
func (sp *ServiceProxy) ProxyToClient() http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()
		
		// /api prefix'ini kaldır
		originalPath := r.URL.Path
		if len(originalPath) >= 4 && originalPath[:4] == "/api" {
			r.URL.Path = originalPath[4:]
			if r.URL.Path == "" {
				r.URL.Path = "/"
			}
		}
		
		log.Printf("[CLIENT] %s %s -> %s from %s", r.Method, originalPath, r.URL.Path, r.RemoteAddr)
		sp.ClientProxy.ServeHTTP(w, r)
		duration := time.Since(start)
		log.Printf("[CLIENT] Completed in %v", duration)
	})
}

// ProxyToLedger - İsteği ledger service'e yönlendirir
// /api prefix'ini kaldırarak backend servise gönderir
func (sp *ServiceProxy) ProxyToLedger() http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()
		
		// /api prefix'ini kaldır (backend zaten /api/v1/ledger bekliyor, sadece /api kısmını kaldır)
		originalPath := r.URL.Path
		if len(originalPath) >= 4 && originalPath[:4] == "/api" {
			r.URL.Path = originalPath[4:]
			if r.URL.Path == "" {
				r.URL.Path = "/"
			}
		}
		
		log.Printf("[LEDGER] %s %s -> %s from %s", r.Method, originalPath, r.URL.Path, r.RemoteAddr)
		sp.LedgerProxy.ServeHTTP(w, r)
		duration := time.Since(start)
		log.Printf("[LEDGER] Completed in %v", duration)
	})
}

// ProxyToAccount - İsteği account service'e yönlendirir
// /api prefix'ini kaldırarak backend servise gönderir
func (sp *ServiceProxy) ProxyToAccount() http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()
		
		// /api prefix'ini kaldır
		originalPath := r.URL.Path
		if len(originalPath) >= 4 && originalPath[:4] == "/api" {
			r.URL.Path = originalPath[4:]
			if r.URL.Path == "" {
				r.URL.Path = "/"
			}
		}
		
		log.Printf("[ACCOUNT] %s %s -> %s from %s", r.Method, originalPath, r.URL.Path, r.RemoteAddr)
		sp.AccountProxy.ServeHTTP(w, r)
		duration := time.Since(start)
		log.Printf("[ACCOUNT] Completed in %v", duration)
	})
}

// ProxyToCard - İsteği card service'e yönlendirir
// /api prefix'ini kaldırarak backend servise gönderir
func (sp *ServiceProxy) ProxyToCard() http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()
		
		// /api prefix'ini kaldır
		originalPath := r.URL.Path
		if len(originalPath) >= 4 && originalPath[:4] == "/api" {
			r.URL.Path = originalPath[4:]
			if r.URL.Path == "" {
				r.URL.Path = "/"
			}
		}
		
		log.Printf("[CARD] %s %s -> %s from %s", r.Method, originalPath, r.URL.Path, r.RemoteAddr)
		sp.CardProxy.ServeHTTP(w, r)
		duration := time.Since(start)
		log.Printf("[CARD] Completed in %v", duration)
	})
}

// ProxyToTransaction - İsteği transaction service'e yönlendirir
// /api prefix'ini kaldırarak backend servise gönderir
func (sp *ServiceProxy) ProxyToTransaction() http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()
		
		// /api prefix'ini kaldır
		originalPath := r.URL.Path
		if len(originalPath) >= 4 && originalPath[:4] == "/api" {
			r.URL.Path = originalPath[4:]
			if r.URL.Path == "" {
				r.URL.Path = "/"
			}
		}
		
		log.Printf("[TRANSACTION] %s %s -> %s from %s", r.Method, originalPath, r.URL.Path, r.RemoteAddr)
		sp.TransactionProxy.ServeHTTP(w, r)
		duration := time.Since(start)
		log.Printf("[TRANSACTION] Completed in %v", duration)
	})
}

