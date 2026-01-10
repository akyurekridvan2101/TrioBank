package com.triobank.ledger.domain.model;

/**
 * EntryType - Muhasebe kayıt tipi
 * 
 * Çift taraflı muhasebe sisteminde her kayıt
 * ya DEBIT ya da CREDIT olur, başka seçenek yoktur.
 * 
 * Temel kural: Her işlemde(Transaction da) toplam DEBIT = toplam CREDIT olmalıdır.
 */
public enum EntryType {

    /**
     * DEBIT - Borç (hesaptan para çıkışı)
     * 
     * Kullanım alanları:
     * - Para transferinde gönderen hesap
     * - ATM'den para çekme
     * - Ödeme yapma
     * - Komisyon kesme
     * 
     * Örnek: Ali'nin hesabından 100 TL çıktı → DEBIT
     */
    DEBIT,

    /**
     * CREDIT - Alacak (hesaba para girişi)
     * 
     * Kullanım alanları:
     * - Para transferinde alan hesap
     * - Maaş yatırma
     * - Para yatırma
     * - Faiz ekleme
     * 
     * Örnek: Ayşe'nin hesabına 100 TL girdi → CREDIT
     */
    CREDIT
}
