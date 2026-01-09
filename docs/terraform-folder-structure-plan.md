# Terraform Klasör Yapısı Planı

## 🎯 Amaç

Terraform ile EC2 ve EKS'i tek komutla kurmak ve yeniden ayağa kaldırmak.

## 📁 Önerilen Klasör Yapısı

```
infrastructure/
├── terraform/
│   ├── main.tf                    # Provider ve backend yapılandırması
│   ├── variables.tf               # Tüm değişkenler
│   ├── outputs.tf                 # Çıktılar (IP'ler, ARN'ler, vb.)
│   ├── terraform.tfvars.example   # Örnek değişken dosyası
│   │
│   ├── ec2.tf                     # EC2 instance tanımı
│   │   ├── Security Group (basit, minimal güvenlik)
│   │   ├── Key Pair
│   │   └── EC2 Instance (t2.micro)
│   │
│   └── eks.tf                     # EKS cluster tanımı
│       ├── EKS Cluster
│       ├── Managed Node Group
│       └── IAM Roles
│
└── kubernetes/                    # Mevcut Kubernetes dosyaları (değişmez)
    └── ...
```

## 🔧 Dosya İçerikleri (Özet)

### `main.tf`
- AWS Provider yapılandırması
- Backend (opsiyonel - state dosyası için)
- Region: eu-north-1

### `variables.tf`
- `cluster_name`: EKS cluster adı
- `ec2_instance_type`: EC2 tipi (default: t2.micro)
- `region`: AWS region
- `vpc_cidr`: VPC CIDR (opsiyonel)

### `ec2.tf`
- Security Group (SSH + Database portları)
- Key Pair (otomatik oluşturulur veya mevcut kullanılır)
- EC2 Instance (t2.micro, Amazon Linux 2)
- **Minimal güvenlik**: Sadece gerekli portlar açık

### `eks.tf`
- EKS Cluster (basit yapılandırma)
- Managed Node Group (t3.medium, 2 node)
- IAM Roles (otomatik)
- **Sorun çıkartmasın**: Minimal, test için yeterli

### `outputs.tf`
- EC2 Public IP
- EC2 Private IP
- EKS Cluster Name
- EKS kubeconfig path
- kubectl komutları

## 🚀 Kullanım

```bash
# 1. Terraform initialize
cd infrastructure/terraform
terraform init

# 2. Plan (değişiklikleri göster)
terraform plan

# 3. Apply (kurulum)
terraform apply

# 4. Destroy (silme)
terraform destroy
```

## 🔐 Güvenlik (Minimal - Test İçin)

### EC2 Security Group
- **SSH (22)**: 0.0.0.0/0 (her yerden - test için)
- **MSSQL (1433)**: EKS VPC CIDR (10.0.0.0/16)
- **MongoDB (27017)**: EKS VPC CIDR
- **Redis (6379)**: EKS VPC CIDR
- **Vault (8200)**: EKS VPC CIDR

### EKS
- Public API endpoint (test için)
- Minimal IAM permissions

## 📊 Maliyet

- **EC2 (t2.micro)**: Ücretsiz (12 ay)
- **EKS Control Plane**: ~$72/ay (ücretsiz değil, ama gerekli)
- **EKS Node Group (2x t3.medium)**: ~$60/ay
- **Toplam**: ~$132/ay (EC2 ücretsiz)

## ✅ Avantajlar

1. ✅ Tek komutla kurulum (`terraform apply`)
2. ✅ Tek komutla silme (`terraform destroy`)
3. ✅ State yönetimi (Terraform state dosyası)
4. ✅ Version control (Git'te saklanır)
5. ✅ Tekrar üretilebilir (idempotent)

## ⚠️ Notlar

- **State dosyası**: Lokal olarak saklanır (`.terraform/` klasöründe)
- **Key Pair**: Terraform otomatik oluşturur veya mevcut kullanır
- **Güvenlik**: Test için minimal, production'da artırılabilir

## 🎯 Sonraki Adımlar

1. ✅ Klasör yapısını oluştur
2. ✅ `main.tf` - Provider yapılandırması
3. ✅ `variables.tf` - Değişkenler
4. ✅ `ec2.tf` - EC2 instance
5. ✅ `eks.tf` - EKS cluster
6. ✅ `outputs.tf` - Çıktılar
7. ✅ Test et

---

**Onaylıyor musunuz? Onaylarsanız Terraform dosyalarını oluşturmaya başlayacağım.**

