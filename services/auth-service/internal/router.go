package internal

import (
	"log"
	"net/http"
)

func StartRouter(r Repo) {
	// handler will be placed here

	http.HandleFunc("/auth/login", middleware(r.login))

	err := http.ListenAndServe("localhost:8080", nil)
	if err != nil {
		log.Fatal(err.Error())
	}
}

func middleware(handler http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		// middleware islemleri (logging, rate and limiting cors etc.)

		handler(w, r)
	}
}
