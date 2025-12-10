package internal

import (
	"context"
	"encoding/json"
	"net/http"
	"strconv"
	"time"
)

func sendMailHandler(w http.ResponseWriter, r *http.Request) {
	var receiver Receiver
	err := json.NewDecoder(r.Body).Decode(&receiver)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		return
	}
	deadline := r.Header.Get("X-Request-Deadline")
	deadlineInt64, err := strconv.ParseInt(deadline, 10, 64)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	ctx, cancel := context.WithDeadline(r.Context(), time.UnixMilli(deadlineInt64))
	defer cancel()
	err = SendMail(ctx, receiver)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
	w.WriteHeader(http.StatusOK)

}
