package internal

import "time"

// EventEnvelope - Kafka event wrapper from auth-service
type EventEnvelope struct {
	Metadata EventMetadata `json:"metadata"`
	Payload  interface{}   `json:"payload"` // Keep as interface{} for JSON unmarshaling
}

// EventMetadata - Event metadata from auth-service
type EventMetadata struct {
	EventID       string    `json:"event_id"`
	EventType     string    `json:"event_type"`
	EventVersion  string    `json:"event_version"`
	Timestamp     time.Time `json:"timestamp"`
	CorrelationID string    `json:"correlation_id"`
}

// UserCreatedPayload - User data from auth-service UserCreated event
// Auth-service User struct JSON tags: uuid, name, surname, email, tel, tc, createdAt
type UserCreatedPayload struct {
	UUID      string    `json:"uuid"`
	Name      string    `json:"name"`
	Surname   string    `json:"surname"`
	Email     string    `json:"email"`
	Tel       string    `json:"tel"`
	Tc        string    `json:"tc"`        // lowercase 'tc' (auth-service format)
	CreatedAt time.Time `json:"createdAt"` // camelCase (auth-service format)
}

// UserDeletedPayload - User deletion data from auth-service UserDeleted event
type UserDeletedPayload struct {
	UUID string `json:"uuid"`
}
