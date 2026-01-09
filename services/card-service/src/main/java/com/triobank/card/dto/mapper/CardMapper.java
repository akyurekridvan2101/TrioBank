package com.triobank.card.dto.mapper;

import com.triobank.card.domain.model.*;
import com.triobank.card.dto.response.CardResponse;
import com.triobank.card.dto.response.CreditCardResponse;
import com.triobank.card.dto.response.DebitCardResponse;
import com.triobank.card.dto.response.VirtualCardResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Card Mapper (MapStruct)
 *
 * Entity -> DTO dönüşümleri. MapStruct compile-time'da kod üretir.
 * Account Service ile aynı yapıda.
 */
@Mapper(componentModel = "spring")
public interface CardMapper {

    /**
     * Kart tipine göre (Polymorphic) doğru response'a çevirir.
     * KRİTİK: Sanal kartta tam numara dönerken, diğerlerinde maskeli döner.
     */
    default CardResponse toResponse(Card card) {
        if (card instanceof VirtualCard) {
            return toVirtualCardResponse((VirtualCard) card);
        } else if (card instanceof DebitCard) {
            return toDebitCardResponse((DebitCard) card);
        } else if (card instanceof CreditCard) {
            return toCreditCardResponse((CreditCard) card);
        }
        return toBaseResponse(card);
    }

    /**
     * Genel kart mapping (fallback).
     * Default olarak maskeli numara döner.
     */
    @Mapping(target = "cardType", expression = "java(card.getCardType().name())")
    @Mapping(target = "number", source = "maskedNumber")
    @Mapping(target = "cardBrand", expression = "java(card.getCardBrand().name())")
    @Mapping(target = "status", expression = "java(card.getStatus().name())")
    CardResponse toBaseResponse(Card card);

    /**
     * Debit kart mapping: Numara her zaman maskeli.
     */
    @Mapping(target = "cardType", expression = "java(debitCard.getCardType().name())")
    @Mapping(target = "number", source = "maskedNumber")
    @Mapping(target = "cardBrand", expression = "java(debitCard.getCardBrand().name())")
    @Mapping(target = "status", expression = "java(debitCard.getStatus().name())")
    DebitCardResponse toDebitCardResponse(DebitCard debitCard);

    /**
     * Kredi kartı mapping: Numara her zaman maskeli.
     */
    @Mapping(target = "cardType", expression = "java(creditCard.getCardType().name())")
    @Mapping(target = "number", source = "maskedNumber")
    @Mapping(target = "cardBrand", expression = "java(creditCard.getCardBrand().name())")
    @Mapping(target = "status", expression = "java(creditCard.getStatus().name())")
    CreditCardResponse toCreditCardResponse(CreditCard creditCard);

    /**
     * Sanal kart mapping:
     * Numara AÇIK (unmasked) verilir, CVV de gösterilir.
     */
    @Mapping(target = "cardType", expression = "java(virtualCard.getCardType().name())")
    @Mapping(target = "number", source = "cardNumber")
    @Mapping(target = "cvv", source = "cvv")
    @Mapping(target = "cardBrand", expression = "java(virtualCard.getCardBrand().name())")
    @Mapping(target = "status", expression = "java(virtualCard.getStatus().name())")
    VirtualCardResponse toVirtualCardResponse(VirtualCard virtualCard);
}
