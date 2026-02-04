package com.quant.making.book;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * OrderBookService 深度报价测试
 * 验证真实的深度报价场景
 */
@ExtendWith(MockitoExtension.class)
class OrderBookDepthTest {

    @Mock
    private OrderBookRepository orderBookRepository;

    @InjectMocks
    private OrderBookService orderBookService;

    private static final String TEST_SYMBOL = "XAUUSD";
    private static final Integer DOMESTIC_GOLD = OrderBook.MARKET_DOMESTIC_GOLD;
    private static final String DIMPLE = "DIMPLE";

    @BeforeEach
    void setUp() {
        orderBookService.clearAllOrderBooks();
    }

    /**
     * 测试场景：完整的深度报价数据结构
     * 
     * 真实场景：每个档位都包含完整字段：
     * - 档位 (priceLevel): 价格档位序号
     * - 来源 (source): 价源标识
     * - 类型 (marketType): 市场类型
     * - 方向 (side): BUY/SELL
     * - 价格 (price): 档位价格
     * - 数量 (quantity): 档位数量
     */
    @Test
    @DisplayName("TC-OB-001-Enhanced: 完整深度报价数据结构测试")
    void testCompleteDepthQuoteDataStructure() {
        // Given - 准备完整的深度报价数据
        // 每个报价都包含：档位、来源、类型、方向、价格、数量
        
        List<DepthQuote> completeQuotes = Arrays.asList(
            // Bid 档位
            new DepthQuote(1, "DIMPLE", DOMESTIC_GOLD, OrderBook.BUY, 
                          new BigDecimal("2000.00"), new BigDecimal("100.00")),
            new DepthQuote(2, "DIMPLE", DOMESTIC_GOLD, OrderBook.BUY, 
                          new BigDecimal("1999.50"), new BigDecimal("150.00")),
            new DepthQuote(3, "DIMPLE", DOMESTIC_GOLD, OrderBook.BUY, 
                          new BigDecimal("1999.00"), new BigDecimal("200.00")),
            
            // Ask 档位
            new DepthQuote(1, "DIMPLE", DOMESTIC_GOLD, OrderBook.SELL, 
                          new BigDecimal("2000.50"), new BigDecimal("80.00")),
            new DepthQuote(2, "DIMPLE", DOMESTIC_GOLD, OrderBook.SELL, 
                          new BigDecimal("2001.00"), new BigDecimal("120.00")),
            new DepthQuote(3, "DIMPLE", DOMESTIC_GOLD, OrderBook.SELL, 
                          new BigDecimal("2001.50"), new BigDecimal("180.00"))
        );

        // When - 发送完整结构的深度报价
        for (DepthQuote quote : completeQuotes) {
            orderBookService.updateQuote(
                TEST_SYMBOL, 
                quote.getMarketType(), 
                quote.getSource(), 
                quote.getSide(), 
                quote.getPrice(), 
                quote.getQuantity()
            );
        }

        // Then - 验证聚合结果
        
        OrderBook orderBook = orderBookService.getOrderBook(TEST_SYMBOL);
        assertThat(orderBook).isNotNull();
        assertThat(orderBook.getSymbol()).isEqualTo(TEST_SYMBOL);
        assertThat(orderBook.getMarketType()).isEqualTo(DOMESTIC_GOLD);

        // 验证Best Bid (最高买价)
        OrderBook.PriceLevel bestBid = orderBook.getBestBid();
        assertThat(bestBid).isNotNull();
        assertThat(bestBid.getPrice()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(bestBid.getTotalBuyQty()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(bestBid.getSources()).containsKey("DIMPLE");

        // 验证Best Ask (最低卖价)
        OrderBook.PriceLevel bestAsk = orderBook.getBestAsk();
        assertThat(bestAsk).isNotNull();
        assertThat(bestAsk.getPrice()).isEqualByComparingTo(new BigDecimal("2000.50"));
        assertThat(bestAsk.getTotalSellQty()).isEqualByComparingTo(new BigDecimal("80.00"));

        // 验证Spread
        BigDecimal spread = bestAsk.getPrice().subtract(bestBid.getPrice());
        assertThat(spread).isEqualByComparingTo(new BigDecimal("0.50"));

        // 验证总档位数 (聚合后相同价格的档位合并)
        assertThat(orderBook.getAggregatedLevels()).hasSize(6);
    }

    /**
     * 测试场景：多价源完整数据结构
     * 
     * 模拟真实的场景：
     * - Dimple提供境内黄金深度报价
     * - CFETS提供境内外汇深度报价
     * - 外资行提供境外市场深度报价
     */
    @Test
    @DisplayName("TC-OB-001-Enhanced: 多价源完整数据结构测试")
    void testMultiSourceCompleteDataStructure() {
        // Given - 准备三个价源的完整深度报价数据
        
        // Dimple - 境内黄金
        List<DepthQuote> dimpleQuotes = Arrays.asList(
            new DepthQuote(1, "DIMPLE", OrderBook.MARKET_DOMESTIC_GOLD, OrderBook.BUY, 
                          new BigDecimal("380.00"), new BigDecimal("50.00")),
            new DepthQuote(2, "DIMPLE", OrderBook.MARKET_DOMESTIC_GOLD, OrderBook.BUY, 
                          new BigDecimal("379.80"), new BigDecimal("80.00")),
            new DepthQuote(1, "DIMPLE", OrderBook.MARKET_DOMESTIC_GOLD, OrderBook.SELL, 
                          new BigDecimal("380.30"), new BigDecimal("40.00")),
            new DepthQuote(2, "DIMPLE", OrderBook.MARKET_DOMESTIC_GOLD, OrderBook.SELL, 
                          new BigDecimal("380.50"), new BigDecimal("70.00"))
        );

        // CFETS - 境内外汇
        List<DepthQuote> cfetsQuotes = Arrays.asList(
            new DepthQuote(1, "CFETS", OrderBook.MARKET_FOREIGN_EXCHANGE, OrderBook.BUY, 
                          new BigDecimal("7.2500"), new BigDecimal("1000000")),
            new DepthQuote(2, "CFETS", OrderBook.MARKET_FOREIGN_EXCHANGE, OrderBook.BUY, 
                          new BigDecimal("7.2450"), new BigDecimal("1500000")),
            new DepthQuote(1, "CFETS", OrderBook.MARKET_FOREIGN_EXCHANGE, OrderBook.SELL, 
                          new BigDecimal("7.2550"), new BigDecimal("800000")),
            new DepthQuote(2, "CFETS", OrderBook.MARKET_FOREIGN_EXCHANGE, OrderBook.SELL, 
                          new BigDecimal("7.2600"), new BigDecimal("1200000"))
        );

        // 外资行 - 境外外汇
        List<DepthQuote> foreignBankQuotes = Arrays.asList(
            new DepthQuote(1, "FOREIGN_BANK", OrderBook.MARKET_OFFSHORE, OrderBook.BUY, 
                          new BigDecimal("1.0800"), new BigDecimal("500000")),
            new DepthQuote(1, "FOREIGN_BANK", OrderBook.MARKET_OFFSHORE, OrderBook.SELL, 
                          new BigDecimal("1.0850"), new BigDecimal("400000"))
        );

        // When - 发送三个价源的深度报价
        for (DepthQuote quote : dimpleQuotes) {
            orderBookService.updateQuote("AU9999", quote.getMarketType(), 
                                      quote.getSource(), quote.getSide(), 
                                      quote.getPrice(), quote.getQuantity());
        }
        
        for (DepthQuote quote : cfetsQuotes) {
            orderBookService.updateQuote("USDJPY", quote.getMarketType(), 
                                      quote.getSource(), quote.getSide(), 
                                      quote.getPrice(), quote.getQuantity());
        }
        
        for (DepthQuote quote : foreignBankQuotes) {
            orderBookService.updateQuote("EURUSD", quote.getMarketType(), 
                                      quote.getSource(), quote.getSide(), 
                                      quote.getPrice(), quote.getQuantity());
        }

        // Then - 验证各订单簿独立
        
        // 验证境内黄金订单簿
        OrderBook goldBook = orderBookService.getOrderBook("AU9999");
        assertThat(goldBook.getMarketType()).isEqualTo(OrderBook.MARKET_DOMESTIC_GOLD);
        assertThat(goldBook.getBestBid().getPrice()).isEqualByComparingTo(new BigDecimal("380.00"));
        assertThat(goldBook.getBestBid().getSources()).containsKey("DIMPLE");

        // 验证境内外汇订单簿
        OrderBook forexBook = orderBookService.getOrderBook("USDJPY");
        assertThat(forexBook.getMarketType()).isEqualTo(OrderBook.MARKET_FOREIGN_EXCHANGE);
        assertThat(forexBook.getBestBid().getPrice()).isEqualByComparingTo(new BigDecimal("7.2500"));
        assertThat(forexBook.getBestAsk().getPrice()).isEqualByComparingTo(new BigDecimal("7.2550"));

        // 验证境外外汇订单簿
        OrderBook offshoreBook = orderBookService.getOrderBook("EURUSD");
        assertThat(offshoreBook.getMarketType()).isEqualTo(OrderBook.MARKET_OFFSHORE);
        assertThat(offshoreBook.getBestBid().getPrice()).isEqualByComparingTo(new BigDecimal("1.0800"));
        assertThat(offshoreBook.getBestAsk().getPrice()).isEqualByComparingTo(new BigDecimal("1.0850"));
    }

    /**
     * 测试场景：多价源深度报价聚合
     * 
     * 真实场景：可能有多个价源提供深度报价
     * 例如：Dimple提供境内黄金，CFETS提供外汇
     */
    @Test
    @DisplayName("TC-OB-001-Enhanced: 多价源深度报价聚合")
    void testMultiSourceDepthAggregation() {
        // Given - 境内黄金（Dimple）和境外黄金（外资行）两个价源
        List<DepthLevel> domesticGoldBid = Arrays.asList(
            new DepthLevel(new BigDecimal("380.00"), new BigDecimal("50.00")),   // 境内最优买价
            new DepthLevel(new BigDecimal("379.80"), new BigDecimal("80.00"))
        );
        List<DepthLevel> domesticGoldAsk = Arrays.asList(
            new DepthLevel(new BigDecimal("380.30"), new BigDecimal("40.00")),   // 境内最优卖价
            new DepthLevel(new BigDecimal("380.50"), new BigDecimal("70.00"))
        );

        List<DepthLevel> offshoreGoldBid = Arrays.asList(
            new DepthLevel(new BigDecimal("2000.00"), new BigDecimal("200.00")), // 境外最优买价
            new DepthLevel(new BigDecimal("1999.50"), new BigDecimal("300.00"))
        );
        List<DepthLevel> offshoreGoldAsk = Arrays.asList(
            new DepthLevel(new BigDecimal("2000.50"), new BigDecimal("150.00")), // 境外最优卖价
            new DepthLevel(new BigDecimal("2001.00"), new BigDecimal("250.00"))
        );

        // When - 分别更新境内和境外黄金报价
        updateDepthQuote("AU9999", DOMESTIC_GOLD, "DIMPLE", domesticGoldBid, domesticGoldAsk);
        updateDepthQuote("XAUUSD", OrderBook.MARKET_OFFSHORE, "FOREIGN_BANK", offshoreGoldBid, offshoreGoldAsk);

        // Then - 验证各自的订单簿
        OrderBook domesticBook = orderBookService.getOrderBook("AU9999");
        OrderBook offshoreBook = orderBookService.getOrderBook("XAUUSD");

        // 验证境内黄金
        assertThat(domesticBook.getBestBid().getPrice()).isEqualByComparingTo(new BigDecimal("380.00"));
        assertThat(domesticBook.getBestAsk().getPrice()).isEqualByComparingTo(new BigDecimal("380.30"));
        assertThat(domesticBook.getAggregatedLevels()).hasSize(4);

        // 验证境外黄金
        assertThat(offshoreBook.getBestBid().getPrice()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(offshoreBook.getBestAsk().getPrice()).isEqualByComparingTo(new BigDecimal("2000.50"));
        assertThat(offshoreBook.getAggregatedLevels()).hasSize(4);

        // 验证两个订单簿独立
        assertThat(domesticBook).isNotEqualTo(offshoreBook);
    }

    /**
     * 测试场景：深度报价更新（全量替换）
     * 
     * 真实场景：价源推送新的深度报价时，通常是全量替换而非增量更新
     */
    @Test
    @DisplayName("TC-OB-001-Enhanced: 深度报价全量替换")
    void testDepthQuoteFullReplacement() {
        // Given - 初始深度报价
        List<DepthLevel> initialBid = Arrays.asList(
            new DepthLevel(new BigDecimal("100.00"), new BigDecimal("50.00"))
        );
        List<DepthLevel> initialAsk = Arrays.asList(
            new DepthLevel(new BigDecimal("100.10"), new BigDecimal("40.00"))
        );
        updateDepthQuote(TEST_SYMBOL, OrderBook.MARKET_FOREIGN_EXCHANGE, "CFETS", initialBid, initialAsk);

        // When - 全量替换为新的深度报价
        List<DepthLevel> newBid = Arrays.asList(
            new DepthLevel(new BigDecimal("100.20"), new BigDecimal("80.00")),  // 价格变动
            new DepthLevel(new BigDecimal("100.10"), new BigDecimal("100.00"))
        );
        List<DepthLevel> newAsk = Arrays.asList(
            new DepthLevel(new BigDecimal("100.30"), new BigDecimal("60.00")),  // 价格变动
            new DepthLevel(new BigDecimal("100.40"), new BigDecimal("90.00"))
        );
        
        // 模拟全量替换（先清空该价源的档位，再添加新档位）
        orderBookService.clearOrderBook(TEST_SYMBOL);
        updateDepthQuote(TEST_SYMBOL, OrderBook.MARKET_FOREIGN_EXCHANGE, "CFETS", newBid, newAsk);

        // Then - 验证订单簿已更新
        OrderBook orderBook = orderBookService.getOrderBook(TEST_SYMBOL);
        assertThat(orderBook.getAggregatedLevels()).hasSize(4); // 2 bid + 2 ask
        assertThat(orderBook.getBestBid().getPrice()).isEqualByComparingTo(new BigDecimal("100.20"));
        assertThat(orderBook.getBestAsk().getPrice()).isEqualByComparingTo(new BigDecimal("100.30"));
    }

    /**
     * 测试场景：深度报价中的价源优先级
     * 
     * 真实场景：当多个价源有相同价格时，可能需要按优先级聚合
     */
    @Test
    @DisplayName("TC-OB-001-Enhanced: 多价源同档位聚合")
    void testMultiSourceSameLevelAggregation() {
        // Given - 两个价源在相同价格有报价
        // 价源A在2000.00有100手买单
        orderBookService.updateQuote(TEST_SYMBOL, DOMESTIC_GOLD, "SOURCE_A", 
                                   OrderBook.BUY, new BigDecimal("2000.00"), new BigDecimal("100"));
        
        // 价源B在2000.00有150手买单
        orderBookService.updateQuote(TEST_SYMBOL, DOMESTIC_GOLD, "SOURCE_B", 
                                   OrderBook.BUY, new BigDecimal("2000.00"), new BigDecimal("150"));

        // When - 获取聚合结果
        OrderBook.PriceLevel level = orderBookService.getOrderBook(TEST_SYMBOL)
                                                   .getAggregatedLevels()
                                                   .get(new BigDecimal("2000.00"));

        // Then - 验证数量正确聚合
        assertThat(level).isNotNull();
        assertThat(level.getTotalBuyQty()).isEqualByComparingTo(new BigDecimal("250")); // 100 + 150
        assertThat(level.getSources()).hasSize(2); // 两个价源
        assertThat(level.getSources().get("SOURCE_A")).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(level.getSources().get("SOURCE_B")).isEqualByComparingTo(new BigDecimal("150"));
    }

    // ===================== Helper Methods =====================

    /**
     * 深度报价档位内部类
     * 包含完整的报价字段：档位、来源、类型、方向、价格、数量
     */
    private static class DepthQuote {
        private final int priceLevel;    // 档位序号
        private final String source;      // 来源
        private final Integer marketType; // 类型
        private final Integer side;       // 方向
        private final BigDecimal price;   // 价格
        private final BigDecimal quantity;// 数量

        public DepthQuote(int priceLevel, String source, Integer marketType, 
                         Integer side, BigDecimal price, BigDecimal quantity) {
            this.priceLevel = priceLevel;
            this.source = source;
            this.marketType = marketType;
            this.side = side;
            this.price = price;
            this.quantity = quantity;
        }

        // Getters
        public int getPriceLevel() { return priceLevel; }
        public String getSource() { return source; }
        public Integer getMarketType() { return marketType; }
        public Integer getSide() { return side; }
        public BigDecimal getPrice() { return price; }
        public BigDecimal getQuantity() { return quantity; }
    }

    /**
     * 深度报价档位内部类（简化版）
     */
    private static class DepthLevel {
        private final BigDecimal price;
        private final BigDecimal quantity;

        public DepthLevel(BigDecimal price, BigDecimal quantity) {
            this.price = price;
            this.quantity = quantity;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public BigDecimal getQuantity() {
            return quantity;
        }
    }
    
    /**
     * 辅助方法：更新深度报价
     */
    private void updateDepthQuote(String symbol, Integer marketType, String source,
                                  List<DepthLevel> bids, List<DepthLevel> asks) {
        for (DepthLevel bid : bids) {
            orderBookService.updateQuote(symbol, marketType, source, 
                                       OrderBook.BUY, bid.getPrice(), bid.getQuantity());
        }
        for (DepthLevel ask : asks) {
            orderBookService.updateQuote(symbol, marketType, source, 
                                       OrderBook.SELL, ask.getPrice(), ask.getQuantity());
        }
    }
}
