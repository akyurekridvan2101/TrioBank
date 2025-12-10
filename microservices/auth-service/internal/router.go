package internal

import (
	"log"
	"net/http"

	"github.com/TrioBank/triobank-platform/microservices/auth-service/config"
)

func StartRouter(r Repo) {
	// handler will be placed here

	http.HandleFunc("/auth/login", middleware(r.login))
	http.HandleFunc("/auth/login/confirm", middleware(r.LoginConfirm))
	http.HandleFunc("/auth/register", middleware(r.Register))
	http.HandleFunc("/auth/register/confirm", middleware(r.RegisterConfirm))

	//http.HandleFunc("/auth/register", middleware(r.))

	err := http.ListenAndServe(config.GetEnv("AUTH_SERVICE_PORT"), nil)
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
