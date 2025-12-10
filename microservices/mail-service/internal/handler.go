package internal

import (
	"context"
	"encoding/json"
	"net/http"
	"time"
)

func sendMailHandler(w http.ResponseWriter, r *http.Request) {
	var receiver Receiver
	err := json.NewDecoder(r.Body).Decode(&receiver)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		return
	}
	ctx, cancel := context.WithTimeout(r.Context(), 10*time.Second)
	defer cancel()
	err = SendMail(ctx, receiver)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
	w.WriteHeader(http.StatusOK)

}
