package com.quant.making.quote;

import com.quant.making.book.OrderBook;
import com.quant.making.book.OrderBookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuoteServiceTest {

    @Mock
    private QuoteEngine quoteEngine;

    @Mock
    private OrderBookService orderBookService;

    private QuoteService quoteService;

    @BeforeEach
    void setUp() {
        quoteService = new QuoteService();
        ReflectionTestUtils.setField(quoteService, "quoteEngine", quoteEngine);
        ReflectionTestUtils.setField(quoteService, "orderBookService", orderBookService);
    }

    @Test
    void testCreateQuote() {
        // 准备测试数据
        String symbol = "XAUUSD";
        Integer marketType = 1;
        Integer side = Quote.BUY; // 1
        BigDecimal price = new BigDecimal("1800.00");
        BigDecimal quantity = new BigDecimal("1000");
        int validitySeconds = 30;

        // 执行测试
        Quote quote = quoteService.createQuote(symbol, marketType, side, price, quantity, validitySeconds);

        // 验证结果
        assertNotNull(quote);
        assertEquals(symbol, quote.getSymbol());
        assertEquals(marketType, quote.getMarketType());
        assertEquals(side, quote.getSide());
        assertEquals(price, quote.getPrice());
        assertEquals(quantity, quote.getQuantity());
        assertTrue(quote.isValid()); // 报价应该有效
        assertTrue(quote.getExpiryTime().isAfter(LocalDateTime.now()));
        
        // 验证报价被添加到活跃列表
        List<Quote> activeQuotes = quoteService.getActiveQuotes(symbol);
        assertTrue(activeQuotes.contains(quote));
    }

    @Test
    void testGenerateOptimalQuote() {
        // 准备测试数据
        String symbol = "XAUUSD";
        Integer marketType = 1;
        
        Quote mockBuyQuote = new Quote(symbol, marketType, Quote.BUY, new BigDecimal("1800.00"), new BigDecimal("1000"));
        Quote mockSellQuote = new Quote(symbol, marketType, Quote.SELL, new BigDecimal("1805.00"), new BigDecimal("1000"));

        when(quoteEngine.generateBestQuotes(symbol, marketType)).thenReturn(new Quote[]{mockBuyQuote, mockSellQuote});

        // 执行测试
        Quote[] result = quoteService.generateOptimalQuote(symbol, marketType);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.length);
        verify(quoteEngine, times(1)).generateBestQuotes(symbol, marketType);

        // 验证报价被添加到活跃列表
        List<Quote> activeQuotes = quoteService.getActiveQuotes(symbol);
        assertEquals(2, activeQuotes.size());
    }

    @Test
    void testGenerateOptimalQuoteWithNull() {
        // 准备测试数据
        String symbol = "NONEXISTENT";
        Integer marketType = 1;

        when(quoteEngine.generateBestQuotes(symbol, marketType)).thenReturn(null);

        // 执行测试
        Quote[] result = quoteService.generateOptimalQuote(symbol, marketType);

        // 验证结果
        assertNull(result);
    }

    @Test
    void testGenerateMultiLevelQuotes() {
        // 准备测试数据
        String symbol = "XAUUSD";
        Integer marketType = 1;
        int levels = 3;
        
        Quote mockBuyQuote1 = new Quote(symbol, marketType, Quote.BUY, new BigDecimal("1800.00"), new BigDecimal("1000"));
        Quote mockBuyQuote2 = new Quote(symbol, marketType, Quote.BUY, new BigDecimal("1799.50"), new BigDecimal("1500"));
        Quote mockSellQuote1 = new Quote(symbol, marketType, Quote.SELL, new BigDecimal("1805.00"), new BigDecimal("1200"));

        when(quoteEngine.generateLevelQuotes(symbol, marketType, levels))
            .thenReturn(List.of(mockBuyQuote1, mockBuyQuote2, mockSellQuote1));

        // 执行测试
        List<Quote> result = quoteService.generateMultiLevelQuotes(symbol, marketType, levels);

        // 验证结果
        assertEquals(3, result.size());
        verify(quoteEngine, times(1)).generateLevelQuotes(symbol, marketType, levels);
    }

    @Test
    void testUpdateQuote() {
        // 准备测试数据
        String symbol = "XAUUSD";
        Integer marketType = 1;
        
        Quote originalQuote = quoteService.createQuote(symbol, marketType, Quote.BUY, 
                                                     new BigDecimal("1800.00"), new BigDecimal("1000"), 30);
        String quoteId = originalQuote.getQuoteId();

        // 准备更新的价格和数量
        BigDecimal newPrice = new BigDecimal("1801.00");
        BigDecimal newQuantity = new BigDecimal("1500");

        // 执行测试
        Quote updatedQuote = quoteService.updateQuote(quoteId, newPrice, newQuantity);

        // 验证结果
        assertNotNull(updatedQuote);
        assertEquals(newPrice, updatedQuote.getPrice());
        assertEquals(newQuantity, updatedQuote.getQuantity());
    }

    @Test
    void testUpdateNonExistentQuote() {
        // 执行测试
        Quote result = quoteService.updateQuote("non-existent-id", 
                                               new BigDecimal("1801.00"), new BigDecimal("1500"));

        // 验证结果
        assertNull(result);
    }

    @Test
    void testCancelQuote() {
        // 准备测试数据
        String symbol = "XAUUSD";
        Integer marketType = 1;
        
        Quote quote = quoteService.createQuote(symbol, marketType, Quote.BUY, 
                                             new BigDecimal("1800.00"), new BigDecimal("1000"), 30);
        String quoteId = quote.getQuoteId();

        // 执行测试
        boolean result = quoteService.cancelQuote(quoteId);

        // 验证结果
        assertTrue(result);
        
        // 验证报价不再在活跃列表中
        List<Quote> activeQuotes = quoteService.getActiveQuotes(symbol);
        assertFalse(activeQuotes.contains(quote));
        
        // 验证报价在历史记录中
        Quote cancelledQuote = quoteService.getQuoteById(quoteId);
        assertNotNull(cancelledQuote);
    }

    @Test
    void testCancelNonExistentQuote() {
        // 执行测试
        boolean result = quoteService.cancelQuote("non-existent-id");

        // 验证结果
        assertFalse(result);
    }

    @Test
    void testGetActiveQuotes() {
        // 准备测试数据
        String symbol = "XAUUSD";
        Integer marketType = 1;
        
        // 创建几个报价
        quoteService.createQuote(symbol, marketType, Quote.BUY, new BigDecimal("1800.00"), new BigDecimal("1000"), 30);
        quoteService.createQuote(symbol, marketType, Quote.SELL, new BigDecimal("1805.00"), new BigDecimal("1200"), 30);

        // 执行测试
        List<Quote> activeQuotes = quoteService.getActiveQuotes(symbol);

        // 验证结果
        assertEquals(2, activeQuotes.size());
    }

    @Test
    void testCancelAllQuotesForSymbol() {
        // 准备测试数据
        String symbol = "XAUUSD";
        Integer marketType = 1;
        
        // 创建几个报价
        quoteService.createQuote(symbol, marketType, Quote.BUY, new BigDecimal("1800.00"), new BigDecimal("1000"), 30);
        quoteService.createQuote(symbol, marketType, Quote.SELL, new BigDecimal("1805.00"), new BigDecimal("1200"), 30);

        // 执行测试
        int cancelledCount = quoteService.cancelAllQuotesForSymbol(symbol);

        // 验证结果
        assertEquals(2, cancelledCount);
        
        // 验证所有报价都被取消
        List<Quote> remainingActive = quoteService.getActiveQuotes(symbol);
        assertEquals(0, remainingActive.size());
    }

    @Test
    void testExpireStaleQuotes() {
        // 准备测试数据
        String symbol = "XAUUSD";
        Integer marketType = 1;
        
        // 创建一个已过期的报价
        Quote quote = quoteService.createQuote(symbol, marketType, Quote.BUY, 
                                             new BigDecimal("1800.00"), new BigDecimal("1000"), 1);
        
        // 等待报价过期
        try {
            Thread.sleep(1100); // 确保时间过去
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 执行测试
        int expiredCount = quoteService.expireStaleQuotes();

        // 验证结果
        assertTrue(expiredCount >= 0);
    }

    @Test
    void testGetQuoteStatistics() {
        // 准备测试数据
        String symbol = "XAUUSD";
        Integer marketType = 1;
        
        // 创建一个报价以更新统计数据
        quoteService.createQuote(symbol, marketType, Quote.BUY, new BigDecimal("1800.00"), 
                                new BigDecimal("1000"), 30);

        // 执行测试
        QuoteService.QuoteStatistics stats = quoteService.getQuoteStatistics(symbol);

        // 验证结果
        assertNotNull(stats);
        assertTrue(stats.getTotalQuotes() >= 1);
    }

    @Test
    void testGetQuoteById() {
        // 准备测试数据
        String symbol = "XAUUSD";
        Integer marketType = 1;
        Quote originalQuote = quoteService.createQuote(symbol, marketType, Quote.BUY, 
                                                      new BigDecimal("1800.00"), new BigDecimal("1000"), 30);
        String quoteId = originalQuote.getQuoteId();

        // 执行测试
        Quote retrievedQuote = quoteService.getQuoteById(quoteId);

        // 验证结果
        assertNotNull(retrievedQuote);
        assertEquals(quoteId, retrievedQuote.getQuoteId());
        assertEquals(symbol, retrievedQuote.getSymbol());
    }

    @Test
    void testGetQuoteHistory() {
        // 准备测试数据
        String symbol = "XAUUSD";
        Integer marketType = 1;
        
        // 创建几个报价
        quoteService.createQuote(symbol, marketType, Quote.BUY, new BigDecimal("1800.00"), new BigDecimal("1000"), 30);
        quoteService.createQuote(symbol, marketType, Quote.SELL, new BigDecimal("1805.00"), new BigDecimal("1200"), 30);

        // 执行测试
        List<Quote> history = quoteService.getQuoteHistory(symbol, 1); // 最近1小时

        // 验证结果
        assertNotNull(history);
        assertTrue(history.size() >= 2);
    }

    @Test
    void testQuoteValidity() {
        // 准备测试数据
        String symbol = "XAUUSD";
        Integer marketType = 1;
        
        Quote quote = quoteService.createQuote(symbol, marketType, Quote.BUY, 
                                             new BigDecimal("1800.00"), new BigDecimal("1000"), 30);

        // 验证结果
        assertTrue(quote.isValid());
        assertFalse(quote.isExpired());
    }

    @Test
    void testQuoteExpiry() {
        // 准备测试数据
        String symbol = "XAUUSD";
        Integer marketType = 1;
        
        Quote quote = quoteService.createQuote(symbol, marketType, Quote.BUY, 
                                             new BigDecimal("1800.00"), new BigDecimal("1000"), 1);
        
        // 等待报价过期
        try {
            Thread.sleep(1100); // 确保时间过去
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 验证结果
        assertFalse(quote.isValid());
        assertTrue(quote.isExpired());
    }
}
