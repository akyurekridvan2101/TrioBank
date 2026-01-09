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
	// .env dosyası zorunlu değil, k8s ortamında secret/configmap üzerinden alıyoruz.
	err := godotenv.Load("config/.env")
	if err != nil {
		// Sadece local/dev ortamındaysak log basalım, prod ortamında zaten env var'lar set edilmiş olmalı.
		if os.Getenv("APP_ENV") == "dev" || os.Getenv("APP_ENV") == "" {
			log.Println("Info: .env file not found, using environment variables from system")
		}
	}
}
