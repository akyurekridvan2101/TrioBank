package com.triobank.transaction.dto.mapper;

import com.triobank.transaction.domain.model.Transaction;
import com.triobank.transaction.dto.response.TransactionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Transaction Mapper - MapStruct DTO Converter
 * 
 * Converts Transaction entities to Response DTOs.
 * MapStruct automatically generates implementation code at compile time.
 * 
 * Pattern: Copied from Account Service for consistency.
 * 
 * Why MapStruct?
 * - Eliminates boilerplate setter/getter code
 * - Type-safe at compile time
 * - Better performance than reflection-based mappers
 */
@Mapper(componentModel = "spring")
public interface TransactionMapper {

    /**
     * Maps Transaction entity to TransactionResponse DTO.
     * Field names match 1:1, so most mappings are automatic.
     * 
     * Enum conversions:
     * - transactionType: TransactionType.TRANSFER → "TRANSFER"
     * - status: TransactionStatus.COMPLETED → "COMPLETED"
     * 
     * @param transaction Database entity
     * @return API response DTO
     */
    @Mapping(target = "transactionType", expression = "java(transaction.getTransactionType().name())")
    @Mapping(target = "status", expression = "java(transaction.getStatus().name())")
    TransactionResponse toResponse(Transaction transaction);
}
