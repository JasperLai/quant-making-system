package com.quant.making.book;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * 订单簿聚合视图
 * 包含按价格聚合的档位信息
 */
public class OrderBook {
    
    private String symbol;
    private Integer marketType;
    private Map<BigDecimal, PriceLevel> aggregatedLevels;
    
    // 静态内部类：价格档位
    public static class PriceLevel {
        private BigDecimal price;
        private BigDecimal totalBuyQty;
        private BigDecimal totalSellQty;
        private Map<String, BigDecimal> sources;  // source -> quantity
        
        public PriceLevel() {
            this.totalBuyQty = BigDecimal.ZERO;
            this.totalSellQty = BigDecimal.ZERO;
            this.sources = new java.util.HashMap<>();
        }
        
        public PriceLevel(BigDecimal price) {
            this.price = price;
            this.totalBuyQty = BigDecimal.ZERO;
            this.totalSellQty = BigDecimal.ZERO;
            this.sources = new java.util.HashMap<>();
        }
        
        public void addBuyQuantity(String source, BigDecimal quantity) {
            this.totalBuyQty = this.totalBuyQty.add(quantity);
            this.sources.merge(source, quantity, BigDecimal::add);
        }
        
        public void addSellQuantity(String source, BigDecimal quantity) {
            this.totalSellQty = this.totalSellQty.add(quantity);
            this.sources.merge(source, quantity, BigDecimal::add);
        }
        
        // Getter 和 Setter
        public BigDecimal getPrice() {
            return price;
        }
        
        public void setPrice(BigDecimal price) {
            this.price = price;
        }
        
        public BigDecimal getTotalBuyQty() {
            return totalBuyQty;
        }
        
        public void setTotalBuyQty(BigDecimal totalBuyQty) {
            this.totalBuyQty = totalBuyQty;
        }
        
        public BigDecimal getTotalSellQty() {
            return totalSellQty;
        }
        
        public void setTotalSellQty(BigDecimal totalSellQty) {
            this.totalSellQty = totalSellQty;
        }
        
        public Map<String, BigDecimal> getSources() {
            return sources;
        }
        
        public void setSources(Map<String, BigDecimal> sources) {
            this.sources = sources;
        }
        
        public boolean hasBuyOrders() {
            return totalBuyQty.compareTo(BigDecimal.ZERO) > 0;
        }
        
        public boolean hasSellOrders() {
            return totalSellQty.compareTo(BigDecimal.ZERO) > 0;
        }
        
        @Override
        public String toString() {
            return "PriceLevel{" +
                    "price=" + price +
                    ", totalBuyQty=" + totalBuyQty +
                    ", totalSellQty=" + totalSellQty +
                    ", sources=" + sources +
                    '}';
        }
    }
    
    // 默认构造函数
    public OrderBook() {
        this.aggregatedLevels = new java.util.concurrent.ConcurrentHashMap<>();
    }
    
    public OrderBook(String symbol, Integer marketType) {
        this.symbol = symbol;
        this.marketType = marketType;
        this.aggregatedLevels = new java.util.concurrent.ConcurrentHashMap<>();
    }
    
    // 添加报价到订单簿
    public void addQuote(String source, Integer side, BigDecimal price, BigDecimal quantity) {
        PriceLevel level = aggregatedLevels.computeIfAbsent(price, PriceLevel::new);
        if (side == 1) {  // BUY
            level.addBuyQuantity(source, quantity);
        } else if (side == 2) {  // SELL
            level.addSellQuantity(source, quantity);
        }
    }
    
    // 获取最优买方价格（最高买价）
    public PriceLevel getBestBid() {
        return aggregatedLevels.entrySet().stream()
                .filter(e -> e.getValue().hasBuyOrders())
                .max(java.util.Comparator.comparing(Map.Entry::getKey))
                .map(Map.Entry::getValue)
                .orElse(null);
    }
    
    // 获取最优卖方价格（最低卖价）
    public PriceLevel getBestAsk() {
        return aggregatedLevels.entrySet().stream()
                .filter(e -> e.getValue().hasSellOrders())
                .min(java.util.Comparator.comparing(Map.Entry::getKey))
                .map(Map.Entry::getValue)
                .orElse(null);
    }
    
    // Getter 和 Setter
    public String getSymbol() {
        return symbol;
    }
    
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
    
    public Integer getMarketType() {
        return marketType;
    }
    
    public void setMarketType(Integer marketType) {
        this.marketType = marketType;
    }
    
    public Map<BigDecimal, PriceLevel> getAggregatedLevels() {
        return aggregatedLevels;
    }
    
    public void setAggregatedLevels(Map<BigDecimal, PriceLevel> aggregatedLevels) {
        this.aggregatedLevels = aggregatedLevels;
    }
    
    // 常量定义
    public static final int BUY = 1;
    public static final int SELL = 2;
    
    // 市场类型常量
    public static final int MARKET_DOMESTIC_GOLD = 1;      // 境内黄金
    public static final int MARKET_FOREIGN_EXCHANGE = 2;   // 境内外汇
    public static final int MARKET_OFFSHORE = 3;           // 境外
    
    @Override
    public String toString() {
        return "OrderBook{" +
                "symbol='" + symbol + '\'' +
                ", marketType=" + marketType +
                ", aggregatedLevels.size()=" + aggregatedLevels.size() +
                '}';
    }
}
