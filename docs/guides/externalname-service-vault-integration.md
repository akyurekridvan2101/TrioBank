# ExternalName Service - Vault Integration Sorunu ve Çözümü

## 🔍 Sorun

ExternalName Service'lerde `externalName` field'ı boş bırakılamaz çünkü:
1. Helm template'inde `required "externalName is required"` kontrolü var
2. Vault secret'ları runtime'da External Secrets Operator tarafından oluşturuluyor
3. Helm render zamanında secret henüz yok

## ❌ Mevcut Durum (Yanlış)

```yaml
databases:
  mongodb:
    externalName: ""  # ❌ Helm render hatası verir!
```

**Hata:**
```
Error: execution error at (db-service.yaml:146:7): externalName is required
```

## ✅ Çözüm Seçenekleri

### Seçenek 1: InitContainer ile Patch (Önerilen)

InitContainer secret'ı okuyup ExternalName Service'i patch eder:

```yaml
initContainers:
  - name: patch-externalname
    image: bitnami/kubectl:latest
    command:
      - sh
      - -c
      - |
        # Secret'tan değeri oku
        MONGO_ADDRESS=$(cat /secrets/mongo_address)
        
        # ExternalName Service'i patch et
        kubectl patch service auth-mongodb \
          -n triobank \
          --type='json' \
          -p='[{"op": "replace", "path": "/spec/externalName", "value": "'$MONGO_ADDRESS'"}]'
    volumeMounts:
      - name: secrets
        mountPath: /secrets
        readOnly: true
volumes:
  - name: secrets
    secret:
      secretName: auth-db-credentials
```

**Avantajlar:**
- ✅ Secret runtime'da oluştuktan sonra patch edilir
- ✅ Helm template'inde hata vermez
- ✅ Dinamik güncelleme yapılabilir

**Dezavantajlar:**
- ⚠️ InitContainer ekstra complexity
- ⚠️ kubectl yetkisi gerekir

---

### Seçenek 2: Helm Template'de Secret Reference (Basit)

Secret'ı env var olarak inject edip, InitContainer'da okuyup patch etmek:

```yaml
# values.yaml
databases:
  mongodb:
    externalName: "{{ .Values.mongoAddress }}"  # Helm variable
```

**Dezavantaj:**
- ❌ Helm render zamanında secret henüz yok
- ❌ Template'de secret okuyamayız

---

### Seçenek 3: Geçici Değer + InitContainer (Pratik)

Template'de geçici bir değer koyup, InitContainer ile güncellemek:

```yaml
# Template'de geçici değer
externalName: "placeholder"  # Helm render için gerekli

# InitContainer ile patch
initContainers:
  - name: update-externalname
    image: bitnami/kubectl:latest
    command:
      - sh
      - -c
      - |
        ADDRESS=$(cat /secrets/mongo_address)
        kubectl patch service auth-mongodb -n triobank \
          --type='json' \
          -p='[{"op": "replace", "path": "/spec/externalName", "value": "'$ADDRESS'"}]'
```

---

## 🎯 Önerilen Çözüm

**Seçenek 1 (InitContainer ile Patch)** kullanılmalı çünkü:
1. ✅ Helm template hatası vermez
2. ✅ Secret runtime'da oluştuktan sonra patch edilir
3. ✅ Dinamik güncelleme yapılabilir

---

## 📝 Implementation

### 1. Template'de Geçici Değer

```yaml
# values.yaml
databases:
  mongodb:
    externalName: "placeholder"  # Helm render için gerekli
```

### 2. InitContainer Ekle

```yaml
# deployment.yaml
initContainers:
  - name: patch-mongodb-externalname
    image: bitnami/kubectl:latest
    command:
      - sh
      - -c
      - |
        MONGO_ADDRESS=$(cat /secrets/mongo_address)
        kubectl patch service auth-mongodb \
          -n {{ .Release.Namespace }} \
          --type='json' \
          -p='[{"op": "replace", "path": "/spec/externalName", "value": "'$MONGO_ADDRESS'"}]'
    volumeMounts:
      - name: db-secrets
        mountPath: /secrets
        readOnly: true
volumes:
  - name: db-secrets
    secret:
      secretName: auth-db-credentials
```

---

## ⚠️ Alternatif: Template'de Direkt Kullanım (Mümkün Değil)

```yaml
# ❌ Bu çalışmaz - Secret henüz yok
externalName: {{ .Values.externalSecrets[0].targetSecret.mongo_address }}
```

**Neden çalışmaz:**
- Helm render zamanında secret henüz oluşmamış
- External Secrets Operator runtime'da oluşturuyor

---

## ✅ Sonuç

**Mevcut yaklaşım (boş bırakmak) çalışmaz** çünkü:
- Helm template `required` kontrolü yapıyor
- Boş değer hata verir

**Doğru yaklaşım:**
1. Template'de geçici değer (`placeholder`)
2. InitContainer ile secret'ı okuyup patch et


