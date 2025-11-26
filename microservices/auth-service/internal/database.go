package internal

import (
	"context"
	"log"
	"time"

	"github.com/TrioBank/triobank-platform/microservices/auth-service/config"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
	"go.mongodb.org/mongo-driver/mongo/readpref"
)

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
	}
	db := client.Database(config.GetEnv("MONGO_NAME"))
	return db
}
