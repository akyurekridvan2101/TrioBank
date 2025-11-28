package internal

import (
	"context"
	"fmt"
	"log"
	"time"

	"github.com/TrioBank/triobank-platform/microservices/auth-service/config"
	"github.com/redis/go-redis/v9"
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

func (db MongoDB) X() {

}
func (db RedisDB) Y() {

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
