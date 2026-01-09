# Şifremi Unuttum (Forgot Password) Özelliği - Detaylı Plan ve Rapor

## 📋 Mevcut Durum Analizi



### Backend Durumu
- ❌ **Şifre sıfırlama endpoint'i YOK**
- ✅ Email gönderme sistemi mevcut (`SendMail` fonksiyonu)
- ✅ Redis session yönetimi mevcut
- ✅ Doğrulama kodu sistemi mevcut (Login/Register'da kullanılıyor)
- ✅ Şifre hash'leme mevcut (`pkg.HashPassword`, `pkg.HashedPasswordControl`)
- ✅ MongoDB kullanıcı veritabanı mevcut (TC, Email, HashedPassword alanları var)

### Frontend Durumu
- ⚠️ **Placeholder mevcut**: Login sayfasında "Şifremi Unuttum?" linki var ama sadece toast gösteriyor
- ❌ Şifre sıfırlama sayfaları/akışı YOK
- ✅ Doğrulama kodu sayfası mevcut (verify-email) - Tekrar kullanılabilir
- ✅ Timer sistemi mevcut (startVerificationTimer)
- ✅ Form validasyon sistemi mevcut

### Mevcut Sistem Akışı (Login/Register)
```
1. Kullanıcı TC + Şifre girer → POST /auth/login
2. Backend TC+Şifre doğrular → User bulur
3. Backend 4 haneli kod üretir (1000-9999)
4. Backend SessionId + Kod'u Redis'e kaydeder (3dk TTL)
5. Backend kodu email'e gönderir
6. Frontend doğrulama sayfasına yönlendirir
7. Kullanıcı kodu girer → POST /auth/login/confirm
8. Backend kod'u doğrular → Token'lar üretir → Kullanıcı giriş yapar
```

---

## 🎯 Önerilen Şifre Sıfırlama Akışı

### Akış Diagramı
```
[Şifremi Unuttum Linki]
        ↓
[Adım 1: TC + Email Giriş]
    Kullanıcı TC ve Email girer
        ↓
    POST /auth/forgot-password/initiate
        ↓
    Backend: TC + Email eşleşmesini kontrol eder
        ↓
    Backend: 4 haneli kod üretir
        ↓
    Backend: SessionId + Kod Redis'e kaydeder (3dk TTL, type: "password-reset")
        ↓
    Backend: Kodu email'e gönderir
        ↓
[Adım 2: Kod Doğrulama]
    Frontend: Doğrulama kod sayfasına yönlendirir (mevcut verify-email sayfası kullanılabilir)
        ↓
    Kullanıcı kodu girer
        ↓
    POST /auth/forgot-password/verify-code
        ↓
    Backend: Kod'u doğrular
        ↓
    Backend: SessionId'yi password-reset için işaretler
        ↓
[Adım 3: Yeni Şifre Belirleme]
    Frontend: Yeni şifre giriş sayfasına yönlendirir
        ↓
    Kullanıcı yeni şifreyi girer (2 kere: şifre + şifre tekrar)
        ↓
    POST /auth/forgot-password/reset
        ↓
    Backend: SessionId + Kod kontrolü yapar
        ↓
    Backend: Yeni şifreyi hash'ler
        ↓
    Backend: MongoDB'de şifreyi günceller
        ↓
    Backend: Redis'ten session'ı temizler
        ↓
[Başarılı: Giriş Sayfasına Yönlendirme]
    Frontend: "Şifreniz başarıyla değiştirildi. Giriş yapabilirsiniz" mesajı
        ↓
    Kullanıcı login sayfasına yönlendirilir
```

---

## 🔧 Backend İmplementasyon Planı

### 1. Yeni Endpoint'ler

#### `POST /auth/forgot-password/initiate`
**Amaç:** Şifre sıfırlama işlemini başlatır, TC+Email doğrular, kod gönderir

**Request Body:**
```json
{
  "tc": "11111111111",
  "email": "user@example.com"
}
```

**Response (200):**
```json
{
  "sessionId": "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXo="
}
```

**Hata Durumları:**
- `400`: Body eksik veya hatalı
- `401`: TC ve Email eşleşmiyor
- `404`: Kullanıcı bulunamadı
- `429`: Rate limit (3 dakikada 1 istek)
- `500`: Email gönderim hatası, Redis hatası

**İmplementasyon Detayları:**
- TC ile kullanıcıyı MongoDB'den bul
- Kullanıcının email'i ile girilen email'i karşılaştır
- Rate limiting: `setAndControlLimitByTc` kullan (login ile aynı)
- 4 haneli kod üret (1000-9999)
- SessionId üret (32 byte random)
- Redis'e kaydet: Key formatı `password-reset:{sessionId}` veya mevcut session sistemi kullanılabilir
- Redis value: `{"code": 1234, "userId": "...", "type": "password-reset"}`
- TTL: 3 dakika (180 saniye)
- Email gönder (`SendMail` fonksiyonu kullan)

---

#### `POST /auth/forgot-password/verify-code`
**Amaç:** Doğrulama kodunu kontrol eder, şifre sıfırlama için onay verir

**Request Body:**
```json
{
  "session-id": "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXo=",
  "code": "1234"
}
```

**Response (200):**
```json
{
  "verified": true,
  "sessionId": "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXo="
}
```

**Hata Durumları:**
- `400`: Body eksik veya kod formatı yanlış
- `401`: Kod yanlış veya session geçersiz
- `404`: Session bulunamadı (expire olmuş)
- `500`: Redis hatası

**İmplementasyon Detayları:**
- SessionId ve kod'u Redis'ten kontrol et
- Kod yanlışsa hata döndür
- Kod doğruysa session'ı password-reset için aktif tut (TTL'i uzatabilirsiniz)
- Session type'ını kontrol et (sadece "password-reset" tipindeki session'lar kabul edilmeli)

---

#### `POST /auth/forgot-password/reset`
**Amaç:** Yeni şifreyi kaydeder

**Request Body:**
```json
{
  "session-id": "YWJjZEVmZ2hpamtsbW5vcHFyc3R1dnd4eXo=",
  "code": "1234",
  "new_password": "NewPassword123!"
}
```

**Response (200):**
```
password reset successfully
```

**Hata Durumları:**
- `400`: Body eksik, şifre boş, şifre çok kısa
- `401`: Session veya kod geçersiz
- `404`: Session bulunamadı (expire olmuş)
- `500`: Şifre hash hatası, database güncelleme hatası

**İmplementasyon Detayları:**
- SessionId ve kod'u tekrar doğrula (güvenlik için)
- Session type'ını kontrol et (sadece "password-reset")
- UserId'yi session'dan al
- Yeni şifreyi hash'le (`pkg.HashPassword`)
- MongoDB'de kullanıcının şifresini güncelle
- Redis'ten session'ı sil (güvenlik için bir kere kullanılabilir hale getir)
- **ÖNEMLİ:** Tüm aktif refresh token'ları deaktif et (güvenlik için - kullanıcı tüm cihazlardan çıkış yapmalı)

---

### 2. Database Interface Güncellemeleri

**Yeni Fonksiyonlar:**
```go
// DataBaseI interface'ine eklenecek:
verifyUserEmail(ctx context.Context, tc string, email string) (User, error)
updatePassword(ctx context.Context, userId primitive.ObjectID, newHashedPassword string) error
```

**İmplementasyon:**
- `verifyUserEmail`: TC ile user bul, email'i karşılaştır
- `updatePassword`: Zaten var gibi görünüyor (handler.go'da kullanılıyor), kontrol edilmeli

---

### 3. Session Manager Güncellemeleri

**Redis Key Yapısı:**
```
Mevcut: session:{sessionId} -> {"code": 1234, "userId": "..."}
Önerilen: session:{sessionId} -> {"code": 1234, "userId": "...", "type": "password-reset"}
```

**Yeni Fonksiyonlar (opsiyonel):**
```go
savePasswordResetSession(ctx context.Context, userId primitive.ObjectID, sessionId string, code int64) error
verifyPasswordResetSession(ctx context.Context, sessionId string, code int64) (primitive.ObjectID, error)
```

---

### 4. Router Güncellemeleri

`internal/router.go` dosyasına eklenecek:
```go
http.HandleFunc("/auth/forgot-password/initiate", middleware(r.ForgotPasswordInitiate))
http.HandleFunc("/auth/forgot-password/verify-code", middleware(r.ForgotPasswordVerifyCode))
http.HandleFunc("/auth/forgot-password/reset", middleware(r.ForgotPasswordReset))
```

---

## 🎨 Frontend İmplementasyon Planı

### 1. Yeni Sayfalar

#### Sayfa: `forgot-password-step1` (TC + Email Giriş)
**Dosya:** `web/index.html`
**Konum:** Login sayfasından sonra

**Form Alanları:**
- T.C. Kimlik No (11 hane)
- E-Posta

**Validasyonlar:**
- TC: 11 hane, boş olamaz
- Email: Geçerli format, boş olamaz

**Akış:**
- Form submit → `POST /auth/forgot-password/initiate`
- Başarılı → `showPage('forgot-password-verify')` + sessionId kaydet
- Hata → Hata mesajı göster

---

#### Sayfa: `forgot-password-verify` (Kod Doğrulama)
**Dosya:** `web/index.html`
**Not:** Mevcut `verify-email` sayfası modifiye edilebilir veya yeni sayfa oluşturulabilir

**Form Alanları:**
- 4 haneli doğrulama kodu

**Akış:**
- Timer: 3 dakika (mevcut `startVerificationTimer` kullanılabilir)
- Form submit → `POST /auth/forgot-password/verify-code`
- Başarılı → `showPage('forgot-password-reset')`
- Hata → Hata mesajı göster

---

#### Sayfa: `forgot-password-reset` (Yeni Şifre Belirleme)
**Dosya:** `web/index.html`
**Yeni sayfa**

**Form Alanları:**
- Yeni Şifre (min 8 karakter)
- Yeni Şifre (Tekrar) - onay için

**Validasyonlar:**
- Şifre: Min 8 karakter, boş olamaz
- Şifre Tekrar: İlk şifre ile eşleşmeli

**Akış:**
- Form submit → `POST /auth/forgot-password/reset`
- Başarılı → Toast mesajı + `showPage('login')`
- Hata → Hata mesajı göster

---

### 2. JavaScript Fonksiyonları

**Yeni Fonksiyonlar (`web/app.js`):**
```javascript
// Şifre sıfırlama akışını başlat
async function initiatePasswordReset(tc, email)

// Şifre sıfırlama kodunu doğrula
async function verifyPasswordResetCode(sessionId, code)

// Yeni şifreyi kaydet
async function resetPassword(sessionId, code, newPassword)
```

**Değiştirilecek Fonksiyonlar:**
- Login sayfasındaki "Şifremi Unuttum?" linki → `showPage('forgot-password-step1')`

---

### 3. State Management

**SessionId Yönetimi:**
- `currentSessionId` değişkeni kullanılabilir (mevcut sistemde var)
- Yeni: `passwordResetSessionId` eklenebilir veya `currentSessionId` kullanılabilir

**Flow Type:**
- Mevcut: `authFlowType` = 'login' | 'register'
- Güncelleme: `authFlowType` = 'login' | 'register' | 'password-reset'

---

## 🔒 Güvenlik Önlemleri

### 1. Rate Limiting
- ✅ TC başına 3 dakikada 1 istek (mevcut sistemle uyumlu)
- ✅ Redis ile atomic kontrol (SetNX)

### 2. Session Yönetimi
- ✅ SessionId 32 byte random (yeterince güçlü)
- ✅ TTL: 3 dakika (kısa süre)
- ✅ Tek kullanımlık: Reset sonrası session silinmeli

### 3. Doğrulama
- ✅ TC + Email eşleşmesi zorunlu (email tahminini zorlaştırır)
- ✅ Kod doğrulaması zorunlu
- ✅ Session type kontrolü (sadece password-reset session'ları kabul)

### 4. Şifre Güvenliği
- ✅ Şifre hash'leme (bcrypt)
- ✅ Minimum 8 karakter (frontend validasyonu)
- ✅ Eski refresh token'ları deaktif et (tüm cihazlardan çıkış)

### 5. Güvenlik Açıkları Önleme
- ⚠️ Brute-force: Rate limiting ile korunuyor
- ⚠️ Session hijacking: HTTPS zorunlu, kısa TTL
- ⚠️ Email spoofing: Backend'de email kontrolü yok (production'da SPF/DKIM önerilir)

---

## 📝 İmplementasyon Adımları

### Backend (Öncelik: Yüksek)
1. ✅ Database interface'e `verifyUserEmail` ekle
2. ✅ Handler'a `ForgotPasswordInitiate` ekle
3. ✅ Handler'a `ForgotPasswordVerifyCode` ekle
4. ✅ Handler'a `ForgotPasswordReset` ekle
5. ✅ Router'a endpoint'leri ekle
6. ✅ Redis session yapısına `type` field'ı ekle (opsiyonel, mevcut yapı da kullanılabilir)
7. ✅ OpenAPI dokümantasyonunu güncelle

### Frontend (Öncelik: Yüksek)
1. ✅ `forgot-password-step1` sayfası ekle
2. ✅ `forgot-password-verify` sayfası ekle (veya mevcut verify-email'i modifiye et)
3. ✅ `forgot-password-reset` sayfası ekle
4. ✅ Login sayfasındaki linki güncelle
5. ✅ JavaScript fonksiyonlarını ekle
6. ✅ Validasyonları ekle
7. ✅ Hata mesajlarını ekle
8. ✅ Başarı akışını test et

### Test Senaryoları
1. ✅ TC + Email eşleşmesi doğru → Kod gönderilmeli
2. ✅ TC + Email eşleşmesi yanlış → Hata mesajı
3. ✅ Kod doğru → Yeni şifre sayfasına yönlendirme
4. ✅ Kod yanlış → Hata mesajı
5. ✅ Kod expire → Hata mesajı
6. ✅ Şifre değiştirme başarılı → Login sayfasına yönlendirme
7. ✅ Rate limiting → 3 dakikada 1 istek kontrolü
8. ✅ Session hijacking → Tek kullanımlık session kontrolü

---

## 🚨 Önemli Notlar ve Öneriler

### 1. Mevcut Sistemle Uyumluluk
- ✅ Login akışı ile aynı mantık (kod sistemi, email gönderimi, session yönetimi)
- ✅ Mevcut `verify-email` sayfası ve timer sistemi kullanılabilir
- ✅ Mevcut error handling ve toast sistemleri kullanılabilir

### 2. Alternatif Yaklaşımlar
- **Opsi 1:** Mevcut session sistemine `type` field'ı eklemek
- **Opsi 2:** Ayrı Redis key pattern kullanmak (`password-reset:{sessionId}`)
- **Opsi 3:** Mevcut `verify-email` sayfasını multi-purpose yapmak (type'a göre farklı davranış)

### 3. Gelecek İyileştirmeler
- 📧 Email template'i güzelleştirme
- 🔐 Şifre güçlülük kontrolü ekleme (büyük harf, küçük harf, rakam, özel karakter)
- 📱 SMS ile kod gönderme seçeneği
- 🔄 "Kodu tekrar gönder" butonu
- ⏱️ Timer'ı 5 dakikaya çıkarma (kullanıcı deneyimi için)

### 4. API Gateway Entegrasyonu
- ⚠️ `/auth/forgot-password/*` endpoint'leri authentication gerektirmemeli
- ⚠️ Rate limiting API Gateway'de de kontrol edilmeli

---

## ✅ Sonuç

Bu plan, mevcut sistem mimarisine uyumlu, güvenli ve kullanıcı dostu bir şifre sıfırlama özelliği sunar. İmplementasyon yaklaşık **2-3 gün** sürebilir (backend + frontend + test).

**Öncelik Sırası:**
1. Backend endpoint'leri (1 gün)
2. Frontend sayfaları ve validasyonlar (1 gün)
3. Test ve hata düzeltmeleri (0.5-1 gün)

**Riskler:**
- ⚠️ Email servisi çalışmazsa kod gönderilemez
- ⚠️ Redis down olursa session kaybolur
- ⚠️ Rate limiting çok sıkı olursa kullanıcı deneyimi kötüleşir

**Önerilen İlk Test:**
1. Backend endpoint'lerini Postman ile test et
2. Frontend'i backend'e bağla
3. End-to-end akışı test et
4. Güvenlik testleri yap (rate limiting, session hijacking)

---

**Hazırlayan:** AI Assistant  
**Tarih:** 2024  
**Versiyon:** 1.0

