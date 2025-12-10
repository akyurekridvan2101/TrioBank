# Platform Mimarisi (Kubernetes)

Bu doküman, `triobank` kümesinin teknik mimarisini, katmanlarını ve dış kaynaklarla (Host/Cloud) olan entegrasyonunu detaylandırır.

## Mimari Vizyon
Sistem, **Nested Architecture (İç İçe Katmanlar)** prensibiyle tasarlanmıştır. Karmaşıklığı yönetmek için bileşenler mantıksal kutulara ayrılmıştır.

*   **Tek Namespace (`triobank`):** Tüm bileşenler tek bir düzlemde çalışır, iletişim bariyerleri kaldırılmıştır.
*   **Environment Agnostic:** Kubernetes, dış kaynaklara (DB, Vault) sadece Proxy (Service) üzerinden erişir. Bu sayede altyapı Local (Docker) veya Cloud (AWS) üzerinde değişmeden çalışabilir.

## Mimari Şema

Aşağıdaki diyagram, sistemin Kubernetes içindeki mantıksal dağılımını ve veri akışını gösterir.

![Kubernetes Cluster Architecture](../assets/k8s_cluster_arch.svg)

---

## Katman Detayları

Şemada görülen katmanların teknik sorumlulukları şunlardır:

### 1. Infrastructure (Altyapı Çekirdeği)
Sistemin omurgasıdır. Diğer tüm servisler bu katmana bağımlıdır.

*   **Kafka Cluster:** Event'lerin aktığı ana damar.
*   **Kafka Connect:** Veritabanı değişikliklerini (CDC) yakalayıp Kafka'ya aktaran mekanizma.
    *   *Özerk Yapı:* Her domain kendi Connector'ını (`ledger-cdc`) bu kümeye deploy eder.
*   **Kafka Operator:** Bu bileşenlerin yaşam döngüsünü (kurulum, upgrade) yöneten robot.

### 2. Services (İş Katmanı)
İş mantığının koştuğu yerdir. Her servis kendi "Domain Kutusu" içinde izole edilir.

#### Ledger Domain (Örnek Model)
*   **Ledger App:** Hesap hareketlerini işleyen ana uygulama.
*   **Migration Job:** Veritabanı şemasını güncelleyen geçici iş parçacığı.
*   **Secret Management:** Uygulama, Vault'tan şifresini `ExternalSecret` aracılığıyla otomatik alır.

---

## Veri Akışı (Data Flow)

Şemadaki okların temsil ettiği kritik veri yolları:

1.  **CDC Pipeline (Outbox Pattern):**
    *   `MSSQL` -> `ProxyDB` -> `Connector (Connect Runtime)` -> `Kafka`
    *   *Açıklama:* Veritabanına yazılan her satır, Connector tarafından okunur ve Kafka'ya event olarak basılır. Uygulamanın bundan haberi olmaz (Decoupled).

2.  **Service Communication:**
    *   `Ledger App` -> `Kafka` (Async Event Üretimi/Tüketimi)
    *   `Ledger App` -> `ProxyDB` (Senkron Veri Okuma)

Bu yapı, sistemin **Loose Coupled** (Gevşek Bağlı) ve yüksek ölçeklenebilir olmasını sağlar.

---


Tüm mikroservisler (`ledger`, `payment` vb.) belirli bir standardı takip eder. Bu standart, geliştirme ve operasyon süreçlerini birleştirir.

### Servis Envanteri (Erişim Noktaları)
Kubernetes içindeki servislerin birbirine nasıl ulaşacağı aşağıda tanımlanmıştır.

| Servis Adı | DNS Adresi (`.triobank.svc`) | Port | Açıklama |
| :--- | :--- | :--- | :--- |
| **Kafka Cluster** | `kafka-kafka-bootstrap` | `9092` | Tüm servisler Event üretmek için buraya bağlanır. |
| **Kafka Connect** | `connect-connect-api` | `8083` | CDC Connector'ları buraya POST edilir. |
| **Vault** | `vault` | `8200` | Şifreler buradan çekilir (App'ler direkt gitmez, ESO gider). |
| **Ledger App** | `ledger` | `8080` | Muhasebe API'si (Diğer servisler buraya HTTP isteği atar). |
| **Mssql Proxy** | `ledger-mssql` | `1433` | Host veritabanına açılan kapı. |

> **Not:** DNS adresleri kısadır (`ledger`, `kafka-kafka-bootstrap`) çünkü hepsi aynı `triobank` namespace'indedir.
