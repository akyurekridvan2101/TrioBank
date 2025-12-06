package internal

import (
	"context"
	"encoding/json"
	"net/http"
	"strconv"
	"time"
)

func (s *SmtpPool) sendMailHandler(w http.ResponseWriter, r *http.Request) {

	var requestBody string
	var receiver Receiver
	if r.Method != http.MethodPost {
		w.WriteHeader(http.StatusBadRequest)
		return
	}
	err := json.NewDecoder(r.Body).Decode(&receiver)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		return
	}
	requestBody = r.Header.Get("X-Request-Deadline")
	timeoutInt64, err := strconv.ParseInt(requestBody, 10, 64)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("Invalid context deadline header"))
		return
	}

	ctx, cancel := context.WithDeadline(r.Context(), time.UnixMilli(timeoutInt64))
	defer cancel()
	channel := make(chan error)
	go SendMail(ctx, channel, s, receiver)

	select {
	case err = <-channel:
		if err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			_, _ = w.Write([]byte("the mail could not be sent"))
			return
		}
		w.WriteHeader(http.StatusOK)
		return
	case <-ctx.Done():
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("email sending timed out or cancelled"))
		return
	}

}
