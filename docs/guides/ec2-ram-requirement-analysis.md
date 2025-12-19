# EC2 RAM Gereksinim Analizi

**Soru:** 2GB RAM 4 servis için (MSSQL, MongoDB, Redis, Vault) yeterli mi?

**Kısa Cevap:** ❌ **Yeterli değil!** En az 4GB RAM önerilir.

---

## 📊 Servis RAM Gereksinimleri

### 1. MSSQL Server 2022
- **Minimum:** 2GB RAM
- **Önerilen:** 4GB RAM
- **Durum:** ⚠️ 2GB RAM ile minimum seviyede çalışır, performans sınırlı olabilir

### 2. MongoDB 6.0
- **Minimum:** 500MB RAM
- **Önerilen:** 1-2GB RAM
- **Durum:** ✅ 2GB RAM ile çalışabilir

### 3. Redis 7.0
- **Minimum:** 100MB RAM
- **Önerilen:** 200-500MB RAM
- **Durum:** ✅ 2GB RAM ile rahatlıkla çalışır

### 4. Vault 1.15
- **Minimum:** 100MB RAM
- **Önerilen:** 200-500MB RAM
- **Durum:** ✅ 2GB RAM ile rahatlıkla çalışır

---

## 💾 Toplam RAM Gereksinimi

### Senaryo 1: Minimum Kullanım
- MSSQL: 2GB (minimum)
- MongoDB: 500MB
- Redis: 200MB
- Vault: 200MB
- **Toplam:** ~2.9GB RAM

### Senaryo 2: Normal Kullanım
- MSSQL: 3GB
- MongoDB: 1GB
- Redis: 300MB
- Vault: 300MB
- **Toplam:** ~4.6GB RAM

### Senaryo 3: Yoğun Kullanım
- MSSQL: 4GB
- MongoDB: 2GB
- Redis: 500MB
- Vault: 500MB
- **Toplam:** ~7GB RAM

---

## ⚠️ 2GB RAM ile Sorunlar

### 1. MSSQL Minimum Gereksinim
- MSSQL Server 2022 **en az 2GB RAM** gerektiriyor
- 2GB RAM ile sadece MSSQL çalışabilir
- Diğer servisler (MongoDB, Redis, Vault) için RAM kalmaz

### 2. Swap Kullanımı
- RAM yetersiz olduğunda sistem **swap** (disk) kullanır
- Swap çok yavaştır (RAM'den 100-1000x daha yavaş)
- Performans ciddi şekilde düşer

### 3. OOM (Out of Memory) Hataları
- RAM dolduğunda Linux **OOM Killer** devreye girer
- Rastgele process'leri öldürür
- Servisler beklenmedik şekilde kapanabilir

---

## ✅ Önerilen Çözümler

### Seçenek 1: t3.medium (4GB RAM) ✅ ÖNERİLEN
- **Maliyet:** ~$0.04/saat
- **RAM:** 4GB
- **Durum:** ✅ 4 servis için yeterli
- **3 gün maliyet:** ~$2.88
- **5 gün maliyet:** ~$4.80

**Avantajlar:**
- 4 servis rahatlıkla çalışır
- Performans sorunu olmaz
- OOM riski düşük

### Seçenek 2: t3.large (8GB RAM)
- **Maliyet:** ~$0.08/saat
- **RAM:** 8GB
- **Durum:** ✅ Fazlasıyla yeterli
- **3 gün maliyet:** ~$5.76
- **5 gün maliyet:** ~$9.60

**Avantajlar:**
- Yoğun kullanımda bile sorun olmaz
- Gelecekte büyüme için hazır
- En güvenli seçenek

### Seçenek 3: t3.small (2GB RAM) ⚠️ RİSKLİ
- **Maliyet:** ~$0.02/saat
- **RAM:** 2GB
- **Durum:** ⚠️ Sıkışabilir, performans sorunları olabilir
- **3 gün maliyet:** ~$1.44
- **5 gün maliyet:** ~$2.40

**Sorunlar:**
- MSSQL minimum gereksinim (2GB) ile çalışır
- Diğer servisler için RAM kalmaz
- Swap kullanımı → yavaş performans
- OOM riski yüksek

---

## 🎯 Sonuç ve Öneri

### ❌ 2GB RAM Yeterli Değil
- MSSQL tek başına 2GB istiyor
- Diğer 3 servis için RAM kalmaz
- Performans sorunları ve OOM riski yüksek

### ✅ 4GB RAM Önerilir
- 4 servis rahatlıkla çalışır
- Performans sorunu olmaz
- Maliyet makul (~$2.88/3 gün)

### 🚀 8GB RAM Güvenli
- Yoğun kullanımda bile sorun olmaz
- Gelecekte büyüme için hazır
- Biraz daha pahalı ama güvenli

---

## 📝 AWS Console'dan Instance Type Yükseltme

**Rehber:** `docs/guides/ec2-manual-instance-type-upgrade.md`

**Kısa Adımlar:**
1. AWS Console → EC2 → Instances
2. Instance'ı seç → Stop
3. Actions → Instance Settings → Change Instance Type
4. `t3.medium` (4GB) veya `t3.large` (8GB) seç
5. Start

**Süre:** ~5 dakika

---

## 💰 Maliyet Karşılaştırması (3-5 Gün)

| Instance Type | RAM | 3 Gün | 5 Gün | Durum |
|--------------|-----|-------|-------|-------|
| t3.small | 2GB | $1.44 | $2.40 | ⚠️ Riskli |
| t3.medium | 4GB | $2.88 | $4.80 | ✅ Önerilen |
| t3.large | 8GB | $5.76 | $9.60 | ✅ Güvenli |

**100 dolar kredi ile:** Hepsi rahatlıkla kullanılabilir! ✅

---

## 🎯 Final Öneri

**t3.medium (4GB RAM)** seçin:
- ✅ 4 servis için yeterli
- ✅ Performans sorunu olmaz
- ✅ Maliyet makul
- ✅ 100 dolar kredi ile rahatlıkla kullanılabilir

**t3.small (2GB RAM)** sadece test için uygun, production için riskli!

