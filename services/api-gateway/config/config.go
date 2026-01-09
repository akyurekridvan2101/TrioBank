package config

import (
	"os"
)

func GetEnv(name string) string {
	return os.Getenv(name)
}

// LoadEnv uyumluluk için duruyor, içi boş
// Env değişkenleri Kubernetes tarafından inject edilir
func LoadEnv() {
	// K8s deployment'ında bir şey yapmasına gerek yok
	// ConfigMap ve Secret'lar zaten env var olarak geliyor
}
