# EC2 Instance Type Yükseltme - MSSQL İçin

**Tarih:** 23 Aralık 2025  
**Durum:** ✅ EC2 instance type `t3.medium` (4GB RAM) olarak güncellendi

---

## 📊 Maliyet Analizi

### Mevcut Durum
- **EC2 Instance:** `t2.micro` (1GB RAM) → ❌ MSSQL için yetersiz
- **Sorun:** MSSQL Server 2022 en az 2GB RAM gerektiriyor

### Yeni Durum
- **EC2 Instance:** `t3.medium` (4GB RAM) → ✅ MSSQL için yeterli
- **Maliyet:** ~$0.04/saat (eu-north-1)

### 3-5 Günlük Kullanım Maliyeti

| Süre | Saat | t3.medium Maliyeti | t3.large Maliyeti |
|------|------|-------------------|-------------------|
| 3 gün | 72 saat | ~$2.88 | ~$5.76 |
| 5 gün | 120 saat | ~$4.80 | ~$9.60 |

**Sonuç:** 100 dolar kredi ile rahatlıkla 3-5 gün kullanılabilir! ✅

---

## 🔧 Instance Type Seçenekleri

### t3.small (2GB RAM)
- **Maliyet:** ~$0.02/saat
- **RAM:** 2GB (MSSQL minimum gereksinim)
- **Durum:** ⚠️ Minimum seviye, performans sınırlı olabilir
- **3 gün maliyet:** ~$1.44
- **5 gün maliyet:** ~$2.40

### t3.medium (4GB RAM) ✅ ÖNERİLEN
- **Maliyet:** ~$0.04/saat
- **RAM:** 4GB (MSSQL için rahat)
- **Durum:** ✅ Önerilen seviye
- **3 gün maliyet:** ~$2.88
- **5 gün maliyet:** ~$4.80

### t3.large (8GB RAM)
- **Maliyet:** ~$0.08/saat
- **RAM:** 8GB (MSSQL için fazlasıyla yeterli)
- **Durum:** ✅ Güvenli ama daha pahalı
- **3 gün maliyet:** ~$5.76
- **5 gün maliyet:** ~$9.60

---

## 🚀 Uygulama Adımları

### 1. Terraform Değişkenini Güncelle
```bash
# variables.tf dosyasında zaten güncellendi:
ec2_instance_type = "t3.medium"
```

### 2. Terraform Apply
```bash
cd infrastructure/terraform
terraform plan  # Değişiklikleri kontrol et
terraform apply # EC2 instance type'ını güncelle
```

**Not:** Terraform mevcut EC2 instance'ı destroy edip yenisini oluşturacak. Bu işlem sırasında:
- EC2 instance yeniden oluşturulacak
- Yeni Private IP alınacak (prod overlay'lerde güncellenmeli)
- Docker Compose servisleri yeniden kurulmalı

### 3. Yeni Private IP'yi Güncelle
```bash
# Terraform output'tan yeni Private IP'yi al
terraform output ec2_private_ip

# Prod overlay'lerdeki externalName değerlerini güncelle
# Tüm servislerde: externalName: "YENİ_PRIVATE_IP"
```

### 4. Docker Compose Servislerini Yeniden Kur
```bash
ssh -i infrastructure/terraform/triobank-ec2-key.pem ec2-user@<YENİ_PUBLIC_IP>
cd ~/triobank-databases
docker-compose up -d
```

---

## 💰 Toplam Maliyet Tahmini (3-5 Gün)

### EC2 (t3.medium)
- 3 gün: ~$2.88
- 5 gün: ~$4.80

### EKS Cluster
- Cluster: ~$0.10/saat = ~$7.20 (3 gün) / ~$12.00 (5 gün)
- Node Group (2x t3.micro): ~$0.02/saat/node = ~$2.88 (3 gün) / ~$4.80 (5 gün)

### Diğer (NAT Gateway, Load Balancer, vb.)
- ~$5-10 (3-5 gün)

### Toplam Tahmini
- **3 gün:** ~$18-20
- **5 gün:** ~$27-32

**Sonuç:** 100 dolar kredi ile rahatlıkla 3-5 gün kullanılabilir! ✅

---

## ⚠️ Önemli Notlar

1. **Free Tier Limitleri:** 100 dolar kredi ile Free Tier limitleri geçerli değil, istediğiniz instance type'ı kullanabilirsiniz.

2. **Instance Değişikliği:** Terraform apply sırasında EC2 instance yeniden oluşturulacak, bu yüzden:
   - Private IP değişecek
   - Prod overlay'lerdeki `externalName` güncellenmeli
   - Docker Compose servisleri yeniden kurulmalı

3. **Veri Kaybı:** EC2 instance yeniden oluşturulduğunda volume'ler korunur, ancak Docker volume'leri kaybolabilir. Önemli veriler için backup alın.

4. **MSSQL:** t3.medium (4GB RAM) MSSQL için yeterli, ancak yoğun kullanımda t3.large (8GB RAM) daha güvenli olabilir.

---

## 🎯 Sonuç

✅ **t3.medium (4GB RAM)** seçildi - MSSQL için yeterli ve 100 dolar kredi ile 3-5 gün rahatlıkla kullanılabilir!

**Sonraki Adım:** `terraform apply` ile EC2 instance type'ını güncelle.


