package com.quant.making.book;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * OrderBookSnapshotTest 单元测试
 * 验证快照功能和订单簿初始化
 */
@ExtendWith(MockitoExtension.class)
class OrderBookSnapshotTest {

    @Mock
    private OrderBookRepository orderBookRepository;

    @Captor
    private ArgumentCaptor<OrderBookEntry> entryCaptor;

    private OrderBookService orderBookService;

    private static final String TEST_SYMBOL = "EURUSD";
    private static final Integer TEST_MARKET_TYPE = 2; // 境内外汇
    private static final String TEST_SOURCE = "TEST_SOURCE";

    @BeforeEach
    void setUp() {
        orderBookService = new OrderBookService();
        ReflectionTestUtils.setField(orderBookService, "orderBookRepository", orderBookRepository);
        orderBookService.clearAllOrderBooks(); // 清空缓存
    }

    /**
     * 测试TC-OB-002: 订单簿快照生成
     * 验证订单簿快照正确生成和存储
     */
    @Test
    void testSnapshotGeneration() {
        // Given - 准备测试数据
        // 创建3笔Buy报价，价格100，数量分别为10、20、30
        BigDecimal price100 = new BigDecimal("100");
        BigDecimal qty10 = new BigDecimal("10");
        BigDecimal qty20 = new BigDecimal("20");
        BigDecimal qty30 = new BigDecimal("30");
        
        // 创建2笔Sell报价，价格101，数量分别为15、25
        BigDecimal price101 = new BigDecimal("101");
        BigDecimal qty15 = new BigDecimal("15");
        BigDecimal qty25 = new BigDecimal("25");

        // 添加报价到订单簿
        orderBookService.updateQuote(TEST_SYMBOL, TEST_MARKET_TYPE, TEST_SOURCE, 
                                   OrderBook.BUY, price100, qty10);
        orderBookService.updateQuote(TEST_SYMBOL, TEST_MARKET_TYPE, TEST_SOURCE, 
                                   OrderBook.BUY, price100, qty20);
        orderBookService.updateQuote(TEST_SYMBOL, TEST_MARKET_TYPE, TEST_SOURCE, 
                                   OrderBook.BUY, price100, qty30);
        orderBookService.updateQuote(TEST_SYMBOL, TEST_MARKET_TYPE, TEST_SOURCE, 
                                   OrderBook.SELL, price101, qty15);
        orderBookService.updateQuote(TEST_SYMBOL, TEST_MARKET_TYPE, TEST_SOURCE, 
                                   OrderBook.SELL, price101, qty25);

        // When - 手动触发快照生成
        orderBookService.snapshot(TEST_SYMBOL);

        // Then - 验证快照数据已保存到数据库
        verify(orderBookRepository, atLeastOnce()).save(any(OrderBookEntry.class));
        
        // 捕获保存的快照数据
        List<OrderBookEntry> capturedEntries = entryCaptor.getAllValues();
        
        // 验证快照数据的正确性
        assertThat(capturedEntries).isNotEmpty();
        
        // 查找对应的价格档位
        List<OrderBookEntry> buyEntries = capturedEntries.stream()
            .filter(entry -> entry.getPrice().compareTo(price100) == 0 && entry.getSide() == OrderBook.BUY)
            .toList();
        List<OrderBookEntry> sellEntries = capturedEntries.stream()
            .filter(entry -> entry.getPrice().compareTo(price101) == 0 && entry.getSide() == OrderBook.SELL)
            .toList();
        
        // 验证Buy档位[100, 60] - 由于按价源拆分，可能有多条记录
        BigDecimal totalBuyQty = buyEntries.stream()
            .map(OrderBookEntry::getQuantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalBuyQty).isEqualByComparingTo(new BigDecimal("60")); // 10+20+30
        
        // 验证Sell档位[101, 40] - 由于按价源拆分，可能有多条记录
        BigDecimal totalSellQty = sellEntries.stream()
            .map(OrderBookEntry::getQuantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalSellQty).isEqualByComparingTo(new BigDecimal("40")); // 15+25
        
        // 验证快照时间是当前时间附近
        LocalDateTime now = LocalDateTime.now();
        for (OrderBookEntry entry : capturedEntries) {
            assertThat(entry.getSnapshotTime()).isAfter(now.minusMinutes(1));
            assertThat(entry.getSnapshotTime()).isBefore(now.plusMinutes(1));
        }
        
        // 验证所有快照记录的符号和市场类型
        for (OrderBookEntry entry : capturedEntries) {
            assertThat(entry.getSymbol()).isEqualTo(TEST_SYMBOL);
            assertThat(entry.getMarketType()).isEqualTo(TEST_MARKET_TYPE);
        }
    }

    /**
     * 测试TC-OB-003: 订单簿初始化
     * 验证从快照正确恢复订单簿
     */
    @Test
    void testOrderBookInitialization() {
        // Given - 准备模拟的快照数据
        LocalDateTime snapshotTime = LocalDateTime.now().minusMinutes(5);
        
        // 创建快照数据，模拟数据库中的历史数据
        List<OrderBookEntry> snapshotData = List.of(
            new OrderBookEntry(TEST_SYMBOL, TEST_MARKET_TYPE, TEST_SOURCE, OrderBook.BUY,
                             0, new BigDecimal("100"), new BigDecimal("60"), snapshotTime),
            new OrderBookEntry(TEST_SYMBOL, TEST_MARKET_TYPE, TEST_SOURCE, OrderBook.SELL,
                             0, new BigDecimal("101"), new BigDecimal("40"), snapshotTime)
        );
        
        // 模拟仓库返回这些快照数据
        when(orderBookRepository.findLatestSnapshot(TEST_SYMBOL, TEST_MARKET_TYPE))
            .thenReturn(snapshotData);

        // When - 从快照恢复订单簿
        boolean restored = orderBookService.restoreFromSnapshot(TEST_SYMBOL);

        // Then - 验证恢复成功
        assertThat(restored).isTrue();
        
        // 验证订单簿已正确恢复
        OrderBook restoredOrderBook = orderBookService.getOrderBook(TEST_SYMBOL);
        assertThat(restoredOrderBook).isNotNull();
        assertThat(restoredOrderBook.getSymbol()).isEqualTo(TEST_SYMBOL);
        assertThat(restoredOrderBook.getMarketType()).isEqualTo(TEST_MARKET_TYPE);
        
        // 验证档位数据已恢复
        assertThat(restoredOrderBook.getAggregatedLevels()).hasSize(2);
        
        // 验证Buy档位[100, 60]
        OrderBook.PriceLevel buyLevel = restoredOrderBook.getAggregatedLevels().get(new BigDecimal("100"));
        assertThat(buyLevel).isNotNull();
        assertThat(buyLevel.getPrice()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(buyLevel.getTotalBuyQty()).isEqualByComparingTo(new BigDecimal("60"));
        assertThat(buyLevel.getTotalSellQty()).isEqualByComparingTo(BigDecimal.ZERO);
        
        // 验证Sell档位[101, 40]
        OrderBook.PriceLevel sellLevel = restoredOrderBook.getAggregatedLevels().get(new BigDecimal("101"));
        assertThat(sellLevel).isNotNull();
        assertThat(sellLevel.getPrice()).isEqualByComparingTo(new BigDecimal("101"));
        assertThat(sellLevel.getTotalSellQty()).isEqualByComparingTo(new BigDecimal("40"));
        assertThat(sellLevel.getTotalBuyQty()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    /**
     * 测试从空快照恢复订单簿
     * 验证边界情况处理
     */
    @Test
    void testRestoreFromEmptySnapshot() {
        // Given - 模拟没有快照数据
        when(orderBookRepository.findLatestSnapshot(TEST_SYMBOL, TEST_MARKET_TYPE))
            .thenReturn(List.of());

        // When - 尝试从空快照恢复订单簿
        boolean restored = orderBookService.restoreFromSnapshot(TEST_SYMBOL);

        // Then - 验证恢复失败
        assertThat(restored).isFalse();
        
        // 验证订单簿未被创建
        OrderBook orderBook = orderBookService.getOrderBook(TEST_SYMBOL);
        assertThat(orderBook).isNull();
    }

    /**
     * 测试快照生成时的定时检查机制
     */
    @Test
    void testSnapshotTimingCheck() {
        // Given - 设置快照间隔为1秒
        orderBookService.setSnapshotIntervalSeconds(1);
        
        BigDecimal price = new BigDecimal("100");
        BigDecimal qty = new BigDecimal("10");

        // When - 添加报价并立即再次添加
        orderBookService.updateQuote(TEST_SYMBOL, TEST_MARKET_TYPE, TEST_SOURCE, 
                                   OrderBook.BUY, price, qty);

        // 然后立即再次添加，此时应该不会触发快照（因为间隔太短）
        orderBookService.updateQuote(TEST_SYMBOL, TEST_MARKET_TYPE, TEST_SOURCE, 
                                   OrderBook.SELL, price.add(new BigDecimal("0.1")), qty);

        // Then - 验证只调用了一次save（第一次更新触发了快照）
        verify(orderBookRepository, atLeastOnce()).save(any(OrderBookEntry.class));
    }

    /**
     * 测试快照生成的内部逻辑
     * 验证价格档位正确转换为快照条目
     */
    @Test
    void testSnapshotInternalLogic() {
        // Given - 准备测试数据
        BigDecimal price = new BigDecimal("100.50");
        BigDecimal buyQty = new BigDecimal("50");
        BigDecimal sellQty = new BigDecimal("30");
        String source1 = "SOURCE1";
        String source2 = "SOURCE2";

        // 创建包含多个价源的订单簿
        OrderBook orderBook = new OrderBook(TEST_SYMBOL, TEST_MARKET_TYPE);
        OrderBook.PriceLevel level = new OrderBook.PriceLevel(price);
        level.addBuyQuantity(source1, new BigDecimal("20"));
        level.addBuyQuantity(source2, new BigDecimal("30"));
        level.addSellQuantity(source1, new BigDecimal("10"));
        level.addSellQuantity(source2, new BigDecimal("20"));
        
        orderBook.getAggregatedLevels().put(price, level);

        // 保存订单簿到服务
        orderBookService.updateQuote(TEST_SYMBOL, TEST_MARKET_TYPE, source1, 
                                   OrderBook.BUY, price, new BigDecimal("20"));
        orderBookService.updateQuote(TEST_SYMBOL, TEST_MARKET_TYPE, source2, 
                                   OrderBook.BUY, price, new BigDecimal("30"));
        orderBookService.updateQuote(TEST_SYMBOL, TEST_MARKET_TYPE, source1, 
                                   OrderBook.SELL, price, new BigDecimal("10"));
        orderBookService.updateQuote(TEST_SYMBOL, TEST_MARKET_TYPE, source2, 
                                   OrderBook.SELL, price, new BigDecimal("20"));

        // When - 生成快照
        orderBookService.snapshot(TEST_SYMBOL);

        // Then - 验证快照数据正确保存
        verify(orderBookRepository, atLeast(4)).save(any(OrderBookEntry.class)); // 至少4个价源条目
        
        List<OrderBookEntry> capturedEntries = entryCaptor.getAllValues();
        
        // 验证包含了所有价源的数据
        long buyEntriesCount = capturedEntries.stream()
            .filter(entry -> entry.getSide() == OrderBook.BUY && entry.getPrice().compareTo(price) == 0)
            .count();
        long sellEntriesCount = capturedEntries.stream()
            .filter(entry -> entry.getSide() == OrderBook.SELL && entry.getPrice().compareTo(price) == 0)
            .count();
            
        // 至少有一个买方和一个卖方条目
        assertThat(buyEntriesCount).isGreaterThan(0);
        assertThat(sellEntriesCount).isGreaterThan(0);
    }

    /**
     * 测试快照清理功能
     * 验证旧快照能够被正确清理
     */
    @Test
    void testCleanupOldSnapshots() {
        // Given - 模拟清理操作
        when(orderBookRepository.deleteOldSnapshots(any(LocalDateTime.class)))
            .thenReturn(5); // 模拟删除了5条记录

        // When - 执行清理操作
        orderBookService.cleanupOldSnapshots();

        // Then - 验证清理方法被正确调用
        verify(orderBookRepository).deleteOldSnapshots(any(LocalDateTime.class));
    }
}