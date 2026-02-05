package com.quant.making.audit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password="
})
@Transactional
class AuditServiceTest {

    @Autowired
    private AuditService auditService;

    @Test
    void testLogEvent() {
        // 测试记录简单事件
        AuditEvent event = auditService.logEvent(AuditEventType.ORDER_CREATED);
        
        assertNotNull(event.getId());
        assertEquals(AuditEventType.ORDER_CREATED, event.getEventType());
        assertNotNull(event.getEventId());
        assertNotNull(event.getTimestamp());
    }

    @Test
    void testLogEventWithSymbol() {
        // 测试记录带符号的事件
        String symbol = "BTC-USDT";
        AuditEvent event = auditService.logEvent(AuditEventType.ORDER_CREATED, symbol);
        
        assertNotNull(event.getId());
        assertEquals(AuditEventType.ORDER_CREATED, event.getEventType());
        assertEquals(symbol, event.getSymbol());
        assertNotNull(event.getEventId());
        assertNotNull(event.getTimestamp());
    }

    @Test
    void testLogEventWithDetails() {
        // 测试记录带详情的事件
        String symbol = "ETH-USDT";
        String details = "Test order creation with details";
        AuditEvent event = auditService.logEvent(AuditEventType.ORDER_CREATED, symbol, details);
        
        assertNotNull(event.getId());
        assertEquals(AuditEventType.ORDER_CREATED, event.getEventType());
        assertEquals(symbol, event.getSymbol());
        assertEquals(details, event.getDetails());
        assertNotNull(event.getEventId());
        assertNotNull(event.getTimestamp());
    }

    @Test
    void testLogEventWithAllParams() {
        // 测试记录带所有参数的事件
        String symbol = "BTC-USDT";
        String quoteId = "Q123456";
        String orderId = "O123456";
        String tradeId = "T123456";
        String details = "Full event details";
        String riskCheckResult = "PASSED";

        AuditEvent event = auditService.logEvent(
            AuditEventType.TRADE_EXECUTED, symbol, quoteId, orderId, tradeId, details, riskCheckResult
        );
        
        assertNotNull(event.getId());
        assertEquals(AuditEventType.TRADE_EXECUTED, event.getEventType());
        assertEquals(symbol, event.getSymbol());
        assertEquals(quoteId, event.getQuoteId());
        assertEquals(orderId, event.getOrderId());
        assertEquals(tradeId, event.getTradeId());
        assertEquals(details, event.getDetails());
        assertEquals(riskCheckResult, event.getRiskCheckResult());
        assertNotNull(event.getEventId());
        assertNotNull(event.getTimestamp());
    }

    @Test
    void testQueryEventsByTimeRange() {
        // 先记录一些事件
        auditService.logEvent(AuditEventType.ORDER_CREATED, "BTC-USDT");
        auditService.logEvent(AuditEventType.ORDER_UPDATED, "ETH-USDT");
        
        // 查询指定时间范围内的事件
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime past = now.minusMinutes(1);
        Pageable pageable = PageRequest.of(0, 10);
        
        Page<AuditEvent> events = auditService.queryEvents(past, now, pageable);
        
        assertTrue(events.getContent().size() >= 2);
        assertTrue(events.getContent().stream()
            .anyMatch(e -> e.getEventType() == AuditEventType.ORDER_CREATED && "BTC-USDT".equals(e.getSymbol())));
        assertTrue(events.getContent().stream()
            .anyMatch(e -> e.getEventType() == AuditEventType.ORDER_UPDATED && "ETH-USDT".equals(e.getSymbol())));
    }

    @Test
    void testQueryEventsByEventType() {
        // 先记录一些事件
        auditService.logEvent(AuditEventType.ORDER_CREATED, "BTC-USDT");
        auditService.logEvent(AuditEventType.ORDER_CREATED, "ETH-USDT");
        auditService.logEvent(AuditEventType.QUOTE_GENERATED, "BTC-USDT");
        
        Pageable pageable = PageRequest.of(0, 10);
        
        // 查询特定类型的事件
        Page<AuditEvent> orderCreatedEvents = auditService.queryEvents(AuditEventType.ORDER_CREATED, pageable);
        
        assertEquals(2, orderCreatedEvents.getContent().size());
        assertTrue(orderCreatedEvents.getContent().stream()
            .allMatch(e -> e.getEventType() == AuditEventType.ORDER_CREATED));
    }

    @Test
    void testQueryEventsBySymbol() {
        // 先记录一些事件
        auditService.logEvent(AuditEventType.ORDER_CREATED, "BTC-USDT");
        auditService.logEvent(AuditEventType.ORDER_CREATED, "BTC-USDT");
        auditService.logEvent(AuditEventType.ORDER_CREATED, "ETH-USDT");
        
        Pageable pageable = PageRequest.of(0, 10);
        
        // 查询特定符号的事件
        Page<AuditEvent> btcEvents = auditService.queryEvents("BTC-USDT", pageable);
        
        assertEquals(2, btcEvents.getContent().size());
        assertTrue(btcEvents.getContent().stream()
            .allMatch(e -> "BTC-USDT".equals(e.getSymbol())));
    }

    @Test
    void testQueryEventsByTimeRangeAndEventType() {
        // 先记录一些事件
        auditService.logEvent(AuditEventType.ORDER_CREATED, "BTC-USDT");
        auditService.logEvent(AuditEventType.ORDER_CREATED, "ETH-USDT");
        auditService.logEvent(AuditEventType.QUOTE_GENERATED, "BTC-USDT");
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime past = now.minusMinutes(1);
        Pageable pageable = PageRequest.of(0, 10);
        
        // 查询特定时间范围内特定类型的事件
        Page<AuditEvent> events = auditService.queryEvents(past, now, AuditEventType.ORDER_CREATED, pageable);
        
        assertTrue(events.getContent().size() >= 2);
        assertTrue(events.getContent().stream()
            .allMatch(e -> e.getEventType() == AuditEventType.ORDER_CREATED));
    }

    @Test
    void testQueryEventsByTimeRangeAndSymbol() {
        // 先记录一些事件
        auditService.logEvent(AuditEventType.ORDER_CREATED, "BTC-USDT");
        auditService.logEvent(AuditEventType.ORDER_UPDATED, "BTC-USDT");
        auditService.logEvent(AuditEventType.ORDER_CREATED, "ETH-USDT");
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime past = now.minusMinutes(1);
        Pageable pageable = PageRequest.of(0, 10);
        
        // 查询特定时间范围内特定符号的事件
        Page<AuditEvent> events = auditService.queryEvents(past, now, "BTC-USDT", pageable);
        
        assertEquals(2, events.getContent().size());
        assertTrue(events.getContent().stream()
            .allMatch(e -> "BTC-USDT".equals(e.getSymbol())));
    }

    @Test
    void testQueryEventsByEventTypeAndSymbol() {
        // 先记录一些事件
        auditService.logEvent(AuditEventType.ORDER_CREATED, "BTC-USDT");
        auditService.logEvent(AuditEventType.ORDER_CREATED, "BTC-USDT");
        auditService.logEvent(AuditEventType.ORDER_CREATED, "ETH-USDT");
        auditService.logEvent(AuditEventType.QUOTE_GENERATED, "BTC-USDT");
        
        Pageable pageable = PageRequest.of(0, 10);
        
        // 查询特定类型和符号的事件
        Page<AuditEvent> events = auditService.queryEvents(AuditEventType.ORDER_CREATED, "BTC-USDT", pageable);
        
        assertEquals(2, events.getContent().size());
        assertTrue(events.getContent().stream()
            .allMatch(e -> e.getEventType() == AuditEventType.ORDER_CREATED && "BTC-USDT".equals(e.getSymbol())));
    }
}