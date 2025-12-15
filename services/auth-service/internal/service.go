package internal

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"strconv"
	"time"

	"github.com/TrioBank/triobank-platform/microservices/auth-service/config"
	"github.com/google/uuid"
	"github.com/segmentio/kafka-go"
)

type EventMetadata struct {
	EventID       string    `json:"event_id"`
	EventType     string    `json:"event_type"`
	EventVersion  string    `json:"event_version"`
	Timestamp     time.Time `json:"timestamp"`
	CorrelationID string    `json:"correlation_id"`
}

type EventEnvelope struct {
	Metadata EventMetadata `json:"metadata"`
	Payload  interface{}   `json:"payload"`
}

func SendMail(ctx context.Context, repo Repo, requestData smsRequestData, errChannel chan error) {
	url := "http://" + config.GetEnv("MAIL_SERVICE_PORT") + "/send"
	data, err := json.Marshal(requestData)
	if err != nil {
		errChannel <- err
		return
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, url, bytes.NewReader(data))
	if err != nil {
		errChannel <- err
		return
	}
	deadline, _ := ctx.Deadline()

	req.Header.Set("X-Request-Deadline", strconv.FormatInt(deadline.UnixMilli(), 10))
	req.Header.Set("X-Internal-Secret", config.GetEnv("SECRET_KEY"))

	response, err := repo.Client.Do(req)
	if err != nil {
		errChannel <- err
		return
	}
	defer response.Body.Close()
	if response.StatusCode != http.StatusOK {
		errChannel <- fmt.Errorf("unexpected status: %d", response.StatusCode)
		return
	} else {
		errChannel <- nil
		return
	}
}

func SendKafkaEvent(
	writer *kafka.Writer,
	ctx context.Context,
	key string,
	payload interface{},
	topic string,
	eventType string,
) error {

	envelope := EventEnvelope{
		Metadata: EventMetadata{
			EventID:       uuid.New().String(),
			EventType:     eventType,
			EventVersion:  "v1",
			Timestamp:     time.Now().UTC(),
			CorrelationID: getCorrelationID(ctx),
		},
		Payload: payload,
	}

	value, err := json.Marshal(envelope)
	if err != nil {
		return fmt.Errorf("failed to marshal event: %w", err)
	}

	message := kafka.Message{
		Topic: topic,
		Key:   []byte(key),
		Value: value,
		Time:  time.Now().UTC(),
		Headers: []kafka.Header{
			{Key: "event-type", Value: []byte(eventType)},
			{Key: "event-version", Value: []byte("v1")},
		},
	}

	// Retry logic ile gönder
	err = retry(ctx, 3, time.Second, func() error {
		return writer.WriteMessages(ctx, message)
	})

	if err != nil {
		log.Printf("Failed to send event %s: %v", eventType, err)
		return fmt.Errorf("failed to send kafka event: %w", err)
	}

	log.Printf("Successfully sent event %s with ID %s", eventType, envelope.Metadata.EventID)
	return nil
}
func retry(ctx context.Context, attempts int, sleep time.Duration, fn func() error) error {
	if err := fn(); err != nil {
		if attempts--; attempts > 0 {
			time.Sleep(sleep)
			return retry(ctx, attempts, 2*sleep, fn)
		}
		return err
	}
	return nil
}
func getCorrelationID(ctx context.Context) string {
	if correlationID, ok := ctx.Value("correlation_id").(string); ok && correlationID != "" {
		return correlationID
	}
	return uuid.New().String()
}
