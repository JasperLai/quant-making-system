package com.quant.making.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    Page<AuditEvent> findByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    Page<AuditEvent> findByEventType(AuditEventType eventType, Pageable pageable);

    Page<AuditEvent> findBySymbol(String symbol, Pageable pageable);

    Page<AuditEvent> findByTimestampBetweenAndEventType(LocalDateTime startTime, LocalDateTime endTime, AuditEventType eventType, Pageable pageable);

    Page<AuditEvent> findByTimestampBetweenAndSymbol(LocalDateTime startTime, LocalDateTime endTime, String symbol, Pageable pageable);

    Page<AuditEvent> findByEventTypeAndSymbol(AuditEventType eventType, String symbol, Pageable pageable);

    @Query("SELECT ae FROM AuditEvent ae WHERE ae.timestamp BETWEEN :startTime AND :endTime AND ae.eventType = :eventType AND ae.symbol = :symbol")
    Page<AuditEvent> findByTimestampAndEventTypeAndSymbol(@Param("startTime") LocalDateTime startTime, 
                                                         @Param("endTime") LocalDateTime endTime, 
                                                         @Param("eventType") AuditEventType eventType, 
                                                         @Param("symbol") String symbol, 
                                                         Pageable pageable);
}