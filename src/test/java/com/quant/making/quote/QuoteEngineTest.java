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
class QuoteEngineTest {

    @Mock
    private OrderBookService orderBookService;

    private QuoteEngine quoteEngine;

    @BeforeEach
    void setUp() {
        quoteEngine = new QuoteEngine();
        ReflectionTestUtils.setField(quoteEngine, "orderBookService", orderBookService);
    }

    @Test
    void testGenerateBestQuotes() {
        // 准备测试数据
        String symbol = "XAUUSD";
        Integer marketType = 1;
        
        OrderBook mockOrderBook = new OrderBook(symbol, marketType);
        mockOrderBook.addQuote("source1", OrderBook.BUY, new BigDecimal("1800.00"), new BigDecimal("1000"));
        mockOrderBook.addQuote("source1", OrderBook.SELL, new BigDecimal("1805.00"), new BigDecimal("1000"));

        when(orderBookService.getOrderBook(symbol)).thenReturn(mockOrderBook);

        // 执行测试
        Quote[] quotes = quoteEngine.generateBestQuotes(symbol, marketType);

        // 验证结果
        assertNotNull(quotes);
        assertEquals(2, quotes.length);
        
        Quote buyQuote = quotes[0];
        Quote sellQuote = quotes[1];
        
        assertNotNull(buyQuote);
        assertNotNull(sellQuote);
        assertEquals(symbol, buyQuote.getSymbol());
        assertEquals(symbol, sellQuote.getSymbol());
        assertEquals(OrderBook.BUY, buyQuote.getSide());
        assertEquals(OrderBook.SELL, sellQuote.getSide());
    }

    @Test
    void testGenerateBestQuotesWithNullOrderBook() {
        // 准备测试数据
        String symbol = "NONEXISTENT";
        Integer marketType = 1;

        when(orderBookService.getOrderBook(symbol)).thenReturn(null);

        // 执行测试
        Quote[] quotes = quoteEngine.generateBestQuotes(symbol, marketType);

        // 验证结果
        assertNull(quotes);
    }

    @Test
    void testCalculateSpread() {
        // 准备测试数据
        String symbol = "XAUUSD";
        OrderBook mockOrderBook = new OrderBook(symbol, 1);
        mockOrderBook.addQuote("source1", OrderBook.BUY, new BigDecimal("1800.00"), new BigDecimal("1000"));
        mockOrderBook.addQuote("source1", OrderBook.SELL, new BigDecimal("1805.00"), new BigDecimal("1000"));

        when(orderBookService.getOrderBook(symbol)).thenReturn(mockOrderBook);

        // 执行测试
        BigDecimal spread = quoteEngine.calculateSpread(symbol);

        // 验证结果
        assertNotNull(spread);
        assertEquals(new BigDecimal("5.00"), spread);
    }

    @Test
    void testCalculateSpreadWithNullValues() {
        // 准备测试数据
        String symbol = "NONEXISTENT";

        when(orderBookService.getOrderBook(symbol)).thenReturn(null);

        // 执行测试
        BigDecimal spread = quoteEngine.calculateSpread(symbol);

        // 验证结果
        assertNull(spread);
    }

    @Test
    void testGenerateLevelQuotes() {
        // 准备测试数据
        String symbol = "XAUUSD";
        Integer marketType = 1;
        int levels = 2;
        
        OrderBook mockOrderBook = new OrderBook(symbol, marketType);
        mockOrderBook.addQuote("source1", OrderBook.BUY, new BigDecimal("1800.00"), new BigDecimal("1000"));
        mockOrderBook.addQuote("source1", OrderBook.BUY, new BigDecimal("1799.50"), new BigDecimal("1500"));
        mockOrderBook.addQuote("source1", OrderBook.SELL, new BigDecimal("1805.00"), new BigDecimal("1200"));
        mockOrderBook.addQuote("source1", OrderBook.SELL, new BigDecimal("1805.50"), new BigDecimal("800"));

        when(orderBookService.getOrderBook(symbol)).thenReturn(mockOrderBook);

        // 执行测试
        List<Quote> quotes = quoteEngine.generateLevelQuotes(symbol, marketType, levels);

        // 验证结果
        assertNotNull(quotes);
        assertTrue(quotes.size() >= 2); // 至少有2个报价（一个买一个卖）
        
        // 检查是否有买价和卖价
        boolean hasBuy = quotes.stream().anyMatch(q -> q.getSide() == OrderBook.BUY);
        boolean hasSell = quotes.stream().anyMatch(q -> q.getSide() == OrderBook.SELL);
        
        assertTrue(hasBuy);
        assertTrue(hasSell);
    }

    @Test
    void testCalculateMidPrice() {
        // 准备测试数据
        String symbol = "XAUUSD";
        OrderBook mockOrderBook = new OrderBook(symbol, 1);
        mockOrderBook.addQuote("source1", OrderBook.BUY, new BigDecimal("1800.00"), new BigDecimal("1000"));
        mockOrderBook.addQuote("source1", OrderBook.SELL, new BigDecimal("1805.00"), new BigDecimal("1000"));

        when(orderBookService.getOrderBook(symbol)).thenReturn(mockOrderBook);

        // 执行测试
        BigDecimal midPrice = quoteEngine.calculateMidPrice(symbol);

        // 验证结果
        assertNotNull(midPrice);
        assertEquals(new BigDecimal("1802.50000000"), midPrice);
    }

    @Test
    void testCalculatePipSpread() {
        // 准备测试数据
        String symbol = "XAUUSD";
        OrderBook mockOrderBook = new OrderBook(symbol, 1);
        mockOrderBook.addQuote("source1", OrderBook.BUY, new BigDecimal("1800.00"), new BigDecimal("1000"));
        mockOrderBook.addQuote("source1", OrderBook.SELL, new BigDecimal("1805.00"), new BigDecimal("1000"));

        when(orderBookService.getOrderBook(symbol)).thenReturn(mockOrderBook);

        // 执行测试
        BigDecimal pipSpread = quoteEngine.calculatePipSpread(symbol, new BigDecimal("0.01"));

        // 验证结果
        assertNotNull(pipSpread);
        assertEquals(new BigDecimal("500.0000"), pipSpread);
    }

    @Test
    void testHasSufficientLiquidity() {
        // 准备测试数据
        String symbol = "XAUUSD";
        OrderBook mockOrderBook = new OrderBook(symbol, 1);
        mockOrderBook.addQuote("source1", OrderBook.BUY, new BigDecimal("1800.00"), new BigDecimal("1000"));
        mockOrderBook.addQuote("source1", OrderBook.SELL, new BigDecimal("1805.00"), new BigDecimal("1000"));

        when(orderBookService.getOrderBook(symbol)).thenReturn(mockOrderBook);

        // 执行测试
        boolean hasLiquidity = quoteEngine.hasSufficientLiquidity(symbol);

        // 验证结果
        assertTrue(hasLiquidity);
    }

    @Test
    void testHasInsufficientLiquidity() {
        // 准备测试数据
        String symbol = "XAUUSD";

        when(orderBookService.getOrderBook(symbol)).thenReturn(null);

        // 执行测试
        boolean hasLiquidity = quoteEngine.hasSufficientLiquidity(symbol);

        // 验证结果
        assertFalse(hasLiquidity);
    }

    @Test
    void testGetBestBidPrice() {
        // 准备测试数据
        String symbol = "XAUUSD";
        OrderBook.PriceLevel mockBid = new OrderBook.PriceLevel(new BigDecimal("1800.00"));

        when(orderBookService.getBestBid(symbol)).thenReturn(mockBid);

        // 执行测试
        BigDecimal bestBidPrice = quoteEngine.getBestBidPrice(symbol);

        // 验证结果
        assertEquals(new BigDecimal("1800.00"), bestBidPrice);
    }

    @Test
    void testGetBestAskPrice() {
        // 准备测试数据
        String symbol = "XAUUSD";
        OrderBook.PriceLevel mockAsk = new OrderBook.PriceLevel(new BigDecimal("1805.00"));

        when(orderBookService.getBestAsk(symbol)).thenReturn(mockAsk);

        // 执行测试
        BigDecimal bestAskPrice = quoteEngine.getBestAskPrice(symbol);

        // 验证结果
        assertEquals(new BigDecimal("1805.00"), bestAskPrice);
    }

    @Test
    void testSetAndGetSpreadBuffer() {
        // 执行测试
        quoteEngine.setSpreadBuffer("XAUUSD", new BigDecimal("0.0001"));
        
        // 验证结果
        // 由于是内部方法，我们通过生成报价来验证
        String symbol = "XAUUSD";
        Integer marketType = 1;
        
        OrderBook mockOrderBook = new OrderBook(symbol, marketType);
        mockOrderBook.addQuote("source1", OrderBook.BUY, new BigDecimal("1800.00"), new BigDecimal("1000"));
        mockOrderBook.addQuote("source1", OrderBook.SELL, new BigDecimal("1805.00"), new BigDecimal("1000"));

        when(orderBookService.getOrderBook(symbol)).thenReturn(mockOrderBook);

        Quote[] quotes = quoteEngine.generateBestQuotes(symbol, marketType);
        assertNotNull(quotes);
    }

    @Test
    void testClearCache() {
        // 执行测试 - 清理特定品种缓存
        quoteEngine.clearCache("XAUUSD");
        
        // 清理所有缓存
        quoteEngine.clearCache(null);
        
        // 验证结果 - 不应抛出异常
        assertDoesNotThrow(() -> quoteEngine.clearCache("TEST"));
    }

    @Test
    void testGetNextQuoteSequence() {
        // 执行测试
        long seq1 = quoteEngine.getNextQuoteSequence();
        long seq2 = quoteEngine.getNextQuoteSequence();
        
        // 验证结果
        assertEquals(seq1 + 1, seq2);
    }
}
