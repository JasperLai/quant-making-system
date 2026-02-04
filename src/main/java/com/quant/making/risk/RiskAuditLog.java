package com.quant.making.risk;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 风控审计日志实体
 * 记录风控检查的结果和相关信息
 */
public class RiskAuditLog {
    
    // 日志ID
    private String logId;
    
    // 检查时间
    private LocalDateTime checkTime;
    
    // 交易ID
    private String tradeId;
    
    // 交易符号
    private String symbol;
    
    // 交易方向
    private Integer side;
    
    // 交易价格
    private BigDecimal price;
    
    // 交易数量
    private BigDecimal quantity;
    
    // 交易金额
    private BigDecimal amount;
    
    // 风控规则类型
    private String ruleType;
    
    // 检查结果 (true=通过, false=拦截)
    private Boolean passed;
    
    // 拦截原因
    private String reason;
    
    // 用户ID
    private String userId;
    
    // 客户端IP
    private String clientIp;
    
    // 风控参数快照
    private String configSnapshot;
    
    public RiskAuditLog() {
        this.checkTime = LocalDateTime.now();
    }
    
    public RiskAuditLog(String tradeId, String symbol, Integer side, BigDecimal price, 
                        BigDecimal quantity, String ruleType, Boolean passed, String reason) {
        this();
        this.tradeId = tradeId;
        this.symbol = symbol;
        this.side = side;
        this.price = price;
        this.quantity = quantity;
        this.amount = price.multiply(quantity);
        this.ruleType = ruleType;
        this.passed = passed;
        this.reason = reason;
    }
    
    // Getters and Setters
    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public LocalDateTime getCheckTime() {
        return checkTime;
    }

    public void setCheckTime(LocalDateTime checkTime) {
        this.checkTime = checkTime;
    }

    public String getTradeId() {
        return tradeId;
    }

    public void setTradeId(String tradeId) {
        this.tradeId = tradeId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public Boolean getPassed() {
        return passed;
    }

    public void setPassed(Boolean passed) {
        this.passed = passed;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getConfigSnapshot() {
        return configSnapshot;
    }

    public void setConfigSnapshot(String configSnapshot) {
        this.configSnapshot = configSnapshot;
    }
}