# Production Overlay Analizi ve Vault Stratejisi

## 📊 Özet

Bu dokümantasyon, tüm mikroservislerin **dev** ve **prod** overlay'lerini karşılaştırır, prod için gerekli değişiklikleri listeler ve değişken değerlerin Vault'ta nasıl saklanacağını açıklar.

---

## 🔍 Mevcut Durum Analizi

### Overlay Durumu

| Servis | Dev Overlay | Prod Overlay | Durum |
|--------|-------------|--------------|-------|
| **auth-service** | ✅ Var | ✅ Var | Tamam |
| **client-service** | ✅ Var | ❌ Yok | Oluşturulmalı |
| **mail-service** | ✅ Var | ❌ Yok | Oluşturulmalı |
| **api-gateway** | ✅ Var | ❌ Yok | Oluşturulmalı |
| **account-service** | ✅ Var | ❌ Yok | Oluşturulmalı |
| **ledger-service** | ✅ Var | ❌ Yok | Oluşturulmalı |
| **card-service** | ✅ Var | ❌ Yok | Oluşturulmalı |
| **transaction-service** | ✅ Var | ❌ Yok | Oluşturulmalı |

---

## 📋 Servis Bazında Prod Overlay Gereksinimleri

### 1. Auth Service ✅ (Mevcut - Güncellenmeli)

#### Mevcut Prod Overlay Durumu
- ✅ Replica count: 2 (doğru)
- ✅ Resources: Production seviyesi (doğru)
- ⚠️ Database externalName: Placeholder değerler var
- ⚠️ Ingress host: Placeholder domain var
- ⚠️ Vault path: `secret/prod/services/auth` (doğru)

#### Gerekli Değişiklikler

**1. Database ExternalName (EC2 Private IP)**
```yaml
# ŞU AN (Yanlış):
externalName: "your-mongodb-atlas-cluster.mongodb.net"
externalName: "your-redis-cloud-endpoint.cache.amazonaws.com"

# OLMASI GEREKEN (Vault'tan):
externalName: "${VAULT_EC2_PRIVATE_IP}"  # Vault'tan: infrastructure/ec2/private_ip
```

**2. Ingress Host (Domain)**
```yaml
# ŞU AN (Placeholder):
host: api.yourdomain.com

# OLMASI GEREKEN (Vault'tan):
host: "${VAULT_DOMAIN}"  # Vault'tan: infrastructure/domain/api
```

---

### 2. Client Service ❌ (Oluşturulmalı)

#### Dev Overlay Özellikleri
- Replica: 1
- ExternalName: `host.minikube.internal`
- Vault path: `secret/dev/services/client`
- Ingress: nginx, boş host

#### Prod Overlay Gereksinimleri

**Oluşturulması Gereken Dosya:** `services/client-service/k8s/overlays/prod/values.yaml`

```yaml
# Gerekli Değişiklikler:

1. replicaCount: 1 → 2 (Production için)

2. image.pullPolicy: Always → IfNotPresent

3. config.APP_ENV: "development" → "production"

4. databases.mssql.externalName:
   Dev: "host.minikube.internal"
   Prod: "${VAULT_EC2_PRIVATE_IP}"  # Vault'tan

5. externalSecrets.vaultPath:
   Dev: "secret/dev/services/client"
   Prod: "secret/prod/services/client"

6. resources: Artırılmalı
   limits:
     cpu: 500m → 1000m
     memory: 512Mi → 1Gi

7. ingress:
   className: nginx → alb
   host: "" → "${VAULT_DOMAIN}"  # Vault'tan
   annotations: ALB annotations eklenmeli
```

---

### 3. Mail Service ❌ (Oluşturulmalı)

#### Dev Overlay Özellikleri
- Replica: 1
- Resources: Minimal (200m CPU, 128Mi memory)
- Vault path: `secret/dev/services/mail`
- Ingress: nginx

#### Prod Overlay Gereksinimleri

**Oluşturulması Gereken Dosya:** `services/mail-service/k8s/overlays/prod/values.yaml`

```yaml
# Gerekli Değişiklikler:

1. replicaCount: 1 → 2

2. image.pullPolicy: Always → IfNotPresent

3. config.APP_ENV: "development" → "production"

4. externalSecrets.vaultPath:
   Dev: "secret/dev/services/mail"
   Prod: "secret/prod/services/mail"

5. resources: Artırılmalı
   limits:
     cpu: 200m → 500m
     memory: 128Mi → 512Mi

6. ingress:
   className: nginx → alb
   host: "" → "${VAULT_DOMAIN}"  # Vault'tan
   annotations: ALB annotations
```

---

### 4. API Gateway ❌ (Oluşturulmalı)

#### Dev Overlay Özellikleri
- Replica: 1
- Redis externalName: `host.minikube.internal`
- Vault path: `secret/dev/services/api-gateway`
- Ingress: nginx, boş host

#### Prod Overlay Gereksinimleri

**Oluşturulması Gereken Dosya:** `services/api-gateway/k8s/overlays/prod/values.yaml`

```yaml
# Gerekli Değişiklikler:

1. replicaCount: 1 → 3 (API Gateway kritik, daha fazla replica)

2. image.pullPolicy: Always → IfNotPresent

3. config.APP_ENV: "development" → "production"

4. databases.redis.externalName:
   Dev: "host.minikube.internal"
   Prod: "${VAULT_EC2_PRIVATE_IP}"  # Vault'tan

5. externalSecrets.vaultPath:
   Dev: "secret/dev/services/api-gateway"
   Prod: "secret/prod/services/api-gateway"

6. resources: Önemli ölçüde artırılmalı
   limits:
     cpu: 500m → 2000m
     memory: 256Mi → 1Gi

7. ingress:
   className: nginx → alb
   host: "" → "${VAULT_DOMAIN}"  # Vault'tan (ana domain)
   annotations: ALB annotations (internet-facing)
```

---

### 5. Account Service ❌ (Oluşturulmalı)

#### Dev Overlay Özellikleri
- Replica: 1
- MSSQL externalName: `host.minikube.internal`
- Ingress: nginx

#### Prod Overlay Gereksinimleri

**Oluşturulması Gereken Dosya:** `services/account-service/k8s/overlays/prod/values.yaml`

```yaml
# Gerekli Değişiklikler:

1. replicaCount: 1 → 2

2. image.pullPolicy: Always → IfNotPresent

3. app.environment: "development" → "production"
   app.logLevel: "debug" → "info"

4. databases.mssql.externalName:
   Dev: "host.minikube.internal"
   Prod: "${VAULT_EC2_PRIVATE_IP}"  # Vault'tan

5. externalSecrets (varsa):
   vaultPath: "secret/dev/services/account" → "secret/prod/services/account"

6. resources: Artırılmalı
   limits:
     cpu: 500m → 1000m
     memory: 1024Mi → 2Gi

7. ingress:
   className: nginx → alb
   host: "" → "${VAULT_DOMAIN}"  # Vault'tan
   annotations: ALB annotations
```

---

### 6. Ledger Service ❌ (Oluşturulmalı)

#### Dev Overlay Özellikleri
- Replica: 1
- MSSQL externalName: `host.minikube.internal`
- Ingress: nginx

#### Prod Overlay Gereksinimleri

**Oluşturulması Gereken Dosya:** `services/ledger-service/k8s/overlays/prod/values.yaml`

```yaml
# Gerekli Değişiklikler:

1. replicaCount: 1 → 2

2. image.pullPolicy: Always → IfNotPresent

3. app.environment: "development" → "production"
   app.logLevel: "debug" → "info"

4. databases.mssql.externalName:
   Dev: "host.minikube.internal"
   Prod: "${VAULT_EC2_PRIVATE_IP}"  # Vault'tan

5. externalSecrets (varsa):
   vaultPath: "secret/dev/services/ledger" → "secret/prod/services/ledger"

6. resources: Artırılmalı
   limits:
     cpu: 500m → 1000m
     memory: 1024Mi → 2Gi

7. ingress:
   className: nginx → alb
   host: "" → "${VAULT_DOMAIN}"  # Vault'tan
   annotations: ALB annotations
```

---

### 7. Card Service ❌ (Oluşturulmalı)

#### Dev Overlay Özellikleri
- Replica: 1
- MSSQL externalName: `host.minikube.internal`
- Ingress: nginx

#### Prod Overlay Gereksinimleri

**Oluşturulması Gereken Dosya:** `services/card-service/k8s/overlays/prod/values.yaml`

```yaml
# Gerekli Değişiklikler:

1. replicaCount: 1 → 2

2. image.pullPolicy: Always → IfNotPresent

3. app.environment: "development" → "production"
   app.logLevel: "debug" → "info"

4. databases.mssql.externalName:
   Dev: "host.minikube.internal"
   Prod: "${VAULT_EC2_PRIVATE_IP}"  # Vault'tan

5. externalSecrets (varsa):
   vaultPath: "secret/dev/services/card" → "secret/prod/services/card"

6. resources: Artırılmalı
   limits:
     cpu: 500m → 1000m
     memory: 1024Mi → 2Gi

7. ingress:
   className: nginx → alb
   host: "" → "${VAULT_DOMAIN}"  # Vault'tan
   annotations: ALB annotations
```

---

### 8. Transaction Service ❌ (Oluşturulmalı)

#### Dev Overlay Özellikleri
- Replica: 1
- MSSQL externalName: `host.minikube.internal`
- Ingress: nginx

#### Prod Overlay Gereksinimleri

**Oluşturulması Gereken Dosya:** `services/transaction-service/k8s/overlays/prod/values.yaml`

```yaml
# Gerekli Değişiklikler:

1. replicaCount: 1 → 2

2. image.pullPolicy: Always → IfNotPresent

3. app.environment: "development" → "production"
   app.logLevel: "debug" → "info"

4. databases.mssql.externalName:
   Dev: "host.minikube.internal"
   Prod: "${VAULT_EC2_PRIVATE_IP}"  # Vault'tan

5. externalSecrets (varsa):
   vaultPath: "secret/dev/services/transaction" → "secret/prod/services/transaction"

6. resources: Artırılmalı
   limits:
     cpu: 500m → 1000m
     memory: 1024Mi → 2Gi

7. ingress:
   className: nginx → alb
   host: "" → "${VAULT_DOMAIN}"  # Vault'tan
   annotations: ALB annotations
```

---

## 🔐 Vault'ta Saklanması Gereken Değerler

### Strateji: Infrastructure ve Environment Değerleri

Kodda sürekli değiştirmek yerine, **infrastructure** ve **environment** seviyesindeki değerleri Vault'ta saklayalım.

### Vault Secret Yapısı

```
secret/
├── dev/
│   └── services/
│       ├── auth
│       ├── client
│       ├── mail
│       ├── api-gateway
│       ├── account
│       ├── ledger
│       ├── card
│       └── transaction
│
├── prod/
│   └── services/
│       ├── auth
│       ├── client
│       ├── mail
│       ├── api-gateway
│       ├── account
│       ├── ledger
│       ├── card
│       └── transaction
│
└── infrastructure/
    ├── ec2/
    │   ├── private_ip          # EC2 Private IP (10.0.0.166)
    │   ├── public_ip           # EC2 Public IP (opsiyonel)
    │   └── hostname            # EC2 Hostname (opsiyonel)
    │
    ├── domain/
    │   ├── api                 # API Domain (api.yourdomain.com)
    │   ├── frontend            # Frontend Domain (app.yourdomain.com)
    │   └── base                # Base Domain (yourdomain.com)
    │
    ├── eks/
    │   ├── cluster_name        # EKS Cluster Name
    │   └── region              # AWS Region
    │
    └── database/
        ├── mssql_port          # MSSQL Port (1433)
        ├── mongodb_port        # MongoDB Port (27017)
        ├── redis_port          # Redis Port (6379)
        └── vault_port          # Vault Port (8200)
```

---

## 📝 Vault Secret Örnekleri

### 1. Infrastructure - EC2

**Path:** `secret/infrastructure/ec2`

```json
{
  "private_ip": "10.0.0.166",
  "public_ip": "51.20.93.33",
  "hostname": "triobank-databases"
}
```

**Kullanım:**
- Tüm servislerde `databases.*.externalName` için
- MSSQL, MongoDB, Redis, Vault bağlantıları için

---

### 2. Infrastructure - Domain

**Path:** `secret/infrastructure/domain`

```json
{
  "api": "api.yourdomain.com",
  "frontend": "app.yourdomain.com",
  "base": "yourdomain.com"
}
```

**Kullanım:**
- Tüm servislerde `ingress.hosts[].host` için
- API Gateway için ana domain
- Diğer servisler için subdomain'ler

---

### 3. Infrastructure - Database Ports

**Path:** `secret/infrastructure/database`

```json
{
  "mssql_port": "1433",
  "mongodb_port": "27017",
  "redis_port": "6379",
  "vault_port": "8200"
}
```

**Kullanım:**
- Database connection string'lerinde
- Service port yapılandırmalarında

---

## 🔄 Helm Values'ta Vault Referansları

### Örnek: Auth Service Prod Overlay

```yaml
# services/auth-service/k8s/overlays/prod/values.yaml

# Vault'tan infrastructure değerlerini çek
infrastructure:
  ec2:
    private_ip: ""  # ExternalSecret ile doldurulacak
  domain:
    api: ""        # ExternalSecret ile doldurulacak

# Database Services
databases:
  mongodb:
    enabled: true
    serviceName: auth-mongodb
    type: mongodb
    portName: mongodb
    externalName: "${infrastructure.ec2.private_ip}"  # Vault'tan
    annotations:
      description: "Auth MongoDB database (Production)"
  
  redis:
    enabled: true
    serviceName: auth-redis
    type: redis
    portName: redis
    externalName: "${infrastructure.ec2.private_ip}"  # Vault'tan
    annotations:
      description: "Auth Redis cache (Production)"

# Ingress
ingress:
  enabled: true
  className: alb
  annotations:
    alb.ingress.kubernetes.io/scheme: internet-facing
    alb.ingress.kubernetes.io/target-type: ip
  hosts:
    - host: "${infrastructure.domain.api}"  # Vault'tan
      paths:
        - path: /auth
          pathType: Prefix
```

---

## 🎯 Implementation Stratejisi

### Adım 1: Vault'ta Infrastructure Secret'ları Oluştur

```bash
# EC2 Private IP
vault kv put secret/infrastructure/ec2 \
  private_ip="10.0.0.166" \
  public_ip="51.20.93.33"

# Domain
vault kv put secret/infrastructure/domain \
  api="api.yourdomain.com" \
  frontend="app.yourdomain.com" \
  base="yourdomain.com"

# Database Ports
vault kv put secret/infrastructure/database \
  mssql_port="1433" \
  mongodb_port="27017" \
  redis_port="6379" \
  vault_port="8200"
```

### Adım 2: ExternalSecret Template'leri Güncelle

Her servis için infrastructure secret'larını çeken ExternalSecret oluştur:

```yaml
# services/*/k8s/templates/infrastructure-secret.yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: infrastructure-config
spec:
  refreshInterval: "24h"
  secretStoreRef:
    name: vault-backend
    kind: ClusterSecretStore
  target:
    name: infrastructure-config
    creationPolicy: Owner
  data:
    - secretKey: ec2_private_ip
      remoteRef:
        key: secret/infrastructure/ec2
        property: private_ip
    - secretKey: domain_api
      remoteRef:
        key: secret/infrastructure/domain
        property: api
```

### Adım 3: Helm Values'ta Kullan

```yaml
# values.yaml veya prod overlay'de
databases:
  mssql:
    externalName: ""  # InitContainer veya env var ile doldurulacak
```

**InitContainer ile:**
```yaml
initContainers:
  - name: load-infrastructure-config
    image: busybox
    command: ['sh', '-c']
    args:
      - |
        export EC2_IP=$(cat /etc/infrastructure/ec2_private_ip)
        # Helm template'te kullan
```

---

## 📊 Özet Tablo: Prod Overlay Gereksinimleri

| Servis | Replica | Resources | ExternalName | Ingress | Vault Path |
|--------|---------|-----------|--------------|---------|------------|
| **auth-service** | 1→2 | Artır | EC2 IP | ALB + Domain | prod/services/auth |
| **client-service** | 1→2 | Artır | EC2 IP | ALB + Domain | prod/services/client |
| **mail-service** | 1→2 | Artır | - | ALB + Domain | prod/services/mail |
| **api-gateway** | 1→3 | Önemli artış | EC2 IP | ALB + Domain | prod/services/api-gateway |
| **account-service** | 1→2 | Artır | EC2 IP | ALB + Domain | prod/services/account |
| **ledger-service** | 1→2 | Artır | EC2 IP | ALB + Domain | prod/services/ledger |
| **card-service** | 1→2 | Artır | EC2 IP | ALB + Domain | prod/services/card |
| **transaction-service** | 1→2 | Artır | EC2 IP | ALB + Domain | prod/services/transaction |

---

## ✅ Sonraki Adımlar

1. **Vault'ta Infrastructure Secret'ları Oluştur**
   - EC2 Private IP
   - Domain bilgileri
   - Database portları

2. **Prod Overlay'leri Oluştur**
   - 7 servis için prod overlay oluştur
   - Vault referanslarını ekle

3. **ExternalSecret Template'leri Güncelle**
   - Infrastructure secret'larını çeken ExternalSecret ekle
   - Her servis için

4. **Helm Values Güncellemeleri**
   - Vault'tan gelen değerleri kullan
   - Hard-coded değerleri kaldır

---

## 🔗 İlgili Dosyalar

- `services/*/k8s/overlays/dev/values.yaml` - Dev overlay'ler
- `services/*/k8s/overlays/prod/values.yaml` - Prod overlay'ler (oluşturulacak)
- `services/*/k8s/templates/externalsecret.yaml` - Vault secret çekme
- `infrastructure/kubernetes/base/platform/helm-charts/external-secrets-operator/` - ESO yapılandırması

---

**Not:** Bu dokümantasyon sadece analiz ve strateji içerir. Kod değişikliği yapılmamıştır.

