package config

import (
	"log"
	"os"

	"github.com/joho/godotenv"
)

func GetEnv(name string) string {
	return os.Getenv(name)
}

func LoadEnv() {
	err := godotenv.Load("config/.env")
	if err != nil {
		log.Println("setting env variable error: ", err.Error())
	}
}
