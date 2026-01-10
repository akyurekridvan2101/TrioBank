# Kubernetes Platform Dokümantasyonu

Bu bölüm, TrioBank'ın Kubernetes altyapısını, platform mimarisini, dağıtım iş akışlarını (GitOps) ve gizli veri (secret) yönetimini kapsar.

## 📑 İçindekiler

### 1. [Platform Mimarisi](01-platform-architecture.md)
EKS cluster kurulumu, node grupları, ingress denetleyicileri ve ağ yapılandırmasına genel bakış.

### 2. [GitOps & ArgoCD](02-gitops-argocd.md)
ArgoCD kullanarak uyguladığımız dağıtım stratejisi. Git üzerindeki değişikliklerin cluster'a nasıl yansıdığının açıklaması.

### 3. [Secret Yönetimi](03-secret-management.md)
Hassas verilerin HashiCorp Vault kullanılarak nasıl yönetildiği ve ExternalSecrets veya CSI sürücüleri aracılığıyla Kubernetes ile nasıl entegre edildiği.

## ☸️ Temel Bileşenler

- **Cluster**: AWS EKS
- **Ingress**: Nginx Ingress Controller
- **Sertifikalar**: Cert-Manager (Let's Encrypt)
- **Gözlemlenebilirlik**: Prometheus & Grafana (İzleme), ELK/Loki (Loglama)
- **GitOps**: ArgoCD
