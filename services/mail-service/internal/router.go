package internal

import (
	"log"
	"net/http"
	"os"
)

func StartRouter(client Client) {

	http.Handle("/send", middleWare(client.sendMailHandler))
	http.HandleFunc("/health", healthCheckHandler)

	err := http.ListenAndServe(os.Getenv("MAIL_SERVICE_PORT"), nil)
	if err != nil {
		log.Fatal(err)
	}
}

func healthCheckHandler(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusOK)
	w.Write([]byte("OK"))
}

func middleWare(f http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		// middleware layer that implement logging

		if r.Header.Get("X-Internal-Secret") == os.Getenv("SECRET_KEY") {
			f(w, r)
		} else {
			w.WriteHeader(http.StatusUnauthorized)
			return
		}

	}
}
