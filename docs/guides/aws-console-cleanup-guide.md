# AWS Console - Kalıntı Temizleme Rehberi

Bu rehber, AWS Console'dan EKS cluster kalıntılarını temizlemek için adım adım yönlendirme içerir.

## 🎯 Hızlı Kontrol Listesi

### 1. EKS Cluster Kontrolü
### 2. CloudFormation Stack'leri Kontrolü
### 3. VPC ve Network Kaynakları
### 4. IAM Roles ve Policies
### 5. EC2 Instances (Eğer varsa)

---

## 📋 ADIM ADIM TEMİZLİK

### ADIM 1: EKS Cluster Kontrolü ve Silme

1. **AWS Console'a giriş yap**: https://console.aws.amazon.com
2. **Region kontrolü**: Sağ üstte `eu-north-1` (Stockholm) seçili olduğundan emin ol
3. **EKS servisine git**:
   - Arama çubuğuna "EKS" yaz
   - **"Elastic Kubernetes Service"** seç
4. **Cluster'ı kontrol et**:
   - Sol menüden **"Clusters"** seç
   - `triobank-cluster` var mı kontrol et
5. **Eğer varsa sil**:
   - Cluster'ı seç
   - **"Delete"** butonuna tıkla
   - Onayla
   - ⏱️ **5-10 dakika** sürebilir

**Kontrol**: Cluster listede görünmemeli

---

### ADIM 2: CloudFormation Stack'leri Kontrolü ve Silme

1. **CloudFormation servisine git**:
   - Arama çubuğuna "CloudFormation" yaz
   - **"CloudFormation"** seç
2. **Stack'leri kontrol et**:
   - Sol menüden **"Stacks"** seç
   - Filtre: `triobank-cluster` yaz
   - Şu stack'leri ara:
     - `eksctl-triobank-cluster-cluster`
     - `eksctl-triobank-cluster-nodegroup-ng-1`
     - `eksctl-triobank-cluster-addon-vpc-cni`
3. **Her stack için**:
   - Stack'i seç
   - **"Delete"** butonuna tıkla
   - Eğer **"TerminationProtection"** hatası alırsan:
     - Stack'i seç → **"Stack actions"** → **"Change termination protection"**
     - **"Disable"** seç → **"Save"**
     - Tekrar **"Delete"** yap
   - Onayla
   - ⏱️ **3-5 dakika** sürebilir

**Kontrol**: Stack'ler listede görünmemeli (DELETE_COMPLETE durumunda olabilir, sorun değil)

---

### ADIM 3: VPC ve Network Kaynakları Kontrolü

1. **VPC servisine git**:
   - Arama çubuğuna "VPC" yaz
   - **"VPC"** seç
2. **VPC'leri kontrol et**:
   - Sol menüden **"Your VPCs"** seç
   - `triobank-cluster` veya `eksctl-triobank-cluster` içeren VPC'leri ara
   - **ÖNEMLİ**: Terraform'un oluşturduğu VPC'yi silme! (Name: `triobank-cluster-vpc`)
3. **Eğer eksctl VPC'si varsa**:
   - VPC'yi seç → **"Delete VPC"**
   - Onayla

**Kontrol**: Sadece Terraform VPC'si kalmalı

---

### ADIM 4: IAM Roles Kontrolü

1. **IAM servisine git**:
   - Arama çubuğuna "IAM" yaz
   - **"IAM"** seç
2. **Roles kontrolü**:
   - Sol menüden **"Roles"** seç
   - Filtre: `triobank-cluster` veya `eksctl-triobank-cluster` yaz
   - Şu role'leri ara:
     - `eksctl-triobank-cluster-cluster-ServiceRole-*`
     - `eksctl-triobank-cluster-nodegroup-*-NodeInstanceRole-*`
3. **Eğer varsa sil**:
   - Role'ü seç → **"Delete"** → Onayla
   - ⚠️ **Dikkat**: Terraform'un oluşturduğu role'leri silme!

**Kontrol**: Sadece Terraform role'leri kalmalı

---

### ADIM 5: EC2 Instances Kontrolü

1. **EC2 servisine git**:
   - Arama çubuğuna "EC2" yaz
   - **"EC2"** seç
2. **Instances kontrolü**:
   - Sol menüden **"Instances"** seç
   - Filtre: `triobank-cluster` yaz
   - EKS node'ları varsa (eksctl'den kalan):
     - Instance'ları seç → **"Instance state"** → **"Terminate instance"**
     - Onayla

**Kontrol**: Sadece Terraform EC2 instance'ı kalmalı (Name: `triobank-cluster-databases`)

---

## ✅ Temizlik Sonrası Kontrol

### Hızlı Kontrol Komutları

Terminal'de çalıştır:

```bash
# EKS Cluster
aws eks list-clusters --region eu-north-1

# CloudFormation Stack'leri
aws cloudformation list-stacks --region eu-north-1 --query "StackSummaries[?contains(StackName, 'triobank-cluster')]"

# VPC'ler
aws ec2 describe-vpcs --region eu-north-1 --filters "Name=tag:Name,Values=*triobank*" --query 'Vpcs[*].{Name:Tags[?Key==`Name`].Value|[0],VpcId:VpcId}'

# IAM Roles
aws iam list-roles --query "Roles[?contains(RoleName, 'triobank-cluster')].RoleName"
```

---

## 🚀 Terraform Apply

Kalıntılar temizlendikten sonra:

```bash
cd infrastructure/terraform
terraform apply
```

---

## 📝 Notlar

- **Terraform kaynakları**: Name tag'i `triobank-cluster-*` ile başlar
- **eksctl kaynakları**: Name tag'i `eksctl-triobank-cluster-*` ile başlar
- **Dikkat**: Terraform'un oluşturduğu kaynakları silme!

