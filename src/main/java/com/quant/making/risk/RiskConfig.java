package com.quant.making.risk;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 风控配置实体
 * 从 application.yml 加载风控参数配置
 */
@Component
@ConfigurationProperties(prefix = "risk")
public class RiskConfig {
    
    // 是否启用风控
    private Boolean enabled = true;
    
    // 单笔最大交易金额
    private BigDecimal maxSingleTradeAmount = new BigDecimal("1000000");  // 100万
    
    // 单日最大交易金额
    private BigDecimal maxDailyTradeAmount = new BigDecimal("10000000");  // 1000万
    
    // 单品种最大持仓
    private BigDecimal maxPositionPerSymbol = new BigDecimal("500000");   // 50万
    
    // 最大点差限制
    private BigDecimal maxSpreadLimit = new BigDecimal("0.01");          // 1%
    
    // 最大档位偏离
    private Integer maxLevelDeviation = 5;                               // 5档
    
    // 每秒最大订单数限制
    private Integer maxOrdersPerSecond = 10;
    
    // 最大杠杆倍数
    private BigDecimal maxLeverage = new BigDecimal("10");
    
    // 最大亏损限额 (负数表示亏损)
    private BigDecimal maxLossLimit = new BigDecimal("-100000");         // -10万
    
    // 黑名单配置 (禁止交易的品种)
    private List<String> blacklist = new ArrayList<>();
    
    // 白名单配置 (仅允许交易的品种)
    private List<String> whitelist = new ArrayList<>();
    
    // Getters and Setters
    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public BigDecimal getMaxSingleTradeAmount() {
        return maxSingleTradeAmount;
    }

    public void setMaxSingleTradeAmount(BigDecimal maxSingleTradeAmount) {
        this.maxSingleTradeAmount = maxSingleTradeAmount;
    }

    public BigDecimal getMaxDailyTradeAmount() {
        return maxDailyTradeAmount;
    }

    public void setMaxDailyTradeAmount(BigDecimal maxDailyTradeAmount) {
        this.maxDailyTradeAmount = maxDailyTradeAmount;
    }

    public BigDecimal getMaxPositionPerSymbol() {
        return maxPositionPerSymbol;
    }

    public void setMaxPositionPerSymbol(BigDecimal maxPositionPerSymbol) {
        this.maxPositionPerSymbol = maxPositionPerSymbol;
    }

    public BigDecimal getMaxSpreadLimit() {
        return maxSpreadLimit;
    }

    public void setMaxSpreadLimit(BigDecimal maxSpreadLimit) {
        this.maxSpreadLimit = maxSpreadLimit;
    }

    public Integer getMaxLevelDeviation() {
        return maxLevelDeviation;
    }

    public void setMaxLevelDeviation(Integer maxLevelDeviation) {
        this.maxLevelDeviation = maxLevelDeviation;
    }

    public Integer getMaxOrdersPerSecond() {
        return maxOrdersPerSecond;
    }

    public void setMaxOrdersPerSecond(Integer maxOrdersPerSecond) {
        this.maxOrdersPerSecond = maxOrdersPerSecond;
    }

    public BigDecimal getMaxLeverage() {
        return maxLeverage;
    }

    public void setMaxLeverage(BigDecimal maxLeverage) {
        this.maxLeverage = maxLeverage;
    }

    public BigDecimal getMaxLossLimit() {
        return maxLossLimit;
    }

    public void setMaxLossLimit(BigDecimal maxLossLimit) {
        this.maxLossLimit = maxLossLimit;
    }

    public List<String> getBlacklist() {
        return blacklist;
    }

    public void setBlacklist(List<String> blacklist) {
        this.blacklist = blacklist;
    }

    public List<String> getWhitelist() {
        return whitelist;
    }

    public void setWhitelist(List<String> whitelist) {
        this.whitelist = whitelist;
    }
}
