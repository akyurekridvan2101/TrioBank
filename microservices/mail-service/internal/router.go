package internal

import (
	"log"
	"net/http"
	"os"
)

func StartRouter() {

	http.Handle("/send", middleWare(sendMailHandler))

	err := http.ListenAndServe(os.Getenv("MAIL_SERVICE_PORT"), nil)
	if err != nil {
		log.Fatal(err)
	}
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
