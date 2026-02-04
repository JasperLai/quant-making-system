package com.quant.making.book;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * OrderBook 单元测试
 * 验证订单簿聚合视图功能
 */
class OrderBookTest {

    private OrderBook orderBook;
    private static final String TEST_SYMBOL = "EURUSD";
    private static final Integer TEST_MARKET_TYPE = 2; // 境内外汇
    private static final String TEST_SOURCE = "TEST_SOURCE";

    @BeforeEach
    void setUp() {
        orderBook = new OrderBook(TEST_SYMBOL, TEST_MARKET_TYPE);
    }

    /**
     * 测试相同价格买方订单聚合
     * 验证相同价格的买单能够正确累加数量
     */
    @Test
    void testAddQuote_BuyAggregation() {
        // Given - 准备测试数据
        BigDecimal price = new BigDecimal("100");
        BigDecimal qty1 = new BigDecimal("10");
        BigDecimal qty2 = new BigDecimal("20");
        BigDecimal qty3 = new BigDecimal("30");
        BigDecimal expectedTotal = new BigDecimal("60"); // 10+20+30

        // When - 添加多笔相同价格的买单
        orderBook.addQuote(TEST_SOURCE, OrderBook.BUY, price, qty1);
        orderBook.addQuote(TEST_SOURCE, OrderBook.BUY, price, qty2);
        orderBook.addQuote(TEST_SOURCE, OrderBook.BUY, price, qty3);

        // Then - 验证聚合结果
        Map<BigDecimal, OrderBook.PriceLevel> levels = orderBook.getAggregatedLevels();
        assertThat(levels).containsKey(price);
        
        OrderBook.PriceLevel level = levels.get(price);
        assertThat(level).isNotNull();
        assertThat(level.getPrice()).isEqualByComparingTo(price);
        assertThat(level.getTotalBuyQty()).isEqualByComparingTo(expectedTotal);
        assertThat(level.getTotalSellQty()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(level.hasBuyOrders()).isTrue();
        assertThat(level.hasSellOrders()).isFalse();
    }

    /**
     * 测试相同价格卖方订单聚合
     * 验证相同价格的卖单能够正确累加数量
     */
    @Test
    void testAddQuote_SellAggregation() {
        // Given - 准备测试数据
        BigDecimal price = new BigDecimal("101");
        BigDecimal qty1 = new BigDecimal("15");
        BigDecimal qty2 = new BigDecimal("25");
        BigDecimal expectedTotal = new BigDecimal("40"); // 15+25

        // When - 添加多笔相同价格的卖单
        orderBook.addQuote(TEST_SOURCE, OrderBook.SELL, price, qty1);
        orderBook.addQuote(TEST_SOURCE, OrderBook.SELL, price, qty2);

        // Then - 验证聚合结果
        Map<BigDecimal, OrderBook.PriceLevel> levels = orderBook.getAggregatedLevels();
        assertThat(levels).containsKey(price);
        
        OrderBook.PriceLevel level = levels.get(price);
        assertThat(level).isNotNull();
        assertThat(level.getPrice()).isEqualByComparingTo(price);
        assertThat(level.getTotalSellQty()).isEqualByComparingTo(expectedTotal);
        assertThat(level.getTotalBuyQty()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(level.hasSellOrders()).isTrue();
        assertThat(level.hasBuyOrders()).isFalse();
    }

    /**
     * 测试获取最优买方和卖方价格
     * 验证最优价格提取逻辑的正确性
     */
    @Test
    void testGetBestBid_Ask() {
        // Given - 准备测试数据，创建多个价格档位
        BigDecimal lowBid = new BigDecimal("99.80");
        BigDecimal midBid = new BigDecimal("99.90");
        BigDecimal highBid = new BigDecimal("100.00");  // 最优买价
        BigDecimal lowAsk = new BigDecimal("100.10");   // 最优卖价
        BigDecimal midAsk = new BigDecimal("100.20");
        BigDecimal highAsk = new BigDecimal("100.30");
        
        BigDecimal qty = new BigDecimal("10");

        // 添加买价档位
        orderBook.addQuote(TEST_SOURCE, OrderBook.BUY, lowBid, qty);
        orderBook.addQuote(TEST_SOURCE, OrderBook.BUY, midBid, qty);
        orderBook.addQuote(TEST_SOURCE, OrderBook.BUY, highBid, qty);
        
        // 添加卖价档位
        orderBook.addQuote(TEST_SOURCE, OrderBook.SELL, lowAsk, qty);
        orderBook.addQuote(TEST_SOURCE, OrderBook.SELL, midAsk, qty);
        orderBook.addQuote(TEST_SOURCE, OrderBook.SELL, highAsk, qty);

        // When - 获取最优买价和卖价
        OrderBook.PriceLevel bestBid = orderBook.getBestBid();
        OrderBook.PriceLevel bestAsk = orderBook.getBestAsk();

        // Then - 验证最优价格提取结果
        // 最优买价应该是最高买价
        assertThat(bestBid).isNotNull();
        assertThat(bestBid.getPrice()).isEqualByComparingTo(highBid);
        assertThat(bestBid.hasBuyOrders()).isTrue();
        assertThat(bestBid.hasSellOrders()).isFalse();
        
        // 最优卖价应该是最低卖价
        assertThat(bestAsk).isNotNull();
        assertThat(bestAsk.getPrice()).isEqualByComparingTo(lowAsk);
        assertThat(bestAsk.hasSellOrders()).isTrue();
        assertThat(bestAsk.hasBuyOrders()).isFalse();
    }

    /**
     * 测试混合买卖订单聚合
     * 验证同一价格档位可以同时包含买卖订单
     */
    @Test
    void testMixedBuySellAtSamePrice() {
        // Given - 准备测试数据
        BigDecimal samePrice = new BigDecimal("100.00");
        BigDecimal buyQty = new BigDecimal("50");
        BigDecimal sellQty = new BigDecimal("30");

        // When - 在同一价格添加买卖订单
        orderBook.addQuote(TEST_SOURCE, OrderBook.BUY, samePrice, buyQty);
        orderBook.addQuote(TEST_SOURCE, OrderBook.SELL, samePrice, sellQty);

        // Then - 验证同一档位包含买卖数量
        OrderBook.PriceLevel level = orderBook.getAggregatedLevels().get(samePrice);
        assertThat(level).isNotNull();
        assertThat(level.getPrice()).isEqualByComparingTo(samePrice);
        assertThat(level.getTotalBuyQty()).isEqualByComparingTo(buyQty);
        assertThat(level.getTotalSellQty()).isEqualByComparingTo(sellQty);
        assertThat(level.hasBuyOrders()).isTrue();
        assertThat(level.hasSellOrders()).isTrue();
    }

    /**
     * 测试价格档位排序
     * 验证价格档位按照价格升序排列
     */
    @Test
    void testPriceLevelSorting() {
        // Given - 准备测试数据，按乱序添加价格
        BigDecimal[] prices = {
            new BigDecimal("102.00"),
            new BigDecimal("98.00"),
            new BigDecimal("100.00"),
            new BigDecimal("101.00"),
            new BigDecimal("99.00")
        };
        
        BigDecimal qty = new BigDecimal("10");

        // When - 按乱序添加价格档位
        for (BigDecimal price : prices) {
            orderBook.addQuote(TEST_SOURCE, OrderBook.BUY, price, qty);
        }

        // Then - 验证价格档位按升序排列
        Map<BigDecimal, OrderBook.PriceLevel> levels = orderBook.getAggregatedLevels();
        assertThat(levels).hasSize(5);
        
        // 检查所有价格是否都存在
        for (BigDecimal price : prices) {
            assertThat(levels).containsKey(price);
        }
        
        // 验证最优买价（最高买价）是102.00
        OrderBook.PriceLevel bestBid = orderBook.getBestBid();
        assertThat(bestBid.getPrice()).isEqualByComparingTo(new BigDecimal("102.00"));
    }

    /**
     * 测试空订单簿情况
     * 验证没有订单时的边界情况
     */
    @Test
    void testEmptyOrderBook() {
        // Given - 空订单簿
        OrderBook emptyOrderBook = new OrderBook(TEST_SYMBOL, TEST_MARKET_TYPE);

        // When - 获取最优价格
        OrderBook.PriceLevel bestBid = emptyOrderBook.getBestBid();
        OrderBook.PriceLevel bestAsk = emptyOrderBook.getBestAsk();

        // Then - 验证返回null
        assertThat(bestBid).isNull();
        assertThat(bestAsk).isNull();
        assertThat(emptyOrderBook.getAggregatedLevels()).isEmpty();
    }

    /**
     * 测试价格档位内部类功能
     */
    @Test
    void testPriceLevelClass() {
        // Given - 创建价格档位
        BigDecimal price = new BigDecimal("100.00");
        OrderBook.PriceLevel level = new OrderBook.PriceLevel(price);

        // When - 添加买卖数量
        level.addBuyQuantity(TEST_SOURCE, new BigDecimal("10"));
        level.addSellQuantity(TEST_SOURCE, new BigDecimal("20"));

        // Then - 验证档位属性
        assertThat(level.getPrice()).isEqualByComparingTo(price);
        assertThat(level.getTotalBuyQty()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(level.getTotalSellQty()).isEqualByComparingTo(new BigDecimal("20"));
        assertThat(level.hasBuyOrders()).isTrue();
        assertThat(level.hasSellOrders()).isTrue();
        
        // 验证sources映射
        assertThat(level.getSources()).containsKey(TEST_SOURCE);
        assertThat(level.getSources().get(TEST_SOURCE)).isEqualByComparingTo(new BigDecimal("30")); // 10+20
    }

    /**
     * 测试价源数量跟踪
     */
    @Test
    void testSourceQuantityTracking() {
        // Given - 准备多个价源
        String source1 = "SOURCE1";
        String source2 = "SOURCE2";
        BigDecimal price = new BigDecimal("100.00");
        BigDecimal qty1 = new BigDecimal("10");
        BigDecimal qty2 = new BigDecimal("20");

        // When - 添加来自不同价源的订单
        orderBook.addQuote(source1, OrderBook.BUY, price, qty1);
        orderBook.addQuote(source2, OrderBook.BUY, price, qty2);

        // Then - 验证价源数量正确跟踪
        OrderBook.PriceLevel level = orderBook.getAggregatedLevels().get(price);
        assertThat(level).isNotNull();
        assertThat(level.getTotalBuyQty()).isEqualByComparingTo(new BigDecimal("30")); // 10+20
        
        // 验证各个价源的数量
        assertThat(level.getSources()).hasSize(2);
        assertThat(level.getSources()).containsEntry(source1, qty1);
        assertThat(level.getSources()).containsEntry(source2, qty2);
    }
}