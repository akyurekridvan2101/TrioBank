#!/bin/bash

# bu kodu çalıştırarak kafka topic'lerini manuel olarak kurabilirim

# Script to manually create Kafka topics for auth-service
# Usage: ./scripts/create-kafka-topics.sh

echo "Creating Kafka topics for auth-service..."

# Create UserCreated topic
docker exec AuthServiceKafka kafka-topics --create \
  --topic UserCreated \
  --partitions 3 \
  --replication-factor 1 \
  --if-not-exists \
  --bootstrap-server localhost:9092

if [ $? -eq 0 ]; then
  echo "✓ UserCreated topic created successfully"
else
  echo "✗ Failed to create UserCreated topic"
fi

# Create UserDeleted topic
docker exec AuthServiceKafka kafka-topics --create \
  --topic UserDeleted \
  --partitions 3 \
  --replication-factor 1 \
  --if-not-exists \
  --bootstrap-server localhost:9092

if [ $? -eq 0 ]; then
  echo "✓ UserDeleted topic created successfully"
else
  echo "✗ Failed to create UserDeleted topic"
fi

# List all topics
echo ""
echo "Current Kafka topics:"
docker exec AuthServiceKafka kafka-topics --list --bootstrap-server localhost:9092
