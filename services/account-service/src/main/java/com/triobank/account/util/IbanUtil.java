package com.triobank.account.util;

import java.math.BigInteger;
import java.util.Random;

/**
 * IBAN Üretim ve Doğrulama Aracı
 * 
 * Türkiye Standartlarına (TR) uygun, MOD-97 algoritmasıyla çalışan
 * geçerli IBAN'lar üretir.
 * 
 * Format: TRkk bbbbb r ccccccccccccccccccc
 * TR: Ülke Kodu (2)
 * kk: Kontrol Basamakları (2) (Check Digits)
 * bbbbb: Banka Kodu (5) (TrioBank: 21010)
 * r: Rezerv Alan (1) (Genelde 0)
 * c...c: Hesap Numarası (16)
 */
public class IbanUtil {

    private static final String COUNTRY_CODE = "TR";
    private static final String BANK_CODE = "21010"; // TrioBank Kodu (21 Diyarbakır + 01 Adana
    private static final String RESERVE_DIGIT = "0";

    /**
     * Rastgele 16 haneli bir iç hesap numarası üretir ve bunu
     * geçerli bir IBAN'a dönüştürür.
     */
    public static String generateIban() {
        // 1. Rastgele 16 haneli hesap numarası üret (Internal Account Number)
        String internalAccountNumber = generateRandomAccountNumber(16);

        // 2. IBAN'ı hesapla
        return calculateIban(internalAccountNumber);
    }

    /**
     * Verilen bir IBAN'ın geçerli olup olmadığını (MOD-97) kontrol eder.
     * Sadece TR değil, tüm standart ISO-13616 IBAN'ları kontrol edebilir.
     */
    public static boolean isValid(String iban) {
        if (iban == null || iban.length() < 15 || iban.length() > 34) {
            return false;
        }

        // Boşlukları temizle ve büyüt
        String cleanedIban = iban.replace(" ", "").toUpperCase();

        // Ülke Kodu (İlk 4 karakteri sona al)
        // Örn: TR5610999... -> 10999...TR56
        String reordered = cleanedIban.substring(4) + cleanedIban.substring(0, 4);

        // Harfleri Rakam Yap
        StringBuilder numericIban = new StringBuilder();
        for (char c : reordered.toCharArray()) {
            if (Character.isDigit(c)) {
                numericIban.append(c);
            } else if (Character.isLetter(c)) {
                // A=10, B=11 ... Z=35
                numericIban.append(c - 'A' + 10);
            } else {
                return false; // Geçersiz karakter
            }
        }

        // Mod-97 Hesapla
        BigInteger bigInt = new BigInteger(numericIban.toString());
        return bigInt.mod(new BigInteger("97")).intValue() == 1;
    }

    private static String calculateIban(String accountNumber) {
        // Adım 1: BBAN Oluştur (Banka Kodu + Rezerv + Hesap No)
        String bban = BANK_CODE + RESERVE_DIGIT + accountNumber;

        // Adım 2: Ülke Kodu ve '00' ekle (TR00) -> Sayısal Karşılığı
        // T=29, R=27 -> 292700
        String countryCodeNumeric = "292700"; // TR00'ın sayısal karşılığı sabittir.

        // Adım 3: Birleştir (BBAN + TR00)
        String tempIban = bban + countryCodeNumeric;

        // Adım 4: MOD-97 Hesapla
        BigInteger bigInt = new BigInteger(tempIban);
        int mod97 = bigInt.mod(new BigInteger("97")).intValue();

        // Adım 5: Check Digits Bul (98 - Mod)
        int checkDigitsVal = 98 - mod97;
        String checkDigits = (checkDigitsVal < 10 ? "0" : "") + checkDigitsVal;

        // Sonuç: TR + CheckDigits + BBAN
        return COUNTRY_CODE + checkDigits + bban;
    }

    private static String generateRandomAccountNumber(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
