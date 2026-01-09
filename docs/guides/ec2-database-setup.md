# EC2'de Veritabanları Kurulum Rehberi

Bu doküman, EC2 instance'ında MSSQL, MongoDB, Redis ve Vault'u Docker Compose ile kurmayı açıklar.

## 🎯 Senaryo

```
┌─────────────────┐
│   EKS Cluster   │  ← Mikroservisleriniz (Kubernetes)
│  (triobank)     │
└────────┬────────┘
         │ ExternalName Service
         │ DNS: mssql.ec2.internal
         │      mongodb.ec2.internal
         │      redis.ec2.internal
         ↓
┌─────────────────┐
│  EC2 Instance   │  ← Veritabanları (Docker Compose)
│  (t2.micro)     │
│  IP: 1.2.3.4    │
│                 │
│  Docker Compose │
│  - MSSQL:1433   │
│  - MongoDB:27017│
│  - Redis:6379   │
│  - Vault:8200   │
└─────────────────┘
```

## 📋 Adım Adım Plan

### ADIM 1: EC2 Instance Oluşturma
### ADIM 2: SSH ile Bağlanma
### ADIM 3: Docker Kurulumu
### ADIM 4: Docker Compose Kurulumu
### ADIM 5: Servisleri Başlatma
### ADIM 6: EKS'ten Bağlantı

---

## ADIM 1: EC2 Instance Oluşturma

### 1.1 AWS Console'dan Oluşturma

1. **AWS Console** → **EC2** → **Launch Instance**

2. **Name**: `triobank-databases`

3. **AMI (İşletim Sistemi)**: 
   - **Amazon Linux 2023** veya **Amazon Linux 2** seçin
   - (Ücretsiz tier için uygun)

4. **Instance Type**:
   - **t2.micro** seçin (12 ay ücretsiz)
   - 1 vCPU, 1 GB RAM

5. **Key Pair**:
   - **Create new key pair** tıklayın
   - Name: `triobank-ec2-key`
   - Key pair type: **RSA**
   - Private key file format: **.pem**
   - **Create key pair** → Key otomatik indirilir (`.pem` dosyası)

6. **Network Settings**:
   - **Security Group**: Create new security group
   - **SSH (22)**: My IP (sadece sizin IP'nizden)
   - **Custom TCP (1433)**: EKS cluster'ın VPC CIDR'ı (10.0.0.0/16)
   - **Custom TCP (27017)**: EKS cluster'ın VPC CIDR'ı (10.0.0.0/16)
   - **Custom TCP (6379)**: EKS cluster'ın VPC CIDR'ı (10.0.0.0/16)
   - **Custom TCP (8200)**: EKS cluster'ın VPC CIDR'ı (10.0.0.0/16)

7. **Storage**:
   - **20 GB** (ücretsiz tier: 30 GB'a kadar ücretsiz)

8. **Launch Instance**

### 1.2 AWS CLI ile Oluşturma (Alternatif)

```bash
# Key pair oluştur
aws ec2 create-key-pair \
  --key-name triobank-ec2-key \
  --query 'KeyMaterial' \
  --output text > triobank-ec2-key.pem

chmod 400 triobank-ec2-key.pem

# Security Group oluştur
SG_ID=$(aws ec2 create-security-group \
  --group-name triobank-databases-sg \
  --description "Security group for TrioBank databases" \
  --region eu-north-1 \
  --query 'GroupId' --output text)

# SSH portunu aç (sadece kendi IP'nizden)
MY_IP=$(curl -s https://checkip.amazonaws.com)
aws ec2 authorize-security-group-ingress \
  --group-id $SG_ID \
  --protocol tcp \
  --port 22 \
  --cidr $MY_IP/32 \
  --region eu-north-1

# Database portlarını aç (EKS VPC'den)
aws ec2 authorize-security-group-ingress \
  --group-id $SG_ID \
  --protocol tcp \
  --port 1433 \
  --cidr 10.0.0.0/16 \
  --region eu-north-1

aws ec2 authorize-security-group-ingress \
  --group-id $SG_ID \
  --protocol tcp \
  --port 27017 \
  --cidr 10.0.0.0/16 \
  --region eu-north-1

aws ec2 authorize-security-group-ingress \
  --group-id $SG_ID \
  --protocol tcp \
  --port 6379 \
  --cidr 10.0.0.0/16 \
  --region eu-north-1

aws ec2 authorize-security-group-ingress \
  --group-id $SG_ID \
  --protocol tcp \
  --port 8200 \
  --cidr 10.0.0.0/16 \
  --region eu-north-1

# EC2 instance oluştur
aws ec2 run-instances \
  --image-id ami-0e4c4188af8f5afd6 \
  --instance-type t2.micro \
  --key-name triobank-ec2-key \
  --security-group-ids $SG_ID \
  --region eu-north-1 \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=triobank-databases}]'
```

---

## ADIM 2: SSH ile Bağlanma

### 2.1 EC2 IP Adresini Bulma

```bash
# AWS Console'dan: EC2 → Instances → Public IPv4 address
# Veya CLI ile:
aws ec2 describe-instances \
  --filters "Name=tag:Name,Values=triobank-databases" \
  --query 'Reservations[0].Instances[0].PublicIpAddress' \
  --output text \
  --region eu-north-1
```

### 2.2 SSH ile Bağlanma

```bash
# Key dosyasını güvenli hale getir
chmod 400 triobank-ec2-key.pem

# EC2'ye bağlan
ssh -i triobank-ec2-key.pem ec2-user@<EC2_IP_ADDRESS>

# İlk bağlantıda "Are you sure you want to continue connecting?" sorusuna "yes" deyin
```

**Bağlandıktan sonra:**
```bash
# EC2'de olduğunuzu kontrol edin
whoami  # ec2-user çıkmalı
pwd     # /home/ec2-user
```

---

## ADIM 3: Docker Kurulumu

EC2'de (SSH bağlantısından):

```bash
# Amazon Linux 2023 için
sudo yum update -y
sudo yum install docker -y
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ec2-user

# Docker'ın kurulduğunu kontrol et
docker --version

# Yeni bir SSH oturumu açın (group değişikliği için)
exit
# Tekrar bağlanın
ssh -i triobank-ec2-key.pem ec2-user@<EC2_IP_ADDRESS>

# Artık sudo olmadan docker çalışmalı
docker ps
```

---

## ADIM 4: Docker Compose Kurulumu

```bash
# Docker Compose kur
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Kontrol et
docker-compose --version
```

---

## ADIM 5: Servisleri Başlatma

### 5.1 Docker Compose Dosyasını Yükleme

Lokal bilgisayarınızdan:

```bash
# Docker Compose dosyasını EC2'ye kopyala
scp -i triobank-ec2-key.pem local/docker-compose.yaml ec2-user@<EC2_IP>:/home/ec2-user/
scp -i triobank-ec2-key.pem .env ec2-user@<EC2_IP>:/home/ec2-user/  # Eğer varsa
```

EC2'de:

```bash
# .env dosyası oluştur (eğer yoksa)
cat > .env << 'EOF'
MSSQL_SA_PASSWORD=TrioBank123
MONGO_INITDB_ROOT_USERNAME=root
MONGO_INITDB_ROOT_PASSWORD=TrioBank123
REDIS_PASSWORD=TrioBank123
EOF

# Servisleri başlat
docker-compose up -d

# Kontrol et
docker-compose ps
```

### 5.2 Vault Unseal (İlk Kurulum)

```bash
# Vault'u initialize et
docker exec -ti local-vault vault operator init

# Çıkan 5 Unseal Key'i ve Root Token'ı saklayın!
# Örnek çıktı:
# Unseal Key 1: xxx
# Unseal Key 2: xxx
# ...
# Initial Root Token: hvs.xxx

# 3 Unseal Key ile vault'u aç
docker exec -ti local-vault vault operator unseal <KEY_1>
docker exec -ti local-vault vault operator unseal <KEY_2>
docker exec -ti local-vault vault operator unseal <KEY_3>

# Root Token ile login
docker exec -ti local-vault vault login <ROOT_TOKEN>
```

---

## ADIM 6: EKS'ten Bağlantı

### 6.1 EC2 Private IP'yi Bulma

```bash
# EC2'nin private IP'sini bul
aws ec2 describe-instances \
  --filters "Name=tag:Name,Values=triobank-databases" \
  --query 'Reservations[0].Instances[0].PrivateIpAddress' \
  --output text \
  --region eu-north-1
```

### 6.2 EKS'te ExternalName Service Oluşturma

EKS cluster'ınızda:

```bash
# Namespace oluştur (eğer yoksa)
kubectl create namespace triobank

# MSSQL ExternalName Service
kubectl apply -f - <<EOF
apiVersion: v1
kind: Service
metadata:
  name: mssql-ec2
  namespace: triobank
spec:
  type: ExternalName
  externalName: <EC2_PRIVATE_IP>
  ports:
  - port: 1433
    targetPort: 1433
    protocol: TCP
    name: mssql
EOF

# MongoDB ExternalName Service
kubectl apply -f - <<EOF
apiVersion: v1
kind: Service
metadata:
  name: mongodb-ec2
  namespace: triobank
spec:
  type: ExternalName
  externalName: <EC2_PRIVATE_IP>
  ports:
  - port: 27017
    targetPort: 27017
    protocol: TCP
    name: mongodb
EOF

# Redis ExternalName Service
kubectl apply -f - <<EOF
apiVersion: v1
kind: Service
metadata:
  name: redis-ec2
  namespace: triobank
spec:
  type: ExternalName
  externalName: <EC2_PRIVATE_IP>
  ports:
  - port: 6379
    targetPort: 6379
    protocol: TCP
    name: redis
EOF
```

---

## ✅ Test

### EC2'de Servisleri Kontrol

```bash
# Tüm container'lar çalışıyor mu?
docker-compose ps

# Log'ları kontrol
docker-compose logs mssql
docker-compose logs mongodb
docker-compose logs redis
docker-compose logs vault
```

### EKS'ten Bağlantı Testi

```bash
# Test pod oluştur
kubectl run test-mssql --image=mcr.microsoft.com/mssql-tools --rm -it --restart=Never --namespace=triobank -- /bin/bash

# Pod içinde:
# sqlcmd -S mssql-ec2.triobank.svc.cluster.local,1433 -U sa -P TrioBank123
```

---

## 🔐 Güvenlik Notları

1. **Security Group**: Sadece EKS VPC'den erişime izin ver
2. **Key Pair**: Private key'i güvende tut
3. **SSH**: Sadece kendi IP'nizden erişim
4. **Database Şifreleri**: Production'da güçlü şifreler kullanın

---

## 📊 Maliyet

- **t2.micro**: 12 ay ücretsiz (750 saat/ay)
- **EBS Storage**: 30 GB'a kadar ücretsiz
- **Data Transfer**: 15 GB ücretsiz

**Toplam**: İlk 12 ay ~$0/ay

---

## 🎯 Sonraki Adımlar

1. ✅ EC2 instance oluştur
2. ✅ SSH ile bağlan
3. ✅ Docker kur
4. ✅ Servisleri başlat
5. ✅ EKS'ten bağlan

