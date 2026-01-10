package com.triobank.card.domain.model;

/**
 * Card Status Enum
 * 
 * Kart durumları
 */
public enum CardStatus {
    ACTIVE, // Kullanılabilir
    BLOCKED, // Bloke (geçici/kalıcı)
    EXPIRED, // Süresi dolmuş
    CANCELLED // İptal edilmiş
}
