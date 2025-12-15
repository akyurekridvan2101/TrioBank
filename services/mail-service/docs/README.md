# Mail Service Documentation

## 📧 Genel Bakış

Mail servisi, TrioBank platformunda e-posta gönderimi için kullanılan internal mikroservistir. Resend API kullanarak güvenli ve hızlı e-posta gönderimi sağlar.

### Temel Özellikler
- ✅ Resend API entegrasyonu
- ✅ Internal servisler arası güvenlik (X-Internal-Secret)
- ✅ Context-based timeout yönetimi
- ✅ Health check endpoint
- ✅ Docker containerized deployment

---

## 🚀 API Endpoints

### 1. **POST /send**
E-posta gönderir (doğrulama kodu için).

**Headers:**
```
X-Internal-Secret: your-secret-key
X-Request-Deadline: 1702563600000
Content-Type: application/json
```

**Request Body:**
```json
{
  "receiver": "user@example.com",
  "code": "123456"
}
```

**Response:**
- `200 OK` - E-posta başarıyla gönderildi
- `400 Bad Request` - Geçersiz request body
- `401 Unauthorized` - X-Internal-Secret geçersiz
- `500 Internal Server Error` - E-posta gönderilemedi

---

### 2. **GET /health**
Servis sağlık kontrolü.

**Response:**
```
200 OK
```

---

## 🔒 Güvenlik

### Internal Secret
Tüm `/send` istekleri `X-Internal-Secret` header'ı ile korunmaktadır.

```bash
X-Internal-Secret: <SECRET_KEY from .env>
```

### Request Deadline
Her request için deadline belirlenmesi zorunludur:

```bash
X-Request-Deadline: <Unix timestamp in milliseconds>
```

---

## 🐳 Docker Deployment

### Build
```bash
docker build -t mail-service:latest .
```

### Run
```bash
docker run -d \
  -p 8081:8081 \
  --name MailService \
  --env-file config/.env \
  --network triobank-network \
  mail-service:latest
```

### Docker Compose
```bash
docker-compose up -d
```

---

## 🔧 Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `SENDER_MAIL` | Gönderen e-posta adresi | `noreply@triobank.com` |
| `SENDER_PASSWORD` | SMTP şifresi (Gmail App Password) | `xxxx xxxx xxxx xxxx` |
| `SMTP_HOST` | SMTP sunucusu | `smtp.gmail.com` |
| `SMTP_PORT` | SMTP port | `587` |
| `SECRET_KEY` | Internal servisler arası güvenlik anahtarı | `your-secret-key` |
| `MAIL_SERVICE_PORT` | Servisin dinlediği port | `0.0.0.0:8081` |
| `RESEND_API_KEY` | Resend API anahtarı | `re_xxx...` |

---

## 📝 Kullanım Örnekleri

### cURL
```bash
curl -X POST http://localhost:8081/send \
  -H "Content-Type: application/json" \
  -H "X-Internal-Secret: your-secret-key" \
  -H "X-Request-Deadline: 1702563600000" \
  -d '{
    "receiver": "user@example.com",
    "code": "123456"
  }'
```

### Go (Auth Service'den)
```go
import (
    "bytes"
    "encoding/json"
    "net/http"
    "time"
)

func sendVerificationEmail(email, code string) error {
    url := "http://MailService:8081/send"
    
    data := map[string]string{
        "receiver": email,
        "code":     code,
    }
    
    body, _ := json.Marshal(data)
    req, _ := http.NewRequest("POST", url, bytes.NewReader(body))
    
    req.Header.Set("Content-Type", "application/json")
    req.Header.Set("X-Internal-Secret", os.Getenv("SECRET_KEY"))
    req.Header.Set("X-Request-Deadline", fmt.Sprint(time.Now().Add(10*time.Second).UnixMilli()))
    
    client := &http.Client{}
    resp, err := client.Do(req)
    if err != nil {
        return err
    }
    defer resp.Body.Close()
    
    if resp.StatusCode != 200 {
        return fmt.Errorf("mail service error: %d", resp.StatusCode)
    }
    
    return nil
}
```

---

## 🏥 Health Check

### Docker Health Check
```yaml
healthcheck:
  test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8081/health"]
  interval: 30s
  timeout: 3s
  retries: 3
  start_period: 10s
```

### Manual Check
```bash
curl http://localhost:8081/health
# Response: OK
```

---

## 🔄 Servisler Arası İletişim

```
┌─────────────┐                    ┌──────────────┐
│             │  POST /send        │              │
│ Auth Service│ ──────────────────>│ Mail Service │
│             │  X-Internal-Secret │              │
└─────────────┘                    └──────────────┘
                                          │
                                          ▼
                                   ┌─────────────┐
                                   │ Resend API  │
                                   └─────────────┘
                                          │
                                          ▼
                                   📧 User Email
```

---

## 📊 Error Handling

Mail service şu hataları döndürür:

| Status | Durum | Açıklama |
|--------|-------|----------|
| 200 | Success | E-posta başarıyla gönderildi |
| 400 | Bad Request | Request body parse edilemedi |
| 401 | Unauthorized | X-Internal-Secret header yanlış veya eksik |
| 500 | Server Error | Resend API hatası veya timeout |

---

## 🛠️ Development

### Yerel Geliştirme
```bash
# .env dosyasını yapılandır
cp config/.env.example config/.env

# Servisi çalıştır
go run cmd/main.go
```

### Test
```bash
# Health check
curl http://localhost:8081/health

# E-posta gönderme testi
curl -X POST http://localhost:8081/send \
  -H "Content-Type: application/json" \
  -H "X-Internal-Secret: $(grep SECRET_KEY config/.env | cut -d'=' -f2 | tr -d '\"')" \
  -H "X-Request-Deadline: $(date -d '+10 seconds' +%s)000" \
  -d '{"receiver":"test@example.com","code":"123456"}'
```

---

## 📦 Dependencies

- `github.com/joho/godotenv` - Environment variable management
- `github.com/resend/resend-go/v3` - Resend API client

---

## 🔗 İlgili Servisler

- **Auth Service**: Mail service kullanıcısı (doğrulama kodları)
- **Gateway**: Mail service proxy

---

## 📄 OpenAPI Specification

Detaylı API dokümantasyonu için [openapi.yaml](./openapi.yaml) dosyasına bakın.

## 🐛 Troubleshooting

### E-posta gönderilemiyor
1. `RESEND_API_KEY` kontrol edin
2. `SENDER_MAIL` doğru mu kontrol edin
3. Resend dashboard'da API key aktif mi?

### 401 Unauthorized
- `X-Internal-Secret` header değeri `.env` ile eşleşiyor mu?

### 500 Internal Server Error
- Request deadline geçerli mi? (gelecekte bir zaman olmalı)
- Resend API limitlerine ulaşılmış olabilir

---

## 📞 İletişim

Sorularınız için: TrioBank Development Team
