package pkg

import (
	"time"

	"github.com/TrioBank/triobank-platform/microservices/auth-service/config"
	"github.com/golang-jwt/jwt/v5"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"golang.org/x/crypto/bcrypt"
)

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

func CreateRefreshToken(userId primitive.ObjectID) (string, error) {
	claims := jwt.RegisteredClaims{
		Issuer:    "auth-service",
		Subject:   userId.String(),
		ExpiresAt: &jwt.NumericDate{time.Now().Add(time.Hour * 7 * 24)},
	}

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	tokenString, err := token.SignedString([]byte(config.GetEnv("TOKEN_SIGNATURE")))
	if err != nil {
		return "", err
	}
	return tokenString, nil
}

func CreateAccessToken(userId primitive.ObjectID) (string, error) {
	claims := jwt.RegisteredClaims{
		Issuer:    "auth-service",
		Subject:   userId.String(),
		ExpiresAt: &jwt.NumericDate{time.Now().Add(time.Minute * 15)},
	}

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	tokenString, err := token.SignedString([]byte(config.GetEnv("TOKEN_SIGNATURE")))
	if err != nil {
		return "", err
	}
	return tokenString, nil
}
