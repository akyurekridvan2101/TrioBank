# EC2 Instance Type Manuel Güncelleme Rehberi

**Durum:** AWS Free Tier kısıtlaması nedeniyle Terraform ile `t3.medium`/`t3.large` oluşturulamıyor.  
**Çözüm:** AWS Console'dan manuel olarak instance type değiştirilebilir.

---

## 🎯 EC2 Nedir?

**Amazon EC2 (Elastic Compute Cloud):** AWS'de sanal sunucu (server) oluşturmanızı sağlayan servistir. Bilgisayarınız gibi çalışan, internet üzerinden erişebileceğiniz bir sunucudur.

### Temel Kavramlar:
- **Instance:** Bir EC2 sunucusu
- **Instance Type:** Sunucunun özellikleri (CPU, RAM, vb.)
  - `t3.micro`: 1 vCPU, 1GB RAM (Free Tier)
  - `t3.small`: 1 vCPU, 2GB RAM (MSSQL minimum)
  - `t3.medium`: 2 vCPU, 4GB RAM (Önerilen)
  - `t3.large`: 2 vCPU, 8GB RAM (Güvenli)
- **Public IP:** İnternet'ten erişim için IP adresi
- **Private IP:** VPC içinde erişim için IP adresi (EKS'ten bağlanmak için)

---

## 🔧 AWS Console'dan Instance Type Değiştirme

### Adım 1: AWS Console'a Giriş
1. https://console.aws.amazon.com adresine gidin
2. AWS hesabınızla giriş yapın
3. **EC2** servisini seçin

### Adım 2: Instance'ı Bul
1. Sol menüden **Instances** → **Instances** seçin
2. `triobank-cluster-databases` adlı instance'ı bulun
3. Instance'ı seçin (checkbox işaretleyin)

### Adım 3: Instance'ı Durdur
1. **Instance state** → **Stop instance** seçin
2. Onaylayın
3. Instance durumu **stopped** olana kadar bekleyin (1-2 dakika)

### Adım 4: Instance Type'ı Değiştir
1. Instance seçiliyken **Actions** → **Instance settings** → **Change instance type** seçin
2. **Instance type** dropdown'ından **t3.medium** (veya **t3.large**) seçin
3. **Apply** butonuna tıklayın

### Adım 5: Instance'ı Başlat
1. **Instance state** → **Start instance** seçin
2. Instance durumu **running** olana kadar bekleyin (1-2 dakika)

### Adım 6: Yeni IP Adreslerini Not Al
1. Instance seçiliyken **Details** sekmesinde:
   - **Public IPv4 address:** Yeni Public IP (SSH için)
   - **Private IPv4 address:** Yeni Private IP (EKS'ten bağlanmak için)

---

## 📝 Yeni IP Adreslerini Güncelleme

### 1. Terraform Output'tan Kontrol
```bash
cd infrastructure/terraform
terraform output ec2_private_ip
terraform output ec2_public_ip
```

### 2. Prod Overlay'lerdeki IP'leri Güncelle
Tüm servislerin `prod/values.yaml` dosyalarında `externalName` değerini yeni Private IP ile güncelleyin:

```yaml
databases:
  mssql:
    externalName: "YENİ_PRIVATE_IP"  # Örnek: 10.0.0.167
```

**Güncellenecek Dosyalar:**
- `services/account-service/k8s/overlays/prod/values.yaml`
- `services/transaction-service/k8s/overlays/prod/values.yaml`
- `services/ledger-service/k8s/overlays/prod/values.yaml`
- `services/card-service/k8s/overlays/prod/values.yaml`
- `services/client-service/k8s/overlays/prod/values.yaml`
- `services/auth-service/k8s/overlays/prod/values.yaml` (MongoDB ve Redis için)
- `services/api-gateway/k8s/overlays/prod/values.yaml` (Redis için)

---

## 🔗 EC2'ye Nasıl Bağlanılır?

### SSH ile Bağlanma
```bash
# Terraform output'tan SSH komutunu al
cd infrastructure/terraform
terraform output ec2_ssh_command

# Örnek çıktı:
# ssh -i triobank-ec2-key.pem ec2-user@51.20.93.33

# Komutu çalıştır
ssh -i triobank-ec2-key.pem ec2-user@YENİ_PUBLIC_IP
```

### İlk Bağlantıda
- "Are you sure you want to continue connecting?" sorusuna **yes** yazın
- EC2'ye bağlandığınızda terminal prompt'u değişecek: `[ec2-user@ip-10-0-0-xxx ~]$`

---

## 🐳 Docker Compose Servislerini Kurma

### Adım 1: EC2'ye Bağlan
```bash
ssh -i infrastructure/terraform/triobank-ec2-key.pem ec2-user@YENİ_PUBLIC_IP
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

### Adım 4: .env Dosyası Oluştur
```bash
cat > .env << 'EOF'
MSSQL_SA_PASSWORD=TrioBank123
MONGO_INITDB_ROOT_USERNAME=root
MONGO_INITDB_ROOT_PASSWORD=TrioBank123
REDIS_PASSWORD=TrioBank123
EOF
```

### Adım 5: Servisleri Başlat
```bash
docker-compose up -d
```

### Adım 6: Servis Durumunu Kontrol Et
```bash
docker-compose ps
```

---

## 🔐 Vault Kurulumu

### Adım 1: Vault'u Initialize Et
```bash
docker exec -ti local-vault vault operator init
```

**ÖNEMLİ:** Çıkan 5 Unseal Key'i ve Root Token'ı **MUTLAKA** saklayın!

### Adım 2: Vault'u Unseal Et
```bash
# İlk 3 Unseal Key ile unseal et:
docker exec -ti local-vault vault operator unseal <KEY_1>
docker exec -ti local-vault vault operator unseal <KEY_2>
docker exec -ti local-vault vault operator unseal <KEY_3>
```

### Adım 3: Vault'a Giriş Yap
```bash
docker exec -ti local-vault vault login <ROOT_TOKEN>
```

### Adım 4: Secret Engine'ı Etkinleştir
```bash
docker exec -ti local-vault vault secrets enable -path=secret kv-v2
```

---

## ✅ Kontrol Komutları

### Container Durumları
```bash
docker ps
# veya
docker-compose ps
```

### Servis Testleri
```bash
# MongoDB
docker exec local-mongo mongosh --eval 'db.adminCommand("ping")'

# Redis
docker exec local-redis redis-cli -a TrioBank123 ping

# Vault
docker exec local-vault vault status
```

### Logları Görüntüle
```bash
# Tüm servisler
docker-compose logs

# Belirli bir servis
docker-compose logs mssql
docker-compose logs mongodb
docker-compose logs redis
docker-compose logs vault
```

---

## 🎯 Özet

1. ✅ AWS Console'dan instance type'ı `t3.medium` veya `t3.large` yap
2. ✅ Yeni Private IP'yi not al
3. ✅ Prod overlay'lerdeki `externalName` değerlerini güncelle
4. ✅ EC2'ye SSH ile bağlan
5. ✅ Docker Compose dosyalarını oluştur
6. ✅ Servisleri başlat
7. ✅ Vault'u init ve unseal et

**Hazırsınız!** 🚀


