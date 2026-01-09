# TrioBank Data Seeding Tool

50.000 kullanıcı ve ilgili verileri TrioBank platformuna eklemek için standalone Go script'i.

## 🎯 Ne Yapar?

Bu script **doğrudan veritabanlarına** bağlanarak şu kayıtları oluşturur:

- **50.000 User** (MongoDB - Auth Service)
- **50.000 Client** (MSSQL - Client Service)
- **~100.000 Account** (her kullanıcı için 1-3 hesap)
- **~50.000 Card** (her kullanıcı için 0-2 kart)
- **~300.000 Transaction** (her kullanıcı için 0-10 işlem)
- **~600.000 Ledger Entry** (her transaction için 2 entry)

**Toplam: ~1.150.000 kayıt**

## ✨ Özellikler

- ✅ **Gerçekçi Türkçe veriler**: Geçerli TC kimlik numaraları, Türkçe isimler, adresler
- ✅ **Foreign key tutarlılığı**: Tüm ilişkilendirmeler doğru şekilde yapılır
- ✅ **Batch processing**: 1000'lik gruplar halinde hızlı işleme
- ✅ **Progress tracking**: Anlık ilerleme ve tahmini süre
- ✅ **Hata yönetimi**: Transaction desteği ile atomik işlemler

## 🚀 Hızlı Başlangıç

### 1. Config Dosyasını Düzenle

```bash
cd /home/sametztrk/Desktop/triobank/scripts/seed_data
cp config.env.example config.env
nano config.env
```

Veritabanı bağlantı bilgilerinizi girin. **Özellikle port numaralarını kontrol edin!**

### 2. Dependencies Yükle

```bash
go mod download
```

### 3. Script'i Çalıştır

```bash
# Tam run: 50.000 kullanıcı
go run . 

# Küçük test: 100 kullanıcı ile test edin
TOTAL_USERS=100 go run .

# Sadece mevcut kayıtları kontrol et
go run . --verify
```

## ⏱️ Beklenen Süreler

| Kullanıcı Sayısı | Tahmini Süre |
|------------------|--------------|
| 100 (test) | ~30 saniye |
| 1.000 | ~5 dakika |
| 10.000 | ~30 dakika |
| 50.000 | **~2-3 saat** |

*Süreler sisteminizin performansına göre değişir.*

## 📋 Önkoşullar

Tüm servisler **çalışıyor** olmalı:

```bash
# MongoDB (Port 27017)
docker ps | grep AuthServiceDB

# MSSQL Servisleri
docker ps | grep MSSQL
```

## 🔧 Konfigürasyon

### config.env Örneği

```bash
MONGO_URI=mongodb://root:Triobank@1234@localhost:27017
MONGO_DB=auth_db

CLIENT_DB_URI=sqlserver://sa:ClientService@2024!@localhost:1433?database=client_db
# ... diğer servisler

TOTAL_USERS=50000
BATCH_SIZE=1000
```

### Port Listesi (Varsayılan)

- **MongoDB**: 27017
- **Client Service MSSQL**: 1433
- **Account Service MSSQL**: 1434
- **Card Service MSSQL**: 1435
- **Transaction Service MSSQL**: 1436
- **Ledger Service MSSQL**: 1437

## 📊 Oluşturulan Veri Örnekleri

### User
- **TC Kimlik**: Geçerli algoritma ile oluşturulur (11 haneli)
- **Email**: `user_12345_a1b2c3d4@triobank.test`
- **Telefon**: `+90 555 123 45 67` formatında
- **Şifre**: Hepsi `Triobank123!` (bcrypt hash)

### Account
- **IBAN**: TR + checksum + unique number
- **Para Birimleri**: TRY, USD, EUR
- **Bakiye**: 1.000 - 100.000 TRY arası

### Card
- **Kart Numaraları**: Luhn algoritması ile geçerli VISA numaraları
- **Tip**: DEBIT veya VIRTUAL
- **CVV**: 3 haneli geçerli kod

## 🐛 Sorun Giderme

### "MongoDB connection error"
```bash
# MongoDB çalışıyor mu?
docker ps | grep AuthServiceDB

# Port doğru mu?
netstat -an | grep 27017
```

### "MSSQL connection error"
```bash
# Tüm MSSQL containerları çalışıyor mu?
docker ps | grep MSSQL

# Portları kontrol et
docker ps --format "{{.Names}}\t{{.Ports}}" | grep MSSQL
```

### "Failed to insert" hataları
- Foreign key hatası: Sıralı ekleme yapıldığından bu olmamalı
- Unique constraint: Script tekrar çalıştırılıyor olabilir, veritabanlarını temizleyin

## 🧹 Veritabanını Temizleme

```bash
# MongoDB
mongo auth_db --eval "db.Users.deleteMany({})"

# MSSQL (her servis için)
sqlcmd -S localhost,1433 -d client_db -Q "TRUNCATE TABLE clients"
sqlcmd -S localhost,1434 -d account_db -Q "TRUNCATE TABLE accounts"
# ... diğer servisler
```

## ✅ Doğrulama

Script tamamlandıktan sonra:

```bash
# Otomatik doğrulama
go run . --verify

# Manuel sorgu örnekleri
mongo auth_db --eval "db.Users.count()"
sqlcmd -S localhost,1433 -d client_db -Q "SELECT COUNT(*) FROM clients"
```

## 📝 Notlar

- ⚠️ **Bu script Kafka eventlerini BYPASS eder** - Doğrudan veritabanına yazar
- ⚠️ **Production'da kullanmayın** - Sadece test/development için
- ✅ **İdempotent değil** - Aynı verileri tekrar ekleyemezsiniz, önce temizleyin
- ✅ **Batch processing** - Bellek kullanımı optimize edilmiştir

## 🎉 Başarılı Çıktı Örneği

```
🚀 TrioBank Data Seeding Started...
📊 Target: 50000 users with related data
📦 Batch size: 1000

✅ MongoDB connected
✅ MSSQL connected: client-service
✅ MSSQL connected: account-service
...

📦 Processing batch 1-1000 (size: 1000)...
✅ Users inserted to MongoDB
✅ Clients inserted to MSSQL
✅ Accounts inserted
✅ Cards inserted
✅ Transactions inserted
✅ Ledger entries inserted
⏱  Batch completed in 45s | Total progress: 1000/50000 (2.0%) | Est. remaining: 37m

...

🎉 ✨ Data seeding completed! ✨
📊 Total users created: 50000
⏱  Total time: 2h 15m
⚡ Average: 6.17 users/second
```

## 📞 Destek

Sorun yaşarsanız:
1. `--verify` ile mevcut verileri kontrol edin
2. Docker containerlarının sağlıklı olduğunu doğrulayın
3. Log dosyalarını inceleyin
