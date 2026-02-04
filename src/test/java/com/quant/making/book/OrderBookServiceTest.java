package com.quant.making.book;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * OrderBookService 单元测试
 * 验证核心业务逻辑
 */
@ExtendWith(MockitoExtension.class)
class OrderBookServiceTest {

    @Mock
    private OrderBookRepository orderBookRepository;

    @InjectMocks
    private OrderBookService orderBookService;

    private static final String TEST_SYMBOL = "EURUSD";
    private static final Integer TEST_MARKET_TYPE = 2; // 境内外汇
    private static final String TEST_SOURCE = "TEST_SOURCE";

    @BeforeEach
    void setUp() {
        // 重置服务状态
        orderBookService.clearAllOrderBooks();
    }

    /**
     * 测试TC-OB-001: 订单簿档位聚合
     * 验证相同价格的订单能够正确聚合
     */
    @Test
    void testUpdateQuote_Aggregation() {
        // Given - 准备测试数据
        BigDecimal price100 = new BigDecimal("100");
        BigDecimal price101 = new BigDecimal("101");
        
        // 创建3笔Buy报价，价格100，数量分别为10、20、30
        BigDecimal qty10 = new BigDecimal("10");
        BigDecimal qty20 = new BigDecimal("20");
        BigDecimal qty30 = new BigDecimal("30");
        
        // 创建2笔Sell报价，价格101，数量分别为15、25
        BigDecimal qty15 = new BigDecimal("15");
        BigDecimal qty25 = new BigDecimal("25");

        // When - 执行业务操作
        // 添加3笔Buy报价，价格100
        orderBookService.updateQuote(TEST_SYMBOL, TEST_MARKET_TYPE, TEST_SOURCE, 
                                   OrderBook.BUY, price100, qty10);
        orderBookService.updateQuote(TEST_SYMBOL, TEST_MARKET_TYPE, TEST_SOURCE, 
                                   OrderBook.BUY, price100, qty20);
        orderBookService.updateQuote(TEST_SYMBOL, TEST_MARKET_TYPE, TEST_SOURCE, 
                                   OrderBook.BUY, price100, qty30);

        // 添加2笔Sell报价，价格101
        orderBookService.updateQuote(TEST_SYMBOL, TEST_MARKET_TYPE, TEST_SOURCE, 
                                   OrderBook.SELL, price101, qty15);
        orderBookService.updateQuote(TEST_SYMBOL, TEST_MARKET_TYPE, TEST_SOURCE, 
                                   OrderBook.SELL, price101, qty25);

        // Then - 验证结果
        OrderBook orderBook = orderBookService.getOrderBook(TEST_SYMBOL);
        assertThat(orderBook).isNotNull();
        assertThat(orderBook.getSymbol()).isEqualTo(TEST_SYMBOL);
        
        // 验证Buy档位[100, 60]
        OrderBook.PriceLevel buyLevel = orderBook.getAggregatedLevels().get(price100);
        assertThat(buyLevel).isNotNull();
        assertThat(buyLevel.getPrice()).isEqualByComparingTo(price100);
        assertThat(buyLevel.getTotalBuyQty()).isEqualByComparingTo(new BigDecimal("60")); // 10+20+30
        assertThat(buyLevel.getTotalSellQty()).isEqualByComparingTo(BigDecimal.ZERO);
        
        // 验证Sell档位[101, 40]
        OrderBook.PriceLevel sellLevel = orderBook.getAggregatedLevels().get(price101);
        assertThat(sellLevel).isNotNull();
        assertThat(sellLevel.getPrice()).isEqualByComparingTo(price101);
        assertThat(sellLevel.getTotalSellQty()).isEqualByComparingTo(new BigDecimal("40")); // 15+25
        assertThat(sellLevel.getTotalBuyQty()).isEqualByComparingTo(BigDecimal.ZERO);
        
        // 验证总共有2个价格档位
        assertThat(orderBook.getAggregatedLevels()).hasSize(2);
    }

    /**
     * 测试获取最优买方价格
     * 验证能够正确提取最高买价
     */
    @Test
    void testGetBestBid() {
        // Given - 准备测试数据
        BigDecimal highBid = new BigDecimal("100.50");  // 最高买价
        BigDecimal midBid = new BigDecimal("100.40");
        BigDecimal lowBid = new BigDecimal("100.30");
        BigDecimal askPrice = new BigDecimal("100.60");
        
        BigDecimal qty = new BigDecimal("10");

        // 添加多个买价档位
        orderBookService.updateQuote(TEST_SYMBOL, TEST_MARKET_TYPE, TEST_SOURCE, 
                                   OrderBook.BUY, lowBid, qty);
        orderBookService.updateQuote(TEST_SYMBOL, TEST_MARKET_TYPE, TEST_SOURCE, 
                                   OrderBook.BUY, midBid, qty);
        orderBookService.updateQuote(TEST_SYMBOL, TEST_MARKET_TYPE, TEST_SOURCE, 
                                   OrderBook.BUY, highBid, qty);
        // 添加卖价档位
        orderBookService.updateQuote(TEST_SYMBOL, TEST_MARKET_TYPE, TEST_SOURCE, 
                                   OrderBook.SELL, askPrice, qty);

        // When - 获取最优买价
        OrderBook.PriceLevel bestBid = orderBookService.getBestBid(TEST_SYMBOL);

        // Then - 验证最优买价是最高买价
        assertThat(bestBid).isNotNull();
        assertThat(bestBid.getPrice()).isEqualByComparingTo(highBid);
        assertThat(bestBid.hasBuyOrders()).isTrue();
        assertThat(bestBid.hasSellOrders()).isFalse();
    }

    /**
     * 测试获取最优卖方价格
     * 验证能够正确提取最低卖价
     */
    @Test
    void testGetBestAsk() {
        // Given - 准备测试数据
        BigDecimal bidPrice = new BigDecimal("100.40");
        BigDecimal lowAsk = new BigDecimal("100.50");  // 最低卖价
        BigDecimal midAsk = new BigDecimal("100.60");
        BigDecimal highAsk = new BigDecimal("100.70");
        
        BigDecimal qty = new BigDecimal("10");

        // 添加买价档位
        orderBookService.updateQuote(TEST_SYMBOL, TEST_MARKET_TYPE, TEST_SOURCE, 
                                   OrderBook.BUY, bidPrice, qty);
        // 添加多个卖价档位
        orderBookService.updateQuote(TEST_SYMBOL, TEST_MARKET_TYPE, TEST_SOURCE, 
                                   OrderBook.SELL, highAsk, qty);
        orderBookService.updateQuote(TEST_SYMBOL, TEST_MARKET_TYPE, TEST_SOURCE, 
                                   OrderBook.SELL, midAsk, qty);
        orderBookService.updateQuote(TEST_SYMBOL, TEST_MARKET_TYPE, TEST_SOURCE, 
                                   OrderBook.SELL, lowAsk, qty);

        // When - 获取最优卖价
        OrderBook.PriceLevel bestAsk = orderBookService.getBestAsk(TEST_SYMBOL);

        // Then - 验证最优卖价是最低卖价
        assertThat(bestAsk).isNotNull();
        assertThat(bestAsk.getPrice()).isEqualByComparingTo(lowAsk);
        assertThat(bestAsk.hasSellOrders()).isTrue();
        assertThat(bestAsk.hasBuyOrders()).isFalse();
    }

    /**
     * 测试获取完整订单簿
     * 验证能够正确返回订单簿对象
     */
    @Test
    void testGetOrderBook() {
        // Given - 准备测试数据
        BigDecimal price = new BigDecimal("100");
        BigDecimal qty = new BigDecimal("10");

        // 添加报价数据
        orderBookService.updateQuote(TEST_SYMBOL, TEST_MARKET_TYPE, TEST_SOURCE, 
                                   OrderBook.BUY, price, qty);

        // When - 获取订单簿
        OrderBook orderBook = orderBookService.getOrderBook(TEST_SYMBOL);

        // Then - 验证订单簿对象
        assertThat(orderBook).isNotNull();
        assertThat(orderBook.getSymbol()).isEqualTo(TEST_SYMBOL);
        assertThat(orderBook.getMarketType()).isEqualTo(TEST_MARKET_TYPE);
        assertThat(orderBook.getAggregatedLevels()).isNotEmpty();
        assertThat(orderBook.getAggregatedLevels()).containsKey(price);
        
        // 验证档位数据
        OrderBook.PriceLevel level = orderBook.getAggregatedLevels().get(price);
        assertThat(level).isNotNull();
        assertThat(level.getPrice()).isEqualByComparingTo(price);
        assertThat(level.getTotalBuyQty()).isEqualByComparingTo(qty);
    }

    /**
     * 测试批量更新报价
     */
    @Test
    void testUpdateQuotes_Batch() {
        // Given - 准备批量报价数据
        List<OrderBookService.QuoteData> quotes = List.of(
            new OrderBookService.QuoteData(TEST_SYMBOL, TEST_MARKET_TYPE, TEST_SOURCE, 
                                         OrderBook.BUY, new BigDecimal("100"), new BigDecimal("10")),
            new OrderBookService.QuoteData(TEST_SYMBOL, TEST_MARKET_TYPE, TEST_SOURCE, 
                                         OrderBook.SELL, new BigDecimal("101"), new BigDecimal("20")),
            new OrderBookService.QuoteData(TEST_SYMBOL, TEST_MARKET_TYPE, TEST_SOURCE, 
                                         OrderBook.BUY, new BigDecimal("102"), new BigDecimal("30"))
        );

        // When - 批量更新报价
        orderBookService.updateQuotes(quotes);

        // Then - 验证所有报价都已处理
        OrderBook orderBook = orderBookService.getOrderBook(TEST_SYMBOL);
        assertThat(orderBook).isNotNull();
        assertThat(orderBook.getAggregatedLevels()).hasSize(3); // 三个不同价格
        
        // 验证各个档位
        assertThat(orderBook.getAggregatedLevels().get(new BigDecimal("100")).getTotalBuyQty())
            .isEqualByComparingTo(new BigDecimal("10"));
        assertThat(orderBook.getAggregatedLevels().get(new BigDecimal("101")).getTotalSellQty())
            .isEqualByComparingTo(new BigDecimal("20"));
        assertThat(orderBook.getAggregatedLevels().get(new BigDecimal("102")).getTotalBuyQty())
            .isEqualByComparingTo(new BigDecimal("30"));
    }

    /**
     * 测试快照生成
     */
    @Test
    void testSnapshotGeneration() {
        // Given - 准备测试数据
        BigDecimal price = new BigDecimal("100");
        BigDecimal qty = new BigDecimal("10");
        
        orderBookService.updateQuote(TEST_SYMBOL, TEST_MARKET_TYPE, TEST_SOURCE, 
                                   OrderBook.BUY, price, qty);

        // When - 手动触发快照
        orderBookService.snapshot(TEST_SYMBOL);

        // Then - 验证快照数据已保存到仓库
        verify(orderBookRepository, atLeastOnce()).save(any(OrderBookEntry.class));
    }
}