package internal

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"log"
	"time"

	"github.com/TrioBank/triobank-platform/microservices/auth-service/config"
	"github.com/TrioBank/triobank-platform/microservices/auth-service/pkg"
	"github.com/redis/go-redis/v9"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
	"go.mongodb.org/mongo-driver/mongo/readpref"
)

type MongoDB struct {
	Db *mongo.Database
}
type RedisDB struct {
	Db *redis.Client
}

func (db MongoDB) loginControl(ctx context.Context, data loginData) (User, error) {
	var user User
	collection := db.Db.Collection("Users").FindOne(ctx, bson.M{"tc": data.Tc})
	err := collection.Decode(&user)
	if err != nil {
		return user, err
	}
	err = pkg.HashedPasswordControl(data.Password, user.HashedPassword)
	if err != nil {
		return user, err
	} else {
		return user, nil
	}
}

func (db MongoDB) createRefreshAndAccessToken(ctx context.Context, userId primitive.ObjectID) (string, string, error) {
	var refresh Tokens
	refresh.UserId = userId
	refresh.IsActive = true
	refresh.CreatedAt = time.Now()
	refresh.ExpiredAt = time.Now().Add(time.Hour * 7 * 24)

	refreshToken, err := pkg.CreateRefreshToken(userId)
	if err != nil {
		return "", "", err
	}
	refresh.Token = refreshToken

	accessToken, err := pkg.CreateAccessToken(userId)
	if err != nil {
		return "", "", err
	}
	collection := db.Db.Collection("Tokens")
	_, err = collection.InsertOne(ctx, refresh)
	if err != nil {
		return "", "", err
	}
	return refreshToken, accessToken, nil
}

func (db RedisDB) saveSessionId(ctx context.Context, userId primitive.ObjectID, sessionId string, code int64) error {
	value := struct {
		Code   int64
		UserId primitive.ObjectID
	}{Code: code, UserId: userId}

	v, err := json.Marshal(value)
	if err != nil {
		return err
	}
	_, err = db.Db.Set(ctx, sessionId, v, time.Minute*3).Result()
	if err != nil {
		return err
	} else {
		return nil
	}
}
func (db RedisDB) controlSessionId(ctx context.Context, sessionId string, code int64) (primitive.ObjectID, error) {
	value := struct {
		Code   int64
		UserId primitive.ObjectID
	}{}
	respond := db.Db.Get(ctx, sessionId)
	result, err := respond.Bytes()
	if err != nil && !errors.Is(err, redis.Nil) {
		return primitive.ObjectID{}, err
	} else if errors.Is(err, redis.Nil) {
		return [12]byte{}, ErrCodeExpired
	}
	err = json.Unmarshal(result, &value)
	if err != nil {
		return [12]byte{}, err
	}
	if value.Code != code {
		return [12]byte{}, ErrCodeDoesntMatched
	}
	return value.UserId, err

}

func StartMongoDB() *mongo.Database {
	ctx, cancel := context.WithTimeout(context.Background(), time.Second*3)
	defer cancel()
	client, err := mongo.Connect(ctx, options.Client().ApplyURI(config.GetEnv("MONGO_URI")))
	if err != nil {
		log.Fatal("mongodb connection error: ", err.Error())
	}
	err = client.Ping(context.Background(), readpref.Primary())
	if err != nil {
		log.Fatal("mongodb ping error: ", err.Error())
	} else {
		fmt.Println("mongodb is running")
	}
	db := client.Database(config.GetEnv("MONGO_NAME"))
	return db
} // mongo database'i baslatir
func StartRedisDB() *redis.Client {
	ctx, cancel := context.WithTimeout(context.Background(), time.Second*3)
	defer cancel()
	client := redis.NewClient(&redis.Options{
		Addr:       config.GetEnv("REDIS_PORT"),
		DB:         0,
		ClientName: config.GetEnv("REDIS_NAME"),
	})
	pong, err := client.Ping(ctx).Result()
	if err != nil {
		log.Fatal("redis ping error: ", err.Error())
	} else {
		fmt.Println("redis is running", pong)
	}
	return client
} // redis'i baslatir
