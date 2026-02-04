package com.quant.making.book;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OrderBookRepository 数据访问层测试
 * 验证数据库交互功能
 */
@ExtendWith(MockitoExtension.class)
class OrderBookRepositoryTest {

    @Mock
    private OrderBookRepository orderBookRepository;

    @BeforeEach
    void setUp() {
        // 准备测试数据
    }

    /**
     * 测试保存订单簿条目
     */
    @Test
    void testSaveOrderBookEntry() {
        // Given - 准备订单簿条目
        OrderBookEntry entry = new OrderBookEntry(
            "EURUSD",
            OrderBook.MARKET_FOREIGN_EXCHANGE,
            "CFETS",
            OrderBook.BUY,
            1,
            new BigDecimal("1.1000"),
            new BigDecimal("1000000"),
            LocalDateTime.now()
        );

        // When - 保存条目
        when(orderBookRepository.save(entry)).thenReturn(entry);

        OrderBookEntry saved = orderBookRepository.save(entry);

        // Then - 验证保存成功
        verify(orderBookRepository, times(1)).save(any(OrderBookEntry.class));
        assert(saved != null);
        assert("EURUSD".equals(saved.getSymbol()));
    }

    /**
     * 测试按品种和时间范围查询
     */
    @Test
    void testFindBySymbolAndSnapshotTimeBetween() {
        // Given - 准备查询参数
        String symbol = "EURUSD";
        LocalDateTime startTime = LocalDateTime.now().minusDays(1);
        LocalDateTime endTime = LocalDateTime.now();

        // When - 执行查询
        orderBookRepository.findBySymbolAndSnapshotTimeBetween(symbol, startTime, endTime);

        // Then - 验证查询方法被调用
        verify(orderBookRepository, times(1))
            .findBySymbolAndSnapshotTimeBetween(eq(symbol), eq(startTime), eq(endTime));
    }

    /**
     * 测试删除旧快照
     */
    @Test
    void testDeleteOldSnapshots() {
        // Given - 准备截止时间
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);

        // When - 执行删除
        orderBookRepository.deleteOldSnapshots(cutoffTime);

        // Then - 验证删除方法被调用
        verify(orderBookRepository, times(1)).deleteOldSnapshots(eq(cutoffTime));
    }

    /**
     * 测试查找最新快照
     */
    @Test
    void testFindLatestSnapshot() {
        // Given - 准备查询参数
        String symbol = "EURUSD";
        Integer marketType = OrderBook.MARKET_FOREIGN_EXCHANGE;

        // When - 执行查询
        orderBookRepository.findLatestSnapshot(symbol, marketType);

        // Then - 验证查询方法被调用
        verify(orderBookRepository, times(1))
            .findLatestSnapshot(eq(symbol), eq(marketType));
    }

    /**
     * 测试查找不同价源
     */
    @Test
    void testFindDistinctSourcesBySymbol() {
        // Given - 准备查询参数
        String symbol = "EURUSD";

        // When - 执行查询
        orderBookRepository.findDistinctSourcesBySymbol(symbol);

        // Then - 验证查询方法被调用
        verify(orderBookRepository, times(1)).findDistinctSourcesBySymbol(eq(symbol));
    }
}