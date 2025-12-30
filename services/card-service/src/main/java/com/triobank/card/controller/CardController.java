package com.triobank.card.controller;

import com.triobank.card.domain.model.Card;
import com.triobank.card.domain.model.CreditCard;
import com.triobank.card.domain.model.DebitCard;
import com.triobank.card.domain.model.VirtualCard;
import com.triobank.card.dto.mapper.CardMapper;
import com.triobank.card.dto.request.IssueCreditCardRequest;
import com.triobank.card.dto.request.IssueDebitCardRequest;
import com.triobank.card.dto.request.IssueVirtualCardRequest;
import com.triobank.card.dto.response.CardResponse;
import com.triobank.card.dto.response.CreditCardResponse;
import com.triobank.card.dto.response.DebitCardResponse;
import com.triobank.card.dto.response.VirtualCardResponse;
import com.triobank.card.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Card Management API Controller
 * 
 * REST endpoints for card issuance and management.
 * Follows the same pattern as AccountController for consistency.
 */
@RestController
@RequestMapping("/v1/cards")
@RequiredArgsConstructor
@Tag(name = "Card Management", description = "Card issuance and management APIs")
public class CardController {

    private final CardService cardService;
    private final CardMapper cardMapper;

    /**
     * [POST] Issue Debit Card
     */
    @PostMapping("/debit")
    @Operation(summary = "Issue debit card", description = "Creates a new debit card linked to an account")
    public ResponseEntity<DebitCardResponse> issueDebitCard(@Valid @RequestBody IssueDebitCardRequest request) {
        DebitCard card = cardService.issueDebitCard(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cardMapper.toDebitCardResponse(card));
    }

    /**
     * [POST] Issue Credit Card
     */
    @PostMapping("/credit")
    @Operation(summary = "Issue credit card", description = "Creates a new credit card linked to an account")
    public ResponseEntity<CreditCardResponse> issueCreditCard(@Valid @RequestBody IssueCreditCardRequest request) {
        CreditCard card = cardService.issueCreditCard(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cardMapper.toCreditCardResponse(card));
    }

    /**
     * [POST] Issue Virtual Card
     */
    @PostMapping("/virtual")
    @Operation(summary = "Issue virtual card", description = "Creates a new virtual card for online transactions")
    public ResponseEntity<VirtualCardResponse> issueVirtualCard(@Valid @RequestBody IssueVirtualCardRequest request) {
        VirtualCard card = cardService.issueVirtualCard(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cardMapper.toVirtualCardResponse(card));
    }

    /**
     * [GET] Get Card Details
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get card details", description = "Retrieves details of a specific card")
    public ResponseEntity<CardResponse> getCard(@PathVariable String id) {
        Card card = cardService.getCard(id);
        return ResponseEntity.ok(cardMapper.toResponse(card));
    }

    /**
     * [GET] Get Cards by Customer or Account
     * 
     * Examples:
     * - /v1/cards?customerId=cust-123 → All cards for customer
     * - /v1/cards?customerId=cust-123&cardType=DEBIT → Only debit cards
     * - /v1/cards?customerId=cust-123&cardType=DEBIT&cardType=CREDIT → Debit and
     * credit cards
     * - /v1/cards?accountId=acc-456 → All cards for specific account
     */
    @GetMapping
    @Operation(summary = "Get cards by customer or account", description = "Lists cards filtered by customerId (with optional cardType filter) or accountId")
    public ResponseEntity<List<CardResponse>> getCards(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) List<com.triobank.card.domain.model.CardType> cardType) {

        List<Card> cards;

        if (customerId != null) {
            // Get cards by customerId with optional cardType filter
            cards = cardService.getCardsByCustomer(customerId, cardType);
        } else if (accountId != null) {
            // Get cards by accountId (existing functionality)
            cards = cardService.getCardsByAccount(accountId, cardType);
        } else {
            throw new IllegalArgumentException("Either customerId or accountId is required");
        }

        List<CardResponse> responses = cards.stream()
                .map(cardMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * [PATCH] Block Card
     */
    @PatchMapping("/{id}/block")
    @Operation(summary = "Block card", description = "Blocks a card with a reason")
    public ResponseEntity<Void> blockCard(
            @PathVariable String id,
            @RequestParam String reason) {

        cardService.blockCard(id, reason);
        return ResponseEntity.noContent().build();
    }

    /**
     * [PATCH] Activate Card
     */
    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate card", description = "Activates a blocked or inactive card")
    public ResponseEntity<Void> activateCard(@PathVariable String id) {
        cardService.activateCard(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * [POST] Authorize Transaction
     * 
     * Validates if card can be used for a transaction.
     * Called by Transaction Service before processing payment.
     */
    @PostMapping("/{id}/authorize")
    @Operation(summary = "Authorize transaction", description = "Validates card for transaction. Returns account ID if authorized.")
    public ResponseEntity<com.triobank.card.dto.response.AuthorizationResponse> authorizeTransaction(
            @PathVariable String id,
            @Valid @RequestBody com.triobank.card.dto.request.AuthorizationRequest request) {

        com.triobank.card.dto.response.AuthorizationResponse response = cardService.authorizeTransaction(
                id,
                request.getAmount(),
                request.getTransactionType(),
                request.getChannel());

        if (response.isAuthorized()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
    }

    /**
     * [POST] Validate PIN
     */
    @PostMapping("/{id}/validate-pin")
    @Operation(summary = "Validate PIN", description = "Validates card PIN for authentication")
    public ResponseEntity<Boolean> validatePin(
            @PathVariable String id,
            @RequestParam String pin) {

        boolean isValid = cardService.validatePin(id, pin);
        return ResponseEntity.ok(isValid);
    }
}
