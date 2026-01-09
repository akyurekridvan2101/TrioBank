package internal

import (
	"context"
	"crypto/rand"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"math/big"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/TrioBank/triobank-platform/microservices/auth-service/pkg"
	"github.com/segmentio/kafka-go"
	"go.mongodb.org/mongo-driver/bson/primitive"
)

type Repo struct {
	DataBase       DataBaseI
	SessionManager SessionManagerI
	Client         *http.Client
	Producer       *kafka.Writer
}

func (repo Repo) login(w http.ResponseWriter, r *http.Request) {
	defer r.Body.Close()
	if r.Method != http.MethodPost {
		w.WriteHeader(http.StatusMethodNotAllowed)
		_, _ = w.Write([]byte("this endpoint can be used only with post request"))
		return
	}
	ctx, cancel := context.WithTimeout(r.Context(), 20*time.Second)
	defer cancel()

	var data loginData
	// Şifre ilk başta plain text gelir, veritabanına yazmadan önce hash'leyeceğiz

	// İstek gövdesini kontrol et
	if err := json.NewDecoder(r.Body).Decode(&data); err != nil || (data.Password == "" || data.Tc == "") {
		w.WriteHeader(http.StatusBadRequest)
		_, _ = w.Write([]byte("the request body could not be decoded or the request may do not include password or tc fields"))
		return
	}

	// TC'yi temizle
	data.Tc = strings.TrimSpace(data.Tc)

	user, err := repo.DataBase.loginControl(ctx, data)
	if err != nil {
		w.WriteHeader(http.StatusUnauthorized)
		_, _ = w.Write([]byte("the login inputs are invalid"))
		return
	}

	result, err := repo.SessionManager.setAndControlLimitById(ctx, user.Id)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
	if !result {
		w.WriteHeader(http.StatusTooManyRequests)
		_, _ = w.Write([]byte("already there is an active process in redis"))
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
	// Manuel test için doğrulama kodunu logluyoruz
	fmt.Printf("LOGIN Verification Code: %d\n", code.Int64()+1000)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("while saving the session id into redis, an error occured"))
		return
	}

	var requestData smsRequestData
	requestData.Receiver = user.Email
	requestData.Code = strconv.FormatInt(code.Int64()+1000, 10)

	errChannel := make(chan error)
	go SendMail(ctx, repo, requestData, errChannel)
	select {
	case x := <-errChannel:
		if x != nil {
			err = repo.SessionManager.removeLimitById(ctx, user.Id)
			if err != nil {
				w.WriteHeader(http.StatusInternalServerError)
				fmt.Print(x.Error())
				_, _ = w.Write([]byte("something went wrong while deleting limit in redis"))
				return
			}
			w.WriteHeader(http.StatusInternalServerError)
			fmt.Print(x.Error())
			_, _ = w.Write([]byte("something went wrong while sending sms"))
			return
		}
	case <-ctx.Done():
		_ = repo.SessionManager.removeLimitById(context.Background(), user.Id)
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("timeout error occured"))
		return
	}

	w.WriteHeader(http.StatusOK)
	_ = json.NewEncoder(w).Encode(map[string]string{
		"sessionId": base64.URLEncoding.EncodeToString(sessionId),
	})
}

func (repo Repo) LoginConfirm(w http.ResponseWriter, r *http.Request) {
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
	// UUID almak için kullanıcıyı DB'den çekiyoruz
	user, err := repo.DataBase.getUserById(ctx, userId)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("could not find user"))
		return
	}
	// Token çiftini oluşturup saklayalım
	refreshToken, accessToken, err := repo.DataBase.createRefreshAndAccessToken(ctx, userId, user.UUID)
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
	err = repo.SessionManager.removeLimitById(ctx, userId)
	if err != nil {
		fmt.Println("removing limit error in confirm ednpoint", err)
	}
	http.SetCookie(w, &cookie)
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write(ret)
}

func (repo Repo) Register(w http.ResponseWriter, r *http.Request) {
	defer r.Body.Close()
	if r.Method != http.MethodPost {
		w.WriteHeader(http.StatusMethodNotAllowed)
		return
	}
	var user User
	var data struct {
		Name     string `json:"name"`
		Surname  string `json:"surname"`
		Email    string `json:"email"`
		Password string `json:"password"`
		Tel      string `json:"tel"`
		Tc       string `json:"tc"`
	}
	err := json.NewDecoder(r.Body).Decode(&data)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		return
	}

	// Girdi verilerini standardize edelim (email küçük harf vs)
	data.Tc = strings.TrimSpace(data.Tc)
	data.Email = strings.TrimSpace(strings.ToLower(data.Email))
	data.Name = strings.TrimSpace(data.Name)
	data.Surname = strings.TrimSpace(data.Surname)
	data.Tel = strings.TrimSpace(data.Tel)

	user.Id = primitive.NewObjectID()
	user.UUID = pkg.GenerateUUID()
	user.Name = data.Name
	user.Surname = data.Surname
	user.Email = data.Email
	user.Tel = data.Tel
	user.Tc = data.Tc
	user.IsActive = true
	user.CreatedAt = time.Now()
	hashedPassword, err := pkg.HashPassword(data.Password)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
	user.HashedPassword = hashedPassword
	ctx, cancel := context.WithTimeout(r.Context(), time.Second*5)
	defer cancel()

	result, err := repo.SessionManager.setAndControlLimitByTc(ctx, user.Tc)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
	if !result {
		w.WriteHeader(http.StatusTooManyRequests)
		_, _ = w.Write([]byte("already there is an active process in redis"))
		return
	}

	err = repo.DataBase.isUserExist(ctx, user.Tc)
	if errors.Is(err, ErrUserAlreadyExist) {
		w.WriteHeader(http.StatusConflict)
		_, _ = w.Write([]byte("the user already exist"))
		return
	} else if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("something went wrong in server"))
		return
	}

	sessionId := make([]byte, 32)
	_, err = rand.Read(sessionId)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("something went wrong in server"))
		return
	}
	err = repo.SessionManager.saveUser(ctx, user)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("something went wrong while saving user into redis"))
		return
	}

	code, err := rand.Int(rand.Reader, big.NewInt(9000))
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("something went wrong while creating verification code"))
		return
	}

	err = repo.SessionManager.saveSessionId(ctx, user.Id, base64.URLEncoding.EncodeToString(sessionId), code.Int64()+1000)
	// Manuel test için doğrulama kodunu logluyoruz
	fmt.Printf("REGISTER Verification Code: %d\n", code.Int64()+1000)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("something went wrong while saving session id"))
		return
	}
	reqData := smsRequestData{user.Email, strconv.FormatInt(code.Int64()+1000, 10)}
	channel := make(chan error, 1)
	go SendMail(ctx, repo, reqData, channel)
	select {
	case <-ctx.Done():
		_ = repo.SessionManager.removeLimitByTc(context.Background(), user.Tc)
		w.WriteHeader(http.StatusRequestTimeout)
		_, _ = w.Write([]byte("timeout"))
		return
	case x := <-channel:
		if x != nil {
			err = repo.SessionManager.removeLimitByTc(ctx, user.Tc)
			if err != nil {
				w.WriteHeader(http.StatusInternalServerError)
				fmt.Print(x.Error())
				_, _ = w.Write([]byte("something went wrong while deleting limit in redis"))
				return
			}
			w.WriteHeader(http.StatusInternalServerError)
			fmt.Print(x.Error())
			_, _ = w.Write([]byte("something went wrong while sending sms"))
			return
		}
	}
	w.WriteHeader(http.StatusOK)
	_ = json.NewEncoder(w).Encode(map[string]string{
		"sessionId": base64.URLEncoding.EncodeToString(sessionId),
	})
}

func (repo Repo) RegisterConfirm(w http.ResponseWriter, r *http.Request) {
	defer r.Body.Close()

	if r.Method != http.MethodPost {
		w.WriteHeader(http.StatusMethodNotAllowed)
		return
	}
	var data confirmData
	err := json.NewDecoder(r.Body).Decode(&data)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		_, _ = w.Write([]byte("the request body should include only session id and verification code"))
		return
	}
	ctx, cancel := context.WithTimeout(r.Context(), time.Minute*5)
	defer cancel()
	code, err := strconv.ParseInt(data.Code, 10, 64)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
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
	user, err := repo.SessionManager.getUser(ctx, userId)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("something went wrong in server"))
		return
	}

	err = repo.DataBase.createUser(ctx, user)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("something went wrong while creating user in db"))
		return
	}

	refreshToken, accessToken, err := repo.DataBase.createRefreshAndAccessToken(ctx, user.Id, user.UUID)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("something went wrong while creating tokens"))
		return
	}
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
	}{accessToken})
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("something went wrong in sending access token"))
		return
	}

	// Kafka event'i gönder (marshalling fonksiyon içinde halledilir)
	err = SendKafkaEvent(repo.Producer, ctx, user.UUID, user, "UserCreated", "UserCreated")
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("UserCreated event could not be sent"))
		return
	}

	err = repo.SessionManager.removeLimitByTc(ctx, user.Tc)
	if err != nil {
		fmt.Println(err.Error())
	}
	err = repo.SessionManager.deleteSessionId(ctx, data.SessionId)
	if err != nil {
		fmt.Println(err.Error())
	}
	http.SetCookie(w, &cookie)
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write(ret)

}

func (repo Repo) Logout(w http.ResponseWriter, r *http.Request) {
	defer r.Body.Close()

	if r.Method != http.MethodPost {
		w.WriteHeader(http.StatusMethodNotAllowed)
		return
	}

	http.SetCookie(w, &http.Cookie{
		Name:     "Refresh-Token",
		Value:    "",
		Path:     "/",
		MaxAge:   -1,
		HttpOnly: true,
		Secure:   true,
	})

	http.SetCookie(w, &http.Cookie{
		Name:     "Access-Token",
		Value:    "",
		Path:     "/",
		MaxAge:   -1,
		HttpOnly: true,
		Secure:   true,
	})

	if c, err := r.Cookie("Refresh-Token"); err == nil {

		ctx, cancel := context.WithTimeout(r.Context(), time.Second*3)
		defer cancel()

		err = repo.DataBase.inActiveRefreshToken(ctx, c.Value)
		fmt.Println(err)
	} else {
		fmt.Println(err)
	}
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write([]byte("logged out successfully"))
}

func (repo Repo) RefreshAccessToken(w http.ResponseWriter, r *http.Request) {
	defer r.Body.Close()

	if r.Method != http.MethodPost {
		w.WriteHeader(http.StatusMethodNotAllowed)
		return
	}
	ctx, cancel := context.WithTimeout(r.Context(), time.Second*3)
	defer cancel()
	c, err := r.Cookie("Refresh-Token")
	if err != nil {
		w.WriteHeader(http.StatusUnauthorized)
		_, _ = w.Write([]byte("the refresh code could not be found"))
		return
	}

	accessToken, err := repo.DataBase.createAccessToken(ctx, c.Value)
	if err != nil {
		w.WriteHeader(http.StatusUnauthorized)
		_, _ = w.Write([]byte("the refresh code is not valid"))
		return
	}
	res, err := json.Marshal(struct {
		AccessToken string `json:"access_token"`
	}{AccessToken: accessToken})
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("something went wrong while converting access token into json"))
		return
	}

	w.WriteHeader(http.StatusOK)
	_, _ = w.Write(res)
}

func (repo Repo) TokenValidation(w http.ResponseWriter, r *http.Request) {
	defer r.Body.Close()

	if r.Method != http.MethodPost {
		w.WriteHeader(http.StatusMethodNotAllowed)
		return
	}

	authorizationHeader := r.Header.Get("Authorization")
	if authorizationHeader == "" {
		w.WriteHeader(http.StatusBadRequest)
		_, _ = w.Write([]byte("authorization header is required"))
		return
	}

	if !strings.HasPrefix(authorizationHeader, "Bearer ") {
		w.WriteHeader(http.StatusBadRequest)
		_, _ = w.Write([]byte("invalid authorization header format"))
		return
	}
	accessToken := strings.TrimPrefix(authorizationHeader, "Bearer ")

	userUUID, err := pkg.ValidateAccessToken(accessToken)
	if err != nil {
		if errors.Is(err, pkg.ErrTokenExpired) {
			w.WriteHeader(http.StatusUnauthorized)
			_, _ = w.Write([]byte("token has expired"))
			return
		}
		w.WriteHeader(http.StatusUnauthorized)
		_, _ = w.Write([]byte("invalid access token"))
		return
	}
	res, err := json.Marshal(struct {
		UserId string `json:"user_id"`
	}{
		UserId: userUUID,
	})
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("internal server error"))
		return
	}

	w.WriteHeader(http.StatusOK)
	_, _ = w.Write(res)
}

func (repo Repo) ChangePassword(w http.ResponseWriter, r *http.Request) {
	defer r.Body.Close()

	if r.Method != http.MethodPost {
		w.WriteHeader(http.StatusMethodNotAllowed)
		return
	}

	token := r.Header.Get("Authorization")
	if token == "" {
		w.WriteHeader(http.StatusBadRequest)
		return
	}

	ctx, cancel := context.WithTimeout(r.Context(), time.Second*3)
	defer cancel()

	if !strings.HasPrefix(token, "Bearer ") {
		w.WriteHeader(http.StatusUnauthorized)
		_, _ = w.Write([]byte("invalid access token"))
		return
	}

	AccessToken := strings.TrimPrefix(token, "Bearer ")

	userUuid, err := pkg.ValidateAccessToken(AccessToken)
	if err != nil {
		w.WriteHeader(http.StatusUnauthorized)
		_, _ = w.Write([]byte("invalid access token"))
		return
	}
	requestData := struct {
		OldPassword string `json:"old_password"`
		NewPassword string `json:"new_password"`
	}{}
	err = json.NewDecoder(r.Body).Decode(&requestData)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		_, _ = w.Write([]byte("old password and new password required"))
		return
	}
	if requestData.NewPassword == "" || requestData.OldPassword == "" {
		w.WriteHeader(http.StatusBadRequest)
		_, _ = w.Write([]byte("the struct of passwords are invalid"))
		return
	}
	if requestData.NewPassword == requestData.OldPassword {
		w.WriteHeader(http.StatusBadRequest)
		_, _ = w.Write([]byte("the new and old passwords are same"))
		return
	}

	userId, err := repo.DataBase.validateUserPassword(ctx, userUuid, requestData.OldPassword)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		_, _ = w.Write([]byte("old password invalid"))
		return
	}
	NewHashedPassword, err := pkg.HashPassword(requestData.NewPassword)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
	result, err := repo.SessionManager.setAndControlLimitById(ctx, userId)
	if !result {
		w.WriteHeader(http.StatusTooManyRequests)
		return
	}

	defer repo.SessionManager.removeLimitById(ctx, userId)

	err = repo.DataBase.updatePassword(ctx, userId, NewHashedPassword)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write([]byte("password updated successfully"))

}

func (repo Repo) DeleteAccount(w http.ResponseWriter, r *http.Request) {
	defer r.Body.Close()

	if r.Method != http.MethodDelete {
		w.WriteHeader(http.StatusMethodNotAllowed)
		return
	}
	c, err := r.Cookie("Refresh-Token")
	if err != nil {
		w.WriteHeader(http.StatusUnauthorized)
		_, _ = w.Write([]byte("there is no refresh token in request"))
		return
	}

	token := r.Header.Get("Authorization")
	if !strings.HasPrefix(token, "Bearer ") {
		w.WriteHeader(http.StatusUnauthorized)
		_, _ = w.Write([]byte("the authorization header is not appropriate"))
		return
	}
	AccessToken := strings.TrimPrefix(token, "Bearer ")

	userUuid, err := pkg.ValidateAccessToken(AccessToken)
	if err != nil {
		w.WriteHeader(http.StatusUnauthorized)
		_, _ = w.Write([]byte("access token is not valid"))
		return
	}
	ctx, cancel := context.WithTimeout(r.Context(), time.Second*3)
	defer cancel()
	request := struct {
		Password string `json:"password"`
	}{}
	err = json.NewDecoder(r.Body).Decode(&request)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		_, _ = w.Write([]byte("request could not be decoded"))
		return
	}
	userId, err := repo.DataBase.validateUserPassword(ctx, userUuid, request.Password)
	if err != nil {
		w.WriteHeader(http.StatusUnauthorized)
		_, _ = w.Write([]byte("password is not valid"))
		return
	}

	err = repo.DataBase.inActiveRefreshToken(ctx, c.Value)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("something went wrong while inactivate refresh token"))
		return
	}
	user, err := repo.DataBase.getUserById(ctx, userId)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("an error occured while getting user by id"))
		return
	}

	err = repo.DataBase.deleteUser(ctx, userId)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("an error occured while deleting user"))
		return
	}
	cookie := http.Cookie{
		Name:     "Refresh-Token",
		Value:    "",
		Secure:   true,
		HttpOnly: true,
		Path:     "/",
		MaxAge:   -1,
	}

	userJson, err := json.Marshal(&user)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
	err = SendKafkaEvent(repo.Producer, ctx, userUuid, userJson, "UserDeleted", "UserDeleted")
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("user deleted event could not sent to kafka server"))
		return
	}

	http.SetCookie(w, &cookie)
	w.WriteHeader(http.StatusOK)
	return
}

func (repo Repo) UpdateUserContact(w http.ResponseWriter, r *http.Request) {
	defer r.Body.Close()

	if r.Method != http.MethodPost && r.Method != http.MethodPut {
		w.WriteHeader(http.StatusMethodNotAllowed)
		return
	}

	token := r.Header.Get("Authorization")
	if token == "" {
		w.WriteHeader(http.StatusBadRequest)
		return
	}

	ctx, cancel := context.WithTimeout(r.Context(), time.Second*5)
	defer cancel()

	if !strings.HasPrefix(token, "Bearer ") {
		w.WriteHeader(http.StatusUnauthorized)
		return
	}

	AccessToken := strings.TrimPrefix(token, "Bearer ")

	userUuid, err := pkg.ValidateAccessToken(AccessToken)
	if err != nil {
		w.WriteHeader(http.StatusUnauthorized)
		return
	}

	var data updateData
	if err := json.NewDecoder(r.Body).Decode(&data); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		return
	}

	userId, err := repo.DataBase.getUserIdByUUID(ctx, userUuid)
	if err != nil {
		w.WriteHeader(http.StatusNotFound)
		return
	}

	// Güncelleme öncesi email ve telefonu temizle
	data.Email = strings.TrimSpace(strings.ToLower(data.Email))
	data.Phone = strings.TrimSpace(data.Phone)

	err = repo.DataBase.updateUserContact(ctx, userId, data.Email, data.Phone)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	_, _ = w.Write([]byte("user contact updated"))
}

// ForgotPasswordInitiate şifre sıfırlama sürecini başlatır
func (repo Repo) ForgotPasswordInitiate(w http.ResponseWriter, r *http.Request) {
	defer r.Body.Close()
	if r.Method != http.MethodPost {
		w.WriteHeader(http.StatusMethodNotAllowed)
		_, _ = w.Write([]byte("this endpoint can be used only with post request"))
		return
	}
	ctx, cancel := context.WithTimeout(r.Context(), 20*time.Second)
	defer cancel()

	var data struct {
		Tc    string `json:"tc"`
		Email string `json:"email"`
	}

	if err := json.NewDecoder(r.Body).Decode(&data); err != nil || data.Tc == "" || data.Email == "" {
		w.WriteHeader(http.StatusBadRequest)
		_, _ = w.Write([]byte("the request body could not be decoded or the request may not include tc or email fields"))
		return
	}

	// Trim TC and Email before verification
	data.Tc = strings.TrimSpace(data.Tc)
	data.Email = strings.TrimSpace(data.Email)

	// Verify TC and Email match
	user, err := repo.DataBase.verifyUserEmail(ctx, data.Tc, data.Email)
	if err != nil {
		if err.Error() == "email does not match" {
			w.WriteHeader(http.StatusUnauthorized)
			_, _ = w.Write([]byte("tc and email do not match"))
			return
		}
		w.WriteHeader(http.StatusNotFound)
		_, _ = w.Write([]byte("user not found"))
		return
	}

	// Rate limiting by TC
	result, err := repo.SessionManager.setAndControlLimitByTc(ctx, user.Tc)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
	if !result {
		w.WriteHeader(http.StatusTooManyRequests)
		_, _ = w.Write([]byte("already there is an active process in redis"))
		return
	}

	// Generate verification code
	code, err := rand.Int(rand.Reader, big.NewInt(9000))
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("while generating the code, an error occured"))
		return
	}

	// Generate session ID
	sessionId := make([]byte, 32)
	_, err = rand.Read(sessionId)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("while generating the session id, an error occured"))
		return
	}

	// Save session to Redis
	err = repo.SessionManager.saveSessionId(ctx, user.Id, base64.URLEncoding.EncodeToString(sessionId), code.Int64()+1000)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("while saving the session id into redis, an error occured"))
		return
	}

	// Send email with code
	var requestData smsRequestData
	requestData.Receiver = user.Email
	requestData.Code = strconv.FormatInt(code.Int64()+1000, 10)

	// LOGGING FOR MANUAL VERIFICATION
	fmt.Printf("FORGOT PASSWORD Verification Code: %d\n", code.Int64()+1000)

	errChannel := make(chan error)
	go SendMail(ctx, repo, requestData, errChannel)
	select {
	case x := <-errChannel:
		if x != nil {
			err = repo.SessionManager.removeLimitByTc(ctx, user.Tc)
			if err != nil {
				w.WriteHeader(http.StatusInternalServerError)
				fmt.Print(x.Error())
				_, _ = w.Write([]byte("something went wrong while deleting limit in redis"))
				return
			}
			w.WriteHeader(http.StatusInternalServerError)
			fmt.Print(x.Error())
			_, _ = w.Write([]byte("something went wrong while sending email"))
			return
		}
	case <-ctx.Done():
		_ = repo.SessionManager.removeLimitByTc(context.Background(), user.Tc)
		w.WriteHeader(http.StatusRequestTimeout)
		_, _ = w.Write([]byte("timeout error occured"))
		return
	}

	w.WriteHeader(http.StatusOK)
	_ = json.NewEncoder(w).Encode(map[string]string{
		"sessionId": base64.URLEncoding.EncodeToString(sessionId),
	})
}

// ForgotPasswordVerifyCode verifies the code for password reset
func (repo Repo) ForgotPasswordVerifyCode(w http.ResponseWriter, r *http.Request) {
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

	// Verify code
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

	// Verify user exists
	_, err = repo.DataBase.getUserById(ctx, userId)
	if err != nil {
		w.WriteHeader(http.StatusNotFound)
		_, _ = w.Write([]byte("user not found"))
		return
	}

	w.WriteHeader(http.StatusOK)
	_ = json.NewEncoder(w).Encode(map[string]interface{}{
		"verified":  true,
		"sessionId": data.SessionId,
	})
}

// ForgotPasswordReset resets the password
func (repo Repo) ForgotPasswordReset(w http.ResponseWriter, r *http.Request) {
	defer r.Body.Close()
	if r.Method != http.MethodPost {
		w.WriteHeader(http.StatusMethodNotAllowed)
		_, _ = w.Write([]byte("only post request is allowed"))
		return
	}

	var data struct {
		SessionId   string `json:"session-id"`
		Code        string `json:"code"`
		NewPassword string `json:"new_password"`
	}

	err := json.NewDecoder(r.Body).Decode(&data)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		_, _ = w.Write([]byte("the request body could not be decoded"))
		return
	}

	if data.NewPassword == "" || len(data.NewPassword) < 8 {
		w.WriteHeader(http.StatusBadRequest)
		_, _ = w.Write([]byte("new password is required and must be at least 8 characters"))
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

	// Verify code again (security measure)
	userId, err := repo.SessionManager.controlSessionId(ctx, data.SessionId, code)
	if err != nil {
		if errors.Is(err, ErrCodeIsNotCorrect) {
			w.WriteHeader(http.StatusUnauthorized)
			_, _ = w.Write([]byte("the code is not correct or session id invalid"))
			return
		} else if errors.Is(err, ErrCodeNotFound) {
			w.WriteHeader(http.StatusUnauthorized)
			_, _ = w.Write([]byte("the code is not found or expired"))
			return
		}
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("something went wrong in server"))
		return
	}

	// Get user to verify existence
	user, err := repo.DataBase.getUserById(ctx, userId)
	if err != nil {
		w.WriteHeader(http.StatusNotFound)
		_, _ = w.Write([]byte("user not found"))
		return
	}

	// Hash new password
	newHashedPassword, err := pkg.HashPassword(data.NewPassword)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("something went wrong while hashing password"))
		return
	}

	// Update password
	err = repo.DataBase.updatePassword(ctx, userId, newHashedPassword)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		_, _ = w.Write([]byte("something went wrong while updating password"))
		return
	}

	// Deactivate all refresh tokens for security (user should login again on all devices)
	err = repo.DataBase.deactivateAllRefreshTokens(ctx, userId)
	if err != nil {
		// Log error but don't fail the request
		fmt.Printf("Warning: could not deactivate refresh tokens: %v\n", err)
	}

	// Delete session from Redis
	err = repo.SessionManager.deleteSessionId(ctx, data.SessionId)
	if err != nil {
		// Log error but don't fail the request
		fmt.Printf("Warning: could not delete session: %v\n", err)
	}

	// Remove rate limit
	err = repo.SessionManager.removeLimitByTc(ctx, user.Tc)
	if err != nil {
		// Log error but don't fail the request
		fmt.Printf("Warning: could not remove rate limit: %v\n", err)
	}

	w.WriteHeader(http.StatusOK)
	_, _ = w.Write([]byte("password reset successfully"))
}

func Health(w http.ResponseWriter, r *http.Request) {
	defer r.Body.Close()
	w.WriteHeader(http.StatusOK)
}
