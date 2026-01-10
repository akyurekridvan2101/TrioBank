# 🛠️ TrioBank Git Çalışma Rehberi

Bu doküman, TrioBank mikroservis monorepo yapısında **hızlı, güvenli ve çatışmasız** geliştirme yapabilmek için hazırlanmıştır.

> **Temel felsefe:**
>
> * `main` **production** → sıkı koruma
> * `develop` **entegrasyon** → kontrollü ama hızlı
> * `feature/*` **özgür alan** → bireysel geliştirme

Bu rehber yalnızca "ne yapacağız"ı değil, **neden böyle yaptığımızı** da açıklar.

---

## 1. Dallanma Yapısı (Branching Model)

![Git Workflow Schema](assets/git-workflow.svg)

Projede **4 tip branch** kullanılır:

| Branch          | Amaç                       | Kurallar                                                                    |
| --------------- | -------------------------- | --------------------------------------------------------------------------- |
| **`main`**      | 🚀 **Production**          | 🔒 Direkt push **yasak**. Sadece PR + review + (varsa) CI ile merge edilir. |
| **`develop`**   | 🧪 **Integration / Test**  | Direkt push **yasak**. PR zorunlu, review opsiyonel.                        |
| **`feature/*`** | 🧑‍💻 **Geliştirme Alanı** | Kuralsız. Force push serbest. İş bitince silinir.                           |
| **`hotfix/*`**  | 🚑 **Acil Müdahale**       | `main`’den açılır, hem `main` hem `develop`’a merge edilir.                 |

---

## 2. Branch Protection Kuralları (Özet)

### `main` Branch

* ❌ Direkt push
* ❌ Force push
* ❌ PR’sız merge
* ❌ Onaysız merge
* ❌ Yorumlar çözülmeden merge
* ❌ (Varsa) testler fail iken merge

➡️ **PR zorunlu + en az 1 onay + squash merge**

### `develop` Branch

* ❌ Direkt push
* ❌ Force push
* ❌ PR’sız merge
* ✅ Review opsiyonel
* ✅ Merge / Squash / Rebase serbest
* ⚠️ CI varsa testler geçmeli

➡️ **Hızlı entegrasyon, ama kontrol altında**

---

## 3. İsimlendirme Standartları (Esnek ama Anlamlı)

Katı kurallar yok; ama **branch adına bakan biri ne yaptığını anlamalı**.

### Önerilen Formatlar

```
feature/auth-service
feature/payment-retry
feature/ridvan/order-api
fix/docker-compose-port
```

### Commit Mesajları

Conventional commit **zorunlu değil**, ama okunabilirlik önemli:

```
feat: order create endpoint eklendi
fix: kafka advertised listener düzeltildi
chore: docker-compose cleanup
```

---

## 4. Günlük Geliştirme Akışı (Standart Senaryo)

### A. Güncel `develop` ile başla

```bash
git checkout develop
git pull origin develop
```

### B. Feature branch aç

```bash
git checkout -b feature/order-service
```

### C. Geliştir & commit at

```bash
git add .
git commit -m "feat: sipariş oluşturma tamamlandı"
```

### D. 🔄 Sync (ÇAKIŞMA ÖNLEME)

PR açmadan **mutlaka**:

```bash
git pull origin develop
```

> ⚠️ Çakışma ihtimali olan dosyalar:
>
> * `docker-compose.yml`
> * `helm/values.yaml`
> * `README.md`

### E. Push & PR

```bash
git push origin feature/order-service
```

GitHub üzerinden **develop → PR** açılır.

---

## 5. Pull Request Kuralları

### `develop` PR

* Review **opsiyonel**
* Hızlı merge edilebilir

### `main` PR

* En az **1 onay zorunlu**
* Yorumlar resolve edilmeden merge olmaz
* Sadece **Squash merge**

---

## 6. Hotfix Akışı (Acil Durum)

Production çökerse `develop` beklenmez.

```bash
git checkout main
git pull origin main
git checkout -b hotfix/login-fix
```

1. Fix yap → commit → push
2. GitHub’da **iki PR aç**:

   * `hotfix/* → main`
   * `hotfix/* → develop`

> Böylece prod ve develop senkron kalır.

---

## 7. Monorepo’da Çakışma Gerçekleri

Java servis klasörleri genelde çakışmaz:

```
auth-service/
payment-service/
order-service/
```

Ama **ortak dosyalar risklidir**:

* 🐳 Docker
* ☸️ Helm
* 📄 Dokümantasyon

➡️ **Çözüm:** PR öncesi her zaman `git pull origin develop`

---

## 8. Pull Request Template (Standart)

```markdown
## 📌 Summary

## 🛠️ What Changed?
- Structural:
- Business Logic:
- Config / Infra:
- Cleanup:

## ✅ Checklist
- [ ] `develop` ile senkronize edildi
- [ ] Docker / Helm değişiklikleri kontrol edildi
- [ ] Breaking change yok
```

---

## 9. Altın Kurallar (TL;DR)

* ❌ `main`’e asla direkt push yok
* ❌ `develop`’a bile force push yok
* ✅ Feature branch’te özgürsün
* 🔄 PR öncesi **her zaman sync**
* 🔥 Hotfix ayrı akış
