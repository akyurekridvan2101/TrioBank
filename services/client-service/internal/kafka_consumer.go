package internal

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"log"
	"time"

	"github.com/TrioBank/triobank-platform/microservices/client-service/config"
	"github.com/google/uuid"
	"github.com/segmentio/kafka-go"
)

// ConsumeUserCreatedEvents listens to UserCreated events from Kafka
func ConsumeUserCreatedEvents(ctx context.Context, repo *Repo) {
	broker := config.GetEnv("KAFKA_BROKER")
	topic := config.GetEnv("KAFKA_TOPIC")
	groupID := config.GetEnv("KAFKA_GROUP_ID")

	if broker == "" || topic == "" || groupID == "" {
		log.Println("Kafka configuration missing, skipping consumer")
		return
	}

	reader := kafka.NewReader(kafka.ReaderConfig{
		Brokers:        []string{broker},
		Topic:          topic,
		GroupID:        groupID,
		MinBytes:       10e3, // 10KB
		MaxBytes:       10e6, // 10MB
		CommitInterval: time.Second,
		StartOffset:    kafka.LastOffset,
	})

	defer reader.Close()

	log.Printf("Kafka consumer started: broker=%s, topic=%s, group=%s", broker, topic, groupID)

	for {
		select {
		case <-ctx.Done():
			log.Println("Kafka consumer shutting down...")
			return
		default:
			msg, err := reader.FetchMessage(ctx)
			if err != nil {
				log.Printf("Error fetching message: %v", err)
				continue
			}

			if err := handleUserCreatedEvent(ctx, repo, msg.Value); err != nil {
				log.Printf("Error handling event: %v", err)
			} else {
				// Commit message after successful processing
				if err := reader.CommitMessages(ctx, msg); err != nil {
					log.Printf("Error committing message: %v", err)
				}
			}
		}
	}
}

// handleUserCreatedEvent processes UserCreated event
func handleUserCreatedEvent(ctx context.Context, repo *Repo, message []byte) error {
	var envelope EventEnvelope
	if err := json.Unmarshal(message, &envelope); err != nil {
		return err
	}

	// Check event type
	if envelope.Metadata.EventType != "UserCreated" {
		log.Printf("Ignoring event type: %s", envelope.Metadata.EventType)
		return nil
	}

	// Parse payload as UserCreatedPayload
	payloadBytes, err := json.Marshal(envelope.Payload)
	if err != nil {
		return err
	}

	var userPayload UserCreatedPayload
	if err := json.Unmarshal(payloadBytes, &userPayload); err != nil {
		return err
	}

	// Convert User to Client
	userUUID, err := uuid.Parse(userPayload.UUID)
	if err != nil {
		log.Printf("Invalid UUID in event: %s", userPayload.UUID)
		return err
	}

	client := &Client{
		ID:        uuid.New(),
		UserID:    userUUID,
		TCNo:      userPayload.Tc, // Use Tc (lowercase) from auth-service
		FirstName: userPayload.Name,
		LastName:  userPayload.Surname,
		Email:     userPayload.Email,
		GSM:       userPayload.Tel,
		BirthDate: time.Date(2000, 1, 1, 0, 0, 0, 0, time.UTC), // Default birth date
		Address: Address{
			Street:     "",
			City:       "",
			District:   "",
			PostalCode: "",
		},
		CreatedAt: time.Now().UTC(),
	}

	// Check if client already exists
	existingClient, err := repo.Db.GetClientByUserID(ctx, userUUID)
	if err == nil && existingClient != nil {
		log.Printf("Client already exists for user_id: %s, skipping", userUUID)
		return nil
	}

	// Create client in database
	if err := repo.Db.CreateClient(ctx, client); err != nil {
		log.Printf("Failed to create client for user_id %s: %v", userUUID, err)
		return err
	}

	log.Printf("Successfully created client for user_id: %s (event_id: %s)",
		userUUID, envelope.Metadata.EventID)

	// Create default account in Account Service
	if repo.AccountClient != nil {
		accountReq := CreateAccountRequest{
			CustomerID:  client.ID.String(),
			ProductCode: "CHECKING_TRY", // Must match product_definitions table
			Currency:    "TRY",
		}
		account, err := repo.AccountClient.CreateAccount(accountReq)
		if err != nil {
			log.Printf("Failed to create account for client %s: %v", client.ID, err)
			// Don't return error - client is created, account creation is secondary
		} else {
			log.Printf("Successfully created account %s for client %s", account.ID, client.ID)
		}
	}

	return nil
}

// ConsumeUserDeletedEvents listens to UserDeleted events from Kafka
func ConsumeUserDeletedEvents(ctx context.Context, repo *Repo) {
	broker := config.GetEnv("KAFKA_BROKER")
	topicDeleted := "UserDeleted" // Hardcoded topic name
	groupID := config.GetEnv("KAFKA_GROUP_ID")

	if broker == "" || topicDeleted == "" || groupID == "" {
		log.Println("Kafka configuration missing for UserDeleted consumer, skipping")
		return
	}

	reader := kafka.NewReader(kafka.ReaderConfig{
		Brokers:        []string{broker},
		Topic:          topicDeleted,
		GroupID:        groupID,
		MinBytes:       10e3, // 10KB
		MaxBytes:       10e6, // 10MB
		CommitInterval: time.Second,
		StartOffset:    kafka.LastOffset,
	})

	defer reader.Close()

	log.Printf("Kafka consumer started for UserDeleted: broker=%s, topic=%s, group=%s", broker, topicDeleted, groupID)

	for {
		select {
		case <-ctx.Done():
			log.Println("UserDeleted Kafka consumer shutting down...")
			return
		default:
			msg, err := reader.FetchMessage(ctx)
			if err != nil {
				log.Printf("Error fetching UserDeleted message: %v", err)
				continue
			}

			if err := handleUserDeletedEvent(ctx, repo, msg.Value); err != nil {
				log.Printf("Error handling UserDeleted event: %v", err)
			} else {
				// Commit message after successful processing
				if err := reader.CommitMessages(ctx, msg); err != nil {
					log.Printf("Error committing UserDeleted message: %v", err)
				}
			}
		}
	}
}

// handleUserDeletedEvent processes UserDeleted event
func handleUserDeletedEvent(ctx context.Context, repo *Repo, message []byte) error {
	var envelope EventEnvelope
	if err := json.Unmarshal(message, &envelope); err != nil {
		return err
	}

	// Check event type
	if envelope.Metadata.EventType != "UserDeleted" {
		log.Printf("Ignoring event type: %s", envelope.Metadata.EventType)
		return nil
	}

	// Parse payload as UserDeletedPayload
	// Auth-service double marshals the user object (once manually, once via SendKafkaEvent)
	// So we need to handle it accordingly
	payloadBytes, err := json.Marshal(envelope.Payload)
	if err != nil {
		return err
	}

	var payloadStr string
	if err := json.Unmarshal(payloadBytes, &payloadStr); err != nil {
		// If it's not a string, it might be already a map/struct
		log.Printf("Payload is not a string, trying direct unmarshal: %v", err)
	} else {
		// If it was a string, it might be Base64 encoded (default behavior for []byte in JSON)
		// Try to decode base64
		decodedBytes, err := base64.StdEncoding.DecodeString(payloadStr)
		if err == nil {
			payloadBytes = decodedBytes
		} else {
			// If not base64, use the string itself
			payloadBytes = []byte(payloadStr)
		}
	}

	var userPayload UserDeletedPayload
	if err := json.Unmarshal(payloadBytes, &userPayload); err != nil {
		log.Printf("Failed to unmarshal UserDeleted payload: %v (payload: %s)", err, string(payloadBytes))
		return err
	}

	userUUID, err := uuid.Parse(userPayload.UUID)
	if err != nil {
		log.Printf("Invalid UUID in UserDeleted event: %s", userPayload.UUID)
		return err
	}

	// Get client to find the customerID (client.ID is used as customerId in Account Service)
	client, err := repo.Db.GetClientByUserID(ctx, userUUID)
	if err != nil {
		if err == ErrClientNotFound {
			log.Printf("Client not found for user_id: %s (already passivized or never created)", userUUID)
			return nil // Not an error - idempotent
		}
		log.Printf("Failed to get client for user_id %s: %v", userUUID, err)
		return err
	}

	// Close all accounts in Account Service
	if repo.AccountClient != nil {
		accounts, err := repo.AccountClient.GetAccountsByCustomerID(client.ID.String())
		if err != nil {
			log.Printf("Failed to get accounts for client %s: %v", client.ID, err)
			// Continue with client passivization even if account fetch fails
		} else {
			for _, account := range accounts {
				if account.Status == "ACTIVE" {
					if err := repo.AccountClient.CloseAccount(account.ID, "User account deleted"); err != nil {
						log.Printf("Failed to close account %s: %v", account.ID, err)
						// Continue with other accounts
					} else {
						log.Printf("Closed account %s for client %s", account.ID, client.ID)
					}
				}
			}
		}
	}

	// Passivize client (soft delete)
	if err := repo.Db.PassivizeClientByUserID(ctx, userUUID); err != nil {
		if err == ErrClientNotFound {
			log.Printf("Client already passivized for user_id: %s", userUUID)
			return nil // Not an error - idempotent
		}
		log.Printf("Failed to passivize client for user_id %s: %v", userUUID, err)
		return err
	}

	log.Printf("Successfully passivized client and closed accounts for user_id: %s (event_id: %s)",
		userUUID, envelope.Metadata.EventID)
	return nil
}
