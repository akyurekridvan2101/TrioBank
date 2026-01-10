package pkg

import (
	"errors"
	"time"

	"github.com/TrioBank/triobank-platform/microservices/auth-service/config"
	"github.com/golang-jwt/jwt/v5"
	"github.com/google/uuid"
	"golang.org/x/crypto/bcrypt"
)

func GenerateUUID() string {
	return uuid.New().String()
}

func HashPassword(password string) (string, error) {
	temp, err := bcrypt.GenerateFromPassword([]byte(password), 10)
	if err != nil {
		return "", err
	}
	return string(temp), err
}

func HashedPasswordControl(password string, hashedPassword string) error {
	err := bcrypt.CompareHashAndPassword([]byte(hashedPassword), []byte(password))
	if err != nil {
		return err
	} else {
		return nil
	}
}

func CreateRefreshToken(userUUID string) (string, error) {
	claims := jwt.RegisteredClaims{
		Issuer:    "auth-service",
		Subject:   userUUID,
		ExpiresAt: &jwt.NumericDate{time.Now().Add(time.Hour * 7 * 24)},
	}

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	tokenString, err := token.SignedString([]byte(config.GetEnv("TOKEN_SIGNATURE")))
	if err != nil {
		return "", err
	}
	return tokenString, nil
}

func CreateAccessToken(userUUID string) (string, error) {
	claims := jwt.RegisteredClaims{
		Issuer:    "auth-service",
		Subject:   userUUID,
		ExpiresAt: &jwt.NumericDate{time.Now().Add(time.Minute * 15)},
	}

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	tokenString, err := token.SignedString([]byte(config.GetEnv("TOKEN_SIGNATURE")))
	if err != nil {
		return "", err
	}
	return tokenString, nil
}

var (
	ErrInvalidToken  = errors.New("invalid access token")
	ErrTokenExpired  = errors.New("token has expired")
	ErrInvalidIssuer = errors.New("invalid token issuer")
)

func ValidateAccessToken(accessToken string) (string, error) {
	token, err := jwt.ParseWithClaims(accessToken, &jwt.RegisteredClaims{}, func(token *jwt.Token) (interface{}, error) {
		if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, ErrInvalidToken
		}
		return []byte(config.GetEnv("TOKEN_SIGNATURE")), nil
	})

	if err != nil {
		if errors.Is(err, jwt.ErrTokenExpired) {
			return "", ErrTokenExpired
		}
		return "", ErrInvalidToken
	}

	claims, ok := token.Claims.(*jwt.RegisteredClaims)
	if !ok || !token.Valid {
		return "", ErrInvalidToken
	}

	if claims.Issuer != "auth-service" {
		return "", ErrInvalidIssuer
	}

	// UUID format kontrolü
	userUUID := claims.Subject
	if _, err := uuid.Parse(userUUID); err != nil {
		return "", ErrInvalidToken
	}

	return userUUID, nil
}
