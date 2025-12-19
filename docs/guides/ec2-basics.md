# EC2 Temel Bilgiler

Bu doküman, EC2'nin ne olduğunu ve nasıl çalıştığını açıklar.

## 🖥️ EC2 Nedir?

**EC2 (Elastic Compute Cloud)** = AWS'in sanal sunucu hizmeti

### Basit Açıklama

EC2, bulutta bir bilgisayar kiralamak gibidir:
- Fiziksel bir sunucu satın almak yerine
- AWS'den sanal bir sunucu kiralarsınız
- İstediğiniz zaman açıp kapatabilirsiniz
- Sadece kullandığınız süre için ödeme yaparsınız

### Lokal Bilgisayarınızla Karşılaştırma

| Özellik | Lokal Bilgisayarınız | EC2 Instance |
|---------|---------------------|--------------|
| **Fiziksel** | Evinizdeki bilgisayar | AWS veri merkezinde |
| **Erişim** | Doğrudan | SSH ile uzaktan |
| **İşletim Sistemi** | Windows/Linux | Linux (Amazon Linux, Ubuntu, vb.) |
| **Kapatma** | Fiziksel kapatma | AWS Console'dan durdurma |
| **Maliyet** | Tek seferlik satın alma | Saatlik/aylık kiralama |

## 🏗️ EC2 Bileşenleri

### 1. Instance Type (Sunucu Boyutu)

Farklı iş yükleri için farklı boyutlar:

**t2.micro** (Ücretsiz Tier - 12 ay)
- 1 vCPU
- 1 GB RAM
- **Ücretsiz!** (12 ay)
- Test/demo için ideal

**t3.medium** (Küçük projeler)
- 2 vCPU
- 4 GB RAM
- ~$30/ay
- Küçük veritabanları için uygun

**t3.large** (Orta projeler)
- 2 vCPU
- 8 GB RAM
- ~$60/ay

### 2. AMI (Amazon Machine Image)

İşletim sistemi görüntüsü:
- **Amazon Linux 2**: AWS'nin özel Linux'u (ücretsiz)
- **Ubuntu**: Popüler Linux dağıtımı
- **Windows Server**: Windows sunucular için

### 3. Security Group (Güvenlik Duvarı)

Hangi portların açık olacağını belirler:
- **SSH (22)**: Sunucuya bağlanmak için
- **HTTP (80)**: Web trafiği
- **HTTPS (443)**: Güvenli web trafiği
- **1433**: MSSQL
- **27017**: MongoDB
- **6379**: Redis
- **8200**: Vault

### 4. Key Pair (SSH Anahtarı)

Sunucuya güvenli bağlanmak için:
- Public key → EC2'ye yüklenir
- Private key → Sizde kalır (gizli!)
- SSH ile bağlanırken kullanılır

## 🔄 EC2'de Ne Yapacağız?

### Senaryo

```
┌─────────────────┐
│   EKS Cluster   │  ← Mikroservisleriniz
│  (triobank)     │
└────────┬────────┘
         │ ExternalName Service
         │ (DNS abstraction)
         ↓
┌─────────────────┐
│  EC2 Instance   │  ← Veritabanları
│  (t2.micro)     │
│                 │
│  Docker Compose │
│  - MSSQL        │
│  - MongoDB      │
│  - Redis        │
│  - Vault        │
└─────────────────┘
```

### Neden EC2?

1. **Ücretsiz Tier**: t2.micro 12 ay ücretsiz
2. **Tam Kontrol**: İstediğiniz yazılımı kurarsınız
3. **Basit**: Docker Compose ile aynı yapı
4. **Maliyet**: Sadece EC2 için ödeme (EKS node'ları zaten var)

## 📊 Maliyet

### Ücretsiz Tier (12 ay)

- **t2.micro**: 750 saat/ay ücretsiz
- **EBS Storage**: 30 GB ücretsiz
- **Data Transfer**: 15 GB ücretsiz

**Toplam**: ~$0/ay (ilk 12 ay)

### 12 Aydan Sonra

- **t2.micro**: ~$8-10/ay
- **EBS Storage**: ~$3/ay (30 GB)
- **Data Transfer**: Kullanıma göre

**Toplam**: ~$10-15/ay

## 🎯 Adım Adım Plan

1. **EC2 Instance Oluşturma**
   - t2.micro seç
   - Amazon Linux 2 AMI
   - Security Group yapılandır

2. **SSH ile Bağlanma**
   - Key pair oluştur
   - EC2'ye bağlan

3. **Docker Kurulumu**
   - Docker install
   - Docker Compose install

4. **Servisleri Çalıştırma**
   - Docker Compose ile MSSQL, MongoDB, Redis, Vault

5. **EKS'ten Bağlantı**
   - ExternalName Service oluştur
   - EKS pod'ları EC2'deki servislere bağlanır

## 🔐 Güvenlik Notları

- **Security Group**: Sadece gerekli portları aç
- **Key Pair**: Private key'i güvende tut
- **SSH**: Sadece kendi IP'nizden erişim (opsiyonel)

## 📝 Sonraki Adımlar

1. EC2 instance oluştur
2. SSH ile bağlan
3. Docker kur
4. Servisleri başlat
5. EKS'ten bağlan

