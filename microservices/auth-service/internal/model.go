package internal

import (
	"context"
	"time"

	"go.mongodb.org/mongo-driver/bson/primitive"
)

type User struct {
	Id             primitive.ObjectID `bson:"_id, omitempty" json:"id"`
	Name           string             `bson:"name" json:"name"`
	Surname        string             `bson:"surname" json:"surname"`
	HashedPassword string             `bson:"hashedPassword" json:"-"`
	Email          string             `bson:"email" json:"email"`
	Tel            string             `bson:"tel" json:"tel"`
	Tc             string             `bson:"tc" json:"tc"`
	CreatedAt      time.Time          `bson:"createdAt" json:"createdAt"`
	IsActive       bool               `bson:"isActive" json:"isActive"`
}

type Tokens struct {
	Id        primitive.ObjectID `bson:"_id, omitempty" json:"id"`
	UserId    primitive.ObjectID `bson:"user_id" json:"userId"`
	Token     string             `bson:"token" json:"token"`
	CreatedAt time.Time          `bson:"createdAt" json:"createdAt"`
	ExpiredAt time.Time          `bson:"expiredAt" json:"expiredAt"`
	IsActive  bool               `bson:"isActive" json:"isActive"`
}
type tokensPair struct {
	RefreshToken string `json:"refresh_token"`
	AccessToken  string `json:"access_token"`
}
type RedisSessionData struct {
	Code   int64              `json:"code"`
	UserId primitive.ObjectID `json:"userId"`
}
type loginData struct {
	Tc       string `json:"tc"`
	Password string `json:"password"`
}
type smsRequestData struct {
	Receiver string `json:"receiver"`
	Code     string `json:"code"`
}
type confirmData struct {
	SessionId string `json:"session-id"`
	Code      string `json:"code"`
}

type DataBaseI interface {
	loginControl(ctx context.Context, data loginData) (User, error)
	createRefreshAndAccessToken(ctx context.Context, id primitive.ObjectID) (string, string, error)
}
type SessionManagerI interface {
	saveSessionId(ctx context.Context, userId primitive.ObjectID, sessionId string, code int64) error
	controlSessionId(ctx context.Context, sessionId string, code int64) (primitive.ObjectID, error)
	deleteSessionId(ctx context.Context, sessionId string) error
}
