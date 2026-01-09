# EC2 Instance Type Seçim Rehberi

## 🎯 Hangi Instance Type'ı Seçmeliyim?

### ✅ ÖNERİLEN: t3.medium (4GB RAM)

**Neden?**
- 4 servis için yeterli (MSSQL, MongoDB, Redis, Vault)
- Performans sorunu olmaz
- Maliyet makul (~$0.04/saat)

---

## 📊 Instance Type Karşılaştırması

### t3.micro
- **RAM:** 1GB
- **vCPU:** 2
- **Durum:** ❌ Yetersiz (MSSQL çalışmaz)

### t3.small (Şu anki)
- **RAM:** 2GB
- **vCPU:** 2
- **Durum:** ⚠️ Minimum (MSSQL çalışır ama sıkışabilir)

### t3.medium ✅ SEÇİN
- **RAM:** 4GB
- **vCPU:** 2
- **Durum:** ✅ Önerilen (4 servis için yeterli)

### t3.large
- **RAM:** 8GB
- **vCPU:** 2
- **Durum:** ✅ Güvenli ama daha pahalı

---

## 🔍 Dropdown'da Nasıl Bulunur?

1. **"New instance type"** alanına `t3.medium` yazın
2. Dropdown'da **t3.medium** seçeneğini görün
3. **t3.medium**'ı seçin
4. **Apply** butonuna tıklayın

**Not:** Dropdown'da arama yapabilirsiniz, `t3.medium` yazınca görünecektir.

---

## 💰 Maliyet

- **t3.medium:** ~$0.04/saat
- **3 gün:** ~$2.88
- **5 gün:** ~$4.80

**100 dolar kredi ile rahatlıkla kullanılabilir!** ✅

---

## ✅ Sonuç

**t3.medium (4GB RAM) seçin!** 🚀


