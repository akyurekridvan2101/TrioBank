# EC2 Mimari Açıklaması

## 🤔 Soru: 3 Farklı EC2 mi, 1 Tane mi?

## ✅ CEVAP: 1 Tane EC2 Yeterli!

### Neden?

EC2 bir bilgisayar gibidir. Bir bilgisayarda birden fazla program çalıştırabilirsiniz:

```
┌─────────────────────────────────┐
│      EC2 Instance (1 tane)      │
│      (t2.micro - Ücretsiz)     │
│                                 │
│  ┌──────────────────────────┐  │
│  │   Docker Compose         │  │
│  │                          │  │
│  │  ┌──────┐  ┌──────┐      │  │
│  │  │MSSQL │  │Mongo │      │  │
│  │  │:1433 │  │:27017│      │  │
│  │  └──────┘  └──────┘      │  │
│  │                          │  │
│  │  ┌──────┐  ┌──────┐      │  │
│  │  │Redis │  │Vault │      │  │
│  │  │:6379 │  │:8200 │      │  │
│  │  └──────┘  └──────┘      │  │
│  └──────────────────────────┘  │
└─────────────────────────────────┘
```

### Lokal Bilgisayarınızla Karşılaştırma

Lokal bilgisayarınızda:
- Docker Compose çalıştırıyorsunuz
- 4 container aynı anda çalışıyor (MSSQL, MongoDB, Redis, Vault)
- Hepsi aynı bilgisayarda

EC2'de de aynı mantık:
- 1 EC2 instance
- Docker Compose ile 4 container
- Hepsi aynı EC2'de

## 💰 Maliyet Karşılaştırması

### Senaryo 1: 1 EC2 (Önerilen ✅)

```
1 EC2 (t2.micro) = ÜCRETSİZ (12 ay)
├── MSSQL
├── MongoDB
├── Redis
└── Vault

Toplam: $0/ay (ilk 12 ay)
```

### Senaryo 2: 3 EC2 (Gereksiz ❌)

```
EC2 #1 (t2.micro) = ÜCRETSİZ
└── MSSQL

EC2 #2 (t2.micro) = ÜCRETSİZ
└── MongoDB

EC2 #3 (t2.micro) = ÜCRETSİZ
└── Redis

Toplam: $0/ay (ilk 12 ay)
AMA: Gereksiz karmaşıklık!
```

## 🎯 Neden 1 EC2 Yeterli?

1. **Basitlik**: Tek bir yerde yönetim
2. **Maliyet**: Aynı (ücretsiz tier)
3. **Performans**: Test/demo için yeterli
4. **Docker Compose**: Zaten böyle tasarlanmış

## ⚠️ Ne Zaman 3 EC2 Gerekir?

Sadece şu durumlarda:
- **Production**: Yüksek trafik, ayrı scaling
- **Güvenlik**: Servislerin birbirinden izole olması gerekiyorsa
- **Yüksek Kullanılabilirlik**: Her servis için ayrı instance

**Ama şu an için**: 1 EC2 yeterli! 🎯

## 📊 Özet

| Özellik | 1 EC2 | 3 EC2 |
|---------|-------|-------|
| **Maliyet** | Ücretsiz | Ücretsiz |
| **Karmaşıklık** | Basit ✅ | Karmaşık ❌ |
| **Yönetim** | Kolay ✅ | Zor ❌ |
| **Test/Demo** | Yeterli ✅ | Gereksiz ❌ |
| **Production** | Yetersiz ❌ | Uygun ✅ |

**Sonuç**: Şu an için **1 EC2** kullanın! 🚀

