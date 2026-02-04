package com.quant.making.book;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 订单簿模块测试数据验证
 * 验证测试数据的正确性和完整性
 */
class OrderBookTestDataValidationTest {

    private static final String TEST_SYMBOL = "EURUSD";
    private static final Integer TEST_MARKET_TYPE = 2; // 境内外汇
    private static final String TEST_SOURCE = "CFETS";

    @BeforeEach
    void setUp() {
        // 测试数据准备
    }

    @Test
    void validateTestPrices() {
        // 验证测试价格数据的有效性
        BigDecimal validPrice = new BigDecimal("1.23456");
        assertTrue(validPrice.compareTo(BigDecimal.ZERO) > 0, "价格应为正数");
        assertTrue(validPrice.scale() <= 8, "价格精度不应超过8位小数");
    }

    @Test
    void validateTestQuantities() {
        // 验证测试数量数据的有效性
        BigDecimal validQuantity = new BigDecimal("1000000");
        assertTrue(validQuantity.compareTo(BigDecimal.ZERO) > 0, "数量应为正数");
        assertTrue(validQuantity.scale() <= 8, "数量精度不应超过8位小数");
    }

    @Test
    void validateMarketTypes() {
        // 验证市场类型常量
        assertEquals(1, OrderBook.MARKET_DOMESTIC_GOLD);
        assertEquals(2, OrderBook.MARKET_FOREIGN_EXCHANGE);
        assertEquals(3, OrderBook.MARKET_OFFSHORE);
    }

    @Test
    void validateSides() {
        // 验证买卖方向常量
        assertEquals(1, OrderBook.BUY);
        assertEquals(2, OrderBook.SELL);
    }

    @Test
    void validateOrderBookEntryCreation() {
        // 验证订单簿条目创建的正确性
        LocalDateTime now = LocalDateTime.now();
        OrderBookEntry entry = new OrderBookEntry(
            TEST_SYMBOL,
            TEST_MARKET_TYPE,
            TEST_SOURCE,
            OrderBook.BUY,
            1,
            new BigDecimal("1.2345"),
            new BigDecimal("1000000"),
            now
        );

        assertEquals(TEST_SYMBOL, entry.getSymbol());
        assertEquals(TEST_MARKET_TYPE, entry.getMarketType());
        assertEquals(TEST_SOURCE, entry.getSource());
        assertEquals(OrderBook.BUY, entry.getSide());
        assertNotNull(entry.getSnapshotTime());
    }

    @Test
    void validateOrderBookCreation() {
        // 验证订单簿创建的正确性
        OrderBook orderBook = new OrderBook(TEST_SYMBOL, TEST_MARKET_TYPE);

        assertEquals(TEST_SYMBOL, orderBook.getSymbol());
        assertEquals(TEST_MARKET_TYPE, orderBook.getMarketType());
        assertNotNull(orderBook.getAggregatedLevels());
    }

    @Test
    void validatePriceLevelCreation() {
        // 验证价格档位创建的正确性
        BigDecimal price = new BigDecimal("1.2345");
        OrderBook.PriceLevel level = new OrderBook.PriceLevel(price);

        assertEquals(price, level.getPrice());
        assertEquals(BigDecimal.ZERO, level.getTotalBuyQty());
        assertEquals(BigDecimal.ZERO, level.getTotalSellQty());
        assertNotNull(level.getSources());
    }
}