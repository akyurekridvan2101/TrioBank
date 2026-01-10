#!/bin/bash
set -e

echo "🚀 Starting Fresh Cluster Setup (TrioBank)..."

echo "🔄 Resetting Minikube..."
minikube delete
minikube start --driver=docker --memory=8192 --cpus=4 --addons=ingress

echo "ns Creating Namespaces..."
kubectl create namespace argocd
kubectl create namespace triobank

echo "🐙 Installing ArgoCD..."
helm repo add argoproj https://argoproj.github.io/argo-helm
helm repo update
helm upgrade --install argocd argoproj/argo-cd \
  --namespace argocd \
  --version 5.46.7 \
  --values infrastructure/kubernetes/argocd/overlays/dev/values.yaml \
  --wait

echo "⏳ Waiting for ArgoCD Server..."
kubectl rollout status deployment argocd-server -n argocd --timeout=300s

echo "⚠️  ÖNEMLİ UYARI: Vault kilidinin (Unseal) açık olduğundan emin olun!"
echo "🔑 Lütfen Vault Root Token'ını girin (Girdiğiniz karakterler gizlenecektir):"
read -s VAULT_TOKEN
echo "✅ Token alındı."

echo "🔑 Creating Vault Token Secret..."
kubectl create secret generic vault-token \
  --from-literal=token=$VAULT_TOKEN \
  --namespace triobank \
  --dry-run=client -o yaml | kubectl apply -f -

# --- Environment Selection ---
echo "🌍 Hangi ortamı kurmak istiyorsunuz?"
echo "1) Dev (Geliştirme - develop branch)"
echo "2) Prod (Üretim - main branch)"
read -r ENV_CHOICE

if [[ "$ENV_CHOICE" == "1" ]]; then
    # ===== DEV ENVIRONMENT =====
    echo "🚀 Dev ortamı seçildi"
    echo "🌿 Default branch: develop"
    echo "❓ Hangi branch deploy edilsin?"
    echo "   → ENTER'a bas = develop branch kullanılır"
    echo "   → Branch adı yaz = o branch kullanılır (örn: feature/yeni-ozellik)"
    read -r CUSTOM_BRANCH

    if [[ -z "$CUSTOM_BRANCH" ]]; then
        # Boş input - develop branch kullan (root.yaml'daki default)
        echo "✅ develop branch ile deploy ediliyor..."
        echo "🌱 Root Application deploy ediliyor..."
        kubectl apply -f infrastructure/kubernetes/argocd/overlays/dev/root.yaml
    else
        # Custom branch override
        echo "✅ $CUSTOM_BRANCH branch ile deploy ediliyor..."
        echo "🌱 Root Application deploy ediliyor (Branch override)..."
        sed "s|targetRevision: .*|targetRevision: $CUSTOM_BRANCH|g; s|branch: .*|branch: $CUSTOM_BRANCH|g" \
            infrastructure/kubernetes/argocd/overlays/dev/root.yaml | kubectl apply -f -
    fi

elif [[ "$ENV_CHOICE" == "2" ]]; then
    # ===== PROD ENVIRONMENT =====
    echo "🚀 Prod ortamı seçildi (main branch)"
    echo "🌱 Root Application deploy ediliyor..."
    kubectl apply -f infrastructure/kubernetes/argocd/overlays/prod/root.yaml

else
    # ===== INVALID INPUT =====
    echo "❌ Hatalı seçim! Sadece 1 veya 2 girebilirsiniz."
    echo "💡 Script'i tekrar çalıştırın: ./scripts/setup-cluster.sh"
    exit 1
fi

echo ""
echo "🌐 ArgoCD UI'ya erişim için port-forward:"
echo "   kubectl port-forward svc/argocd-server -n argocd 8080:443"
echo "   Sonra tarayıcıda: https://localhost:8080"
echo ""
echo "👉 Şimdi ArgoCD'nin tüm servisleri deploy etmesini bekleyin..."
echo "   kubectl get applications -n argocd"


echo "✅ Bootstrap Complete!"
echo "👉 Now wait for Vault to appear, then run 'setup-vault.sh'"
echo "🔑 ArgoCD Password:"
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d; echo
