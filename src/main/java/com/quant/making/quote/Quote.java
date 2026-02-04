package com.quant.making.quote;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 报价实体
 * 表示系统对外发布的报价信息
 */
public class Quote {
    
    // 报价ID
    private String quoteId;
    
    // 交易符号
    private String symbol;
    
    // 市场类型
    private Integer marketType;
    
    // 买卖方向 (1=BUY, 2=SELL)
    private Integer side;
    
    // 报价价格
    private BigDecimal price;
    
    // 报价数量
    private BigDecimal quantity;
    
    // 档位编号
    private Integer level;
    
    // 点差
    private BigDecimal spread;
    
    // 有效期截止时间
    private LocalDateTime expiryTime;
    
    // 有效期持续时间（秒）
    private int validityDuration;
    
    // 创建时间戳
    private LocalDateTime createTime;
    
    // 更新时间戳
    private LocalDateTime updateTime;
    
    // 来源信息
    private String source;
    
    // 关联的订单簿ID（可选）
    private String orderBookId;
    
    // 报价类型（最优价、次优价等）
    private QuoteType quoteType;
    
    public Quote() {
        this.quoteId = UUID.randomUUID().toString();
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        this.level = 0;
        this.validityDuration = 5; // 默认5秒有效期
    }
    
    public Quote(String symbol, Integer marketType, Integer side, BigDecimal price, BigDecimal quantity) {
        this();
        this.symbol = symbol;
        this.marketType = marketType;
        this.side = side;
        this.price = price;
        this.quantity = quantity;
        this.expiryTime = LocalDateTime.now().plusSeconds(validityDuration);
    }
    
    /**
     * 检查报价是否有效
     */
    public boolean isValid() {
        return this.expiryTime.isAfter(LocalDateTime.now());
    }
    
    /**
     * 检查报价是否已过期
     */
    public boolean isExpired() {
        return this.expiryTime.isBefore(LocalDateTime.now());
    }
    
    /**
     * 激活报价
     */
    public void activate() {
        this.expiryTime = LocalDateTime.now().plusSeconds(validityDuration);
    }
    
    // Getters and Setters
    public String getQuoteId() {
        return quoteId;
    }

    public void setQuoteId(String quoteId) {
        this.quoteId = quoteId;
    }

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

    public Integer getSide() {
        return side;
    }

    public void setSide(Integer side) {
        this.side = side;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public BigDecimal getSpread() {
        return spread;
    }

    public void setSpread(BigDecimal spread) {
        this.spread = spread;
    }

    public LocalDateTime getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(LocalDateTime expiryTime) {
        this.expiryTime = expiryTime;
    }

    public int getValidityDuration() {
        return validityDuration;
    }

    public void setValidityDuration(int validityDuration) {
        this.validityDuration = validityDuration;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getOrderBookId() {
        return orderBookId;
    }

    public void setOrderBookId(String orderBookId) {
        this.orderBookId = orderBookId;
    }

    public QuoteType getQuoteType() {
        return quoteType;
    }

    public void setQuoteType(QuoteType quoteType) {
        this.quoteType = quoteType;
    }

    /**
     * 报价状态枚举
     */
    public enum QuoteStatus {
        PENDING,    // 待处理
        ACTIVE,     // 激活中
        EXPIRED,    // 已过期
        CANCELLED   // 已取消
    }
    
    /**
     * 报价类型枚举
     */
    public enum QuoteType {
        BEST_BID,       // 最优买价
        BEST_ASK,       // 最优卖价
        SECOND_BID,     // 次优买价
        SECOND_ASK,     // 次优卖价
        CUSTOM          // 自定义档位
    }
    
    // 静态常量定义
    public static final int BUY = 1;
    public static final int SELL = 2;
}