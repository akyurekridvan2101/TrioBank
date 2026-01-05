package config

import (
	"os"
)

func GetEnv(name string) string {
	return os.Getenv(name)
}

// LoadEnv is kept for compatibility but does nothing
// Environment variables are injected by Kubernetes
func LoadEnv() {
	// No-op for Kubernetes deployment
	// ConfigMap and Secrets are automatically available as env vars
}
