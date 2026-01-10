package com.triobank.ledger.domain.service;

import com.triobank.ledger.domain.model.EntryType;
import com.triobank.ledger.exception.DoubleEntryMismatchException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DoubleEntryValidator - Validates double-entry bookkeeping rules
 */
@Component
public class DoubleEntryValidator {

    /**
     * Simple DTO for validation (decoupled from request DTOs)
     */
    @Getter
    @AllArgsConstructor
    public static class ValidationEntry {
        private final Integer sequence;
        private final EntryType entryType;
        private final BigDecimal amount;
        private final String currency;
    }

    /**
     * Validate entries with all rules
     */
    public void validate(List<ValidationEntry> entries, String expectedCurrency) {
        validateMinimumEntries(entries);
        validateUniqueSequences(entries);
        validateCurrencyConsistency(entries, expectedCurrency);
        validateBalancedEntries(entries);
    }

    private void validateMinimumEntries(List<ValidationEntry> entries) {
        if (entries == null || entries.size() < 2) {
            throw new IllegalArgumentException(
                    String.format("At least 2 entries required. Found: %d",
                            entries == null ? 0 : entries.size()));
        }
    }

    private void validateUniqueSequences(List<ValidationEntry> entries) {
        Set<Integer> sequences = new HashSet<>();

        for (ValidationEntry entry : entries) {
            if (entry.getSequence() == null || entry.getSequence() <= 0) {
                throw new IllegalArgumentException("Invalid sequence: " + entry.getSequence());
            }

            if (!sequences.add(entry.getSequence())) {
                throw new IllegalArgumentException("Duplicate sequence: " + entry.getSequence());
            }
        }
    }

    private void validateCurrencyConsistency(List<ValidationEntry> entries, String expectedCurrency) {
        String firstCurrency = entries.get(0).getCurrency();

        if (firstCurrency == null || firstCurrency.isBlank()) {
            throw new IllegalArgumentException("Currency cannot be null");
        }

        if (expectedCurrency != null && !expectedCurrency.equals(firstCurrency)) {
            throw new IllegalArgumentException(
                    String.format("Currency mismatch. Expected: %s, Found: %s",
                            expectedCurrency, firstCurrency));
        }

        for (int i = 1; i < entries.size(); i++) {
            String currency = entries.get(i).getCurrency();
            if (!firstCurrency.equals(currency)) {
                throw new IllegalArgumentException(
                        String.format("Currency mismatch at entry[%d]. Expected: %s, Found: %s",
                                i, firstCurrency, currency));
            }
        }
    }

    private void validateBalancedEntries(List<ValidationEntry> entries) {
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (ValidationEntry entry : entries) {
            if (entry.getAmount() == null || entry.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Invalid amount for sequence " + entry.getSequence());
            }

            if (entry.getEntryType() == EntryType.DEBIT) {
                totalDebits = totalDebits.add(entry.getAmount());
            } else if (entry.getEntryType() == EntryType.CREDIT) {
                totalCredits = totalCredits.add(entry.getAmount());
            }
        }

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new DoubleEntryMismatchException(totalDebits, totalCredits);
        }
    }
}
