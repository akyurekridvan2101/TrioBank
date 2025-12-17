package config

import (
	"os"

	"github.com/joho/godotenv"
)

func GetEnv(key string) string {
	return os.Getenv(key)
}

func LoadEnv() error {
	// Try to load .env file, but don't fail if it doesn't exist (K8s injects env vars)
	_ = godotenv.Load("config/.env")
	return nil
}
