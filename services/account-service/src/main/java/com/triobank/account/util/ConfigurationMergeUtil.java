package com.triobank.account.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration Merge Utility
 * 
 * Deep merge için utility sınıfı.
 * PATCH işlemlerinde mevcut configuration'ı koruyarak sadece değişen alanları
 * günceller.
 */
public class ConfigurationMergeUtil {

    /**
     * İki Map'i deep merge eder (nested/iç içe JSON'lar için)
     * 
     * Mantık:
     * - target'taki her key korunur
     * - source'taki yeni key'ler eklenir
     * - Ortak key'lerde source değeri kazanır (override)
     * - Nested Map'ler recursive merge edilir
     * 
     * @param target Mevcut configuration (korunacak)
     * @param source Yeni değerler (override edecek)
     * @return Merge edilmiş configuration
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> deepMerge(Map<String, Object> target, Map<String, Object> source) {
        if (target == null) {
            return source != null ? new HashMap<>(source) : new HashMap<>();
        }

        if (source == null || source.isEmpty()) {
            return new HashMap<>(target);
        }

        Map<String, Object> result = new HashMap<>(target);

        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object sourceValue = entry.getValue();

            if (sourceValue == null) {
                // Null değer gönderilirse o key'i sil
                result.remove(key);
                continue;
            }

            Object targetValue = result.get(key);

            // Her iki taraf da Map ise recursive merge
            if (sourceValue instanceof Map && targetValue instanceof Map) {
                Map<String, Object> mergedNested = deepMerge(
                        (Map<String, Object>) targetValue,
                        (Map<String, Object>) sourceValue);
                result.put(key, mergedNested);
            } else {
                // Değilse direkt override
                result.put(key, sourceValue);
            }
        }

        return result;
    }
}
