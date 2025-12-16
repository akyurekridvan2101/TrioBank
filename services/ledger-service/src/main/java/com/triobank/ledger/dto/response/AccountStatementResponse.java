package com.triobank.ledger.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * AccountStatementResponse - Hesap ekstresi response
 * 
 * Endpoint: GET /api/v1/ledger/accounts/{accountId}/statement
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountStatementResponse {

    /** Hesap ID */
    private String accountId;

    /** Para birimi */
    private String currency;

    /** Dönem bilgisi */
    private PeriodInfo period;

    /** Dönem başı bakiye */
    private BigDecimal openingBalance;

    /** Dönem sonu bakiye */
    private BigDecimal closingBalance;

    /** Toplam borç */
    private BigDecimal totalDebits;

    /** Toplam alacak */
    private BigDecimal totalCredits;

    /** Net değişim */
    private BigDecimal netChange;

    /** Entry sayısı */
    private Integer entryCount;

    /** Entry listesi */
    private List<StatementEntryResponse> entries;

    /** Sayfalama bilgisi */
    private PaginationMetadata pagination;

    /**
     * PeriodInfo - Dönem bilgisi
     */
    @Getter
    @Builder
    public static class PeriodInfo {
        private LocalDate startDate;
        private LocalDate endDate;
    }
}
