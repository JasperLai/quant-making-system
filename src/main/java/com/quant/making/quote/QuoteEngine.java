package com.quant.making.quote;

import com.quant.making.book.OrderBook;
import com.quant.making.book.OrderBookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 报价引擎核心
 * 基于订单簿计算最优报价、点差，并生成报价对象
 */
@Component
public class QuoteEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(QuoteEngine.class);
    
    @Autowired
    protected OrderBookService orderBookService;
    
    // 默认报价有效期（秒）
    private static final int DEFAULT_QUOTE_VALIDITY_SECONDS = 5;
    
    // 默认点差缓冲（用于防止穿价）
    private static final BigDecimal DEFAULT_SPREAD_BUFFER = new BigDecimal("0.00001");
    
    // 报价计数器，用于生成报价序号
    private final AtomicLong quoteCounter = new AtomicLong(0);
    
    // 缓存的最新报价: symbol -> [buyQuote, sellQuote]
    private final Map<String, Quote[]> latestQuotesCache = new ConcurrentHashMap<>();
    
    // 缓存的档位报价: symbol -> List<Quote>
    private final Map<String, List<Quote>> levelQuotesCache = new ConcurrentHashMap<>();
    
    // 配置的点差缓冲: symbol -> spread buffer
    private final Map<String, BigDecimal> spreadBufferConfig = new ConcurrentHashMap<>();
    
    /**
     * 计算并生成最优报价
     * 
     * @param symbol 品种代码
     * @param marketType 市场类型
     * @return 包含最优买价和卖价的数组 [buyQuote, sellQuote]
     */
    public Quote[] generateBestQuotes(String symbol, Integer marketType) {
        logger.debug("Generating best quotes for symbol: {}", symbol);
        
        OrderBook orderBook = orderBookService.getOrderBook(symbol);
        if (orderBook == null) {
            logger.warn("OrderBook not found for symbol: {}", symbol);
            return null;
        }
        
        OrderBook.PriceLevel bestBid = orderBook.getBestBid();
        OrderBook.PriceLevel bestAsk = orderBook.getBestAsk();
        
        if (bestBid == null || bestAsk == null) {
            logger.warn("Insufficient liquidity for symbol: {}", symbol);
            return null;
        }
        
        // 计算点差
        BigDecimal rawSpread = bestAsk.getPrice().subtract(bestBid.getPrice());
        BigDecimal spreadBuffer = getSpreadBuffer(symbol);
        BigDecimal effectiveSpread = rawSpread.compareTo(BigDecimal.ZERO) > 0 
                ? rawSpread : spreadBuffer;
        
        // 生成买方报价（最优买价）
        Quote buyQuote = createQuote(
                symbol, marketType, Quote.BUY,
                bestBid.getPrice(), bestBid.getTotalBuyQty(),
                0, effectiveSpread, "ENGINE"
        );
        
        // 生成卖方报价（最优卖价）
        Quote sellQuote = createQuote(
                symbol, marketType, Quote.SELL,
                bestAsk.getPrice(), bestAsk.getTotalSellQty(),
                0, effectiveSpread, "ENGINE"
        );
        
        // 缓存最新报价
        latestQuotesCache.put(symbol, new Quote[]{buyQuote, sellQuote});
        
        logger.info("Best quotes generated for {}: Bid={}, Ask={}, Spread={}", 
                symbol, buyQuote.getPrice(), sellQuote.getPrice(), effectiveSpread);
        
        return new Quote[]{buyQuote, sellQuote};
    }
    
    /**
     * 生成多档位报价
     * 
     * @param symbol 品种代码
     * @param marketType 市场类型
     * @param levels 档位数（0表示所有档位）
     * @return 报价列表
     */
    public List<Quote> generateLevelQuotes(String symbol, Integer marketType, int levels) {
        logger.debug("Generating {} level quotes for symbol: {}", levels, symbol);
        
        OrderBook orderBook = orderBookService.getOrderBook(symbol);
        if (orderBook == null) {
            logger.warn("OrderBook not found for symbol: {}", symbol);
            return new ArrayList<>();
        }
        
        List<Quote> quotes = new ArrayList<>();
        BigDecimal spreadBuffer = getSpreadBuffer(symbol);
        
        // 获取排序后的价格档位
        Map<BigDecimal, OrderBook.PriceLevel> levelsMap = orderBook.getAggregatedLevels();
        
        // 收集所有买价档位
        List<Map.Entry<BigDecimal, OrderBook.PriceLevel>> bidLevels = new ArrayList<>();
        // 收集所有卖价档位
        List<Map.Entry<BigDecimal, OrderBook.PriceLevel>> askLevels = new ArrayList<>();
        
        for (Map.Entry<BigDecimal, OrderBook.PriceLevel> entry : levelsMap.entrySet()) {
            if (entry.getValue().hasBuyOrders()) {
                bidLevels.add(entry);
            }
            if (entry.getValue().hasSellOrders()) {
                askLevels.add(entry);
            }
        }
        
        // 按价格排序：买价从高到低，卖价从低到高
        bidLevels.sort((a, b) -> b.getKey().compareTo(a.getKey()));
        askLevels.sort((a, b) -> a.getKey().compareTo(b.getKey()));
        
        int bidCount = levels > 0 ? Math.min(levels, bidLevels.size()) : bidLevels.size();
        int askCount = levels > 0 ? Math.min(levels, askLevels.size()) : askLevels.size();
        
        // 生成买方多档报价
        for (int i = 0; i < bidCount; i++) {
            Map.Entry<BigDecimal, OrderBook.PriceLevel> level = bidLevels.get(i);
            Quote quote = createQuote(
                    symbol, marketType, Quote.BUY,
                    level.getKey(), level.getValue().getTotalBuyQty(),
                    i, null, "ENGINE"
            );
            quotes.add(quote);
        }
        
        // 生成卖方多档报价
        for (int i = 0; i < askCount; i++) {
            Map.Entry<BigDecimal, OrderBook.PriceLevel> level = askLevels.get(i);
            Quote quote = createQuote(
                    symbol, marketType, Quote.SELL,
                    level.getKey(), level.getValue().getTotalSellQty(),
                    i, null, "ENGINE"
            );
            quotes.add(quote);
        }
        
        // 缓存档位报价
        levelQuotesCache.put(symbol, new ArrayList<>(quotes));
        
        logger.info("Generated {} level quotes for {}", quotes.size(), symbol);
        return quotes;
    }
    
    /**
     * 计算点差
     * 
     * @param symbol 品种代码
     * @return 点差值，如果无法计算则返回null
     */
    public BigDecimal calculateSpread(String symbol) {
        OrderBook orderBook = orderBookService.getOrderBook(symbol);
        if (orderBook == null) {
            return null;
        }
        
        OrderBook.PriceLevel bestBid = orderBook.getBestBid();
        OrderBook.PriceLevel bestAsk = orderBook.getBestAsk();
        
        if (bestBid == null || bestAsk == null) {
            return null;
        }
        
        return bestAsk.getPrice().subtract(bestBid.getPrice());
    }
    
    /**
     * 计算相对点差（以点数表示）
     * 
     * @param symbol 品种代码
     * @param pipSize 每点大小
     * @return 相对点差值
     */
    public BigDecimal calculatePipSpread(String symbol, BigDecimal pipSize) {
        BigDecimal spread = calculateSpread(symbol);
        if (spread == null || pipSize.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        
        return spread.divide(pipSize, 4, RoundingMode.HALF_UP);
    }
    
    /**
     * 计算中间价
     * 
     * @param symbol 品种代码
     * @return 中间价，如果无法计算则返回null
     */
    public BigDecimal calculateMidPrice(String symbol) {
        OrderBook orderBook = orderBookService.getOrderBook(symbol);
        if (orderBook == null) {
            return null;
        }
        
        OrderBook.PriceLevel bestBid = orderBook.getBestBid();
        OrderBook.PriceLevel bestAsk = orderBook.getBestAsk();
        
        if (bestBid == null || bestAsk == null) {
            return null;
        }
        
        return bestBid.getPrice().add(bestAsk.getPrice())
                .divide(new BigDecimal("2"), 8, RoundingMode.HALF_UP);
    }
    
    /**
     * 创建报价对象
     */
    private Quote createQuote(String symbol, Integer marketType, Integer side,
                               BigDecimal price, BigDecimal quantity,
                               Integer level, BigDecimal spread, String source) {
        Quote quote = new Quote(symbol, marketType, side, price, quantity);
        quote.setLevel(level);
        quote.setSpread(spread);
        quote.setSource(source);
        quote.setValidityDuration(DEFAULT_QUOTE_VALIDITY_SECONDS);
        quote.activate();
        return quote;
    }
    
    /**
     * 获取缓存的最优报价
     * 
     * @param symbol 品种代码
     * @return 包含最优买价和卖价的数组 [buyQuote, sellQuote]
     */
    public Quote[] getLatestQuotes(String symbol) {
        return latestQuotesCache.get(symbol);
    }
    
    /**
     * 获取缓存的档位报价
     * 
     * @param symbol 品种代码
     * @return 档位报价列表
     */
    public List<Quote> getLevelQuotes(String symbol) {
        return levelQuotesCache.get(symbol);
    }
    
    /**
     * 设置品种的点差缓冲
     * 
     * @param symbol 品种代码
     * @param buffer 点差缓冲值
     */
    public void setSpreadBuffer(String symbol, BigDecimal buffer) {
        spreadBufferConfig.put(symbol, buffer);
    }
    
    /**
     * 获取品种的点差缓冲
     */
    private BigDecimal getSpreadBuffer(String symbol) {
        return spreadBufferConfig.getOrDefault(symbol, DEFAULT_SPREAD_BUFFER);
    }
    
    /**
     * 刷新报价计数器
     */
    public long getNextQuoteSequence() {
        return quoteCounter.incrementAndGet();
    }
    
    /**
     * 定时任务：检查报价过期
     */
    @Scheduled(fixedRate = 1000)  // 每秒执行一次
    public void checkQuoteExpiry() {
        LocalDateTime now = LocalDateTime.now();
        
        // 检查最新报价过期
        for (Map.Entry<String, Quote[]> entry : latestQuotesCache.entrySet()) {
            Quote[] quotes = entry.getValue();
            if (quotes != null) {
                for (Quote quote : quotes) {
                    if (quote != null && !quote.isValid()) {
                        logger.debug("Quote expired: {}", quote.getQuoteId());
                    }
                }
            }
        }
    }
    
    /**
     * 清理报价缓存
     * 
     * @param symbol 品种代码，为null则清理所有
     */
    public void clearCache(String symbol) {
        if (symbol == null) {
            latestQuotesCache.clear();
            levelQuotesCache.clear();
            logger.info("Cleared all quote caches");
        } else {
            latestQuotesCache.remove(symbol);
            levelQuotesCache.remove(symbol);
            logger.info("Cleared quote cache for symbol: {}", symbol);
        }
    }
    
    /**
     * 检查是否有足够的流动性生成报价
     * 
     * @param symbol 品种代码
     * @return 是否有足够流动性
     */
    public boolean hasSufficientLiquidity(String symbol) {
        OrderBook orderBook = orderBookService.getOrderBook(symbol);
        if (orderBook == null) {
            return false;
        }
        
        return orderBook.getBestBid() != null && orderBook.getBestAsk() != null;
    }
    
    /**
     * 获取最优买方报价价格
     */
    public BigDecimal getBestBidPrice(String symbol) {
        OrderBook.PriceLevel bestBid = orderBookService.getBestBid(symbol);
        return bestBid != null ? bestBid.getPrice() : null;
    }
    
    /**
     * 获取最优卖方报价价格
     */
    public BigDecimal getBestAskPrice(String symbol) {
        OrderBook.PriceLevel bestAsk = orderBookService.getBestAsk(symbol);
        return bestAsk != null ? bestAsk.getPrice() : null;
    }
    
    /**
     * 设置默认报价有效期
     */
    public void setDefaultValiditySeconds(int seconds) {
        // 这里可以扩展为配置项
    }
}
