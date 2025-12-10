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
	collection := db.Db.Collection("Tokens")
	_, err := collection.UpdateMany(ctx, bson.M{"user_id": userId}, bson.M{"$set": bson.M{"isActive": false}})
	if err != nil {
		return "", "", err
	}

	var refresh Tokens
	refresh.Id = primitive.NewObjectID()
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

	_, err = collection.InsertOne(ctx, refresh)
	if err != nil {
		return "", "", err
	}
	return refreshToken, accessToken, nil
}
func (db MongoDB) isUserExist(ctx context.Context, tc string) error {
	collection := db.Db.Collection("Users")
	result := collection.FindOne(ctx, bson.M{"tc": tc})
	if result == nil {
		return ErrUserAlreadyExist
	}
	return nil
}

func (db RedisDB) saveSessionId(ctx context.Context, userId primitive.ObjectID, sessionId string, code int64) error {
	var value RedisSessionData
	value.UserId = userId
	value.Code = code

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
func (db RedisDB) deleteSessionId(ctx context.Context, sessionId string) error {
	_, err := db.Db.Del(ctx, sessionId).Result()
	if err != nil {
		return err
	}
	return nil
}
func (db RedisDB) controlSessionId(ctx context.Context, sessionId string, code int64) (primitive.ObjectID, error) {
	var value RedisSessionData
	respond := db.Db.Get(ctx, sessionId)
	result, err := respond.Bytes()
	if errors.Is(err, redis.Nil) {
		return primitive.ObjectID{}, ErrCodeNotFound
	}
	if err != nil {
		return primitive.ObjectID{}, err
	}
	err = json.Unmarshal(result, &value)
	if err != nil {
		return primitive.ObjectID{}, err
	}
	if value.Code != code {
		return primitive.ObjectID{}, ErrCodeIsNotCorrect
	}
	return value.UserId, nil
}
func (db RedisDB) setAndControlLimitById(ctx context.Context, userId primitive.ObjectID) (bool, error) {
	key := "limit:" + userId.String()
	result, err := db.Db.SetNX(ctx, key, "limited", 3*time.Minute).Result()
	return result, err
}
func (db RedisDB) setAndControlLimitByTc(ctx context.Context, tc string) (bool, error) {
	key := "limit:" + tc
	result, err := db.Db.SetNX(ctx, key, "limited", 3*time.Minute).Result()
	return result, err
}

func (db RedisDB) removeLimitById(ctx context.Context, userId primitive.ObjectID) error {
	key := "limit:" + userId.String()
	_, err := db.Db.Del(ctx, key).Result()
	if err != nil {
		return err
	}
	return nil
}
func (db RedisDB) removeLimitByTc(ctx context.Context, tc string) error {
	key := "limit:" + tc
	_, err := db.Db.Del(ctx, key).Result()
	if err != nil {
		return err
	}
	return nil
}

func (db RedisDB) saveUser(ctx context.Context, user User, sessionId string) error {
	key := "user:" + sessionId
	value, err := json.Marshal(user)
	if err != nil {
		return err
	}
	_, err = db.Db.Set(ctx, key, value, 4*time.Minute).Result()
	if err != nil {
		return err
	}
	return nil
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
