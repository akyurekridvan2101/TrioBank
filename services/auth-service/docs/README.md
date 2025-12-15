# TrioBank Auth Service - Teknik Dokümantasyon

> **Versiyon:** 1.0.0  
> **Son Güncelleme:** 2025-12-14  
> **Durum:** Production Ready ✅

---

## 📋 İçindekiler

1. [Genel Bakış](#genel-bakış)
2. [Mimari](#mimari)
3. [Güvenlik](#güvenlik)
4. [Endpoint'ler](#endpoints)
5. [Veritabanı Şeması](#veritabanı-şeması)
6. [Hata Yönetimi](#hata-yönetimi)
7. [Rate Limiting](#rate-limiting)
8. [Deployment](#deployment)
9. [Environment Variables](#environment-variables)

---

## 🎯 Genel Bakış

Auth Service, TrioBank platformunun merkezi kimlik doğrulama ve yetkilendirme servisidir. Kullanıcı kaydı, giriş, token yönetimi ve hesap işlemlerini yönetir.

### Temel Özellikler

- ✅ **Two-Step Authentication**: Email doğrulama ile güvenli giriş
- ✅ **JWT Token Management**: Access/Refresh token ayrımı
- ✅ **Rate Limiting**: Brute-force saldırı koruması
- ✅ **Kafka Integration**: Mikroservis iletişimi için event publishing
- ✅ **HttpOnly Cookies**: XSS saldırılarına karşı korumalı token saklama
- ✅ **UUID Based**: Servisler arası kullanıcı tanımlama

### Teknoloji Stack'i

| Katman | Teknoloji |
|--------|-----------|
| **Framework** | Go (net/http) |
| **Database** | MongoDB |
| **Cache/Session** | Redis |
| **Message Broker** | Kafka |
| **Authentication** | JWT (golang-jwt/jwt) |
| **Password Hashing** | bcrypt |

---

## 🏛️ Mimari

### Katmanlı Mimari

```
┌─────────────────────────────────────┐
│         Handler Layer               │
│  (HTTP request/response handling)   │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│       Service Layer                 │
│  (Business logic, Kafka, Email)     │
└──────────────┬──────────────────────┘
               │
       ┌───────┴────────┐
       │                │
┌──────▼─────┐   ┌─────▼──────┐
│  Database  │   │  Session   │
│  (MongoDB) │   │  (Redis)   │
└────────────┘   └────────────┘
```

### Veri Akışı

#### 1. Login Flow
```
Client → POST /auth/login
  ↓
Handler: TC/Password kontrolü
  ↓
Redis: Rate limit kontrolü (SetNX)
  ↓
MongoDB: User doğrulama
  ↓
Redis: Session ID + Code kaydet (3dk TTL)
  ↓
Mail Service: Doğrulama kodu gönder
  ↓
Client ← SessionID döner
  ↓
Client → POST /auth/login/confirm {sessionId, code}
  ↓
Redis: Code doğrulama
  ↓
MongoDB: Token'lar oluştur ve kaydet
  ↓
Client ← Access Token (body) + Refresh Token (cookie)
```

#### 2. Token Refresh Flow
```
Client → POST /auth/refresh (Cookie: Refresh-Token)
  ↓
MongoDB: Token doğrulama (isActive, expiry)
  ↓
JWT: Yeni Access Token oluştur
  ↓
Client ← Access Token
```

#### 3. Password Change Flow
```
Client → POST /auth/password-change
  ↓
JWT: Access Token doğrulama
  ↓
MongoDB: Eski şifre kontrolü
  ↓
Redis: Rate limit (user başına)
  ↓
MongoDB: Yeni şifre güncelleme
  ↓
Client ← Success
```

---

## 🔐 Güvenlik

### 1. Token Stratejisi

| Token Type | Geçerlilik | Saklama | Kullanım |
|------------|------------|---------|----------|
| **Access Token** | 15 dakika | Client-side (memory/localStorage) | API istekleri için Authorization header |
| **Refresh Token** | 7 gün | HttpOnly Cookie | Access token yenileme |

### 2. Password Güvenliği

- ✅ **Bcrypt** kullanılıyor (cost: 10)
- ✅ Timing attack koruması (bcrypt.CompareHashAndPassword)
- ⚠️ Password strength validation yok (client-side önerilir)

### 3. Token Invalidation

**Strateji:** Her yeni login'de tüm eski token'lar deaktive edilir

```go
// createRefreshAndAccessToken fonksiyonu
collection.UpdateMany(ctx, bson.M{"user_id": userId}, bson.M{"$set": bson.M{"isActive": false}})
```

**Etkisi:**
- Kullanıcı yeni cihazda login olduğunda diğer cihazlardaki token'lar geçersiz olur
- Multi-device support istiyorsanız bu davranışı değiştirmeniz gerekir

### 4. CORS & Cookies

**Cookie Ayarları:**
```go
cookie := http.Cookie{
    Name:     "Refresh-Token",
    Value:    refreshToken,
    Secure:   true,      // HTTPS zorunlu
    HttpOnly: true,      // JavaScript erişimi yok (XSS koruması)
    Path:     "/",
    MaxAge:   7 * 24 * 60 * 60,  // 7 gün
}
```

**Öneriler:**
- Production'da `SameSite: Strict/Lax` ekleyin (CSRF koruması)
- Domain ayarını production'da güncelleyin

---

## 📡 Endpoints

### Authentication

#### POST /auth/login
Kullanıcı giriş bilgilerini doğrular ve doğrulama kodu gönderir.

**Request:**
```json
{
  "tc": "11111111111",
  "password": "Password123"
}
```

**Success Response (200):**
```json
{
  "sessionId": "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXo="
}
```

**Rate Limiting:** Kullanıcı başına 3 dakikada 1 istek

**Hata Kodları:**
- `400`: Body hatalı
- `401`: TC/şifre yanlış
- `429`: Rate limit aşıldı
- `500`: Sunucu hatası (kod üretimi, email gönderimi, vb.)

---

#### POST /auth/login/confirm
Email'e gelen kodu doğrular ve token'ları üretir.

**Request:**
```json
{
  "session-id": "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXo=",
  "code": "1234"
}
```

**Success Response (200):**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIs..."
}
```
**+ Set-Cookie:** `Refresh-Token=<token>; HttpOnly; Secure; Path=/; Max-Age=604800`

**Hata Kodları:**
- `400`: Body hatalı veya kod format hatası
- `401`: Kod yanlış veya session expire
- `500`: Token üretimi hatası

---

#### POST /auth/register
Yeni kullanıcı kaydı başlatır.

**Request:**
```json
{
  "name": "Ahmet",
  "surname": "Yılmaz",
  "email": "ahmet@example.com",
  "password": "Guclupassword123",
  "tel": "5551234567",
  "tc": "11111111111"
}
```

**Success Response (200):**
```json
{
  "sessionId": "..."
}
```

**Rate Limiting:** TC başına 3 dakikada 1 istek

**Hata Kodları:**
- `409`: Kullanıcı zaten mevcut
- `429`: Rate limit aşıldı
- `500`: Sunucu hatası

---

#### POST /auth/register/confirm
Kayıt doğrulama kodunu kontrol eder ve kullanıcıyı oluşturur.

**Request:** Login confirm ile aynı

**Success:** Token'lar döner + **Kafka Event publish edilir** (`UserCreated`)

---

### Token Operations

#### POST /auth/logout
Kullanıcıyı çıkış yapar ve cookie'leri temizler.

**Request:** Body yok (cookie'den alınır)

**Success Response (200):**
```
logged out successfully
```

**Davranış:**
- Cookie'leri temizler (MaxAge=-1)
- Refresh token'ı MongoDB'de deaktive eder
- **Authentication gerektirmez** (idempotent)

---

#### POST /auth/refresh
Access token'ı yeniler.

**Request:** Body yok (Cookie: Refresh-Token)

**Success Response (200):**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Hata Kodları:**
- `401`: Cookie yok veya token geçersiz/expire

---

#### POST /auth/validation
API Gateway için token doğrulama (internal use).

**Headers:**
```
Authorization: Bearer <access_token>
```

**Success Response (200):**
```json
{
  "user_id": "uuid-format-user-id"
}
```

**Kullanım:**
```
Client → API Gateway → /auth/validation → Backend Service
```

---

### Account Settings

#### POST /auth/password-change
Kullanıcı şifresini değiştirir.

**Headers:**
```
Authorization: Bearer <access_token>
```

**Request:**
```json
{
  "old_password": "OldPassword123",
  "new_password": "NewPassword123"
}
```

**Success Response (200):**
```
password updated successfully
```

**Rate Limiting:** User başına sıralı denemeler arasında limit var

**Validasyonlar:**
- ✅ Bearer token formatı
- ✅ Token geçerliliği
- ✅ Boş string kontrolü
- ✅ Eski/yeni şifre aynı olmamalı
- ✅ Eski şifre doğrulaması
- ⚠️ Yeni şifre strength validation YOK

**Hata Kodları:**
- `400`: Body hatalı, şifreler boş/aynı, eski şifre yanlış
- `401`: Token geçersiz/expire
- `429`: Rate limit
- `500`: Hash veya DB hatası

---

#### DELETE /auth/delete-account
Kullanıcı hesabını kalıcı olarak siler.

**Headers:**
```
Authorization: Bearer <access_token>
Cookie: Refresh-Token
```

**Request:**
```json
{
  "password": "MyPassword123"
}
```

**Success Response (200):**
```
(boş body)
```
**+ Set-Cookie:** `Refresh-Token=; MaxAge=-1`

**Davranış:**
1. Cookie ve token kontrolleri
2. Şifre doğrulaması
3. Refresh token deaktivasyonu
4. User silme
5. **Kafka Event publish** (`UserDeleted`)
6. Cookie temizleme

**⚠️ Önemli:** Bu işlem geri alınamaz!

**Hata Kodları:**
- `400`: Body hatalı
- `401`: Token/cookie yok, şifre yanlış
- `500`: Sunucu hatası

---

## 💾 Veritabanı Şeması

### MongoDB Collections

#### Users Collection
```go
{
  "_id": ObjectID,
  "uuid": "string (UUID v4)",
  "name": "string",
  "surname": "string",
  "hashedPassword": "string (bcrypt)",
  "email": "string",
  "tel": "string",
  "tc": "string (11 karakter)",
  "createdAt": ISODate,
  "isActive": boolean
}
```

**Indexes:** `tc` (unique), `uuid` (unique)

---

#### Tokens Collection
```go
{
  "_id": ObjectID,
  "user_id": ObjectID,
  "user_uuid": "string",
  "token": "string (JWT)",
  "createdAt": ISODate,
  "expiredAt": ISODate,
  "isActive": boolean
}
```

**Indexes:** `user_id`, `token` (unique), `isActive`

---

### Redis Data Structures

#### Session Data (Login/Register)
```
Key: <sessionId>
Value: {"userId": ObjectID, "code": int64}
TTL: 3 minutes
```

#### Rate Limit
```
Key: limit:<userId/tc>
Value: "limited"
TTL: 3 minutes
```

#### Temporary User (Register)
```
Key: user:<userId>
Value: JSON (User struct)
TTL: 4 minutes
```

---

## ⚠️ Hata Yönetimi

### HTTP Status Codes

| Code | Kullanım Alanı |
|------|----------------|
| `200` | İşlem başarılı |
| `400` | Client hatası (body, validation) |
| `401` | Authentication hatası |
| `405` | Yanlış HTTP method |
| `409` | Conflict (kullanıcı zaten var) |
| `429` | Rate limit aşıldı |
| `500` | Sunucu hatası |

### Error Response Format

**Text/Plain:**
```
old password invalid
```

**JSON (sadece token response'larda):**
```json
{
  "access_token": "..."
}
```

---

## 🚦 Rate Limiting

### Stratejisi

**Redis SetNX** ile atomic rate limiting (distributed lock)

### Uygulama Alanları

| Endpoint | Limit Key | TTL | Neden |
|----------|-----------|-----|-------|
| `/auth/login` | `limit:<userId>` | 3dk | Email spam önleme |
| `/auth/register` | `limit:<tc>` | 3dk | Email spam önleme |
| `/auth/password-change` | `limit:<userId>` | - | Brute-force önleme |

### Implementasyon

```go
// Limit set
result, _ := redis.SetNX(ctx, key, "limited", 3*time.Minute)
if !result {
    return 429  // Aktif işlem var
}

// Limit cleanup (defer veya success callback)
defer redis.Del(ctx, key)
```

---

## 🚀 Deployment

### Docker Compose Setup

```yaml
services:
  auth-service:
    build: ./microservices/auth-service
    ports:
      - "8080:8080"
    environment:
      - AUTH_SERVICE_PORT=:8080
      - MONGO_URI=mongodb://mongo:27017
      - REDIS_PORT=redis:6379
      - KAFKA_BROKER=kafka:9092
    depends_on:
      - mongo
      - redis
      - kafka
```

### Health Check

```bash
curl http://localhost:8080/health
```

---

## 🔧 Environment Variables

| Variable | Açıklama | Örnek |
|----------|----------|-------|
| `AUTH_SERVICE_PORT` | Servis port'u | `:8080` |
| `MONGO_URI` | MongoDB connection string | `mongodb://localhost:27017` |
| `MONGO_NAME` | Database adı | `triobank` |
| `REDIS_PORT` | Redis adresi | `localhost:6379` |
| `REDIS_NAME` | Redis client adı | `auth-service` |
| `KAFKA_BROKER` | Kafka broker adresi | `localhost:9092` |
| `TOKEN_SIGNATURE` | JWT secret key | `your-secret-key` |
| `SECRET_KEY` | Internal service secret | `internal-secret` |
| `MAIL_SERVICE_PORT` | Mail service URL | `localhost:8081` |

**⚠️ Güvenlik:** Production'da `TOKEN_SIGNATURE` ve `SECRET_KEY` mutlaka değiştirin!

---

## 📊 Kafka Events

### Published Events

#### UserCreated
```json
{
  "metadata": {
    "event_id": "uuid",
    "event_type": "UserCreated",
    "event_version": "v1",
    "timestamp": "2024-01-01T00:00:00Z",
    "correlation_id": "uuid"
  },
  "payload": {
    "id": "user-object-id",
    "uuid": "user-uuid",
    "name": "...",
    "email": "...",
    ...
  }
}
```

**Topic:** `UserCreated`
**Trigger:** Register confirm başarılı

---

#### UserDeleted
```json
{
  "metadata": { ... },
  "payload": {
    "id": "user-object-id",
    "uuid": "user-uuid",
    ...
  }
}
```

**Topic:** `UserDeleted`
**Trigger:** Delete account başarılı

---

## 🧪 Test Senaryoları

### Happy Path - Full Registration

```bash
# 1. Register
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test",
    "surname": "User",
    "email": "test@test.com",
    "password": "Test123!",
    "tel": "5551234567",
    "tc": "12345678901"
  }'
# Response: {"sessionId": "..."}

# 2. Confirm (kodla beraber)
curl -X POST http://localhost:8080/auth/register/confirm \
  -H "Content-Type: application/json" \
  -d '{
    "session-id": "...",
    "code": "1234"
  }'
# Response: {"access_token": "..."}
# Set-Cookie: Refresh-Token=...
```

### Token Refresh

```bash
curl -X POST http://localhost:8080/auth/refresh \
  -H "Cookie: Refresh-Token=<token>"
```

### Password Change

```bash
curl -X POST http://localhost:8080/auth/password-change \
  -H "Authorization: Bearer <access_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "old_password": "Test123!",
    "new_password": "NewTest123!"
  }'
```

---

## 📝 Notlar

### Bilinmesi Gerekenler

1. **Password Validation:** Client-side validation önerilir (min 8 karakter, büyük/küçük harf, rakam)
2. **Token Invalidation:** Multi-device support için token stratejisi değiştirilmeli
3. **CORS:** Production'da frontend domain'i için CORS ayarları yapılmalı
4. **SameSite Cookie:** CSRF koruması için eklenebilir
5. **Logging:** Structured logging (zap, logrus) eklenebilir
6. **Metrics:** Prometheus metrics eklenebilir

### Best Practices

- ✅ Access token'ı localStorage'da değil **memory'de** tutun (daha güvenli)
- ✅ Refresh işlemini **otomatik** yapın (401 aldığında)
- ✅ Logout'ta hem access hem refresh token'ı temizleyin
- ✅ Password değişikliğinde kullanıcıyı otomatik logout edin

---

## 📞 Destek

Sorularınız için:
- **Dokümantasyon:** `/docs/openapi.yaml`
- **Kod:** `/microservices/auth-service/`
- **Team Lead:** [Sorumlu kişi adı]

---

**Son Güncelleme:** 2025-12-14  
**Versiyon:** 1.0.0 (Production Ready)
