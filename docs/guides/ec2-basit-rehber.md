# EC2 Basit Rehber - Adım Adım

**Yeni EC2 Instance:**
- **Instance ID:** `i-002bb560ad379fea5`
- **Public IP:** `13.62.52.123` (SSH için)
- **Private IP:** `10.0.0.182` (EKS'ten bağlanmak için)
- **Instance Type:** `t3.small` (2GB RAM - MSSQL minimum gereksinim)

---

## 🎯 EC2 Nedir?

**EC2 = Elastic Compute Cloud**

Basitçe: **AWS'de bir bilgisayar/sunucu kiralamak**. Bu bilgisayar:
- İnternet üzerinden erişilebilir
- İstediğiniz yazılımları çalıştırabilirsiniz
- İstediğiniz zaman açıp kapatabilirsiniz

**Bizim Durumumuzda:** EC2'de MSSQL, MongoDB, Redis ve Vault çalıştırıyoruz.

---

## 🔗 EC2'ye Nasıl Bağlanılır?

### 1. SSH Nedir?
**SSH (Secure Shell):** Uzaktan bir bilgisayara güvenli şekilde bağlanmak için kullanılan protokol.

### 2. Bağlantı Komutu
```bash
ssh -i infrastructure/terraform/triobank-ec2-key.pem ec2-user@13.62.52.123
```

**Komut Açıklaması:**
- `ssh`: SSH bağlantısı yap
- `-i infrastructure/terraform/triobank-ec2-key.pem`: Özel anahtar dosyası (şifre yerine)
- `ec2-user`: EC2'deki kullanıcı adı (Amazon Linux için)
- `@13.62.52.123`: EC2'nin Public IP adresi

### 3. İlk Bağlantı
İlk kez bağlanırken şu mesajı göreceksiniz:
```
The authenticity of host '13.62.52.123' can't be established.
Are you sure you want to continue connecting (yes/no)?
```
**Cevap:** `yes` yazın ve Enter'a basın.

### 4. Bağlantı Başarılı!
Bağlandığınızda terminal prompt'u değişecek:
```
[ec2-user@ip-10-0-0-182 ~]$
```
Bu, artık EC2 içinde olduğunuz anlamına gelir!

---

## 🐳 Docker ve Docker Compose Nedir?

### Docker
- **Container:** Bir uygulama ve tüm bağımlılıklarını paketleyen bir kutu
- **Image:** Container'ın şablonu (örnek: `mongo:6.0`, `redis:7.0-alpine`)
- **Container:** Image'den çalışan bir örnek

**Örnek:** MongoDB image'inden bir container başlatırsınız, MongoDB çalışır.

### Docker Compose
- **Birden fazla container'ı birlikte yönetmek** için kullanılan araç
- `docker-compose.yaml` dosyasında tüm servisleri tanımlarsınız
- Tek komutla hepsini başlatıp durdurabilirsiniz

**Bizim Durumumuzda:** MSSQL, MongoDB, Redis ve Vault'u birlikte yönetiyoruz.

---

## 📝 Adım Adım: EC2'de Servisleri Kurma

### Adım 1: EC2'ye Bağlan
```bash
# Proje dizininden
cd /home/akyurek2101/Desktop/triobank
ssh -i infrastructure/terraform/triobank-ec2-key.pem ec2-user@13.62.52.123
```

### Adım 2: Çalışma Dizini Oluştur
```bash
mkdir -p ~/triobank-databases
cd ~/triobank-databases
```

### Adım 3: Docker Compose Dosyası Oluştur
```bash
cat > docker-compose.yaml << 'EOF'
services:
  mssql:
    image: mcr.microsoft.com/mssql/server:2022-latest
    container_name: local-mssql
    hostname: mssql
    restart: unless-stopped
    ports:
      - "1433:1433"
    environment:
      - ACCEPT_EULA=Y
      - SA_PASSWORD=${MSSQL_SA_PASSWORD}
      - MSSQL_PID=Developer
      - MSSQL_AGENT_ENABLED=true
    volumes:
      - mssql_data:/var/opt/mssql

  mongodb:
    image: mongo:6.0
    container_name: local-mongo
    hostname: mongodb
    restart: unless-stopped
    ports:
      - "27017:27017"
    environment:
      - MONGO_INITDB_ROOT_USERNAME=${MONGO_INITDB_ROOT_USERNAME}
      - MONGO_INITDB_ROOT_PASSWORD=${MONGO_INITDB_ROOT_PASSWORD}
    volumes:
      - mongo_data:/data/db

  redis:
    image: redis:7.0-alpine
    container_name: local-redis
    hostname: redis
    restart: unless-stopped
    ports:
      - "6379:6379"
    command: redis-server --requirepass ${REDIS_PASSWORD} --appendonly yes
    volumes:
      - redis_data:/data

  vault:
    image: hashicorp/vault:1.15
    container_name: local-vault
    hostname: vault
    restart: unless-stopped
    ports:
      - "8200:8200"
    cap_add:
      - IPC_LOCK
    environment:
      VAULT_ADDR: 'http://0.0.0.0:8200'
      VAULT_API_ADDR: 'http://0.0.0.0:8200'
      VAULT_LOCAL_CONFIG: '{"storage": {"file": {"path": "/vault/file"}}, "listener": {"tcp": {"address": "0.0.0.0:8200", "tls_disable": true}}, "ui": true, "disable_mlock": true}'
    volumes:
      - vault_data:/vault/file

volumes:
  mssql_data:
  mongo_data:
  redis_data:
  vault_data:
EOF
```

**Ne Yaptık?**
- `docker-compose.yaml` dosyası oluşturduk
- 4 servis tanımladık: MSSQL, MongoDB, Redis, Vault
- Her servis için port, environment variable ve volume tanımladık

### Adım 4: .env Dosyası Oluştur (Şifreler)
```bash
cat > .env << 'EOF'
MSSQL_SA_PASSWORD=TrioBank123
MONGO_INITDB_ROOT_USERNAME=root
MONGO_INITDB_ROOT_PASSWORD=TrioBank123
REDIS_PASSWORD=TrioBank123
EOF
```

**Ne Yaptık?**
- Şifreleri `.env` dosyasına yazdık
- Docker Compose bu dosyayı okuyup environment variable'lara aktaracak

### Adım 5: Servisleri Başlat
```bash
docker-compose up -d
```

**Komut Açıklaması:**
- `docker-compose up`: Servisleri başlat
- `-d`: Detached mode (arka planda çalıştır, terminal'i bloklama)

**Ne Olacak?**
- Docker image'ler indirilecek (ilk seferde)
- Container'lar oluşturulacak ve başlatılacak
- 1-2 dakika sürebilir

### Adım 6: Servis Durumunu Kontrol Et
```bash
docker-compose ps
```

**Beklenen Çıktı:**
```
NAME          IMAGE                  STATUS
local-mongo   mongo:6.0              Up X seconds
local-redis   redis:7.0-alpine      Up X seconds
local-vault   hashicorp/vault:1.15  Up X seconds
local-mssql   mssql/server:2022     Up X seconds
```

**Hepsi "Up" görünüyorsa:** ✅ Başarılı!

---

## 🔐 Vault Kurulumu (Şifre Kasası)

### Adım 1: Vault'u Initialize Et
```bash
docker exec -ti local-vault vault operator init
```

**Ne Olacak?**
- 5 Unseal Key ve 1 Root Token üretilecek
- **ÖNEMLİ:** Bu key'leri ve token'ı **MUTLAKA** saklayın!

**Örnek Çıktı:**
```
Unseal Key 1: abc123...
Unseal Key 2: def456...
Unseal Key 3: ghi789...
Unseal Key 4: jkl012...
Unseal Key 5: mno345...
Initial Root Token: s.xyz789...
```

### Adım 2: Vault'u Unseal Et
Vault güvenlik için "sealed" (mühürlü) durumda. 3 key ile açmanız gerekir:

```bash
docker exec -ti local-vault vault operator unseal <KEY_1>
docker exec -ti local-vault vault operator unseal <KEY_2>
docker exec -ti local-vault vault operator unseal <KEY_3>
```

**Her komuttan sonra:**
```
Unseal Progress: 1/3
Unseal Progress: 2/3
Unseal Progress: 3/3
Sealed: false
```

**"Sealed: false" görünüyorsa:** ✅ Başarılı!

### Adım 3: Vault'a Giriş Yap
```bash
docker exec -ti local-vault vault login <ROOT_TOKEN>
```

**"Success!" mesajı görünüyorsa:** ✅ Başarılı!

### Adım 4: Secret Engine'ı Etkinleştir
```bash
docker exec -ti local-vault vault secrets enable -path=secret kv-v2
```

**"Success!" mesajı görünüyorsa:** ✅ Başarılı!

---

## ✅ Kontrol Komutları

### Container Durumları
```bash
# Tüm container'lar
docker ps

# Sadece bizim container'lar
docker-compose ps
```

### Servis Testleri
```bash
# MongoDB test
docker exec local-mongo mongosh --eval 'db.adminCommand("ping")'
# Beklenen: { ok: 1 }

# Redis test
docker exec local-redis redis-cli -a TrioBank123 ping
# Beklenen: PONG

# Vault durumu
docker exec local-vault vault status
# Beklenen: Sealed: false
```

### Logları Görüntüle
```bash
# Tüm servisler
docker-compose logs

# Belirli bir servis
docker-compose logs mssql
docker-compose logs mongodb
```

---

## 🔄 Yaygın İşlemler

### Servisleri Durdur
```bash
docker-compose stop
```

### Servisleri Başlat
```bash
docker-compose start
```

### Servisleri Yeniden Başlat
```bash
docker-compose restart
```

### Servisleri Durdur ve Sil
```bash
docker-compose down
```

### Servisleri Durdur, Sil ve Volume'leri Sil
```bash
docker-compose down -v
```

### EC2'den Çık
```bash
exit
# veya
Ctrl+D
```

---

## 🎯 Özet - Ne Öğrendik?

1. ✅ **EC2:** AWS'de sanal sunucu
2. ✅ **SSH:** Uzaktan bağlanma yöntemi
3. ✅ **Docker:** Container teknolojisi
4. ✅ **Docker Compose:** Birden fazla container yönetimi
5. ✅ **Vault:** Şifre kasası, init ve unseal gerekli

**Artık EC2'de servisleri kurup yönetebilirsiniz!** 🚀

---

## 📝 Sonraki Adımlar

1. ✅ EC2'de servisleri kur
2. ⏳ Vault'a secret'ları ekle (prod/services/* path'lerinde)
3. ⏳ Prod overlay'lerdeki Private IP'yi güncelle: `10.0.0.182`
4. ⏳ Kubernetes'te servisleri deploy et

