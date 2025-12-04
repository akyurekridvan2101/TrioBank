package internal

import (
	"bytes"
	"context"
	"crypto/rand"
	"encoding/base64"
	"encoding/json"
	"errors"
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
	defer r.Body.Close()
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
	err = repo.SessionManager.saveSessionId(ctx, user.Id, base64.URLEncoding.EncodeToString(sessionId), code.Int64()+1000)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("while saving the session id into redis, an error occured"))
		return
	}

	var requestData smsRequestData
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

func (repo Repo) Confirm(w http.ResponseWriter, r *http.Request) {
	defer r.Body.Close()
	if r.Method != http.MethodPost {
		w.WriteHeader(http.StatusMethodNotAllowed)
		_, _ = w.Write([]byte("only post request is allowed"))
		return
	}
	var data confirmData
	err := json.NewDecoder(r.Body).Decode(&data)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		_, _ = w.Write([]byte("the request body should include only session id and verification code"))
		return
	}
	ctx, cancel := context.WithTimeout(r.Context(), time.Second*5)
	defer cancel()
	code, err := strconv.ParseInt(data.Code, 10, 64)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		_, _ = w.Write([]byte("code type is not correct"))
		return
	}
	userId, err := repo.SessionManager.controlSessionId(ctx, data.SessionId, code)
	if err != nil {
		if errors.Is(err, ErrCodeIsNotCorrect) {
			w.WriteHeader(http.StatusUnauthorized)
			_, _ = w.Write([]byte("the code is not correct or session id invalid"))
			return
		} else if errors.Is(err, ErrCodeNotFound) {
			w.WriteHeader(http.StatusUnauthorized)
			_, _ = w.Write([]byte("the code is not found"))
			return
		}
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("something went wrong in server"))
		return
	}
	// access and refresh token will be created and stored
	refreshToken, accessToken, err := repo.DataBase.createRefreshAndAccessToken(ctx, userId)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("something went wrong in creating refresh and access token"))
		return
	}
	err = repo.SessionManager.deleteSessionId(ctx, data.SessionId)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("deleting session data in redis rised an error"))
		return
	}
	var tokens tokensPair
	tokens.AccessToken = accessToken
	tokens.RefreshToken = refreshToken

	cookie := http.Cookie{
		Name:     "Refresh-Token",
		Value:    refreshToken,
		Secure:   true,
		HttpOnly: true,
		Path:     "/",
		MaxAge:   7 * 24 * 60 * 60,
	}

	ret, err := json.Marshal(struct {
		AccessToken string `json:"access_token"`
	}{tokens.AccessToken})
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("something went wrong in sending access token"))
		return
	}

	http.SetCookie(w, &cookie)
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write(ret)
}
