package internal

import (
	"log"
	"net/http"
)

func StartRouter() {
	// handler will be placed here
	err := http.ListenAndServe("localhost:8080", nil)
	if err != nil {
		log.Fatal(err.Error())
	}
}
