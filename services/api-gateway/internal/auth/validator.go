package auth

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"github.com/TrioBank/triobank-platform/microservices/api-gateway/internal/cache"
)

type AuthsClient struct {
	AuthBaseUrl string
	HttpClient  *http.Client
	Redis       cache.RedisI
}

type ValidatorResponse struct {
	UserId string `json:"user_id"`
}

func NewAuthClient(baseUrl string, r cache.RedisI) *AuthsClient {
	authClient := &AuthsClient{
		AuthBaseUrl: baseUrl,
		HttpClient: &http.Client{
			Timeout: 8 * time.Second,
		},
		Redis: r,
	}
	return authClient
}

func (a *AuthsClient) ValidateToken(ctx context.Context, accessToken string) (string, error) {

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, a.AuthBaseUrl+"/auth/validation", nil)
	if err != nil {
		return "", err
	}

	req.Header.Set("Authorization", fmt.Sprintf("Bearer "+accessToken))

	response, err := a.HttpClient.Do(req)
	if err != nil {
		return "", err
	}
	defer response.Body.Close()

	if response.StatusCode == http.StatusOK {
		var responseData ValidatorResponse
		err = json.NewDecoder(response.Body).Decode(&responseData)
		if err != nil {
			return "", err
		}
		return responseData.UserId, nil
	}
	return "", fmt.Errorf("invalid token: status %d", response.StatusCode)
}
