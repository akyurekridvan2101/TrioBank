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

// ConsumeUserCreatedEvents Kafka'dan UserCreated eventlerini dinler
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
		MinBytes:       10e3, // Min 10KB okuma
		MaxBytes:       10e6, // Max 10MB okuma
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
				// İşlem başarılıysa mesajı commit edelim
				if err := reader.CommitMessages(ctx, msg); err != nil {
					log.Printf("Error committing message: %v", err)
				}
			}
		}
	}
}

// handleUserCreatedEvent: UserCreated eventini işler
func handleUserCreatedEvent(ctx context.Context, repo *Repo, message []byte) error {
	var envelope EventEnvelope
	if err := json.Unmarshal(message, &envelope); err != nil {
		return err
	}

	// Event tipini kontrol et
	if envelope.Metadata.EventType != "UserCreated" {
		log.Printf("Ignoring event type: %s", envelope.Metadata.EventType)
		return nil
	}

	// Payload'ı struct'a çevir
	payloadBytes, err := json.Marshal(envelope.Payload)
	if err != nil {
		return err
	}

	var userPayload UserCreatedPayload
	if err := json.Unmarshal(payloadBytes, &userPayload); err != nil {
		return err
	}

	// User verisini Client formatına dönüştürüyoruz
	userUUID, err := uuid.Parse(userPayload.UUID)
	if err != nil {
		log.Printf("Invalid UUID in event: %s", userPayload.UUID)
		return err
	}

	client := &Client{
		ID:        uuid.New(),
		UserID:    userUUID,
		TCNo:      userPayload.Tc, // Auth servisinden gelen TC'yi kullan (küçük harf)
		FirstName: userPayload.Name,
		LastName:  userPayload.Surname,
		Email:     userPayload.Email,
		GSM:       userPayload.Tel,
		BirthDate: time.Date(2000, 1, 1, 0, 0, 0, 0, time.UTC), // Varsayılan doğum tarihi
		Address: Address{
			Street:     "",
			City:       "",
			District:   "",
			PostalCode: "",
		},
		CreatedAt: time.Now().UTC(),
	}

	// Müşteri zaten var mı diye bak
	existingClient, err := repo.Db.GetClientByUserID(ctx, userUUID)
	if err == nil && existingClient != nil {
		log.Printf("Client already exists for user_id: %s, skipping", userUUID)
		return nil
	}

	// Müşteriyi veritabanına kaydet
	if err := repo.Db.CreateClient(ctx, client); err != nil {
		log.Printf("Failed to create client for user_id %s: %v", userUUID, err)
		return err
	}

	log.Printf("Successfully created client for user_id: %s (event_id: %s)",
		userUUID, envelope.Metadata.EventID)

	// Account servisinde varsayılan hesap açtır
	if repo.AccountClient != nil {
		accountReq := CreateAccountRequest{
			CustomerID:  client.ID.String(),
			ProductCode: "CHECKING_TRY", // product_definitions tablosuyla eşleşmeli
			Currency:    "TRY",
		}
		account, err := repo.AccountClient.CreateAccount(accountReq)
		if err != nil {
			log.Printf("Failed to create account for client %s: %v", client.ID, err)
			// Hata dönme - müşteri oluştu, hesap açılışı ikincil öncelikte
		} else {
			log.Printf("Successfully created account %s for client %s", account.ID, client.ID)
		}
	}

	return nil
}

// ConsumeUserDeletedEvents Kafka'dan silme eventlerini dinler
func ConsumeUserDeletedEvents(ctx context.Context, repo *Repo) {
	broker := config.GetEnv("KAFKA_BROKER")
	topicDeleted := "UserDeleted" // Sabit topic adı
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
				// Mesajı commit et (işlem başarılı)
				if err := reader.CommitMessages(ctx, msg); err != nil {
					log.Printf("Error committing UserDeleted message: %v", err)
				}
			}
		}
	}
}

// handleUserDeletedEvent: Kullanıcı silme işlemini yönetir
func handleUserDeletedEvent(ctx context.Context, repo *Repo, message []byte) error {
	var envelope EventEnvelope
	if err := json.Unmarshal(message, &envelope); err != nil {
		return err
	}

	// Tip kontrolü
	if envelope.Metadata.EventType != "UserDeleted" {
		log.Printf("Ignoring event type: %s", envelope.Metadata.EventType)
		return nil
	}

	// Parse payload as UserDeletedPayload
	// Auth-service user objesini iki kere marshal ediyor (manuel + SendKafkaEvent)
	// Bu yüzden burada ona göre işlem yapmalıyız
	payloadBytes, err := json.Marshal(envelope.Payload)
	if err != nil {
		return err
	}

	var payloadStr string
	if err := json.Unmarshal(payloadBytes, &payloadStr); err != nil {
		// String değilse, zaten map/struct olabilir
		log.Printf("Payload is not a string, trying direct unmarshal: %v", err)
	} else {
		// String ise Base64 olabilir ([]byte JSON'da öyle davranır)
		// Base64 decode etmeyi dene
		decodedBytes, err := base64.StdEncoding.DecodeString(payloadStr)
		if err == nil {
			payloadBytes = decodedBytes
		} else {
			// Base64 değilse direkt string'i kullan
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

	// CustomerID'yi bulmak için client'ı çekiyoruz (client.ID account serviste customerId olarak geçer)
	client, err := repo.Db.GetClientByUserID(ctx, userUUID)
	if err != nil {
		if err == ErrClientNotFound {
			log.Printf("Client not found for user_id: %s (already passivized or never created)", userUUID)
			return nil // Hata değil, işlem zaten yapılmış (idempotent)
		}
		log.Printf("Failed to get client for user_id %s: %v", userUUID, err)
		return err
	}

	// Account servisindeki tüm hesapları kapat
	if repo.AccountClient != nil {
		accounts, err := repo.AccountClient.GetAccountsByCustomerID(client.ID.String())
		if err != nil {
			log.Printf("Failed to get accounts for client %s: %v", client.ID, err)
			// Hesapları çekemesek bile pasife almaya devam et
		} else {
			for _, account := range accounts {
				if account.Status == "ACTIVE" {
					if err := repo.AccountClient.CloseAccount(account.ID, "User account deleted"); err != nil {
						log.Printf("Failed to close account %s: %v", account.ID, err)
						// Diğer hesapları denemeye devam
					} else {
						log.Printf("Closed account %s for client %s", account.ID, client.ID)
					}
				}
			}
		}
	}

	// Müşteriyi pasife al (soft delete)
	if err := repo.Db.PassivizeClientByUserID(ctx, userUUID); err != nil {
		if err == ErrClientNotFound {
			log.Printf("Client already passivized for user_id: %s", userUUID)
			return nil // Hata değil (idempotent)
		}
		log.Printf("Failed to passivize client for user_id %s: %v", userUUID, err)
		return err
	}

	log.Printf("Successfully passivized client and closed accounts for user_id: %s (event_id: %s)",
		userUUID, envelope.Metadata.EventID)
	return nil
}
