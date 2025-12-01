# Secret Management (Vault & ESO)

Bu doküman, hassas verilerin (DB şifreleri, API anahtarları) sistemde nasıl güvende tutulduğunu ve uygulamalara nasıl dağıtıldığını açıklar.

## 🚫 Altın Kural
**Hiçbir şifre (Secret), Git reposunda açık metin (Plain Text) olarak saklanmaz!**

## 🔄 Çalışma Mantığı (Akış)
Sistemde şifreler "Havadan" (Vault'tan) gelir. Dosya sisteminde yaşamazlar.

1.  **Vault (Kasa):** Şifrelerin tek gerçek kaynağıdır (Source of Truth). Şifreler buraya elle veya Terraform ile girilir.
2.  **External Secrets Operator (Kurye):** Kubernetes içinde çalışır, Vault'u sürekli dinler.
3.  **Kubernetes Secret (Paket):** ESO, Vault'tan aldığı şifreyi standart K8s Secret nesnesine çevirir. Uygulamalar sadece bunu görür.

## 🛠️ Nasıl Yeni Secret Eklenir?

Bir geliştirici olarak `Payment Service` için yeni bir API Key'e ihtiyacınız olduğunu varsayalım:

### Adım 1: Vault'a Ekle
Vault arayüzüne (veya CLI) gidip şifreyi tanımlayın:
*   Path: `secret/payment-service/api-keys`
*   Key: `stripe-key`
*   Value: `sk_test_123456...`

### Adım 2: ExternalSecret Tanımla
Helm chart içine (`services/payment/k8s/templates/external-secret.yaml`) şu tanımı yapın:

```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: payment-api-keys
spec:
  # Bu isimle K8s Secret oluşacak
  target:
    name: payment-api-keys
  data:
  - secretKey: STRIPE_KEY          # Uygulamanın göreceği Key
    remoteRef:
      key: secret/payment-service/api-keys  # Vault'taki Path
      property: stripe-key                  # Vault'taki Key
```

### Adım 3: Uygulamaya Ver
Deployment dosyanızda bu secret'ı `envFrom` ile içeri alın:

```yaml
envFrom:
  - secretRef:
      name: payment-api-keys
```

Bitti! Artık uygulamanız `STRIPE_KEY` environment değişkenine sahip.

## ❓ Sık Sorulan Sorular

**S: Vault çökerse ne olur?**
C: Uygulamalar çalışmaya devam eder. Çünkü ESO, şifreyi bir kez alıp Kubernetes Secret olarak (etcd içinde) kaydetmiştir. Sadece yeni şifreler senkronize edilemez.

**S: Şifreyi değiştirdim, uygulamayı restart etmeli miyim?**
C: Evet. ESO, Kubernetes Secret'ı günceller ancak uygulamanın (Pod) yeni env değişkenini alması için yeniden başlatılması (Rollout Restart) gerekir.
