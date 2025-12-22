package com.triobank.account.service;

import com.triobank.account.domain.model.ProductDefinition;
import com.triobank.account.repository.ProductDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Ürün Kataloğu Servisi
 * 
 * Bankanın sunduğu ürün tanımlarına erişim sağlar.
 * 
 * Performans Notu:
 * Bu veriler "Reference Data" statüsündedir (Çok sık değişmez, çok sık okunur).
 * İleride buraya @Cacheable eklenerek veritabanı yükü sıfıra indirilebilir.
 * Şimdilik doğrudan DB'den okuyoruz ama servis katmanı sayesinde o kapıyı açık
 * bıraktık.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true) // Okuma işlemleri için optimize edilmiştir
public class ProductService {

    private final ProductDefinitionRepository productDefinitionRepository;

    /**
     * Tüm aktif ürünleri listeler.
     */
    public List<ProductDefinition> getAllProducts() {
        return productDefinitionRepository.findAll();
    }

    /**
     * Koduna göre ürün detayını getirir.
     */
    public Optional<ProductDefinition> getProductByCode(String code) {
        return productDefinitionRepository.findByCode(code);
    }
}
