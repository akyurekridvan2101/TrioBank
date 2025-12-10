package com.triobank.ledger.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * PaginationMetadata - Sayfalama bilgisi
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationMetadata {

    /** Mevcut sayfa (0-indexed) */
    private Integer page;

    /** Sayfa boyutu */
    private Integer size;

    /** Toplam eleman sayısı */
    private Long totalElements;

    /** Toplam sayfa sayısı */
    private Integer totalPages;
}
