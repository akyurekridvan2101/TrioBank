package com.triobank.card.domain.model;

/**
 * Card Brand Enum
 * 
 * Desteklenen kart markaları ve BIN prefix'leri
 */
public enum CardBrand {
    VISA("4"),
    MASTERCARD("5"),
    AMEX("34", "37"),
    TROY("9");

    private final String[] binPrefixes;

    CardBrand(String... binPrefixes) {
        this.binPrefixes = binPrefixes;
    }

    public String[] getBinPrefixes() {
        return binPrefixes;
    }

    /**
     * Kart numarasından brand'i tespit eder
     */
    public static CardBrand fromCardNumber(String cardNumber) {
        String cleaned = cardNumber.replaceAll("[^0-9]", "");

        for (CardBrand brand : values()) {
            for (String prefix : brand.binPrefixes) {
                if (cleaned.startsWith(prefix)) {
                    return brand;
                }
            }
        }

        throw new IllegalArgumentException("Unknown card brand for number: " + cardNumber);
    }
}
