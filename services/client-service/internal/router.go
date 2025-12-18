package internal

import (
	"log"
	"net/http"

	"github.com/TrioBank/triobank-platform/microservices/client-service/config"
)

func StartRouter(repo Repo) {
	handler := NewHandler(repo)

	// Protected Client endpoints (require X-User-ID)
	http.HandleFunc("/clients/user/", CombinedMiddleware(handler.ClientByUserIDHandler))
	http.HandleFunc("/clients", CombinedMiddleware(handler.ClientsHandler))
	http.HandleFunc("/clients/", CombinedMiddleware(handler.ClientByIDHandler))

	// Health check (public, no auth required, but with timeout)
	http.HandleFunc("/health", PublicMiddleware(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		w.Write([]byte(`{"status":"ok"}`))
	}))

	port := config.GetEnv("CLIENT_SERVICE_PORT")
	log.Printf("Client Service starting on %s", port)

	if err := http.ListenAndServe(port, nil); err != nil {
		log.Fatal("Server failed to start: ", err)
	}
}
