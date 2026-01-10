package com.triobank.card.domain.model;

/**
 * Card Type Enum
 * 
 * Desteklenen kart türleri
 */
public enum CardType {
    DEBIT("Banka Kartı"),
    CREDIT("Kredi Kartı"),
    VIRTUAL("Sanal Kart");

    private final String displayName;

    CardType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
