# EC2'yi t3.medium (4GB RAM) Yükseltme - Hızlı Rehber

**Hedef:** t3.small (2GB) → t3.medium (4GB RAM)

---

## 🚀 Adım Adım (5 Dakika)

### Adım 1: AWS Console'a Giriş
1. https://console.aws.amazon.com adresine gidin
2. AWS hesabınızla giriş yapın
3. **EC2** servisini seçin (arama çubuğuna "EC2" yazın)

### Adım 2: Instance'ı Bul
1. Sol menüden **Instances** → **Instances** seçin
2. `triobank-cluster-databases` adlı instance'ı bulun
3. Instance'ı seçin (checkbox işaretleyin)

**Not:** Instance ID: `i-002bb560ad379fea5`

### Adım 3: Instance'ı Durdur
1. Üstteki **Instance state** butonuna tıklayın
2. **Stop instance** seçin
3. Onaylayın: **Stop** butonuna tıklayın
4. Instance durumu **stopped** olana kadar bekleyin (1-2 dakika)

**Bekleme:** Instance state'in "stopped" olmasını bekleyin.

### Adım 4: Instance Type'ı Değiştir
1. Instance hala seçiliyken, üstteki **Actions** butonuna tıklayın
2. **Instance settings** → **Change instance type** seçin
3. **Instance type** dropdown'ından **t3.medium** seçin
4. **Apply** butonuna tıklayın

**Bekleme:** Değişiklik hemen uygulanır (birkaç saniye).

### Adım 5: Instance'ı Başlat
1. Üstteki **Instance state** butonuna tıklayın
2. **Start instance** seçin
3. Onaylayın: **Start** butonuna tıklayın
4. Instance durumu **running** olana kadar bekleyin (1-2 dakika)

**Bekleme:** Instance state'in "running" olmasını bekleyin.

### Adım 6: Yeni IP Adreslerini Not Al
1. Instance seçiliyken, alttaki **Details** sekmesinde:
   - **Public IPv4 address:** Yeni Public IP (SSH için)
   - **Private IPv4 address:** Yeni Private IP (EKS'ten bağlanmak için)

**ÖNEMLİ:** Private IP değişmiş olabilir! Kontrol edin.

---

## ✅ Kontrol

### Instance Type Kontrolü
1. Instance seçiliyken **Details** sekmesinde:
   - **Instance type:** `t3.medium` görünmeli ✅
   - **vCPU:** 2
   - **Memory:** 4 GiB ✅

### IP Adreslerini Kontrol Et
```bash
# Terraform output'tan kontrol
cd infrastructure/terraform
terraform output ec2_private_ip
terraform output ec2_public_ip
```

**Not:** Private IP değişmişse, prod overlay'lerdeki IP'leri güncellemeniz gerekebilir.

---

## 📝 Private IP Değişirse

Eğer Private IP değiştiyse (örnek: `10.0.0.182` → `10.0.0.XXX`):

1. **Yeni Private IP'yi not alın**
2. **Prod overlay'lerdeki IP'leri güncelleyin:**
   ```bash
   # Tüm prod overlay'lerdeki IP'yi güncelle
   find services -name "values.yaml" -path "*/prod/*" -exec sed -i 's/10\.0\.0\.182/YENİ_PRIVATE_IP/g' {} \;
   ```

**Güncellenecek Dosyalar:**
- `services/account-service/k8s/overlays/prod/values.yaml`
- `services/transaction-service/k8s/overlays/prod/values.yaml`
- `services/ledger-service/k8s/overlays/prod/values.yaml`
- `services/card-service/k8s/overlays/prod/values.yaml`
- `services/client-service/k8s/overlays/prod/values.yaml`
- `services/auth-service/k8s/overlays/prod/values.yaml`
- `services/api-gateway/k8s/overlays/prod/values.yaml`

---

## 🎯 Sonuç

✅ **Instance Type:** t3.small → t3.medium  
✅ **RAM:** 2GB → 4GB  
✅ **4 servis için yeterli!**

**Artık MSSQL, MongoDB, Redis ve Vault rahatlıkla çalışabilir!** 🚀

---

## 💰 Maliyet

- **t3.medium:** ~$0.04/saat
- **3 gün:** ~$2.88
- **5 gün:** ~$4.80

**100 dolar kredi ile rahatlıkla kullanılabilir!** ✅

---

## ⚠️ Sorun Giderme

### Instance durdurulamıyor
- Birkaç dakika bekleyin
- Instance'ın tüm işlemleri tamamlaması gerekebilir

### Instance type değiştirilemiyor
- Instance'ın durumu "stopped" olmalı
- Eğer hala "stopping" ise bekleyin

### IP adresi değişti
- Normal bir durum, prod overlay'lerdeki IP'leri güncelleyin

---

## 📞 Yardım

Sorun yaşarsanız:
1. AWS Console'da instance'ın durumunu kontrol edin
2. Instance logs'larına bakın
3. Rehberi tekrar okuyun: `docs/guides/ec2-manual-instance-type-upgrade.md`


