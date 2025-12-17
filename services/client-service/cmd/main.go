package main

import (
	"context"
	"fmt"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/TrioBank/triobank-platform/microservices/client-service/config"
	"github.com/TrioBank/triobank-platform/microservices/client-service/internal"
)

func init() {
	err := config.LoadEnv()
	if err != nil {
		fmt.Println(err.Error())
	}
}

func main() {
	var repo internal.Repo

	sqlDb, err := internal.StartSqlDb()
	if err != nil {
		log.Fatal("sql db connection is not done: ", err)
	}

	// Initialize database (create tables if they don't exist)
	if err := internal.InitializeDatabase(sqlDb); err != nil {
		log.Fatal("database initialization failed: ", err)
	}
	log.Println("Database initialized successfully")

	repo.Db = &internal.DataBaseSql{DB: sqlDb}

	client := http.Client{Timeout: 8 * time.Second}
	repo.Client = &client

	// Initialize Account Service client
	accountServiceURL := config.GetEnv("ACCOUNT_SERVICE_URL")
	if accountServiceURL != "" {
		repo.AccountClient = internal.NewAccountClient(accountServiceURL, &client)
		log.Println("Account Service client initialized:", accountServiceURL)
	} else {
		log.Println("ACCOUNT_SERVICE_URL not set, account creation will be skipped")
	}

	// Context for graceful shutdown
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	// Start Kafka consumers in background
	go internal.ConsumeUserCreatedEvents(ctx, &repo)
	go internal.ConsumeUserDeletedEvents(ctx, &repo)

	// Handle shutdown signals
	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, syscall.SIGINT, syscall.SIGTERM)

	go func() {
		<-sigChan
		log.Println("Shutdown signal received, stopping services...")
		cancel()
	}()

	internal.StartRouter(repo)
}
