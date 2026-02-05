package com.quant.making.book;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OrderBookEntry 实体单元测试
 */
class OrderBookEntryTest {
    
    @Test
    @DisplayName("全参构造函数测试")
    void testFullConstructor() {
        LocalDateTime now = LocalDateTime.now();
        OrderBookEntry entry = new OrderBookEntry(
            "XAUUSD",
            OrderBook.MARKET_OFFSHORE,
            "SourceA",
            OrderBook.BUY,
            1,
            new BigDecimal("2000.00"),
            new BigDecimal("100.00"),
            now
        );
        
        assertEquals("XAUUSD", entry.getSymbol());
        assertEquals(OrderBook.MARKET_OFFSHORE, entry.getMarketType());
        assertEquals("SourceA", entry.getSource());
        assertEquals(OrderBook.BUY, entry.getSide());
        assertEquals(1, entry.getPriceLevel());
        assertEquals(0, new BigDecimal("2000.00").compareTo(entry.getPrice()));
        assertEquals(0, new BigDecimal("100.00").compareTo(entry.getQuantity()));
        assertEquals(now, entry.getSnapshotTime());
    }
    
    @Test
    @DisplayName("默认构造函数测试")
    void testDefaultConstructor() {
        OrderBookEntry entry = new OrderBookEntry();
        assertNull(entry.getId());
        assertNull(entry.getSymbol());
    }
    
    @Test
    @DisplayName("Setter和Getter测试")
    void testSettersAndGetters() {
        OrderBookEntry entry = new OrderBookEntry();
        
        entry.setId(100L);
        entry.setSymbol("USDJPY");
        entry.setMarketType(OrderBook.MARKET_FOREIGN_EXCHANGE);
        entry.setSource("CFETS");
        entry.setSide(OrderBook.SELL);
        entry.setPriceLevel(5);
        entry.setPrice(new BigDecimal("150.50"));
        entry.setQuantity(new BigDecimal("500.00"));
        LocalDateTime now = LocalDateTime.now();
        entry.setSnapshotTime(now);
        
        assertEquals(100L, entry.getId());
        assertEquals("USDJPY", entry.getSymbol());
        assertEquals(OrderBook.MARKET_FOREIGN_EXCHANGE, entry.getMarketType());
        assertEquals("CFETS", entry.getSource());
        assertEquals(OrderBook.SELL, entry.getSide());
        assertEquals(5, entry.getPriceLevel());
        assertEquals(0, new BigDecimal("150.50").compareTo(entry.getPrice()));
        assertEquals(0, new BigDecimal("500.00").compareTo(entry.getQuantity()));
        assertEquals(now, entry.getSnapshotTime());
    }
    
    @Test
    @DisplayName("toString测试")
    void testToString() {
        OrderBookEntry entry = new OrderBookEntry(
            "XAUUSD",
            OrderBook.MARKET_OFFSHORE,
            "SourceA",
            OrderBook.BUY,
            1,
            new BigDecimal("2000.00"),
            new BigDecimal("100.00"),
            LocalDateTime.now()
        );
        
        String toString = entry.toString();
        assertTrue(toString.contains("XAUUSD"));
        assertTrue(toString.contains("3")); // MARKET_OFFSHORE = 3
        assertTrue(toString.contains("SourceA"));
        assertTrue(toString.contains("1")); // BUY = 1
        assertTrue(toString.contains("2000"));
        assertTrue(toString.contains("100"));
    }
    
    @Test
    @DisplayName("所有市场类型测试")
    void testAllMarketTypes() {
        LocalDateTime now = LocalDateTime.now();
        
        // 境内黄金
        OrderBookEntry goldEntry = new OrderBookEntry(
            "AU9999", OrderBook.MARKET_DOMESTIC_GOLD, "Dimple", 
            OrderBook.BUY, 1, new BigDecimal("380.00"), new BigDecimal("100"), now);
        assertEquals(OrderBook.MARKET_DOMESTIC_GOLD, goldEntry.getMarketType());
        
        // 境内外汇
        OrderBookEntry forexEntry = new OrderBookEntry(
            "USDJPY", OrderBook.MARKET_FOREIGN_EXCHANGE, "CFETS", 
            OrderBook.SELL, 1, new BigDecimal("150.00"), new BigDecimal("1000"), now);
        assertEquals(OrderBook.MARKET_FOREIGN_EXCHANGE, forexEntry.getMarketType());
        
        // 境外
        OrderBookEntry offshoreEntry = new OrderBookEntry(
            "XAUUSD", OrderBook.MARKET_OFFSHORE, "外资行", 
            OrderBook.BUY, 1, new BigDecimal("2000.00"), new BigDecimal("50"), now);
        assertEquals(OrderBook.MARKET_OFFSHORE, offshoreEntry.getMarketType());
    }
    
    @Test
    @DisplayName("精度测试")
    void testPrecision() {
        LocalDateTime now = LocalDateTime.now();
        
        // 测试18位精度，8位小数
        OrderBookEntry entry = new OrderBookEntry(
            "XAUUSD",
            OrderBook.MARKET_OFFSHORE,
            "SourceA",
            OrderBook.BUY,
            1,
            new BigDecimal("12345678901.12345678"),
            new BigDecimal("98765432109.87654321"),
            now
        );
        
        assertEquals(0, new BigDecimal("12345678901.12345678").compareTo(entry.getPrice()));
        assertEquals(0, new BigDecimal("98765432109.87654321").compareTo(entry.getQuantity()));
    }
}
