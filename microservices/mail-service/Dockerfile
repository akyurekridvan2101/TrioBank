# Build stage
FROM golang:1.25.2-alpine AS builder
WORKDIR /app

# Modülleri indir
COPY go.mod go.sum ./
RUN go mod download

# Tüm kodları kopyala
COPY . .

# main.go cmd klasörü içinde olduğu için
RUN go build -o main ./cmd

# Runtime stage
FROM alpine:latest
WORKDIR /app
COPY --from=builder /app/main .

EXPOSE 8081
ENTRYPOINT ["/app/main"]
