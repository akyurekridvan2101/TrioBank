# Terraform Infrastructure - TrioBank

Bu klasör, AWS EKS ve EC2 altyapısını Terraform ile yönetir.

## 📁 Dosya Yapısı

- `main.tf` - Provider ve backend yapılandırması
- `variables.tf` - Tüm değişkenler
- `outputs.tf` - Çıktılar (IP'ler, ARN'ler, vb.)
- `vpc.tf` - VPC, Subnets, Internet Gateway, NAT Gateway
- `ec2.tf` - EC2 instance (MSSQL, MongoDB, Redis, Vault için)
- `eks.tf` - EKS cluster, Node Group, IAM Roles, Load Balancer Controller

## 🎯 Oluşturulan Kaynaklar

### VPC
- VPC (10.0.0.0/16)
- Public Subnets (2 adet) - EC2 ve ALB için
- Private Subnets (2 adet) - EKS node'ları için
- Internet Gateway
- NAT Gateway
- Route Tables

### EC2
- Instance (t2.micro) - Public subnet'te
- Security Group (SSH, MSSQL, MongoDB, Redis, Vault portları)
- Key Pair (SSH için)
- User Data (Docker ve Docker Compose otomatik kurulum)

### EKS
- EKS Cluster (Kubernetes 1.29)
- Managed Node Group (t3.medium, 2 node)
- IAM Roles (Cluster, Node Group)
- OIDC Provider (IRSA için)
- Load Balancer Controller IAM Role (IRSA)

## 🚀 Kullanım

### 1. Terraform Initialize
```bash
cd infrastructure/terraform
terraform init
```

### 2. Plan (Değişiklikleri Göster)
```bash
terraform plan
```

### 3. Apply (Kurulum)
```bash
terraform apply
```

### 4. Destroy (Silme)
```bash
terraform destroy
```

## 📝 Önemli Notlar

### EC2 Private IP
EKS'ten EC2'ye bağlanmak için `ec2_private_ip` output'unu kullanın:
```bash
terraform output ec2_private_ip
```

### kubectl Config
```bash
terraform output kubectl_config_command
# Çıkan komutu çalıştırın
```

### Load Balancer Controller
IAM Role ARN:
```bash
terraform output alb_controller_role_arn
```

Kubernetes'te Service Account oluştururken bu ARN'yi kullanın.

### ArgoCD
EKS endpoint public olduğu için ArgoCD dışarıdan erişebilir:
```bash
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```

## 🔐 Güvenlik

- **SSH**: Her yerden (test için - `allowed_ssh_cidr` değişkeni ile sınırlandırılabilir)
- **Database Portları**: Sadece VPC içinden (10.0.0.0/16)
- **EKS Endpoint**: Public (ArgoCD ve kubectl erişimi için)

## 📊 Maliyet

- **EC2 (t2.micro)**: Ücretsiz (12 ay)
- **EKS Control Plane**: ~$72/ay
- **EKS Node Group (2x t3.medium)**: ~$60/ay
- **NAT Gateway**: ~$32/ay
- **Toplam**: ~$164/ay (EC2 ücretsiz)

## ✅ Sonraki Adımlar

1. ✅ Terraform ile altyapıyı oluştur
2. ✅ EC2'ye SSH ile bağlan
3. ✅ Docker Compose ile servisleri başlat (MSSQL, MongoDB, Redis, Vault)
4. ✅ kubectl config yap
5. ✅ Load Balancer Controller kur
6. ✅ ArgoCD kur
7. ✅ ExternalName Service oluştur (EC2 private IP ile)
8. ✅ Ingress oluştur (Frontend URL için)
