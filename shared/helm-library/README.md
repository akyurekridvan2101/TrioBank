# TrioBank Shared Helm Library

**Ortak Helm template library** - Tüm mikroservislerde kullanılan standart Kubernetes resource template'leri.

> **Amaç:** Kod tekrarını önlemek, standartlaşmayı sağlamak ve tüm servislerde tutarlı deployment yapısı oluşturmak.

---

## İçindekiler

1. [Template'lere Genel Bakış](#templateler)
2. [Helpers - Ortak Fonksiyonlar](#1-helpers-_helperstpl)
3. [Database Service](#2-database-service-_db-servicetpl)
4. [Migration Job](#3-migration-job-_migration-jobtpl)
5. [CDC Connector](#4-cdc-connector-_connectortpl)
6. [Hızlı Başlangıç](#hızlı-başlangıç)
7. [Örnekler](#tam-servis-örnekleri)
8. [Önemli Kurallar](#önemli-kurallar)

---

## Template'ler

| Template | Amaç | Kullanım Senaryosu |
|----------|------|-------------------|
| **`_helpers.tpl`** | Ortak label, isim ve DNS fonksiyonları | **Her servis** - Kubernetes standart label'ları için |
| **`_db-service.tpl`** | External database proxy service (ExternalName) | Servis dış database'e bağlanıyorsa |
| **`_migration-job.tpl`** | Database migration job (Helm hook) | Servis DB migration'ı gerektiriyorsa |
| **`_connector.tpl`** | Debezium CDC connector (Outbox Pattern) | Servis DB event'lerini Kafka'ya göndermeli |

---

## 1. Helpers (`_helpers.tpl`)

Tüm servislerde kullanılan **yardımcı fonksiyonlar**. Bu fonksiyonlar direkt bir Kubernetes resource oluşturmaz, diğer template'ler içinde kullanılır.

### Mevcut Fonksiyonlar

#### `common.name`
Chart'ın kısa adını döner.
```yaml
{{ include "common.name" . }}
# Output: ledger-service
```

#### `common.fullname`
Benzersiz resource adı (release + chart name).
```yaml
{{ include "common.fullname" . }}
# Output: my-release-ledger-service
```

#### `common.chart`
Chart adı ve versiyonu.
```yaml
{{ include "common.chart" . }}
# Output: ledger-service-0.1.0
```

#### `common.labels`
Standart Kubernetes recommended labels.
```yaml
metadata:
  labels:
    {{- include "common.labels" . | nindent 4 }}
```
**Çıktı:**
```yaml
helm.sh/chart: ledger-service-0.1.0
app.kubernetes.io/name: ledger-service
app.kubernetes.io/instance: my-release
app.kubernetes.io/version: "1.0.0"
app.kubernetes.io/managed-by: Helm
app.kubernetes.io/part-of: triobank
```

#### `common.selectorLabels`
Pod selector için minimal label seti (immutable).
```yaml
selector:
  matchLabels:
    {{- include "common.selectorLabels" . | nindent 6 }}
```

#### `common.kafka-bootstrap`
Kafka cluster bootstrap server adresi.
```yaml
{{ include "common.kafka-bootstrap" . }}
# Output: triobank-cluster-kafka-bootstrap.triobank.svc.cluster.local:9092
```

### Kullanım Örneği

**Deployment'ta:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "common.fullname" . }}
  labels:
    {{- include "common.labels" . | nindent 4 }}
spec:
  selector:
    matchLabels:
      {{- include "common.selectorLabels" . | nindent 6 }}
  template:
    metadata:
      labels:
        {{- include "common.selectorLabels" . | nindent 8 }}
```

---

## 2. Database Service (`_db-service.tpl`)

Kubernetes cluster **dışındaki** database'lere erişim için **ExternalName Service** oluşturur. Pod'lar database'e sanki cluster içindeymiş gibi bağlanır.

### Ne İşe Yarar?

**DNS Abstraction Flow:**

| Seviye | Endpoint | Açıklama |
|--------|----------|----------|
| **Pod** | `ledger-mssql.triobank:1433` | Uygulama bu adrese bağlanır |
| **Kubernetes Service** | ExternalName Service | DNS CNAME gibi çalışır |
| **Hedef (Local)** | `host.docker.internal:1433` | Geliştirme ortamı |
| **Hedef (Production)** | `prod-mssql.database.windows.net:1433` | Canlı ortam |

### Nasıl Kullanılır?

**1. Template dosyası oluştur:** `templates/db-service.yaml`
```yaml
{{- include "common.database-services" . }}
```

**2. Values tanımla:** `values.yaml`

#### Tek Database (Basit)
```yaml
database:
  enabled: true
  serviceName: "ledger-mssql"
  externalName: "host.docker.internal"  # Local geliştirme
  port: 1433
  type: "mssql"
```

#### Çoklu Database (Önerilen)
```yaml
databases:
  mssql:
    enabled: true
    serviceName: "ledger-mssql"
    externalName: "prod-mssql.database.windows.net"
    port: 1433
    type: "mssql"
    annotations:
      description: "Primary transactional database"
  
  redis:
    enabled: true
    serviceName: "ledger-redis"
    externalName: "prod-redis.cache.windows.net"
    port: 6379
    type: "redis"
  
  mongodb:
    enabled: true
    serviceName: "ledger-mongo"
    externalName: "prod-mongo.cosmos.azure.com"
    port: 27017
    type: "mongodb"
```

### Desteklenen Database Tipleri

| Type | Default Port | Port Name |
|------|--------------|-----------|
| `mssql` | 1433 | mssql |
| `postgresql` / `postgres` | 5432 | postgresql |
| `mongodb` | 27017 | mongodb |
| `mysql` | 3306 | mysql |
| `redis` | 6379 | redis |

### Environment Örnekleri

**Local Development:**
```yaml
externalName: "host.docker.internal"      # Docker Desktop
externalName: "host.minikube.internal"    # Minikube
```

**Azure Cloud:**
```yaml
externalName: "mydb.database.windows.net"           # Azure SQL
externalName: "mydb.cosmos.azure.com"               # Cosmos DB
externalName: "myredis.redis.cache.windows.net"     # Azure Cache
```

**AWS Cloud:**
```yaml
externalName: "mydb.xxxxx.us-east-1.rds.amazonaws.com"      # RDS
externalName: "myredis.xxxxx.cache.amazonaws.com"           # ElastiCache
```

---

## 3. Migration Job (`_migration-job.tpl`)

Database migration'larını **otomatik** çalıştıran Kubernetes Job. Helm hook ile `helm install/upgrade` öncesi tetiklenir.

### Ne İşe Yarar?

- Helm install/upgrade sırasında otomatik çalışır
- Database yoksa oluşturur (initContainer)
- Migration'ları sırayla çalıştırır ([golang-migrate](https://github.com/golang-migrate/migrate))
- Başarısız olursa retry yapar (backoffLimit)
- Tamamlandıktan sonra otomatik temizlenir (TTL)

### Nasıl Kullanılır?

**1. Template dosyası oluştur:** `templates/migration-job.yaml`
```yaml
{{- include "common.migrationJob" . }}
```

**2. Values tanımla:** `values.yaml`
```yaml
migration:
  enabled: true
  image: "triobank/ledger-migration:v1.0.0"
  imagePullPolicy: "IfNotPresent"
  jdbcParams: "encrypt=false;trustServerCertificate=true"
  backoffLimit: 3                    # Kaç kere retry
  ttlSecondsAfterFinished: 3600      # 1 saat sonra job silinir
  resources:
    limits:
      cpu: "200m"
      memory: "128Mi"
    requests:
      cpu: "50m"
      memory: "64Mi"

database:
  serviceName: "ledger-mssql"
  port: 1433
  name: "ledger_db"

secret:
  name: "ledger-db-credentials"
  usernameKey: "username"          # Optional, default: "username"
  passwordKey: "password"          # Optional, default: "password"
```

**3. Migration Docker Image Hazırla**

Migration image'ınızda `/migrations` klasöründe SQL dosyaları olmalı:
```
/migrations/
├── 001_init.up.sql
├── 002_add_users.up.sql
└── 003_add_index.up.sql
```

**Örnek Dockerfile:**
```dockerfile
FROM migrate/migrate:latest
COPY migrations /migrations
```

### Job Davranışı

| Hook | Ne Zaman Çalışır? | Davranış |
|------|------------------|----------|
| `pre-install` | İlk defa helm install | Migration'ları çalıştırır |
| `pre-upgrade` | Helm upgrade | Yeni migration'ları uygular |

---

## 4. CDC Connector (`_connector.tpl`)

**Debezium CDC Connector** ile database değişikliklerini Kafka'ya event olarak gönderir. **Outbox Pattern** ile event-driven mimari sağlar.

### Ne İşe Yarar?

Database'deki `outbox_events` tablosunu dinler ve her satırı Kafka topic'e event olarak basar.

```
Database (outbox_events) 
    ↓ (Debezium CDC)
Kafka Topic
    ↓
Event Consumers
```

### Nasıl Kullanılır?

**1. Template dosyası oluştur:** `templates/connector.yaml`
```yaml
{{- if .Values.connector.enabled }}
{{- include "common.connector" . }}
{{- end }}
```

**2. Values tanımla:** `values.yaml`
```yaml
connector:
  enabled: true
  name: "ledger-cdc-connector"
  secretVolumeName: "mssql-credentials"    # Kafka Connect'te mount edilmiş secret
  
  database:
    hostname: "ledger-mssql.triobank"
    port: 1433
    names: "ledger_db"
    serverName: "ledger"                   # Topic prefix olarak kullanılır
    encrypt: "false"                       # Local: false, Prod: true
    trustServerCertificate: "true"
  
  table:
    include: "dbo.outbox_events"           # İzlenecek tablo
  
  topic:
    prefix: "ledger"
  
  snapshot:
    mode: "initial"                        # İlk çalıştırmada snapshot al
  
  outbox:
    routeByField: "aggregate_type"         # Bu kolona göre topic'e route et
    topicReplacement: "triobank.local.ledger.${routedByValue}.v1"
    fields:
      id: "id"
      key: "aggregate_id"
      type: "type"
      payload: "payload"
      timestamp: "created_at"
    tombstoneOnEmpty: "false"
    expandJsonPayload: "true"
  
  performance:
    pollInterval: "500"                    # ms
    maxBatchSize: "2048"
  
  errors:
    tolerance: "all"
    logEnable: "true"
    logMessages: "true"
  
  heartbeat:
    interval: "60000"                      # ms
    topicPrefix: "__debezium-heartbeat"
```

### Gereksinimler

**Database'de CDC aktif olmalı:**
```sql
-- Database level CDC
EXEC sys.sp_cdc_enable_db;

-- Table level CDC
EXEC sys.sp_cdc_enable_table
  @source_schema = N'dbo',
  @source_name = N'outbox_events',
  @role_name = NULL;
```

**Outbox tablosu şeması:**
```sql
CREATE TABLE dbo.outbox_events (
    id UNIQUEIDENTIFIER PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id UNIQUEIDENTIFIER NOT NULL,
    type VARCHAR(255) NOT NULL,
    payload NVARCHAR(MAX) NOT NULL,
    created_at DATETIME2 DEFAULT GETUTCDATE()
);
```

### Topic Naming

Template yukarıdaki values ile şu topic'i oluşturur:
```
triobank.local.ledger.ACCOUNT.v1
                     ↑ (aggregate_type değeri)
```

---

## Hızlı Başlangıç

Sıfırdan bir servis için Helm chart oluşturmak:

### 1. Chart Yapısını Oluştur

```bash
cd services/my-service/
mkdir -p helm/my-service/templates
```

### 2. `Chart.yaml` Oluştur

```yaml
apiVersion: v2
name: my-service
description: My Service Helm Chart
type: application
version: 0.1.0
appVersion: "1.0.0"

dependencies:
  - name: common
    version: "0.1.1"
    repository: "file://../../../../shared/helm-library"
```

### 3. Template Dosyaları Oluştur

**`templates/db-service.yaml`**
```yaml
{{- include "common.database-services" . }}
```

**`templates/migration-job.yaml`**
```yaml
{{- include "common.migrationJob" . }}
```

**`templates/connector.yaml`**
```yaml
{{- if .Values.connector.enabled }}
{{- include "common.connector" . }}
{{- end }}
```

**`templates/deployment.yaml`**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "common.fullname" . }}
  labels:
    {{- include "common.labels" . | nindent 4 }}
spec:
  replicas: {{ .Values.replicaCount }}
  selector:
    matchLabels:
      {{- include "common.selectorLabels" . | nindent 6 }}
  template:
    metadata:
      labels:
        {{- include "common.selectorLabels" . | nindent 8 }}
    spec:
      containers:
      - name: {{ .Chart.Name }}
        image: "{{ .Values.image.repository }}:{{ .Values.image.tag }}"
        # ... rest of deployment
```

### 4. `values.yaml` Oluştur

```yaml
replicaCount: 1

image:
  repository: triobank/my-service
  tag: "latest"

# Database Service
database:
  enabled: true
  serviceName: "my-service-mssql"
  externalName: "host.docker.internal"
  port: 1433
  type: "mssql"

# Migration Job
migration:
  enabled: true
  image: "triobank/my-service-migration:v1.0.0"

# CDC Connector
connector:
  enabled: true
  name: "my-service-cdc"
  secretVolumeName: "mssql-credentials"
  database:
    hostname: "my-service-mssql.triobank"
    port: 1433
    names: "my_service_db"
  table:
    include: "dbo.outbox_events"
  topic:
    prefix: "my-service"

# Secret reference
secret:
  name: "my-service-db-credentials"
```

### 5. Dependency Güncelle ve Deploy Et

```bash
# Dependency'leri çek
helm dependency update helm/my-service

# Local test
helm install my-service helm/my-service --dry-run --debug

# Deploy et
helm install my-service helm/my-service -n triobank
```

---

## Tam Servis Örnekleri

### Basit Servis (Sadece Database)

Sadece database bağlantısı olan minimal servis.

**values.yaml:**
```yaml
database:
  enabled: true
  serviceName: "simple-service-db"
  externalName: "host.docker.internal"
  port: 5432
  type: "postgresql"

secret:
  name: "simple-service-credentials"
```

### Tam Stack Servis (DB + Migration + CDC)

Production-ready, event-driven servis.

**values.yaml:**
```yaml
# Multiple databases
databases:
  postgres:
    enabled: true
    serviceName: "account-postgres"
    externalName: "prod-postgres.rds.amazonaws.com"
    port: 5432
    type: "postgresql"
  
  redis:
    enabled: true
    serviceName: "account-redis"
    externalName: "prod-redis.cache.amazonaws.com"
    port: 6379
    type: "redis"

# Migration
migration:
  enabled: true
  image: "triobank/account-migration:v2.1.0"
  backoffLimit: 5
  resources:
    limits:
      cpu: "500m"
      memory: "256Mi"

# CDC Connector
connector:
  enabled: true
  name: "account-cdc-connector"
  secretVolumeName: "postgres-credentials"
  database:
    hostname: "account-postgres.triobank"
    port: 5432
    names: "account_db"
    serverName: "account"
  table:
    include: "public.outbox_events"
  topic:
    prefix: "account"
  outbox:
    routeByField: "aggregate_type"
    topicReplacement: "triobank.prod.account.${routedByValue}.v1"
  performance:
    pollInterval: "100"
    maxBatchSize: "4096"

secret:
  name: "account-db-credentials"
```

---

## Önemli Kurallar

### 1. Template Değişiklikleri TÜM SERVİSLERİ Etkiler

Bu klasördeki template'lerde değişiklik yaparsanız **tüm mikroservisler** etkilenir. Değişiklik yapmadan önce:
- Yerel ortamda test edin
- Backward compatibility düşünün
- Breaking change varsa tüm servisleri güncelleyin

### 2. Yeni Template Ekleme

Yeni template eklerken:
- `common.` prefix kullanın (örn: `common.newFeature`)
- README'ye dokümantasyon ekleyin
- Örnek values.yaml ekleyin

### 3. Naming Convention

```yaml
# Doğru
{{ include "common.labels" . }}

# Yanlış
{{ include "my-labels" . }}
{{ include "labels" . }}
```

### 4. Version Bump

Template değişikliği yaptıktan sonra `Chart.yaml` versiyonunu artırın:
```yaml
version: 0.1.2  # Bumped for template changes
```
