#!/bin/bash

# ==============================================================================
# EKS Cluster Silinene Kadar Bekle ve Terraform Apply
# ==============================================================================

REGION="eu-north-1"
CLUSTER_NAME="triobank-cluster"

echo "=========================================="
echo "EKS Cluster Silinene Kadar Bekleniyor"
echo "=========================================="
echo ""

# Cluster silinene kadar bekle
echo "Cluster durumu kontrol ediliyor..."
while true; do
    STATUS=$(aws eks list-clusters --region $REGION --query "clusters[?@=='$CLUSTER_NAME']" --output text 2>&1)
    
    if [ -z "$STATUS" ] || [ "$STATUS" == "None" ]; then
        echo "✓ Cluster silindi!"
        break
    else
        echo "  Cluster hala var, bekleniyor... ($(date +%H:%M:%S))"
        sleep 30
    fi
done

echo ""
echo "=========================================="
echo "Terraform Apply Başlatılıyor"
echo "=========================================="
echo ""

cd /home/akyurek2101/Desktop/triobank/infrastructure/terraform

# Plan kontrolü
echo "Plan kontrol ediliyor..."
terraform plan -out=tfplan > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✓ Plan başarılı"
    echo ""
    echo "Terraform apply başlatılıyor..."
    terraform apply tfplan
else
    echo "⚠ Plan hatası, tekrar plan yapılıyor..."
    terraform plan
fi

