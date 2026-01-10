package com.triobank.account.repository;

import com.triobank.account.domain.model.ProductDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Ürün Kataloğu Veri Erişim Katmanı (Repository)
 * 
 * Bankanın sunduğu ürün tanımlarına (Vadesiz Hesap, Altın Hesabı vb.) erişim
 * sağlar.
 * 
 * Performans Notu:
 * Ürün tanımları çok sık değişmez (static/reference data). Bu nedenle bu
 * repository
 * üzerinden gelen veriler genellikle Service katmanında Cachelenebilir.
 * 
 * N+1 Kontrolü:
 * Bu entity'nin başka bir entity'ye (OneToMany) ilişkisi olmadığı için
 * N+1 problemi yaşanmaz. Doğrudan çekilebilir.
 */
@Repository
public interface ProductDefinitionRepository extends JpaRepository<ProductDefinition, String> {

    /**
     * Ürün kodu ile ürün tanımını bulur.
     * 
     * Hesap açılışlarında "Böyle bir ürün var mı?" kontrolü için kullanılır.
     * 
     * @param code Ürün kodu (örn: "RETAIL_TRY")
     * @return Varsa ürün tanımı, yoksa boş
     */
    Optional<ProductDefinition> findByCode(String code);

    /**
     * Kodun kullanımda olup olmadığını kontrol eder.
     */
    boolean existsByCode(String code);
}
