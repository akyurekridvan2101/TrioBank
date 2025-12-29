package com.triobank.card.dto.mapper;

import com.triobank.card.domain.model.*;
import com.triobank.card.dto.response.CardResponse;
import com.triobank.card.dto.response.CreditCardResponse;
import com.triobank.card.dto.response.DebitCardResponse;
import com.triobank.card.dto.response.VirtualCardResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Card Mapper - MapStruct DTOConverter
 * 
 * Converts Card entities to Response DTOs.
 * MapStruct automatically generates implementation code at compile time.
 * 
 * Pattern copied from Account Service for consistency.
 */
@Mapper(componentModel = "spring")
public interface CardMapper {

    /**
     * Maps Card (base) to CardResponse with polymorphic dispatch
     * CRITICAL: number field populated based on card type:
     * - Virtual cards: full card number (unmasked)
     * - Other cards: masked number
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
     * Base mapping for generic Card (fallback)
     * number = maskedNumber for non-virtual cards
     */
    @Mapping(target = "cardType", expression = "java(card.getCardType().name())")
    @Mapping(target = "number", source = "maskedNumber")
    @Mapping(target = "cardBrand", expression = "java(card.getCardBrand().name())")
    @Mapping(target = "status", expression = "java(card.getStatus().name())")
    CardResponse toBaseResponse(Card card);

    /**
     * Maps DebitCard to DebitCardResponse
     * number = maskedNumber (always masked for debit cards)
     */
    @Mapping(target = "cardType", expression = "java(debitCard.getCardType().name())")
    @Mapping(target = "number", source = "maskedNumber")
    @Mapping(target = "cardBrand", expression = "java(debitCard.getCardBrand().name())")
    @Mapping(target = "status", expression = "java(debitCard.getStatus().name())")
    DebitCardResponse toDebitCardResponse(DebitCard debitCard);

    /**
     * Maps CreditCard to CreditCardResponse
     * number = maskedNumber (always masked for credit cards)
     */
    @Mapping(target = "cardType", expression = "java(creditCard.getCardType().name())")
    @Mapping(target = "number", source = "maskedNumber")
    @Mapping(target = "cardBrand", expression = "java(creditCard.getCardBrand().name())")
    @Mapping(target = "status", expression = "java(creditCard.getStatus().name())")
    CreditCardResponse toCreditCardResponse(CreditCard creditCard);

    /**
     * Maps VirtualCard to VirtualCardResponse
     * number = cardNumber (FULL PAN, unmasked for virtual cards)
     * cvv = cvv (CVV visible for virtual cards)
     */
    @Mapping(target = "cardType", expression = "java(virtualCard.getCardType().name())")
    @Mapping(target = "number", source = "cardNumber")
    @Mapping(target = "cvv", source = "cvv")
    @Mapping(target = "cardBrand", expression = "java(virtualCard.getCardBrand().name())")
    @Mapping(target = "status", expression = "java(virtualCard.getStatus().name())")
    VirtualCardResponse toVirtualCardResponse(VirtualCard virtualCard);
}
