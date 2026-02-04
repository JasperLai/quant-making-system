package com.quant.making.book;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订单簿服务
 * 负责订单簿数据的更新、查询和快照管理
 */
@Service
public class OrderBookService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderBookService.class);
    
    @Autowired
    protected OrderBookRepository orderBookRepository;
    
    // 内存订单簿缓存: symbol -> OrderBook
    private final Map<String, OrderBook> orderBookCache = new ConcurrentHashMap<>();
    
    // 快照时间间隔配置（秒）
    private int snapshotIntervalSeconds = 60;
    
    // 最后快照时间
    private final Map<String, LocalDateTime> lastSnapshotTime = new ConcurrentHashMap<>();
    
    /**
     * 更新报价
     * 将新的报价数据更新到订单簿中
     * 
     * @param symbol 品种代码
     * @param marketType 市场类型
     * @param source 价源标识
     * @param side BUY=1, SELL=2
     * @param price 价格
     * @param quantity 数量
     */
    public void updateQuote(String symbol, Integer marketType, String source, 
                           Integer side, BigDecimal price, BigDecimal quantity) {
        logger.debug("Updating quote: symbol={}, marketType={}, source={}, side={}, price={}, quantity={}",
                symbol, marketType, source, side, price, quantity);
        
        // 获取或创建订单簿
        OrderBook orderBook = orderBookCache.computeIfAbsent(symbol, 
            k -> new OrderBook(symbol, marketType));
        
        // 更新聚合视图
        orderBook.addQuote(source, side, price, quantity);
        
        // 生成快照（如果需要）
        checkAndGenerateSnapshot(symbol, marketType);
    }
    
    /**
     * 批量更新报价
     */
    public void updateQuotes(List<QuoteData> quotes) {
        for (QuoteData quote : quotes) {
            updateQuote(quote.getSymbol(), quote.getMarketType(), quote.getSource(),
                       quote.getSide(), quote.getPrice(), quote.getQuantity());
        }
    }
    
    /**
     * 获取最优买方价格
     * 
     * @param symbol 品种代码
     * @return 最优买方档位信息
     */
    public OrderBook.PriceLevel getBestBid(String symbol) {
        OrderBook orderBook = orderBookCache.get(symbol);
        if (orderBook == null) {
            logger.warn("OrderBook not found for symbol: {}", symbol);
            return null;
        }
        return orderBook.getBestBid();
    }
    
    /**
     * 获取最优卖方价格
     * 
     * @param symbol 品种代码
     * @return 最优卖方档位信息
     */
    public OrderBook.PriceLevel getBestAsk(String symbol) {
        OrderBook orderBook = orderBookCache.get(symbol);
        if (orderBook == null) {
            logger.warn("OrderBook not found for symbol: {}", symbol);
            return null;
        }
        return orderBook.getBestAsk();
    }
    
    /**
     * 获取完整订单簿
     * 
     * @param symbol 品种代码
     * @return 订单簿聚合视图
     */
    public OrderBook getOrderBook(String symbol) {
        return orderBookCache.get(symbol);
    }
    
    /**
     * 获取所有订单簿
     */
    public Map<String, OrderBook> getAllOrderBooks() {
        return new ConcurrentHashMap<>(orderBookCache);
    }
    
    /**
     * 手动触发快照生成
     * 
     * @param symbol 品种代码
     */
    public void snapshot(String symbol) {
        generateSnapshot(symbol);
    }
    
    /**
     * 手动触发所有订单簿快照
     */
    public void snapshotAll() {
        orderBookCache.keySet().forEach(this::generateSnapshot);
    }
    
    /**
     * 手动触发指定市场和品种的快照
     */
    public void snapshot(String symbol, Integer marketType) {
        OrderBook orderBook = orderBookCache.get(symbol);
        if (orderBook != null) {
            saveSnapshotToDatabase(orderBook, marketType);
        }
    }
    
    /**
     * 定时检查并生成快照
     */
    private void checkAndGenerateSnapshot(String symbol, Integer marketType) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastTime = lastSnapshotTime.get(symbol);
        
        if (lastTime == null || 
            java.time.Duration.between(lastTime, now).getSeconds() >= snapshotIntervalSeconds) {
            generateSnapshot(symbol);
        }
    }
    
    /**
     * 生成订单簿快照
     */
    private void generateSnapshot(String symbol) {
        OrderBook orderBook = orderBookCache.get(symbol);
        if (orderBook == null) {
            logger.warn("Cannot snapshot: OrderBook not found for symbol: {}", symbol);
            return;
        }
        
        saveSnapshotToDatabase(orderBook, orderBook.getMarketType());
        lastSnapshotTime.put(symbol, LocalDateTime.now());
        
        logger.info("Snapshot generated for symbol: {}", symbol);
    }
    
    /**
     * 保存快照到数据库
     */
    private void saveSnapshotToDatabase(OrderBook orderBook, Integer marketType) {
        LocalDateTime snapshotTime = LocalDateTime.now();
        
        for (Map.Entry<BigDecimal, OrderBook.PriceLevel> entry : 
             orderBook.getAggregatedLevels().entrySet()) {
            BigDecimal price = entry.getKey();
            OrderBook.PriceLevel level = entry.getValue();
            
            // 保存买方档位
            if (level.hasBuyOrders()) {
                OrderBookEntry entryBuy = new OrderBookEntry(
                    orderBook.getSymbol(),
                    marketType,
                    null,  // source将在聚合时记录
                    OrderBook.BUY,
                    0,  // priceLevel将在查询时计算
                    price,
                    level.getTotalBuyQty(),
                    snapshotTime
                );
                
                // 为每个价源保存单独记录
                for (Map.Entry<String, BigDecimal> sourceEntry : level.getSources().entrySet()) {
                    OrderBookEntry sourceEntryRecord = new OrderBookEntry(
                        orderBook.getSymbol(),
                        marketType,
                        sourceEntry.getKey(),
                        OrderBook.BUY,
                        0,
                        price,
                        sourceEntry.getValue(),
                        snapshotTime
                    );
                    orderBookRepository.save(sourceEntryRecord);
                }
            }
            
            // 保存卖方档位
            if (level.hasSellOrders()) {
                for (Map.Entry<String, BigDecimal> sourceEntry : level.getSources().entrySet()) {
                    OrderBookEntry sourceEntryRecord = new OrderBookEntry(
                        orderBook.getSymbol(),
                        marketType,
                        sourceEntry.getKey(),
                        OrderBook.SELL,
                        0,
                        price,
                        sourceEntry.getValue(),
                        snapshotTime
                    );
                    orderBookRepository.save(sourceEntryRecord);
                }
            }
        }
    }
    
    /**
     * 从数据库恢复订单簿
     * 
     * @param symbol 品种代码
     * @return 是否恢复成功
     */
    public boolean restoreFromSnapshot(String symbol) {
        List<OrderBookEntry> entries = orderBookRepository.findLatestSnapshot(symbol, null);
        if (entries.isEmpty()) {
            logger.info("No snapshot found for symbol: {}", symbol);
            return false;
        }
        
        OrderBook orderBook = new OrderBook();
        orderBook.setSymbol(symbol);
        if (!entries.isEmpty()) {
            orderBook.setMarketType(entries.get(0).getMarketType());
        }
        
        for (OrderBookEntry entry : entries) {
            orderBook.addQuote(entry.getSource(), entry.getSide(), 
                             entry.getPrice(), entry.getQuantity());
        }
        
        orderBookCache.put(symbol, orderBook);
        logger.info("Restored OrderBook from snapshot for symbol: {}", symbol);
        return true;
    }
    
    /**
     * 清理旧快照（定时任务）
     * 默认保留最近24小时的数据
     */
    @Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨2点执行
    public void cleanupOldSnapshots() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        int deletedCount = orderBookRepository.deleteOldSnapshots(cutoffTime);
        logger.info("Cleaned up {} old snapshot entries", deletedCount);
    }
    
    /**
     * 清空指定品种的订单簿
     */
    public void clearOrderBook(String symbol) {
        orderBookCache.remove(symbol);
        lastSnapshotTime.remove(symbol);
        logger.info("Cleared OrderBook for symbol: {}", symbol);
    }
    
    /**
     * 清空所有订单簿
     */
    public void clearAllOrderBooks() {
        orderBookCache.clear();
        lastSnapshotTime.clear();
        logger.info("Cleared all OrderBooks");
    }
    
    /**
     * 设置快照间隔
     */
    public void setSnapshotIntervalSeconds(int intervalSeconds) {
        this.snapshotIntervalSeconds = intervalSeconds;
    }
    
    /**
     * 报价数据内部类
     */
    public static class QuoteData {
        private String symbol;
        private Integer marketType;
        private String source;
        private Integer side;
        private BigDecimal price;
        private BigDecimal quantity;
        
        public QuoteData() {}
        
        public QuoteData(String symbol, Integer marketType, String source, 
                        Integer side, BigDecimal price, BigDecimal quantity) {
            this.symbol = symbol;
            this.marketType = marketType;
            this.source = source;
            this.side = side;
            this.price = price;
            this.quantity = quantity;
        }
        
        // Getter 和 Setter
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public Integer getMarketType() { return marketType; }
        public void setMarketType(Integer marketType) { this.marketType = marketType; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public Integer getSide() { return side; }
        public void setSide(Integer side) { this.side = side; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    }
}
