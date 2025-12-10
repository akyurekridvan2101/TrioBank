package main

import (
	"fmt"
	"os"

	"github.com/TrioBank/triobank-platform/microservices/mail-service/internal"
	"github.com/joho/godotenv"
)

var s *internal.SmtpPool

func init() {
	err := godotenv.Load("./config/.env")
	if err != nil && os.IsExist(err) {
		fmt.Println("the env variables was not loaded")
	}
}

func main() {
	fmt.Println("router başladı")
	internal.StartRouter(s)
}
