# GitOps Stratejisi & ArgoCD

Bu doküman, sistemin "Tek Tıkla" nasıl kurulduğunu, bağımlılıkların nasıl yönetildiğini ve ArgoCD konfigürasyonunu açıklar.

## 1. "App of Apps" Deseni
Tüm küme hiyerarşik bir yapı ile yönetilir. Tek bir "Kök" uygulama (`root`), diğer tüm uygulamaları doğurur.

*   **Root:** Sistemin giriş noktasıdır. `platform` ve `services` ApplicationSet'lerini tetikler.
*   **Platform:** Altyapı bileşenlerini (Kafka, Vault) kurar. Liste tabanlıdır (`List Generator`) çünkü kontrollü ve sıralı kurulum gerektirir.
*   **Services:** Mikroservisleri kurar. Git tabanlıdır (`Git Generator`), klasör açıldığında servisi **otomatik** keşfeder.

## 2. Sync Waves (Akıllı Kurulum Sırası)
Bileşenlerin birbirine bağımlılığı vardır (Örn: Kafka olmadan Connect çalışamaz). ArgoCD bu sırayı `Sync Wave` değerleri ile yönetir.

| Wave | Adım | Bileşenler | Neden? |
| :--- | :--- | :--- | :--- |
| **100** | Temel | `External Secrets`, `Vault` | Şifreler hazır olmalı. |
| **300** | Operatör | `Kafka Operator` | CRD'leri (Custom Resource Definition) yükler. |
| **400** | Omurga | `Kafka Cluster` | Veri yolu açılır. |
| **500** | Ara Katman | `Kafka Connect` | Kafka hazır olduğu için bağlanabilir. |
| **1000** | Servisler | `Ledger Service` | Altyapı tamamsa uygulama başlar. |

> **Sonuç:** `kubectl apply -f infrastructure/kubernetes/argocd/overlays/prod/root.yaml` komutu verildiğinde, sistem bu sırayla, kaosa düşmeden ayağa kalkar.

## 3. Drift Management (Sapma Yönetimi)
Kubernetes operatörleri (Strimzi, ESO), bizim yazdığımız YAML dosyalarına kendi teknik ayarlarını eklerler. ArgoCD bunu "Sizin kodunuz ile Cluster uyuşmuyor (OutOfSync)" olarak algılar.

Bunu önlemek için `ignoreDifferences` kuralı uygulanmıştır:
*   **KafkaConnector:** Connect kümesi `status` alanlarını güncellediğinde ArgoCD bunu görmezden gelir.
*   **ExternalSecret:** Vault token'ı değiştiğinde ArgoCD sürekli "Eskiye dön" (Revert) demeye çalışmaz.

Bu sayede ArgoCD paneli sürekli "Yeşil" (Synced) kalır ve gereksiz uyarılarla operatörü yormaz.
