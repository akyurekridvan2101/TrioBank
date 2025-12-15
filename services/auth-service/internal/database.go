package internal

import (
	"context"
	"fmt"
	"log"
	"time"

	"github.com/TrioBank/triobank-platform/microservices/auth-service/config"
	"github.com/TrioBank/triobank-platform/microservices/auth-service/pkg"
	"github.com/redis/go-redis/v9"
	"go.mongodb.org/mongo-driver/bson"
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

func (db MongoDB) loginControl(ctx context.Context, data loginData) error {
	var user User
	collection := db.Db.Collection("Users").FindOne(ctx, bson.M{"tc": data.Tc})
	err := collection.Decode(&user)
	if err != nil {
		return err
	}
	err = pkg.HashedPasswordControl(data.Password, user.HashedPassword)
	if err != nil {
		return err
	} else {
		return nil
	}
}

func (db RedisDB) saveSessionId(ctx context.Context, sessionId string, code int64) error {
	_, err := db.Db.Set(ctx, sessionId, code, time.Minute*3).Result()
	if err != nil {
		return err
	} else {
		return nil
	}
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
		Addr:       "localhost:6379",
		DB:         0,
		ClientName: "AuthServiceRedis",
	})
	pong, err := client.Ping(ctx).Result()
	if err != nil {
		log.Fatal("redis ping error: ", err.Error())
	} else {
		fmt.Println("redis is running", pong)
	}
	return client
} // redis'i baslatir
