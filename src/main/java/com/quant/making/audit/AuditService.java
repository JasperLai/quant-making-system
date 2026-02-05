package com.quant.making.audit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class AuditService {

    @Autowired
    private AuditEventRepository auditEventRepository;

    /**
     * 记录审计事件
     */
    public AuditEvent logEvent(AuditEventType eventType, String symbol, String quoteId, String orderId, String tradeId, String details, String riskCheckResult) {
        AuditEvent event = new AuditEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(eventType);
        event.setTimestamp(LocalDateTime.now());
        event.setSymbol(symbol);
        event.setQuoteId(quoteId);
        event.setOrderId(orderId);
        event.setTradeId(tradeId);
        event.setDetails(details);
        event.setRiskCheckResult(riskCheckResult);

        return auditEventRepository.save(event);
    }

    /**
     * 记录简单的审计事件（只有事件类型）
     */
    public AuditEvent logEvent(AuditEventType eventType) {
        return logEvent(eventType, null, null, null, null, null, null);
    }

    /**
     * 记录带有符号的审计事件
     */
    public AuditEvent logEvent(AuditEventType eventType, String symbol) {
        return logEvent(eventType, symbol, null, null, null, null, null);
    }

    /**
     * 记录带有符号和详细信息的审计事件
     */
    public AuditEvent logEvent(AuditEventType eventType, String symbol, String details) {
        return logEvent(eventType, symbol, null, null, null, details, null);
    }

    /**
     * 查询审计事件 - 按时间范围
     */
    public Page<AuditEvent> queryEvents(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        return auditEventRepository.findByTimestampBetween(startTime, endTime, pageable);
    }

    /**
     * 查询审计事件 - 按事件类型
     */
    public Page<AuditEvent> queryEvents(AuditEventType eventType, Pageable pageable) {
        return auditEventRepository.findByEventType(eventType, pageable);
    }

    /**
     * 查询审计事件 - 按交易符号
     */
    public Page<AuditEvent> queryEvents(String symbol, Pageable pageable) {
        return auditEventRepository.findBySymbol(symbol, pageable);
    }

    /**
     * 查询审计事件 - 按时间范围和事件类型
     */
    public Page<AuditEvent> queryEvents(LocalDateTime startTime, LocalDateTime endTime, AuditEventType eventType, Pageable pageable) {
        return auditEventRepository.findByTimestampBetweenAndEventType(startTime, endTime, eventType, pageable);
    }

    /**
     * 查询审计事件 - 按时间范围和交易符号
     */
    public Page<AuditEvent> queryEvents(LocalDateTime startTime, LocalDateTime endTime, String symbol, Pageable pageable) {
        return auditEventRepository.findByTimestampBetweenAndSymbol(startTime, endTime, symbol, pageable);
    }

    /**
     * 查询审计事件 - 按事件类型和交易符号
     */
    public Page<AuditEvent> queryEvents(AuditEventType eventType, String symbol, Pageable pageable) {
        return auditEventRepository.findByEventTypeAndSymbol(eventType, symbol, pageable);
    }

    /**
     * 查询审计事件 - 按时间范围、事件类型和交易符号
     */
    public Page<AuditEvent> queryEvents(LocalDateTime startTime, LocalDateTime endTime, AuditEventType eventType, String symbol, Pageable pageable) {
        return auditEventRepository.findByTimestampAndEventTypeAndSymbol(startTime, endTime, eventType, symbol, pageable);
    }
}