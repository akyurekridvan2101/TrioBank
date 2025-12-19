# AWS Free Tier vs 100 Dolar Kredi - Açıklama

**Önemli:** 100 dolar kredisi olan hesaplarda Free Tier limitleri geçerli değil!

---

## 🎯 Free Tier vs Kredi Farkı

### Free Tier (Kredi Yoksa)
- **Limitler:** Sadece belirli instance type'ları (t2.micro, t3.micro)
- **Süre:** 12 ay veya 750 saat/ay
- **Kısıtlamalar:** t3.medium, t3.large gibi instance'lar kullanılamaz
- **EKS:** Free Tier'da yok

### 100 Dolar Kredi (Bizim Durumumuz) ✅
- **Limitler:** YOK! İstediğiniz instance type'ı kullanabilirsiniz
- **Süre:** Kredi bitene kadar
- **Kısıtlamalar:** YOK! t3.medium, t3.large, hatta daha büyük instance'lar kullanılabilir
- **EKS:** Kullanılabilir (kredi ile)

**Sonuç:** 100 dolar kredisi olan hesaplarda Free Tier limitleri geçerli değil! ✅

---

## ⚠️ Terraform Sorunu

### Neden Terraform ile t3.medium Oluşturulamıyor?

AWS, Free Tier hesaplarında Terraform/API üzerinden Free Tier dışı instance'ları engelliyor. Ancak:

1. **AWS Console'dan manuel olarak** instance type değiştirilebilir
2. **Kredi ile** kullanım yapıldığı için sorun yok
3. Sadece Terraform/API kısıtlaması var, Console'da yok

### Çözüm: AWS Console'dan Manuel Değiştirme

**Rehber:** `docs/guides/ec2-manual-instance-type-upgrade.md`

**Kısa Adımlar:**
1. AWS Console → EC2 → Instances
2. Instance'ı seç → Stop
3. Actions → Instance Settings → Change Instance Type
4. `t3.medium` seç
5. Start

**Süre:** ~5 dakika

---

## 💰 EKS Maliyeti

### EKS Cluster
- **Maliyet:** ~$0.10/saat
- **3 gün (72 saat):** ~$7.20
- **5 gün (120 saat):** ~$12.00

### EKS Node Group (2x t3.micro)
- **Maliyet:** ~$0.01/saat/node = ~$0.02/saat (2 node)
- **3 gün (72 saat):** ~$1.44
- **5 gün (120 saat):** ~$2.40

### EKS Toplam
- **3 gün:** ~$8.64
- **5 gün:** ~$14.40

**Not:** EKS Free Tier'da yok, ama kredi ile kullanılabilir! ✅

---

## 📊 Toplam Maliyet (3-5 Gün)

### Senaryo 1: t3.small (2GB RAM) ⚠️
| Servis | 3 Gün | 5 Gün |
|--------|-------|-------|
| EC2 (t3.small) | $1.44 | $2.40 |
| EKS Cluster | $7.20 | $12.00 |
| EKS Nodes (2x) | $1.44 | $2.40 |
| NAT Gateway | ~$3.00 | ~$5.00 |
| Diğer | ~$2.00 | ~$3.00 |
| **TOPLAM** | **~$15** | **~$25** |

### Senaryo 2: t3.medium (4GB RAM) ✅ ÖNERİLEN
| Servis | 3 Gün | 5 Gün |
|--------|-------|-------|
| EC2 (t3.medium) | $2.88 | $4.80 |
| EKS Cluster | $7.20 | $12.00 |
| EKS Nodes (2x) | $1.44 | $2.40 |
| NAT Gateway | ~$3.00 | ~$5.00 |
| Diğer | ~$2.00 | ~$3.00 |
| **TOPLAM** | **~$17** | **~$27** |

### Senaryo 3: t3.large (8GB RAM) ✅ GÜVENLİ
| Servis | 3 Gün | 5 Gün |
|--------|-------|-------|
| EC2 (t3.large) | $5.76 | $9.60 |
| EKS Cluster | $7.20 | $12.00 |
| EKS Nodes (2x) | $1.44 | $2.40 |
| NAT Gateway | ~$3.00 | ~$5.00 |
| Diğer | ~$2.00 | ~$3.00 |
| **TOPLAM** | **~$19** | **~$32** |

**Sonuç:** 100 dolar kredi ile rahatlıkla 3-5 gün kullanılabilir! ✅

---

## 🎯 EKS İçin Ne Yapmalıyız?

### EKS Zaten Çalışıyor! ✅

EKS cluster'ı zaten oluşturuldu ve çalışıyor:
- **Cluster:** `triobank-cluster`
- **Version:** 1.29
- **Node Group:** 2x t3.micro (Free Tier eligible)

### EKS İçin Yapılacaklar

1. ✅ **EKS Cluster:** Zaten oluşturuldu
2. ✅ **Node Group:** Zaten oluşturuldu (2x t3.micro)
3. ⏳ **kubectl yapılandırması:** Yapılacak
4. ⏳ **Load Balancer Controller:** Kurulacak
5. ⏳ **External Secrets Operator:** Kurulacak
6. ⏳ **ArgoCD:** Kurulacak (opsiyonel)
7. ⏳ **Servisler:** Deploy edilecek

**EKS için ekstra bir şey yapmaya gerek yok!** Zaten çalışıyor. ✅

---

## 📝 Özet

### Free Tier vs Kredi
- ❌ **Free Tier:** t3.medium kullanılamaz (Terraform/API'de)
- ✅ **100 Dolar Kredi:** t3.medium kullanılabilir (AWS Console'dan)
- ✅ **EKS:** Free Tier'da yok, ama kredi ile kullanılabilir

### EC2 İçin
1. ✅ Şu an: t3.small (2GB RAM) - Terraform ile oluşturuldu
2. 🚀 Önerilen: t3.medium (4GB RAM) - AWS Console'dan yükselt
3. 📝 Rehber: `docs/guides/ec2-manual-instance-type-upgrade.md`

### EKS İçin
- ✅ Zaten çalışıyor, ekstra bir şey yapmaya gerek yok
- ⏳ Sonraki adımlar: kubectl config, Load Balancer Controller, servisler

### Toplam Maliyet
- **3 gün:** ~$15-19 (t3.small-t3.large arası)
- **5 gün:** ~$25-32 (t3.small-t3.large arası)
- **100 dolar kredi:** Yeterli! ✅

---

## 🚀 Sonraki Adımlar

1. ✅ EC2 instance type'ı AWS Console'dan `t3.medium` yap
2. ✅ EC2'de Docker Compose servislerini kur
3. ⏳ kubectl yapılandırması
4. ⏳ Kubernetes servislerini deploy et

**Hazırsınız!** 🎉


