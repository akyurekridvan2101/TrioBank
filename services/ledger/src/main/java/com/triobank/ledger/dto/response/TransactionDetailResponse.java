package com.triobank.ledger.dto.response;

import com.triobank.ledger.domain.model.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * TransactionDetailResponse - Transaction detay response
 * 
 * Endpoint: GET /api/v1/ledger/transactions/{id}
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDetailResponse {

    /** Transaction ID */
    private String transactionId;

    /** Transaction tipi */
    private String transactionType;

    /** Toplam tutar */
    private BigDecimal totalAmount;

    /** Para birimi */
    private String currency;

    /** Status */
    private TransactionStatus status;

    /** Muhasebe tarihi */
    private LocalDate postingDate;

    /** Değer tarihi */
    private LocalDate valueDate;

    /** Açıklama */
    private String description;

    /** İşlemi başlatan hesap */
    private String initiatorAccountId;

    /** Referans numarası */
    private String referenceNumber;

    /** Reversal mi? */
    private Boolean reversal;

    /** Orijinal transaction ID (eğer reversal ise) */
    private String originalTransactionId;

    /** Reversal transaction ID (eğer reverse edilmişse) */
    private String reversedByTransactionId;

    /** Entry listesi */
    private List<TransactionEntryDto> entries;

    /** Oluşturma zamanı */
    private Instant createdAt;

    /**
     * TransactionEntryDto - Entry bilgisi
     */
    @Getter
    @Builder
    public static class TransactionEntryDto {
        private Integer sequence;
        private String accountId;
        private String entryType;
        private BigDecimal amount;
        private String currency;
        private String description;
        private String referenceNumber;
    }
}
