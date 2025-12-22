package com.triobank.account.repository;

import com.triobank.account.domain.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Outbox Event Veri Erişim Katmanı
 * 
 * Transactional Outbox Pattern için kullanılır.
 * 
 * İşleyiş:
 * Service katmanı bir işlem yaptığında (örn: Hesap açma), aynı transaction
 * içinde
 * buraya bir event kaydeder.
 * 
 * Not:
 * Buraya yazılan kayıtları biz okumuyoruz. Debezium (CDC Connectoru) veritabanı
 * loglarından okuyup Kafka'ya taşıyor. Bizim görevimiz sadece yazmak.
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    // Standart save() metodu yeterli.
}
