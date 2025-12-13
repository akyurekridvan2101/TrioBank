package internal

import (
	"context"
	"time"

	"go.mongodb.org/mongo-driver/bson/primitive"
)

type User struct {
	Id             primitive.ObjectID `bson:"_id, omitempty" json:"id"`
	UUID           string             `bson:"uuid" json:"uuid"`
	Name           string             `bson:"name" json:"name"`
	Surname        string             `bson:"surname" json:"surname"`
	HashedPassword string             `bson:"hashedPassword" json:"hashedPassword"`
	Email          string             `bson:"email" json:"email"`
	Tel            string             `bson:"tel" json:"tel"`
	Tc             string             `bson:"tc" json:"tc"`
	CreatedAt      time.Time          `bson:"createdAt" json:"createdAt"`
	IsActive       bool               `bson:"isActive" json:"isActive"`
}

type Tokens struct {
	Id        primitive.ObjectID `bson:"_id, omitempty" json:"id"`
	UserId    primitive.ObjectID `bson:"user_id" json:"userId"`
	UserUUID  string             `bson:"user_uuid" json:"userUuid"`
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
	isUserExist(ctx context.Context, tc string) error

	createRefreshAndAccessToken(ctx context.Context, id primitive.ObjectID, userUUID string) (string, string, error)
	createAccessToken(ctx context.Context, refreshToken string) (string, error)
	inActiveRefreshToken(ctx context.Context, refreshToken string) error
	isRefreshTokenExistAndActive(ctx context.Context, refreshToken string) (string, error)

	createUser(ctx context.Context, user User) error
	deleteUser(ctx context.Context, userId primitive.ObjectID) error
	getUserById(ctx context.Context, userId primitive.ObjectID) (User, error)
}

type SessionManagerI interface {
	saveSessionId(ctx context.Context, userId primitive.ObjectID, sessionId string, code int64) error
	controlSessionId(ctx context.Context, sessionId string, code int64) (primitive.ObjectID, error)
	deleteSessionId(ctx context.Context, sessionId string) error

	setAndControlLimitById(ctx context.Context, userId primitive.ObjectID) (bool, error)
	removeLimitById(ctx context.Context, userId primitive.ObjectID) error
	setAndControlLimitByTc(ctx context.Context, tc string) (bool, error)
	removeLimitByTc(ctx context.Context, tc string) error

	saveUser(ctx context.Context, user User) error
	getUser(ctx context.Context, userId primitive.ObjectID) (User, error)
}
