package com.quant.making.quote;

import com.quant.making.book.OrderBookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 报价服务
 * 管理报价的创建、更新、取消以及历史记录
 */
@Service
public class QuoteService {
    
    private static final Logger logger = LoggerFactory.getLogger(QuoteService.class);
    
    @Autowired
    private QuoteEngine quoteEngine;
    
    @Autowired
    private OrderBookService orderBookService;
    
    // 当前活跃报价缓存: symbol -> List<Quote>
    private final Map<String, List<Quote>> activeQuotes = new ConcurrentHashMap<>();
    
    // 报价历史记录: quoteId -> Quote
    private final Map<String, Quote> quoteHistory = new ConcurrentHashMap<>();
    
    // 报价统计: symbol -> QuoteStatistics
    private final Map<String, QuoteStatistics> quoteStats = new ConcurrentHashMap<>();
    
    /**
     * 创建新报价
     *
     * @param symbol 品种代码
     * @param marketType 市场类型
     * @param side 买卖方向 (1=BUY, 2=SELL)
     * @param price 报价价格
     * @param quantity 报价数量
     * @param validitySeconds 有效期（秒）
     * @return 创建的报价对象
     */
    public Quote createQuote(String symbol, Integer marketType, Integer side, BigDecimal price, 
                            BigDecimal quantity, int validitySeconds) {
        Quote quote = new Quote(symbol, marketType, side, price, quantity);
        quote.setValidityDuration(validitySeconds);
        quote.activate(); // 激活报价
        
        // 添加到活跃报价列表
        activeQuotes.computeIfAbsent(symbol, k -> new ArrayList<>()).add(quote);
        
        // 更新统计信息
        updateQuoteStatistics(symbol, quote);
        
        logger.info("Created quote: ID={}, Symbol={}, MarketType={}, Side={}, Price={}, Quantity={}", 
                   quote.getQuoteId(), symbol, marketType, side, price, quantity);
        
        return quote;
    }
    
    /**
     * 基于订单簿生成最优报价
     *
     * @param symbol 品种代码
     * @param marketType 市场类型
     * @return 生成的报价数组 [buyQuote, sellQuote]
     */
    public Quote[] generateOptimalQuote(String symbol, Integer marketType) {
        Quote[] quotes = quoteEngine.generateBestQuotes(symbol, marketType);
        
        if (quotes != null) {
            // 添加到活跃报价列表
            List<Quote> existingQuotes = activeQuotes.computeIfAbsent(symbol, k -> new ArrayList<>());
            for (Quote quote : quotes) {
                if (quote != null) {
                    existingQuotes.add(quote);
                    updateQuoteStatistics(symbol, quote);
                }
            }
            
            logger.info("Generated optimal quotes for symbol: {}", symbol);
            return quotes;
        }
        
        logger.warn("Could not generate optimal quotes for symbol: {}", symbol);
        return null;
    }
    
    /**
     * 批量生成多档位报价
     *
     * @param symbol 品种代码
     * @param marketType 市场类型
     * @param levels 档位数
     * @return 生成的报价列表
     */
    public List<Quote> generateMultiLevelQuotes(String symbol, Integer marketType, int levels) {
        List<Quote> quotes = quoteEngine.generateLevelQuotes(symbol, marketType, levels);
        
        if (quotes.isEmpty()) {
            logger.warn("Could not generate multi-level quotes for symbol: {}", symbol);
            return quotes;
        }
        
        // 添加到活跃报价列表
        List<Quote> existingQuotes = activeQuotes.computeIfAbsent(symbol, k -> new ArrayList<>());
        existingQuotes.addAll(quotes);
        
        // 更新统计信息
        for (Quote quote : quotes) {
            updateQuoteStatistics(symbol, quote);
        }
        
        logger.info("Generated {} multi-level quotes for symbol: {}", quotes.size(), symbol);
        
        return quotes;
    }
    
    /**
     * 更新报价
     * 仅允许在特定条件下更新报价
     *
     * @param quoteId 报价ID
     * @param newPrice 新价格
     * @param newQuantity 新数量
     * @return 更新后的报价对象
     */
    public Quote updateQuote(String quoteId, BigDecimal newPrice, BigDecimal newQuantity) {
        Quote quote = findActiveQuoteById(quoteId);
        if (quote == null) {
            logger.warn("Quote not found for update: ID={}", quoteId);
            return null;
        }
        
        // 检查报价是否过期
        if (quote.isExpired()) {
            logger.warn("Cannot update expired quote: ID={}", quoteId);
            return null;
        }
        
        // 保存旧值用于日志
        BigDecimal oldPrice = quote.getPrice();
        BigDecimal oldQuantity = quote.getQuantity();
        
        // 更新报价信息
        quote.setPrice(newPrice);
        quote.setQuantity(newQuantity);
        quote.setUpdateTime(LocalDateTime.now());
        
        logger.info("Updated quote: ID={}, OldPrice={}, NewPrice={}, OldQuantity={}, NewQuantity={}", 
                   quoteId, oldPrice, newPrice, oldQuantity, newQuantity);
        
        return quote;
    }
    
    /**
     * 取消报价
     *
     * @param quoteId 报价ID
     * @return 是否取消成功
     */
    public boolean cancelQuote(String quoteId) {
        Quote quote = findActiveQuoteById(quoteId);
        if (quote == null) {
            logger.warn("Quote not found for cancellation: ID={}", quoteId);
            return false;
        }
        
        // 从活跃报价列表中移除
        removeQuoteFromActiveList(quote);
        
        // 添加到历史记录
        quoteHistory.put(quote.getQuoteId(), quote);
        
        logger.info("Cancelled quote: ID={}", quoteId);
        
        return true;
    }
    
    /**
     * 取消指定品种的所有报价
     *
     * @param symbol 品种代码
     * @return 取消的报价数量
     */
    public int cancelAllQuotesForSymbol(String symbol) {
        List<Quote> symbolQuotes = activeQuotes.get(symbol);
        if (symbolQuotes == null || symbolQuotes.isEmpty()) {
            logger.info("No active quotes to cancel for symbol: {}", symbol);
            return 0;
        }
        
        int cancelledCount = 0;
        synchronized (symbolQuotes) {
            for (Quote quote : symbolQuotes) {
                quoteHistory.put(quote.getQuoteId(), quote);
                cancelledCount++;
                
                logger.info("Cancelled quote for symbol {}: ID={}", symbol, quote.getQuoteId());
            }
            
            // 清空活跃列表
            symbolQuotes.clear();
        }
        
        logger.info("Cancelled {} quotes for symbol: {}", cancelledCount, symbol);
        return cancelledCount;
    }
    
    /**
     * 获取活跃报价
     *
     * @param symbol 品种代码
     * @return 活跃报价列表
     */
    public List<Quote> getActiveQuotes(String symbol) {
        List<Quote> quotes = activeQuotes.getOrDefault(symbol, new ArrayList<>());
        return quotes.stream()
                .filter(quote -> !quote.isExpired())
                .collect(Collectors.toList());
    }
    
    /**
     * 获取指定报价ID的报价
     *
     * @param quoteId 报价ID
     * @return 报价对象，如果不存在则返回null
     */
    public Quote getQuoteById(String quoteId) {
        // 首先检查活跃报价
        Quote activeQuote = findActiveQuoteById(quoteId);
        if (activeQuote != null) {
            return activeQuote;
        }
        
        // 然后检查历史记录
        return quoteHistory.get(quoteId);
    }
    
    /**
     * 获取报价历史记录
     *
     * @param symbol 品种代码
     * @param hoursBack 查找最近几小时的历史记录
     * @return 历史报价列表
     */
    public List<Quote> getQuoteHistory(String symbol, int hoursBack) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hoursBack);
        
        // 合并活跃报价和历史报价进行筛选
        List<Quote> allQuotes = new ArrayList<>();
        
        // 添加活跃报价
        List<Quote> activeForSymbol = activeQuotes.getOrDefault(symbol, new ArrayList<>());
        allQuotes.addAll(activeForSymbol.stream()
                .filter(q -> q.getCreateTime().isAfter(cutoffTime))
                .collect(Collectors.toList()));
        
        // 添加历史报价
        allQuotes.addAll(quoteHistory.values().stream()
                .filter(q -> q.getSymbol().equals(symbol) && 
                            q.getCreateTime().isAfter(cutoffTime))
                .collect(Collectors.toList()));
        
        return allQuotes.stream()
                .sorted((q1, q2) -> q2.getCreateTime().compareTo(q1.getCreateTime())) // 按时间倒序
                .collect(Collectors.toList());
    }
    
    /**
     * 获取报价统计信息
     *
     * @param symbol 品种代码
     * @return 统计信息对象
     */
    public QuoteStatistics getQuoteStatistics(String symbol) {
        return quoteStats.get(symbol);
    }
    
    /**
     * 检查并清理过期报价
     * 将过期的活跃报价标记为EXPIRED状态
     *
     * @return 清理的报价数量
     */
    public int expireStaleQuotes() {
        int expiredCount = 0;
        LocalDateTime now = LocalDateTime.now();
        
        for (Map.Entry<String, List<Quote>> entry : activeQuotes.entrySet()) {
            String symbol = entry.getKey();
            List<Quote> quotes = entry.getValue();
            
            synchronized (quotes) {
                Iterator<Quote> iterator = quotes.iterator();
                while (iterator.hasNext()) {
                    Quote quote = iterator.next();
                    if (quote.isExpired()) {
                        quoteHistory.put(quote.getQuoteId(), quote);
                        iterator.remove(); // 从活跃列表中移除
                        expiredCount++;
                        
                        logger.info("Expired quote: ID={}", quote.getQuoteId());
                    }
                }
            }
        }
        
        logger.info("Expired {} stale quotes", expiredCount);
        return expiredCount;
    }
    
    /**
     * 根据报价ID查找活跃报价
     *
     * @param quoteId 报价ID
     * @return 报价对象，如果不存在则返回null
     */
    private Quote findActiveQuoteById(String quoteId) {
        for (List<Quote> quotes : activeQuotes.values()) {
            for (Quote quote : quotes) {
                if (quote.getQuoteId().equals(quoteId)) {
                    return quote;
                }
            }
        }
        return null;
    }
    
    /**
     * 从活跃报价列表中移除指定报价
     *
     * @param quote 要移除的报价
     */
    private void removeQuoteFromActiveList(Quote quote) {
        List<Quote> symbolQuotes = activeQuotes.get(quote.getSymbol());
        if (symbolQuotes != null) {
            synchronized (symbolQuotes) {
                symbolQuotes.remove(quote);
            }
        }
    }
    
    /**
     * 更新报价统计信息
     *
     * @param symbol 品种代码
     * @param quote 报价对象
     */
    private void updateQuoteStatistics(String symbol, Quote quote) {
        QuoteStatistics stats = quoteStats.computeIfAbsent(symbol, k -> new QuoteStatistics());
        
        stats.incrementTotalQuotes();
        if (quote.getSide() == Quote.BUY) {
            stats.incrementTotalBuyQuotes();
        } else if (quote.getSide() == Quote.SELL) {
            stats.incrementTotalSellQuotes();
        }
        
        // 更新最新报价时间
        stats.setLastQuoteTime(LocalDateTime.now());
    }
    
    /**
     * 报价统计信息类
     */
    public static class QuoteStatistics {
        private volatile long totalQuotes = 0;
        private volatile long totalBuyQuotes = 0;
        private volatile long totalSellQuotes = 0;
        private volatile LocalDateTime lastQuoteTime;
        
        public void incrementTotalQuotes() {
            this.totalQuotes++;
        }
        
        public void incrementTotalBuyQuotes() {
            this.totalBuyQuotes++;
        }
        
        public void incrementTotalSellQuotes() {
            this.totalSellQuotes++;
        }
        
        // Getters and Setters
        public long getTotalQuotes() {
            return totalQuotes;
        }
        
        public void setTotalQuotes(long totalQuotes) {
            this.totalQuotes = totalQuotes;
        }
        
        public long getTotalBuyQuotes() {
            return totalBuyQuotes;
        }
        
        public void setTotalBuyQuotes(long totalBuyQuotes) {
            this.totalBuyQuotes = totalBuyQuotes;
        }
        
        public long getTotalSellQuotes() {
            return totalSellQuotes;
        }
        
        public void setTotalSellQuotes(long totalSellQuotes) {
            this.totalSellQuotes = totalSellQuotes;
        }
        
        public LocalDateTime getLastQuoteTime() {
            return lastQuoteTime;
        }
        
        public void setLastQuoteTime(LocalDateTime lastQuoteTime) {
            this.lastQuoteTime = lastQuoteTime;
        }
        
        @Override
        public String toString() {
            return "QuoteStatistics{" +
                    "totalQuotes=" + totalQuotes +
                    ", totalBuyQuotes=" + totalBuyQuotes +
                    ", totalSellQuotes=" + totalSellQuotes +
                    ", lastQuoteTime=" + lastQuoteTime +
                    '}';
        }
    }
}