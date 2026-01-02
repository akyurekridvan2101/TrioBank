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
	// .env dosyası opsiyonel - Kubernetes'te ConfigMap/Secret kullanılıyor
	err := godotenv.Load("config/.env")
	if err != nil {
		// Sadece development modunda bilgi ver, production'da sessizce devam et
		if os.Getenv("APP_ENV") == "dev" || os.Getenv("APP_ENV") == "" {
			log.Println("Info: .env file not found, using environment variables from system")
		}
	}
}
