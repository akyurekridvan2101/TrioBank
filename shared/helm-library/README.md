# Shared Templates Library

Bu klasör, tüm mikroservislerin ortak kullandığı **Helm Template**'lerini barındırır. Amaç kod tekrarını önlemek ve standartlaşmayı sağlamaktır.

## 📂 İçerik

| Dosya | Amaç | Ne Zaman Kullanılır? |
| :--- | :--- | :--- |
| **`_helpers.tpl`** | Ortak label'lar ve isim fonksiyonları. | **Her Zaman.** Tüm chartlarda en az bir kez kullanılır. |
| **`_db-service.tpl`** | External Database için Proxy Service (ExternalName). | Servis bir veritabanına bağlanıyorsa. |
| **`_connector.tpl`** | Kafka Connect (CDC) yapılandırması. | Servis DB değişikliklerini Kafka'ya basacaksa (Outbox). |
| **`_migration-job.tpl`** | Veritabanı şema güncellemeleri. | Servis açılışta DB tablosu oluşturacaksa. |

---

## 🛠️ Nasıl Kullanılır?

Helm Chart'ınızın `templates/` klasörüne sadece tek satırlık bir referans dosyası koyarsınız.

### 1. Database Service (`db-service.yaml`)
Local veya Cloud veritabanına erişim için bir köprü kurar.

**Dosya:** `templates/db-service.yaml`
```yaml
{{- include "common.database-services" . }}
```

**Gerekli Değerler (`values.yaml`):**
```yaml
database:
  enabled: true
  serviceName: "ledger-mssql"    # Cluster içindeki DNS adı
  externalName: "host.docker.internal" # Gerçek adres (Prod'da Azure/AWS adresi)
  port: 1433
  type: "mssql"
```

### 2. Migration Job (`migration-job.yaml`)
Uygulama başlamadan önce çalışır ve DB şemasını günceller.

**Dosya:** `templates/migration-job.yaml`
```yaml
{{- include "common.migrationJob" . }}
```

**Gerekli Değerler (`values.yaml`):**
```yaml
migration:
  enabled: true
  image: "triobank/ledger-migration:v1"
  
secret:
  name: "ledger-db-credentials" # Vault'tan gelen secret
```

### 3. CDC Connector (`connector.yaml`)
Veritabanını dinler ve değişiklikleri Kafka'ya basar.

**Dosya:** `templates/connector.yaml`
```yaml
{{- include "common.connector" . }}
```

**Gerekli Değerler (`values.yaml`):**
```yaml
connector:
  enabled: true
  name: "ledger-cdc"
  table:
    include: "dbo.outbox_events"
  secretVolumeName: "mssql-credentials"
```

### 4. Helpers (`_helpers.tpl`)
Bu dosya direkt `include` edilmez, diğer template'lerin içinde parçalar halinde kullanılır.

*   `common.labels`: Standart Kubernetes etiketleri.
*   `common.fullname`: Release adı ile birleşmiş benzersiz isim.

**Örnek Kullanım (Deployment içinde):**
```yaml
metadata:
  labels:
    {{- include "common.labels" . | nindent 4 }}
```

---

## ⚠️ Önemli Kurallar
1.  Bu klasördeki dosyalarda değişiklik yaparsanız **TÜM SERVİSLER** etkilenir.
2.  Değişiklik yapmadan önce mutlaka yerel ortamda test edin.
3.  Yeni bir template eklerken `common.` prefix'i kullanın (örn: `common.newFeature`).
