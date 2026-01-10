package main

import (
	"log"
	"net/http"
	"os"
	"time"

	"github.com/TrioBank/triobank-platform/microservices/auth-service/config"
	"github.com/TrioBank/triobank-platform/microservices/auth-service/internal"
	"github.com/segmentio/kafka-go"
)

func init() {
	config.LoadEnv()
}
func main() {
	var repo internal.Repo
	var redisDb = internal.RedisDB{Db: internal.StartRedisDB()}
	var mongoDb = internal.MongoDB{Db: internal.StartMongoDB()}
	repo.DataBase = mongoDb
	repo.SessionManager = redisDb
	repo.Client = &http.Client{
		Timeout: time.Second * 20,
	}
	repo.Producer = &kafka.Writer{
		Addr:     kafka.TCP(config.GetEnv("KAFKA_BROKER")),
		Topic:    "",
		Balancer: &kafka.LeastBytes{},

		BatchSize:    100,
		BatchTimeout: 10 * time.Millisecond,

		RequiredAcks: kafka.RequireAll,
		MaxAttempts:  3,
		WriteTimeout: 10 * time.Second,
		ReadTimeout:  10 * time.Second,

		Compression: kafka.Snappy,

		ErrorLogger: log.New(os.Stderr, "kafka-producer: ", log.LstdFlags),
		Logger:      log.New(os.Stdout, "kafka-producer: ", log.LstdFlags),

		Async: false,
	}

	internal.StartRouter(repo)
}
