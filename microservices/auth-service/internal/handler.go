package internal

import (
	"bytes"
	"context"
	"crypto/rand"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"math/big"
	"net/http"
	"strconv"
	"time"

	"github.com/TrioBank/triobank-platform/microservices/auth-service/config"
)

type Repo struct {
	DataBase       DataBaseI
	SessionManager SessionManagerI
	Client         *http.Client
}

func (repo Repo) login(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		w.WriteHeader(http.StatusMethodNotAllowed)
		_, _ = w.Write([]byte("this endpoint can be used only with post request"))
		return
	}
	ctx, cancel := context.WithTimeout(r.Context(), 10*time.Second)
	defer cancel()

	var data loginData
	// in first the password will not be hashed, then we will hash it before write into the database

	// the control of request body
	if err := json.NewDecoder(r.Body).Decode(&data); err != nil || (data.Password == "" || data.Tc == "") {
		w.WriteHeader(http.StatusBadRequest)
		_, _ = w.Write([]byte("the request body could not be decoded or the request may do not include password or tc fields"))
		return
	}

	user, err := repo.DataBase.loginControl(ctx, data)
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
	// we did not send verification code to client via sms

	// in here sms should be sent via sms service
	var requestData SmsRequestData
	requestData.Receiver = user.Email
	requestData.Code = strconv.FormatInt(code.Int64()+1000, 10)
	requestDataJson, _ := json.Marshal(requestData)
	targetUrl := "http://" + config.GetEnv("MAIL_SERVICE_PORT") + "/send"
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, targetUrl, bytes.NewReader(requestDataJson))
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte(err.Error()))
		return
	}
	deadline, _ := ctx.Deadline()
	req.Header.Set("X-Request-Deadline", strconv.FormatInt(deadline.UnixMilli(), 10))
	req.Header.Set("X-Internal-Secret", config.GetEnv("SECRET_KEY"))
	response, err := repo.Client.Do(req)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("an error occured while sending a request to sms service"))
		return
	}
	defer func() {
		if err := response.Body.Close(); err == nil {
		} else {
			fmt.Println(err.Error(), "response could not be closed")
		}
	}()
	if response.Status != "200 OK" {
		w.WriteHeader(response.StatusCode)
		_, _ = w.Write([]byte("the sms could not sent"))
		return
	}
	w.WriteHeader(http.StatusOK)
	_ = json.NewEncoder(w).Encode(base64.URLEncoding.EncodeToString(sessionId))

}
