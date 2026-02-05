package com.quant.making.trade;

import com.quant.making.quote.Quote;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成交回报实体类
 */
@Entity
@Table(name = "trade_report")
public class TradeReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trade_id")
    private Long tradeId;

    @Column(name = "quote_id")
    private String quoteId; // 关联报价ID

    @Column(name = "symbol", nullable = false)
    private String symbol; // 交易符号

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false)
    private Side side; // 买卖方向

    @Column(name = "price", precision = 15, scale = 8, nullable = false)
    private BigDecimal price; // 成交价格

    @Column(name = "quantity", precision = 15, scale = 8, nullable = false)
    private BigDecimal quantity; // 成交数量

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp; // 成交时间戳

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TradeStatus status; // 成交状态

    @Column(name = "fee", precision = 15, scale = 8)
    private BigDecimal fee; // 手续费

    @Column(name = "slippage", precision = 15, scale = 8)
    private BigDecimal slippage; // 滑点信息

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 构造函数
    public TradeReport() {}

    public TradeReport(String quoteId, String symbol, Side side, BigDecimal price, BigDecimal quantity) {
        this.quoteId = quoteId;
        this.symbol = symbol;
        this.side = side;
        this.price = price;
        this.quantity = quantity;
        this.status = TradeStatus.PENDING;
        this.fee = BigDecimal.ZERO;
        this.slippage = BigDecimal.ZERO;
    }

    // Getter和Setter方法
    public Long getTradeId() {
        return tradeId;
    }

    public void setTradeId(Long tradeId) {
        this.tradeId = tradeId;
    }

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

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public TradeStatus getStatus() {
        return status;
    }

    public void setStatus(TradeStatus status) {
        this.status = status;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public BigDecimal getSlippage() {
        return slippage;
    }

    public void setSlippage(BigDecimal slippage) {
        this.slippage = slippage;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

/**
 * 买卖方向枚举
 */
enum Side {
    BUY, SELL
}

/**
 * 成交状态枚举
 */
enum TradeStatus {
    PENDING,     // 待成交
    EXECUTED,    // 已成交
    CANCELLED,   // 已取消
    REJECTED     // 已拒绝
}