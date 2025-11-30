package com.triobank.ledger.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * BalanceUpdateDto - Bakiye değişim bilgisi (Internal DTO)
 * 
 * OutboxService tarafından kullanılır
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceUpdateDto {

    /** Hesap ID */
    private String accountId;

    /** Önceki bakiye */
    private BigDecimal previousBalance;

    /** Yeni bakiye */
    private BigDecimal newBalance;

    /** Değişim miktarı */
    private BigDecimal delta;

    /** Para birimi */
    private String currency;
}
