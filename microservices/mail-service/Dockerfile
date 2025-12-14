# Build stage
FROM golang:1.25.2-alpine AS builder

WORKDIR /app

# Install ca-certificates for HTTPS requests
RUN apk --no-cache add ca-certificates

# Copy go mod files
COPY go.mod go.sum ./
RUN go mod download

# Copy source code
COPY . .

# Build with optimizations
RUN CGO_ENABLED=0 GOOS=linux go build \
    -ldflags="-w -s" \
    -a -installsuffix cgo \
    -o main ./cmd

# Runtime stage
FROM alpine:latest

# Install ca-certificates and wget for healthcheck
RUN apk --no-cache add ca-certificates wget

# Create non-root user
RUN addgroup -g 1000 appuser && \
    adduser -D -u 1000 -G appuser appuser

WORKDIR /app

# Copy binary from builder
COPY --from=builder /app/main .

# Change ownership
RUN chown -R appuser:appuser /app

# Use non-root user
USER appuser

EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/health || exit 1

ENTRYPOINT ["/app/main"]