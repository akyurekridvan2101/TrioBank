package com.triobank.transaction.repository;

import com.triobank.transaction.domain.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Outbox Event Repository
 * 
 * Repository for outbox_events table (Transactional Outbox Pattern).
 * Used by Debezium CDC for reliable event publishing.
 * 
 * NOTE: Normally OutboxService uses JdbcTemplate (not JPA) for performance.
 * This repository is for monitoring/cleanup operations only.
 * 
 * Pattern: Copied from other services' outbox repositories
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Find events by aggregate type (for monitoring)
     */
    List<OutboxEvent> findByAggregateType(String aggregateType);

    /**
     * Find events by aggregate ID (for debugging specific transactions)
     */
    List<OutboxEvent> findByAggregateId(String aggregateId);

    /**
     * Find events by type (for monitoring specific event types)
     */
    List<OutboxEvent> findByType(String type);

    /**
     * Find events older than specified timestamp (for cleanup)
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.createdAt < :threshold ORDER BY e.createdAt ASC")
    List<OutboxEvent> findOlderThan(@Param("threshold") Instant threshold);

    /**
     * Count events by aggregate type (statistics)
     */
    long countByAggregateType(String aggregateType);

    /**
     * Count events by type (statistics)
     */
    long countByType(String type);

    /**
     * Delete events older than threshold (cleanup job)
     * 
     * NOTE: Only delete after Debezium has processed them!
     * Usually run as a scheduled job (e.g., delete events older than 7 days)
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.createdAt < :threshold")
    int deleteOlderThan(@Param("threshold") Instant threshold);

    /**
     * Find recent events for monitoring dashboard
     */
    @Query("SELECT e FROM OutboxEvent e ORDER BY e.createdAt DESC")
    List<OutboxEvent> findRecent(org.springframework.data.domain.Pageable pageable);
}
