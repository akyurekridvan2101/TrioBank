package com.triobank.ledger.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * ErrorResponse - Standart hata yanıtı
 * 
 * API'den dönen tüm hatalar bu formatta
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // null alanları gösterme
public class ErrorResponse {

    /**
     * HTTP status code (400, 404, 409, 500, vb.)
     */
    private final int status;

    /**
     * Hata türü (Bad Request, Not Found, Conflict, vb.)
     */
    private final String error;

    /**
     * Uygulama özel hata kodu
     * Örnek: LE-400, LE-404, LE-409
     */
    private final String code;

    /**
     * Kullanıcıya gösterilecek mesaj
     */
    private final String message;

    /**
     * Detaylı hata bilgisi (opsiyonel)
     * Örnek: validation errors, field errors
     */
    private final Map<String, Object> details;

    /**
     * API path (hangi endpoint'te hata oluştu)
     */
    private final String path;

    /**
     * Hata zamanı
     */
    @Builder.Default
    private final Instant timestamp = Instant.now();
}
