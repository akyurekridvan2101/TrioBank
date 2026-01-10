# TrioBank API Gateway

API Gateway, tüm microservice'lere tek giriş noktası sağlayan merkezi bir bileşendir.

## 🎯 Özellikler

- **Request Routing**: İstekleri doğru mikroservise yönlendirme
- **Rate Limiting**: IP ve kullanıcı bazlı istek sınırlama
- **Token Validation**: Korumalı endpoint'ler için JWT doğrulama
- **Reverse Proxy**: Auth Service'e transparent proxy

## 📁 Proje Yapısı

```
api-gateway/
├── cmd/
│   └── main.go              # Entry point
├── config/
│   ├── config.go            # Environment config loader
│   └── .env                  # Environment variables
├── docs/
│   └── openapi.yaml         # API specification
├── internal/
│   ├── auth/                # Auth service client
│   ├── cache/               # Redis client
│   ├── middleware/          # Rate limiter & auth middleware
│   └── proxy/               # Reverse proxy
├── docker-compose.yml       # Full deployment
├── docker-compose.redis.yaml # Redis only (development)
├── Dockerfile
└── README.md
```

## 🚀 Kurulum

### Gereksinimler
- Go 1.25+
- Redis 7.x
- Docker & Docker Compose (opsiyonel)

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `API_GATEWAY_PORT` | Gateway port | `3000` |
| `REDIS_ADDR` | Redis address | `localhost:6380` |
| `REDIS_PASSWORD` | Redis password | - |
| `AUTH_SERVICE_URL` | Auth service URL | `http://localhost:8080` |
| `CLIENT_SERVICE_URL` | Client service URL | `http://localhost:8081` |
| `LEDGER_SERVICE_URL` | Ledger service URL | `http://localhost:8082` |

### Local Development

```bash
# 1. Redis'i başlat
docker compose -f docker-compose.redis.yaml up -d

# 2. Environment ayarla
cp config/.env.example config/.env
# .env dosyasını düzenle

# 3. Gateway'i çalıştır
go run cmd/main.go
```

### Docker ile Çalıştırma

```bash
# Network oluştur (ilk seferde)
docker network create triobank-network

# Gateway'i başlat
docker compose up -d --build
```

## 📡 API Endpoints

### Public Endpoints (Authentication Gerektirmez)

| Method | Path | Description | Rate Limit |
|--------|------|-------------|------------|
| POST | `/auth/login` | Kullanıcı girişi başlat | 10 req/min (IP) |
| POST | `/auth/login/confirm` | Giriş onayı (email kodu) | 10 req/min (IP) |
| POST | `/auth/register` | Kayıt başlat | 10 req/min (IP) |
| POST | `/auth/register/confirm` | Kayıt onayı | 10 req/min (IP) |
| POST | `/auth/logout` | Çıkış yap | - |
| POST | `/auth/refresh` | Token yenile | - |

### Protected Endpoints (Bearer Token Gerekli)

| Method | Path | Description | Rate Limit |
|--------|------|-------------|------------|
| POST | `/auth/password-change` | Şifre değiştir | 100 req/min (User) |
| DELETE | `/auth/delete-account` | Hesap sil | 100 req/min (User) |
| GET | `/api/accounts/{accountId}/statement` | Hesap hareketleri sorgula | 100 req/min (User) |
| GET | `/api/balances/{accountId}` | Bakiye sorgula | 100 req/min (User) |

### Gateway Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/health` | Health check |

## 🔒 Rate Limiting

Gateway iki tür rate limiting uygular:

### IP Based Rate Limiting
- Public endpoint'ler için
- IP başına dakikada 10 istek
- Redis'te `rate:ip:{ip_address}` key'i

### User Based Rate Limiting  
- Protected endpoint'ler için
- User ID başına dakikada 100 istek
- Redis'te `rate:user:{user_id}` key'i

### Rate Limit Aşıldığında

```json
HTTP/1.1 429 Too Many Requests
Retry-After: 60

{
  "error": "rate limit exceeded",
  "retry_after": 60
}
```

## 🔐 Authentication Flow

```
┌─────────┐     ┌─────────────┐     ┌──────────────┐
│ Client  │────▶│ API Gateway │────▶│ Auth Service │
└─────────┘     └─────────────┘     └──────────────┘
     │                 │                    │
     │  1. Request     │                    │
     │  + Bearer Token │                    │
     │────────────────▶│                    │
     │                 │  2. Validate Token │
     │                 │───────────────────▶│
     │                 │                    │
     │                 │  3. User ID        │
     │                 │◀───────────────────│
     │                 │                    │
     │                 │  4. Proxy Request  │
     │                 │───────────────────▶│
     │                 │                    │
     │  5. Response    │  5. Response       │
     │◀────────────────│◀───────────────────│
```

## 📖 API Documentation

OpenAPI specification: [`docs/openapi.yaml`](docs/openapi.yaml)

Swagger UI ile görüntülemek için:
```bash
# Swagger UI container
docker run -p 8082:8080 -e SWAGGER_JSON=/spec/openapi.yaml \
  -v $(pwd)/docs:/spec swaggerapi/swagger-ui
```

Ardından http://localhost:8082 adresini ziyaret edin.

## 🐳 Docker Compose Services

| Service | Port | Description |
|---------|------|-------------|
| `api-gateway` | 3000 | API Gateway |
| `api-gateway-redis` | 6380 | Rate limiting Redis |

## 🔧 Development

### Yeni Endpoint Ekleme

1. `cmd/main.go`'da route tanımla
2. Uygun middleware'leri ekle (rate limiter, auth)
3. `docs/openapi.yaml`'ı güncelle

```go
// Public endpoint örneği
http.Handle("/new/endpoint",
    rateLimiter.IPRateLimit(time.Minute)(serviceProxy.ProxyToAuth()))

// Protected endpoint örneği
http.Handle("/protected/endpoint",
    authMiddleware.RequireAuth(
        rateLimiter.UserRateLimit(time.Minute)(
            serviceProxy.ProxyToAuth())))
```

## ⚠️ Dikkat Edilmesi Gerekenler

1. **triobank-network**: Auth Service ile iletişim için external network gerekli
2. **Redis persistence**: `api_gateway_redis_data` volume ile data kalıcı
3. **Cookie security**: Refresh token HttpOnly cookie olarak gelir
4. **CORS**: Production'da CORS ayarları yapılmalı

## 📊 Monitoring

Health check endpoint'i:
```bash
curl http://localhost:3000/health
# {"status":"healthy"}
```

## 📄 License

MIT License - Detaylar için [LICENSE](../../LICENSE) dosyasına bakın.
