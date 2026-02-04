package com.quant.making.book;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单簿档位实体
 * 对应数据库表 order_book_entry
 */
@Entity
@Table(name = "order_book_entry")
public class OrderBookEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "symbol", nullable = false, length = 32)
    private String symbol;
    
    @Column(name = "market_type", nullable = false)
    private Integer marketType;
    
    @Column(name = "source", nullable = false, length = 32)
    private String source;
    
    @Column(name = "side", nullable = false)
    private Integer side;
    
    @Column(name = "price_level", nullable = false)
    private Integer priceLevel;
    
    @Column(name = "price", nullable = false, precision = 18, scale = 8)
    private BigDecimal price;
    
    @Column(name = "quantity", nullable = false, precision = 18, scale = 8)
    private BigDecimal quantity;
    
    @Column(name = "snapshot_time", nullable = false)
    private LocalDateTime snapshotTime;
    
    // 默认构造函数
    public OrderBookEntry() {
    }
    
    // 全参构造函数
    public OrderBookEntry(String symbol, Integer marketType, String source, Integer side, 
                          Integer priceLevel, BigDecimal price, BigDecimal quantity, LocalDateTime snapshotTime) {
        this.symbol = symbol;
        this.marketType = marketType;
        this.source = source;
        this.side = side;
        this.priceLevel = priceLevel;
        this.price = price;
        this.quantity = quantity;
        this.snapshotTime = snapshotTime;
    }
    
    // Getter 和 Setter
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
    
    public String getSource() {
        return source;
    }
    
    public void setSource(String source) {
        this.source = source;
    }
    
    public Integer getSide() {
        return side;
    }
    
    public void setSide(Integer side) {
        this.side = side;
    }
    
    public Integer getPriceLevel() {
        return priceLevel;
    }
    
    public void setPriceLevel(Integer priceLevel) {
        this.priceLevel = priceLevel;
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
    
    public LocalDateTime getSnapshotTime() {
        return snapshotTime;
    }
    
    public void setSnapshotTime(LocalDateTime snapshotTime) {
        this.snapshotTime = snapshotTime;
    }
    
    @Override
    public String toString() {
        return "OrderBookEntry{" +
                "id=" + id +
                ", symbol='" + symbol + '\'' +
                ", marketType=" + marketType +
                ", source='" + source + '\'' +
                ", side=" + side +
                ", priceLevel=" + priceLevel +
                ", price=" + price +
                ", quantity=" + quantity +
                ", snapshotTime=" + snapshotTime +
                '}';
    }
}
