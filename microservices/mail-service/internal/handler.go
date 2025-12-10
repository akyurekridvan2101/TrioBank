package internal

import (
	"context"
	"encoding/json"
	"net/http"
	"strconv"
	"time"
)

func (s *SmtpPool) sendMailHandler(w http.ResponseWriter, r *http.Request) {
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
