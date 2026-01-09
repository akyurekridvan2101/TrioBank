# AWS EKS Deployment Rehberi - TrioBank

Bu rehber, TrioBank projesini AWS EKS (Elastic Kubernetes Service) üzerinde çalıştırmak için adım adım talimatlar içerir.

## 📋 Ön Gereksinimler

- AWS hesabı (ücretsiz tier aktif)
- Linux/Mac terminal erişimi
- Temel terminal komut bilgisi

---

## ADIM 1: AWS CLI Kurulumu ve Yapılandırma

### 1.1 AWS CLI Kurulumu

**Linux:**
```bash
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install
```

**Mac:**
```bash
brew install awscli
```

**Kurulumu kontrol et:**
```bash
aws --version
```

### 1.2 AWS Kimlik Bilgilerini Yapılandırma

1. AWS Console'da sağ üst köşeden **Account ID**'nize tıklayın
2. **Security credentials** seçeneğine gidin
3. **Access keys** bölümünde **Create access key** butonuna tıklayın
4. **Command Line Interface (CLI)** seçeneğini seçin
5. Access Key ID ve Secret Access Key'i kopyalayın (bir daha gösterilmeyecek!)

**Terminal'de yapılandırma:**
```bash
aws configure
```

Şu bilgileri girin:
- **AWS Access Key ID:** [Kopyaladığınız Access Key]
- **AWS Secret Access Key:** [Kopyaladığınız Secret Key]
- **Default region name:** `eu-north-1` (Stockholm - ücretsiz tier için uygun)
- **Default output format:** `json`

**Yapılandırmayı test et:**
```bash
aws sts get-caller-identity
```

Bu komut sizin AWS hesap bilgilerinizi göstermeli.

---

## ADIM 2: eksctl Kurulumu (EKS Cluster Oluşturma Aracı)

eksctl, EKS cluster'larını kolayca oluşturmak için AWS'nin resmi aracıdır.

### 2.1 eksctl Kurulumu

**Linux/Mac:**
```bash
# Linux
curl --silent --location "https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_Linux_amd64.tar.gz" | tar xz -C /tmp
sudo mv /tmp/eksctl /usr/local/bin

# Mac
brew tap weaveworks/tap
brew install weaveworks/tap/eksctl
```

**Kurulumu kontrol et:**
```bash
eksctl version
```

---

## ADIM 3: EKS Cluster Oluşturma

### 3.1 Cluster Yapılandırma Dosyası Oluşturma

Proje kök dizininde `eks-cluster-config.yaml` dosyası oluşturun:

```bash
cd /home/akyurek2101/Desktop/triobank
cat > eks-cluster-config.yaml << 'EOF'
apiVersion: eksctl.io/v1alpha5
kind: ClusterConfig

metadata:
  name: triobank-cluster
  region: eu-north-1
  version: "1.28"

# Node Group - Ücretsiz tier için minimal kaynaklar
nodeGroups:
  - name: ng-1
    instanceType: t3.medium  # 2 vCPU, 4 GB RAM (ücretsiz tier'da uygun)
    desiredCapacity: 2
    minSize: 1
    maxSize: 3
    volumeSize: 20
    volumeType: gp3
    ssh:
      allow: false
    iam:
      withAddonPolicies:
        albIngress: true  # Load Balancer Controller için gerekli
        cloudWatch: true
        autoScaler: true

# IAM ayarları
iam:
  withOIDC: true  # Load Balancer Controller için gerekli
  serviceRole:
    managedPolicyARNs:
      - arn:aws:iam::aws:policy/AmazonEKSClusterPolicy

# VPC ayarları (otomatik oluşturulacak)
vpc:
  cidr: "10.0.0.0/16"
  nat:
    gateway: Single  # Ücretsiz tier için tek NAT Gateway (maliyet tasarrufu)
EOF
```

### 3.2 Cluster Oluşturma

**⚠️ ÖNEMLİ:** Bu işlem 15-20 dakika sürebilir ve AWS ücretsiz tier limitlerinizi kullanır.

```bash
eksctl create cluster -f eks-cluster-config.yaml
```

Bu komut şunları yapacak:
- VPC oluşturma
- EKS cluster oluşturma
- Node group oluşturma
- kubectl yapılandırması

**İlerlemeyi izle:**
Komut çıktısında ilerleme göreceksiniz. Tamamlandığında şu mesajı göreceksiniz:
```
✓ EKS cluster "triobank-cluster" in "eu-north-1" region is ready
```

### 3.3 Cluster Bağlantısını Test Etme

```bash
kubectl get nodes
```

2 node görmelisiniz:
```
NAME                          STATUS   ROLES    AGE   VERSION
ip-10-0-xxx-xxx.eu-north-1... Ready    <none>   5m    v1.28.x
ip-10-0-xxx-xxx.eu-north-1... Ready    <none>   5m    v1.28.x
```

---

## ADIM 4: AWS Load Balancer Controller Kurulumu

Bu controller, Kubernetes Ingress'lerini AWS Application Load Balancer'a dönüştürür.

### 4.1 IAM Policy Oluşturma

```bash
# Policy dosyasını indir
curl -O https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/v2.7.0/docs/install/iam_policy.json

# Policy'yi AWS'ye yükle
aws iam create-policy \
    --policy-name AWSLoadBalancerControllerIAMPolicy \
    --policy-document file://iam_policy.json
```

**Çıktıdan Policy ARN'yi kopyalayın** (sonraki adımda kullanacağız):
```
arn:aws:iam::136922973429:policy/AWSLoadBalancerControllerIAMPolicy
```

### 4.2 IAM Service Account Oluşturma

```bash
# Önce cluster adınızı ve region'ı değişkenlere atayın
export CLUSTER_NAME=triobank-cluster
export AWS_REGION=eu-north-1
export ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

# OIDC provider oluştur
eksctl utils associate-iam-oidc-provider \
    --region=$AWS_REGION \
    --cluster=$CLUSTER_NAME \
    --approve

# Service account oluştur
eksctl create iamserviceaccount \
    --cluster=$CLUSTER_NAME \
    --namespace=kube-system \
    --name=aws-load-balancer-controller \
    --role-name AmazonEKSLoadBalancerControllerRole \
    --attach-policy-arn=arn:aws:iam::${ACCOUNT_ID}:policy/AWSLoadBalancerControllerIAMPolicy \
    --approve
```

### 4.3 Helm ile Load Balancer Controller Kurulumu

```bash
# Helm repo ekle
helm repo add eks https://aws.github.io/eks-charts
helm repo update

# Controller'ı kur
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
    -n kube-system \
    --set clusterName=$CLUSTER_NAME \
    --set serviceAccount.create=false \
    --set serviceAccount.name=aws-load-balancer-controller

# Kurulumu kontrol et
kubectl get deployment -n kube-system aws-load-balancer-controller
```

---

## ADIM 5: Container Registry Ayarları

### Seçenek 1: Docker Hub Kullanımı (Mevcut)

Docker Hub zaten kullanıyorsunuz (`akyurekridvan2101/*`). Bu durumda ek bir şey yapmanıza gerek yok.

### Seçenek 2: AWS ECR Kullanımı (Önerilen - AWS içinde kalır)

ECR kullanmak isterseniz:

```bash
# ECR repository oluştur
aws ecr create-repository --repository-name triobank/api-gateway --region eu-north-1
aws ecr create-repository --repository-name triobank/auth-service --region eu-north-1
aws ecr create-repository --repository-name triobank/account-service --region eu-north-1
# ... diğer servisler için de aynı şekilde

# ECR'ye login
aws ecr get-login-password --region eu-north-1 | docker login --username AWS --password-stdin ${ACCOUNT_ID}.dkr.ecr.eu-north-1.amazonaws.com

# Image'ları tag'le ve push et
docker tag akyurekridvan2101/api-gateway:latest ${ACCOUNT_ID}.dkr.ecr.eu-north-1.amazonaws.com/triobank/api-gateway:latest
docker push ${ACCOUNT_ID}.dkr.ecr.eu-north-1.amazonaws.com/triobank/api-gateway:latest
```

**Şimdilik Docker Hub ile devam edebilirsiniz.**

---

## ADIM 6: Kubernetes Namespace ve Temel Yapılandırma

### 6.1 Namespace Oluşturma

```bash
kubectl create namespace triobank
```

### 6.2 Docker Hub Secret Oluşturma (Eğer private repo kullanıyorsanız)

```bash
kubectl create secret docker-registry dockerhub-credentials \
    --docker-server=https://index.docker.io/v1/ \
    --docker-username=akyurekridvan2101 \
    --docker-password=<DOCKER_HUB_TOKEN> \
    --docker-email=<EMAIL> \
    -n triobank
```

**Docker Hub token oluşturma:**
1. Docker Hub → Account Settings → Security
2. New Access Token oluştur
3. Token'ı yukarıdaki komutta kullan

---

## ADIM 7: Ingress Yapılandırması

### 7.1 Ingress Class Oluşturma

```bash
cat > ingress-class.yaml << 'EOF'
apiVersion: networking.k8s.io/v1
kind: IngressClass
metadata:
  name: alb
spec:
  controller: ingress.k8s.aws/alb
EOF

kubectl apply -f ingress-class.yaml
```

### 7.2 API Gateway için Ingress Örneği

`services/api-gateway/k8s/templates/ingress.yaml` dosyasını AWS ALB için güncelleyin:

```yaml
{{- if .Values.ingress.enabled -}}
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: {{ include "api-gateway.fullname" . }}
  annotations:
    alb.ingress.kubernetes.io/scheme: internet-facing
    alb.ingress.kubernetes.io/target-type: ip
    alb.ingress.kubernetes.io/listen-ports: '[{"HTTP": 80}, {"HTTPS": 443}]'
    alb.ingress.kubernetes.io/ssl-redirect: '443'
    alb.ingress.kubernetes.io/certificate-arn: <ACM_CERTIFICATE_ARN>  # SSL için (opsiyonel)
    alb.ingress.kubernetes.io/load-balancer-name: triobank-api-gateway
spec:
  ingressClassName: alb
  rules:
    - host: api.triobank.local  # Domain'iniz varsa buraya yazın
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: {{ include "api-gateway.fullname" . }}
                port:
                  number: {{ .Values.service.port }}
{{- end }}
```

---

## ADIM 8: Deployment ve Test

### 8.1 API Gateway'i Deploy Etme

```bash
cd services/api-gateway/k8s
helm install api-gateway . -n triobank
```

### 8.2 Load Balancer URL'sini Öğrenme

```bash
# Ingress'i kontrol et
kubectl get ingress -n triobank

# Load Balancer URL'si birkaç dakika içinde oluşacak
kubectl get ingress api-gateway -n triobank -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'
```

Bu URL'yi kopyalayın, örneğin:
```
triobank-api-gateway-1234567890.eu-north-1.elb.amazonaws.com
```

### 8.3 Test

```bash
# Health check
curl http://triobank-api-gateway-1234567890.eu-north-1.elb.amazonaws.com/health

# API test
curl http://triobank-api-gateway-1234567890.eu-north-1.elb.amazonaws.com/swagger/index.html
```

---

## ADIM 9: Tüm Servisleri Deploy Etme

### 9.1 Sıralı Deployment

Önce platform servisleri (Kafka, Redis, Database), sonra mikroservisler:

```bash
# 1. Platform servisleri (Kafka, Redis, MSSQL)
# infrastructure/kubernetes/base/platform/ klasöründeki servisleri deploy edin

# 2. Mikroservisler
cd /home/akyurek2101/Desktop/triobank
helm install auth-service services/auth-service/k8s -n triobank
helm install account-service services/account-service/k8s -n triobank
helm install ledger-service services/ledger-service/k8s -n triobank
# ... diğer servisler
```

### 9.2 Deployment Durumunu Kontrol

```bash
kubectl get pods -n triobank
kubectl get services -n triobank
kubectl get ingress -n triobank
```

---

## ADIM 10: Maliyet Optimizasyonu (Ücretsiz Tier)

### 10.1 Node Group Ölçeklendirme

Geceleri veya kullanılmadığında node sayısını azaltın:

```bash
# Node sayısını 1'e düşür
eksctl scale nodegroup --cluster=triobank-cluster --name=ng-1 --nodes=1

# Tekrar 2'ye çıkar
eksctl scale nodegroup --cluster=triobank-cluster --name=ng-1 --nodes=2
```

### 10.2 Cluster'ı Durdurma (Geçici)

Cluster'ı tamamen silmek isterseniz:

```bash
eksctl delete cluster --name=triobank-cluster --region=eu-north-1
```

**⚠️ DİKKAT:** Bu işlem geri alınamaz!

---

## 🐛 Sorun Giderme

### Pod'lar başlamıyor

```bash
# Pod loglarını kontrol et
kubectl logs <pod-name> -n triobank

# Pod detaylarını gör
kubectl describe pod <pod-name> -n triobank
```

### Load Balancer oluşmuyor

```bash
# Load Balancer Controller loglarını kontrol et
kubectl logs -n kube-system deployment/aws-load-balancer-controller
```

### Image pull hatası

```bash
# Secret'ı kontrol et
kubectl get secret dockerhub-credentials -n triobank
```

---

## 📚 Ek Kaynaklar

- [AWS EKS Dokümantasyonu](https://docs.aws.amazon.com/eks/)
- [eksctl Dokümantasyonu](https://eksctl.io/)
- [AWS Load Balancer Controller](https://kubernetes-sigs.github.io/aws-load-balancer-controller/)

---

## ✅ Sonraki Adımlar

1. ✅ EKS cluster oluşturuldu
2. ✅ Load Balancer Controller kuruldu
3. ⏳ Servisler deploy ediliyor
4. ⏳ Domain yapılandırması (opsiyonel)
5. ⏳ SSL sertifikası (ACM ile)
6. ⏳ Monitoring ve logging (CloudWatch)

---

**Sorularınız için:** Bu rehberi takip ederken herhangi bir adımda takılırsanız, hata mesajını paylaşın.

