# EC2 Database Servisleri Kurulum Raporu

**Tarih:** 23 Aralık 2025  
**EC2 Instance:** `i-07cb736452543b201` (51.20.93.33)  
**Private IP:** `10.0.0.166`

---

## ✅ Başarıyla Kurulan Servisler

### 1. MongoDB ✅
- **Container:** `local-mongo`
- **Port:** `27017`
- **Status:** ✅ Çalışıyor
- **Credentials:**
  - Username: `root`
  - Password: `TrioBank123`

### 2. Redis ✅
- **Container:** `local-redis`
- **Port:** `6379`
- **Status:** ✅ Çalışıyor
- **Password:** `TrioBank123`
- **Features:** AOF (Append Only File) aktif

### 3. Vault ✅
- **Container:** `local-vault`
- **Port:** `8200`
- **Status:** ✅ Çalışıyor (initialize ve unseal edilmeli)
- **Config:** `/vault/config/local.json`
- **Storage:** File-based (`/vault/file`)

---

## ⚠️ MSSQL Server - Devre Dışı

### Sorun
MSSQL Server 2022 en az **2GB RAM** gerektiriyor, ancak EC2 instance `t2.micro` (1GB RAM) kullanıyor.

### Hata Mesajı
```
sqlservr: This program requires a machine with at least 2000 megabytes of memory.
```

### Çözüm Önerileri

#### Seçenek 1: EC2 Instance Type'ı Değiştir (Önerilen)
```bash
# Terraform'da variables.tf dosyasını güncelle:
ec2_instance_type = "t2.small"  # 2GB RAM
# veya
ec2_instance_type = "t3.small"  # 2GB RAM (daha yeni nesil)
```

**Not:** `t2.small` ve `t3.small` Free Tier kapsamında değil, ücretli.

#### Seçenek 2: MSSQL'i Geçici Olarak Devre Dışı Bırak
MSSQL gerektiren servisler (Account, Transaction, Ledger, Card) şimdilik çalışmayacak.

#### Seçenek 3: MSSQL Express Kullan (Deneysel)
MSSQL Express daha az RAM kullanır, ancak yine de 1GB'den fazla gerektirebilir.

---

## 📋 Vault Kurulum Adımları

### 1. Vault'u Initialize Et
```bash
ssh -i infrastructure/terraform/triobank-ec2-key.pem ec2-user@51.20.93.33
cd ~/triobank-databases
docker exec -ti local-vault vault operator init
```

**Önemli:** Çıkan 5 Unseal Key'i ve Root Token'ı **MUTLAKA** saklayın!

### 2. Vault'u Unseal Et
```bash
# İlk 3 Unseal Key ile unseal et:
docker exec -ti local-vault vault operator unseal <KEY_1>
docker exec -ti local-vault vault operator unseal <KEY_2>
docker exec -ti local-vault vault operator unseal <KEY_3>
```

### 3. Vault'a Giriş Yap
```bash
docker exec -ti local-vault vault login <ROOT_TOKEN>
```

### 4. Vault Secret'larını Ekle
```bash
# Secret engine'ı etkinleştir
docker exec -ti local-vault vault secrets enable -path=secret kv-v2

# Secret'ları ekle (örnek: auth service)
docker exec -ti local-vault vault kv put secret/prod/services/auth \
  mongo_username=root \
  mongo_password=TrioBank123 \
  redis_password=TrioBank123 \
  secret_key=your-secret-key \
  token_signature=your-token-signature
```

---

## 🔗 EKS'ten EC2'ye Bağlantı

### Network Connectivity
- ✅ EC2 ve EKS aynı VPC'de (`vpc-00b2d2256a7ef11f2`)
- ✅ EC2 Private IP: `10.0.0.166` (prod overlay'lerde kullanılıyor)
- ✅ Security Group: VPC CIDR'den gelen trafiğe izin veriyor

### Kubernetes ExternalName Services
Tüm prod overlay'lerde `externalName: "10.0.0.166"` kullanılıyor:
- ✅ Account Service → MSSQL (MSSQL çalışmadığı için şimdilik çalışmayacak)
- ✅ Transaction Service → MSSQL
- ✅ Ledger Service → MSSQL
- ✅ Card Service → MSSQL
- ✅ Client Service → MSSQL
- ✅ Auth Service → MongoDB + Redis
- ✅ API Gateway → Redis

---

## 📊 Servis Durumu Özeti

| Servis | Durum | Port | Not |
|--------|-------|------|-----|
| MongoDB | ✅ Çalışıyor | 27017 | Ready |
| Redis | ✅ Çalışıyor | 6379 | Ready |
| Vault | ✅ Çalışıyor | 8200 | Init/Unseal gerekli |
| MSSQL | ❌ Devre Dışı | 1433 | RAM yetersiz (2GB gerekli) |

---

## 🎯 Sonraki Adımlar

1. ✅ MongoDB, Redis, Vault çalışıyor
2. ⏳ Vault'u init ve unseal et
3. ⏳ Vault'a secret'ları ekle
4. ⚠️  MSSQL için EC2 instance type'ı değiştir (t2.small veya t3.small)
5. ⏳ Kubernetes'te External Secrets Operator'ı yapılandır
6. ⏳ Servisleri deploy et

---

## 📝 Notlar

- **MSSQL:** EC2 instance type'ı `t2.small` veya `t3.small` yapılmalı (ücretli)
- **Vault:** Init ve unseal işlemleri yapılmadı, yapılmalı
- **Secret'lar:** Vault'a secret'lar eklenecek (prod/services/* path'lerinde)
- **Network:** EC2-EKS bağlantısı hazır, servisler deploy edilebilir

