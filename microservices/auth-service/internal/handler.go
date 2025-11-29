package internal

import (
	"context"
	"crypto/rand"
	"encoding/base64"
	"encoding/json"
	"math/big"
	"net/http"
	"time"
)

type Repo struct {
	DataBase       DataBaseI
	SessionManager SessionManagerI
}

func (repo Repo) login(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		w.WriteHeader(http.StatusMethodNotAllowed)
		_, _ = w.Write([]byte("this endpoint can be used only with post request"))
		return
	}
	ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
	defer cancel()

	var data loginData
	// in first the password will not be hashed, then we will hash it before write into the database

	// the control of request body
	if err := json.NewDecoder(r.Body).Decode(&data); err != nil || (data.Password == "" || data.Tc == "") {
		w.WriteHeader(http.StatusBadRequest)
		_, _ = w.Write([]byte("the request body could not be decoded or the request may do not include password or tc fields"))
		return
	}

	err := repo.DataBase.loginControl(ctx, data)
	if err != nil {
		w.WriteHeader(http.StatusUnauthorized)
		_, _ = w.Write([]byte("the login inputs are invalid"))
		return
	}
	code, err := rand.Int(rand.Reader, big.NewInt(9000))
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("while generating the code, an error occured"))
		return
	}
	sessionId := make([]byte, 32)
	_, err = rand.Read(sessionId)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("while generating the session id, an error occured"))
		return
	}
	err = repo.SessionManager.saveSessionId(ctx, base64.URLEncoding.EncodeToString(sessionId), code.Int64()+1000)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("while saving the session id into redis, an error occured"))
		return
	}
	// now we created a structure that generate code and session id
	// also we saved code and session id in redis with timeout
	// the on the last row we send session id to client
	// but we did not send verification code to client via sms

	// in here sms should be sent via sms service

	w.WriteHeader(http.StatusOK)
	_ = json.NewEncoder(w).Encode(base64.URLEncoding.EncodeToString(sessionId))

}
