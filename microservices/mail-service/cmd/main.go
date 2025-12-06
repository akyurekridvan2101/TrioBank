package main

import (
	"fmt"
	"log"
	"net/smtp"
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
	auth := smtp.PlainAuth("", os.Getenv("SENDER_MAIL"), os.Getenv("SENDER_PASSWORD"), os.Getenv("SMTP_HOST"))
	s, err = internal.NewSmtpPool(os.Getenv("SMTP_PORT"), os.Getenv("SMTP_HOST"), auth, 15)
	if err != nil {
		log.Fatal("Smtp connection pool could not be set")
	}
}

func main() {
	fmt.Println("router başladı")
	internal.StartRouter(s)
}
