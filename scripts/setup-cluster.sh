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
echo "1) Dev (Geliştirme - Branch seçebilirsiniz)"
echo "2) Prod (Canlı - Sadece 'main' branch)"
read -r ENV_CHOICE

if [[ "$ENV_CHOICE" == "2" ]]; then
    echo "🚀 Prod Ortamı seçildi. 'main' branch deploy ediliyor..."
    echo "🌱 Applying Root App (Prod Env)..."
    kubectl apply -f infrastructure/kubernetes/argocd/overlays/prod/root.yaml
else
    # --- Branch Detection (Only for Dev) ---
    DETECTED_BRANCH=$(git rev-parse --abbrev-ref HEAD)
    echo "🌿 Tespit Edilen Git Branch: '$DETECTED_BRANCH'"
    echo "❓ Bu branch ile devam edilsin mi? (Enter = Evet / H = Değiştir)"
    read -r USER_CHOICE

    if [[ "$USER_CHOICE" =~ ^[Hh]$ ]]; then
        echo "✏️  Lütfen Branch adını girin (Örn: feature/yenilik):"
        read -r TARGET_BRANCH
    else
        TARGET_BRANCH=$DETECTED_BRANCH
    fi
    
    echo "🚀 Hedef Branch: $TARGET_BRANCH"
    echo "🌱 Applying Root App (Dev Env - Dynamic)..."
    sed "s|targetRevision: .*|targetRevision: $TARGET_BRANCH|g; s|branch: .*|branch: $TARGET_BRANCH|g" infrastructure/kubernetes/argocd/overlays/dev/root.yaml | kubectl apply -f -
fi

echo "✅ Bootstrap Complete!"
echo "👉 Now wait for Vault to appear, then run 'setup-vault.sh'"
echo "🔑 ArgoCD Password:"
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d; echo
