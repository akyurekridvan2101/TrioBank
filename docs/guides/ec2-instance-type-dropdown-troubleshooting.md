# EC2 Instance Type Dropdown Sorun Giderme

## 🔍 Problem: t3.medium Dropdown'da Görünmüyor

### Çözüm 1: Arama Yapın ✅

1. **"New instance type"** alanına `t3.medium` yazın
2. Dropdown otomatik olarak arama yapacak
3. `t3.medium` görünecektir

**Not:** Dropdown'da arama özelliği var, yazdıkça filtreler.

### Çözüm 2: Scroll Yapın

1. Dropdown'da **aşağı kaydırın**
2. `t3.medium` listede daha aşağıda olabilir
3. Mouse wheel veya scroll bar kullanın

### Çözüm 3: Tüm Instance Type'ları Görün

1. Dropdown'u açın
2. En üste gidin
3. Tüm seçenekleri görmek için scroll yapın
4. `t3.medium` mutlaka listede olmalı

---

## ⚠️ Eğer Gerçekten t3.medium Yoksa

### Olası Nedenler:

1. **Free Tier Kısıtlaması:** AWS Console'da da Free Tier dışı instance'lar görünmeyebilir
2. **Region:** eu-north-1'de t3.medium mevcut olmalı
3. **Account Kısıtlaması:** Yeni hesapta bazı instance type'lar görünmeyebilir

### Alternatif Çözümler:

#### Seçenek 1: t3.small ile Devam Et (⚠️ Riskli)
- 2GB RAM ile devam edebilirsiniz
- MSSQL minimum gereksinim (2GB)
- Diğer servisler için RAM sıkışabilir
- Performans sorunları olabilir

#### Seçenek 2: AWS Support'a Başvur
- AWS Support'tan t3.medium'ı aktifleştirmelerini isteyin
- 100 dolar kredisi olan hesaplarda normalde görünmeli

#### Seçenek 3: AWS CLI ile Değiştir
```bash
# Instance'ı durdur
aws ec2 stop-instances --instance-ids i-002bb560ad379fea5

# Instance type'ı değiştir
aws ec2 modify-instance-attribute \
  --instance-id i-002bb560ad379fea5 \
  --instance-type Value=t3.medium

# Instance'ı başlat
aws ec2 start-instances --instance-ids i-002bb560ad379fea5
```

---

## 🎯 Önerilen Adımlar

1. ✅ **Önce arama yapın:** `t3.medium` yazın
2. ✅ **Scroll yapın:** Dropdown'da aşağı kaydırın
3. ⚠️ **Eğer yoksa:** AWS CLI ile deneyin (yukarıdaki komutlar)
4. ⚠️ **Son çare:** t3.small ile devam edin (riskli ama çalışabilir)

---

## 📝 Not

**100 dolar kredisi olan hesaplarda t3.medium normalde görünmeli.** Eğer görünmüyorsa, AWS Console'da bir kısıtlama olabilir. AWS CLI ile deneyebilirsiniz.


