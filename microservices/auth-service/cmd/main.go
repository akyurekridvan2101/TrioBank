package main

import (
	"github.com/TrioBank/triobank-platform/microservices/auth-service/config"
	"github.com/TrioBank/triobank-platform/microservices/auth-service/internal"
)

func init() {
	config.LoadEnv()
}
func main() {
	internal.StartMongoDB()
}
