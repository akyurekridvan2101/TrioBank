package main

import (
	"github.com/TrioBank/triobank-platform/microservices/auth-service/config"
	"github.com/TrioBank/triobank-platform/microservices/auth-service/internal"
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
	internal.StartRouter(repo)
}
