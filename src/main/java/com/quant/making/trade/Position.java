package com.quant.making.trade;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 持仓实体类
 */
@Entity
@Table(name = "position")
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "position_id")
    private Long positionId;

    @Column(name = "symbol", nullable = false)
    private String symbol; // 交易符号

    @Column(name = "quantity", precision = 15, scale = 8, nullable = false)
    private BigDecimal quantity; // 持仓数量（正数表示多头，负数表示空头）

    @Column(name = "avg_price", precision = 15, scale = 8)
    private BigDecimal avgPrice; // 平均持仓成本

    @Column(name = "frozen_quantity", precision = 15, scale = 8)
    private BigDecimal frozenQuantity; // 冻结数量

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 构造函数
    public Position() {
        this.quantity = BigDecimal.ZERO;
        this.avgPrice = BigDecimal.ZERO;
        this.frozenQuantity = BigDecimal.ZERO;
    }

    public Position(String symbol) {
        this.symbol = symbol;
        this.quantity = BigDecimal.ZERO;
        this.avgPrice = BigDecimal.ZERO;
        this.frozenQuantity = BigDecimal.ZERO;
    }

    // Getter和Setter方法
    public Long getPositionId() {
        return positionId;
    }

    public void setPositionId(Long positionId) {
        this.positionId = positionId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getAvgPrice() {
        return avgPrice;
    }

    public void setAvgPrice(BigDecimal avgPrice) {
        this.avgPrice = avgPrice;
    }

    public BigDecimal getFrozenQuantity() {
        return frozenQuantity;
    }

    public void setFrozenQuantity(BigDecimal frozenQuantity) {
        this.frozenQuantity = frozenQuantity;
    }

    /**
     * 获取可用数量（总数量减去冻结数量）
     */
    public BigDecimal getAvailableQuantity() {
        return quantity.subtract(frozenQuantity);
    }

    /**
     * 增加持仓数量
     */
    public void increaseQuantity(BigDecimal amount, BigDecimal price) {
        BigDecimal totalValue = avgPrice.multiply(quantity).add(price.multiply(amount));
        BigDecimal newQuantity = quantity.add(amount);
        
        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            avgPrice = BigDecimal.ZERO;
        } else {
            avgPrice = totalValue.divide(newQuantity, 8, BigDecimal.ROUND_HALF_UP);
        }
        
        quantity = newQuantity;
    }

    /**
     * 减少持仓数量
     */
    public void decreaseQuantity(BigDecimal amount) {
        quantity = quantity.subtract(amount);
        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
            quantity = BigDecimal.ZERO;
        }
    }

    /**
     * 冻结指定数量
     */
    public boolean freezeQuantity(BigDecimal amount) {
        if (getAvailableQuantity().compareTo(amount) >= 0) {
            frozenQuantity = frozenQuantity.add(amount);
            return true;
        }
        return false;
    }

    /**
     * 解冻指定数量
     */
    public boolean unfreezeQuantity(BigDecimal amount) {
        if (frozenQuantity.compareTo(amount) >= 0) {
            frozenQuantity = frozenQuantity.subtract(amount);
            return true;
        }
        return false;
    }
}