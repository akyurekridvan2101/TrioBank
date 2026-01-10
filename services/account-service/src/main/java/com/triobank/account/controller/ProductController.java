package com.triobank.account.controller;

import com.triobank.account.domain.model.ProductDefinition;
import com.triobank.account.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Ürün Kataloğu API
 * 
 * Bankamızın müşterilere sunduğu ürünlerin (Vadesiz TL, Altın Hesabı, Vadeli
 * Mevduat vb.)
 * tanımlarını ve özelliklerini sunar.
 * 
 * Mobil uygulama veya Web şubesi, "Hesap Aç" ekranında kullanıcıya seçenekleri
 * göstermek için bu servisi kullanır.
 */
@RestController
@RequestMapping("/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * [GET] Tüm Ürünleri Listele
     * 
     * Aktif olan tüm bankacılık ürünlerini döner.
     */
    @GetMapping
    public ResponseEntity<List<ProductDefinition>> getAllProducts() {
        // Ürün tanımları doğrudan entity olarak dönülebilir (Value Object gibidirler)
        return ResponseEntity.ok(productService.getAllProducts());
    }

    /**
     * [GET] Ürün Detayı
     * 
     * Belirli bir ürün kodunun (örn: "RETAIL_TRY") detaylarını getirir.
     */
    @GetMapping("/{code}")
    public ResponseEntity<ProductDefinition> getProduct(@PathVariable String code) {
        return productService.getProductByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
