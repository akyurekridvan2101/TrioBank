# Yerel Geliştirme Ortamı Kurulumu (Local Deployment)

Bu rehber, bir geliştiricinin kendi bilgisayarında (Localhost) **tüm TrioBank platformunu** sıfırdan ayağa kaldırmasını sağlar.

---

## � İçindekiler

1. [Ön Koşullar](#️-ön-koşullar)
2. [Hızlı Kurulum](#-hızlı-kurulum)
3. [Script Adım Adım](#-script-adım-adım)
4. [Environment Seçenekleri](#-environment-seçenekleri)
5. [Kurulum Sonrası](#-kurulum-sonrası)
6. [Troubleshooting](#-troubleshooting)

---

## Ön Koşullar

Kuruluma başlamadan önce sisteminizde aşağıdakiler yüklü ve çalışır durumda olmalı:

### Gerekli Araçlar

- **Docker Desktop** (v20.10+)
- **Minikube** (v1.30+)
- **Kubectl** (v1.27+)
- **Helm** (v3.12+)
- **Git**

### İsteğe Bağlı

- **Vault CLI** (debug için)
- **ArgoCD CLI** (management için)

### Sistem Kaynakları

Minikube için **minimum** önerilen kaynaklar:
- **RAM:** 8GB
- **CPU:** 4 core
- **Disk:** 20GB boş alan

---

## Hızlı Kurulum

### 1. Vault'u Hazırla

**Yerel Vault çalışıyor olmalı:**
```bash
# Vault'u başlat (Docker ile)
docker run -d --name vault \
  -p 8200:8200 \
  --cap-add=IPC_LOCK \
  -e VAULT_DEV_ROOT_TOKEN_ID=myroot \
  vault:latest

# Test et
curl http://localhost:8200/v1/sys/health
```

**VEYA** production Vault kullanıyorsan unseal yap:
```bash
vault operator unseal <key1>
vault operator unseal <key2>
vault operator unseal <key3>
```

### 2. Setup Script'i Çalıştır

```bash
# Proje kök dizininde:
./scripts/setup-cluster.sh
```

---

## Script Adım Adım

Script çalıştığında şu adımlar otomatik gerçekleşir:

### Adım 1: Minikube Reset

```
Resetting Minikube...
```

- Eski Minikube cluster'ını siler
- Yeni cluster başlatır (8GB RAM, 4 CPU, ingress addon)

### Adım 2: Namespace Oluşturma

```
Creating Namespaces...
```

- `argocd` namespace (GitOps controller için)
- `triobank` namespace (tüm servisler için)

### Adım 3: ArgoCD Kurulumu

```
Installing ArgoCD...
```

- ArgoCD Helm chart kurulur
- Dev environment values uygulanır
- ArgoCD server hazır olana kadar bekler

### Adım 4: Vault Token

```
⚠️  ÖNEMLİ UYARI: Vault kilidinin (Unseal) açık olduğundan emin olun!
Lütfen Vault Root Token'ını girin:
```

**Vault Token nerede?**
- Dev Vault: `myroot` (yukarıdaki Docker komutunda)
- Prod Vault: Güvenli bir yerde saklanan token

> Girdiğiniz karakterler gizlenir (güvenlik)

### Adım 5: Environment Seçimi

```
🌍 Hangi ortamı kurmak istiyorsunuz?
1) Dev (Geliştirme - develop branch)
2) Prod (Üretim - main branch)
```

**Seçenekler:**
- **1 gir:** Dev ortamı (develop branch)
- **2 gir:** Prod ortamı (main branch)
- **Diğer:** Hata ve çıkış

---

## Environment Seçenekleri

### Option 1: Dev Environment

#### Scenario A: Default (develop branch)

```
Hangi branch deploy edilsin?
   → ENTER'a bas = develop branch kullanılır
   → Branch adı yaz = o branch kullanılır (örn: feature/yeni-ozellik)
[ENTER]
```

**Sonuç:**
- `overlays/dev/root.yaml` deploy edilir
- `targetRevision: develop` (hardcoded)
- Tüm servisler develop'tan çekilir

#### Scenario B: Feature Branch Test

```
Hangi branch deploy edilsin?
feature/my-feature
```

**Sonuç:**
- Root YAML runtime'da `sed` ile değiştirilir
- `targetRevision: feature/my-feature`
- Tüm servisler YOUR feature branch'ten çekilir

**Kullanım senaryosu:** Kendi PC'nde feature branch test etmek

### Option 2: Prod Environment

```
Prod ortamı seçildi (main branch)
```

**Sonuç:**
- `overlays/prod/root.yaml` deploy edilir
- `targetRevision: main` (hardcoded)
- Production-ready konfigürasyon

**Uyarı:** Local'de prod test etmek genelde gereksiz!

---

## Kurulum Sonrası

### 1. ArgoCD UI'a Giriş

**Port-forward:**
```bash
kubectl port-forward svc/argocd-server -n argocd 8085:443
```

**Tarayıcı:**
```
URL: https://localhost:8085
User: admin
Pass: [Script sonunda gösterilen password]
```

**Password'u unuttuysan:**
```bash
kubectl -n argocd get secret argocd-initial-admin-secret \
  -o jsonpath="{.data.password}" | base64 -d
```

### 2. Deployment Durumunu İzle

**ArgoCD UI'da:**
1. `root` application'ı aç
2. `platform` ApplicationSet'i gör
3. `services` ApplicationSet'i gör

**Kubectl ile:**
```bash
# Tüm pod'ları izle
kubectl get pods -n triobank --watch

# ArgoCD application'ları
kubectl get applications -n argocd

# Kafka cluster durumu
kubectl get kafka -n triobank
```

### 3. Platform Servisleri

**Sync wave sırası:**
1. External Secrets Operator (wave: 100)
2. Vault (wave: 200)
3. Kafka Operator (wave: 300)
4. Kafka Cluster (wave: 400)
5. Kafka Connect (wave: 500)
6. Kafka UI (wave: 600)
7. Mikroservisler (wave: 1000)

**Hazır olma süresi:**
- İlk kurulum: ~5-10 dakika
- Sonraki güncellemeler: ~2-3 dakika

### 4. Servis Erişimi

**Kafka UI:**
```bash
kubectl port-forward svc/kafka-ui-kafka-ui -n triobank 8090:80
# URL: http://localhost:8090
```

**Kafka Connect:**
```bash
kubectl port-forward svc/connect-connect-api -n triobank 8083:8083
# URL: http://localhost:8083
```

**Vault (External):**
```bash
# Zaten local'de çalışıyor
# URL: http://localhost:8200
```
