package main

import (
	"fmt"
	"os"

	"github.com/TrioBank/triobank-platform/microservices/mail-service/internal"
	"github.com/joho/godotenv"
	"github.com/resend/resend-go/v3"
)

func init() {
	err := godotenv.Load("./config/.env")
	if err != nil && os.IsExist(err) {
		fmt.Println("the env variables was not loaded")
	}
}

func main() {
	var client internal.Client
	client.MailClient = resend.NewClient(os.Getenv("RESEND_API_KEY"))
	fmt.Println("router başladı")
	internal.StartRouter(client)
}
