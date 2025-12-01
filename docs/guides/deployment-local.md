# Yerel Geliştirme Ortamı Kurulumu (Local Deployment)

Bu rehber, bir geliştiricinin kendi bilgisayarında (Localhost) tüm TrioBank platformunu sıfırdan ayağa kaldırmasını sağlar.

## 🛠️ Ön Koşullar
*   **Docker Desktop** (veya eşdeğeri)
*   **Minikube** (`minikube start` çalışır durumda olmalı)
*   **Kubectl** & **Helm**
*   **Vault CLI** (Opsiyonel, debug için)

## 🚀 Hızlı Kurulum
Tüm kurulum sürecini (Namespace, ArgoCD, Vault Token, Root App) otomasyona bağladık. Tek bir script ile sistemi ayağa kaldırabilirsiniz.

```bash
# Proje kök dizininde çalıştırın:
./setup-cluster.sh
```

### Script Ne Yapar?
1.  `minikube delete` ile eski, bozuk durumu temizler.
2.  `triobank` namespace'ini sıfırdan açar.
3.  **Sizden Vault Token'ını ister** (Güvenli giriş için).
4.  **Git Branch'ini Teyit Eder:** "Şu an `feature/x` üzerindesin, bunu mu deploy edeyim?" diye sorar.
5.  ArgoCD'yi kurar ve sizin branch'iniz ile `root.yaml` uygulamasını başlatır.

> **⚠️ Dikkat:** Scripti çalıştırmadan önce Vault'unuzun Unseal (kilit açık) durumda olduğundan ve Root Token'ın elinizde olduğundan emin olun.

## ✅ Doğrulama

Kurulum bittikten sonra arayüzlere erişmek için:

**ArgoCD:**
```bash
kubectl port-forward svc/argocd-server -n argocd 8085:443
# URL: https://localhost:8085
# User: admin
# Pass: kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d
```

**Kafka UI:**
```bash
kubectl port-forward svc/platform-kafka-ui -n triobank 8090:80
# URL: http://localhost:8090
```
