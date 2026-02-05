package com.quant.making.risk;

import com.quant.making.quote.Quote;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 风控规则引擎
 * 负责执行各类风控检查
 */
public class RiskRuleEngine {
    
    // 风控配置
    private RiskConfig riskConfig;
    
    // 存储当日交易统计
    private Map<String, BigDecimal> dailyTradeAmountMap = new HashMap<>();
    
    // 存储品种持仓统计
    private Map<String, BigDecimal> positionMap = new HashMap<>();
    
    // 存储最近订单时间戳，用于限频检查 (线程安全队列)
    private ConcurrentLinkedQueue<LocalDateTime> recentOrders = new ConcurrentLinkedQueue<>();
    
    // 存储风控日志
    private List<RiskAuditLog> auditLogs = new ArrayList<>();
    
    public RiskRuleEngine(RiskConfig riskConfig) {
        this.riskConfig = riskConfig;
    }
    
    /**
     * 执行前置风控检查
     */
    public RiskCheckResult preTradeCheck(Quote quote) {
        if (!riskConfig.isEnabled()) {
            return new RiskCheckResult(true, "风控未启用", "PRE_TRADE_CHECK");
        }
        
        // 检查单笔交易金额限制
        RiskCheckResult result = checkSingleTradeAmount(quote);
        if (!result.isPassed()) {
            return result;
        }
        
        // 检查黑名单
        result = checkBlacklist(quote.getSymbol());
        if (!result.isPassed()) {
            return result;
        }
        
        // 检查白名单
        result = checkWhitelist(quote.getSymbol());
        if (!result.isPassed()) {
            return result;
        }
        
        // 检查档位偏离
        result = checkLevelDeviation(quote);
        if (!result.isPassed()) {
            return result;
        }
        
        // 检查点差限制
        result = checkSpreadLimit(quote);
        if (!result.isPassed()) {
            return result;
        }
        
        // 检查每秒订单频率
        result = checkOrderFrequency();
        if (!result.isPassed()) {
            return result;
        }
        
        return new RiskCheckResult(true, "前置风控检查通过", "PRE_TRADE_CHECK");
    }
    
    /**
     * 执行事后风控检查
     */
    public RiskCheckResult postTradeCheck(Quote executedQuote, BigDecimal realizedPnL) {
        if (!riskConfig.isEnabled()) {
            return new RiskCheckResult(true, "风控未启用", "POST_TRADE_CHECK");
        }
        
        // 检查单日交易金额
        RiskCheckResult result = checkDailyTradeAmount(executedQuote.getSymbol(), executedQuote.getPrice().multiply(executedQuote.getQuantity()));
        if (!result.isPassed()) {
            return result;
        }
        
        // 检查品种最大持仓
        result = checkPositionLimit(executedQuote.getSymbol(), executedQuote.getQuantity());
        if (!result.isPassed()) {
            return result;
        }
        
        // 检查最大亏损限制
        result = checkLossLimit(realizedPnL);
        if (!result.isPassed()) {
            return result;
        }
        
        return new RiskCheckResult(true, "事后风控检查通过", "POST_TRADE_CHECK");
    }
    
    /**
     * 检查单笔交易金额限制
     */
    private RiskCheckResult checkSingleTradeAmount(Quote quote) {
        BigDecimal tradeAmount = quote.getPrice().multiply(quote.getQuantity());
        if (tradeAmount.compareTo(riskConfig.getMaxSingleTradeAmount()) > 0) {
            String reason = String.format("单笔交易金额%.2f超过限制%.2f", 
                                        tradeAmount.doubleValue(), 
                                        riskConfig.getMaxSingleTradeAmount().doubleValue());
            return new RiskCheckResult(false, reason, "SINGLE_TRADE_AMOUNT_LIMIT");
        }
        return new RiskCheckResult(true, "单笔交易金额检查通过", "SINGLE_TRADE_AMOUNT_LIMIT");
    }
    
    /**
     * 检查单日交易金额限制
     */
    private RiskCheckResult checkDailyTradeAmount(String symbol, BigDecimal tradeAmount) {
        LocalDate today = LocalDate.now();
        String key = symbol + "_" + today.toString();
        
        BigDecimal currentDailyAmount = dailyTradeAmountMap.getOrDefault(key, BigDecimal.ZERO);
        BigDecimal newTotal = currentDailyAmount.add(tradeAmount);
        
        if (newTotal.compareTo(riskConfig.getMaxDailyTradeAmount()) > 0) {
            String reason = String.format("单日交易金额%.2f超过限制%.2f", 
                                        newTotal.doubleValue(), 
                                        riskConfig.getMaxDailyTradeAmount().doubleValue());
            return new RiskCheckResult(false, reason, "DAILY_TRADE_AMOUNT_LIMIT");
        }
        
        dailyTradeAmountMap.put(key, newTotal);
        return new RiskCheckResult(true, "单日交易金额检查通过", "DAILY_TRADE_AMOUNT_LIMIT");
    }
    
    /**
     * 检查品种最大持仓限制
     */
    private RiskCheckResult checkPositionLimit(String symbol, BigDecimal quantity) {
        BigDecimal currentPosition = positionMap.getOrDefault(symbol, BigDecimal.ZERO);
        BigDecimal newPosition = currentPosition.add(quantity);
        
        if (newPosition.compareTo(riskConfig.getMaxPositionPerSymbol()) > 0) {
            String reason = String.format("品种%s持仓%.2f超过限制%.2f", 
                                        symbol,
                                        newPosition.doubleValue(), 
                                        riskConfig.getMaxPositionPerSymbol().doubleValue());
            return new RiskCheckResult(false, reason, "POSITION_LIMIT");
        }
        
        positionMap.put(symbol, newPosition);
        return new RiskCheckResult(true, "品种持仓检查通过", "POSITION_LIMIT");
    }
    
    /**
     * 检查黑名单
     */
    private RiskCheckResult checkBlacklist(String symbol) {
        if (riskConfig.getBlacklist() != null && riskConfig.getBlacklist().contains(symbol)) {
            return new RiskCheckResult(false, "交易品种在黑名单中: " + symbol, "BLACKLIST_CHECK");
        }
        return new RiskCheckResult(true, "黑名单检查通过", "BLACKLIST_CHECK");
    }
    
    /**
     * 检查白名单
     */
    private RiskCheckResult checkWhitelist(String symbol) {
        if (riskConfig.getWhitelist() != null && !riskConfig.getWhitelist().isEmpty() 
            && !riskConfig.getWhitelist().contains(symbol)) {
            return new RiskCheckResult(false, "交易品种不在白名单中: " + symbol, "WHITELIST_CHECK");
        }
        return new RiskCheckResult(true, "白名单检查通过", "WHITELIST_CHECK");
    }
    
    /**
     * 检查档位偏离
     */
    private RiskCheckResult checkLevelDeviation(Quote quote) {
        if (quote.getLevel() != null && quote.getLevel() > riskConfig.getMaxLevelDeviation()) {
            String reason = String.format("档位%d超过最大偏离%d", 
                                        quote.getLevel(), 
                                        riskConfig.getMaxLevelDeviation());
            return new RiskCheckResult(false, reason, "LEVEL_DEVIATION_LIMIT");
        }
        return new RiskCheckResult(true, "档位偏离检查通过", "LEVEL_DEVIATION_LIMIT");
    }
    
    /**
     * 检查点差限制
     */
    private RiskCheckResult checkSpreadLimit(Quote quote) {
        if (quote.getSpread() != null && quote.getSpread().compareTo(riskConfig.getMaxSpreadLimit()) > 0) {
            String reason = String.format("点差%.4f超过限制%.4f", 
                                        quote.getSpread().doubleValue(), 
                                        riskConfig.getMaxSpreadLimit().doubleValue());
            return new RiskCheckResult(false, reason, "SPREAD_LIMIT");
        }
        return new RiskCheckResult(true, "点差检查通过", "SPREAD_LIMIT");
    }
    
    /**
     * 检查订单频率 (线程安全版本)
     * 使用 ConcurrentLinkedQueue 实现高性能无锁限流
     */
    private RiskCheckResult checkOrderFrequency() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusSeconds(5);
        int maxOrders = riskConfig.getMaxOrdersPerSecond();
        
        // 1. 清理过期订单 (非阻塞操作)
        LocalDateTime oldest;
        while ((oldest = recentOrders.peek()) != null && oldest.isBefore(cutoff)) {
            recentOrders.poll();  // 移除过期的订单
        }
        
        // 2. 检查是否超过限制 (原子操作)
        int currentCount = recentOrders.size();
        if (currentCount >= maxOrders) {
            String reason = String.format("订单频率过高，当前%d个订单/5秒，超过限制%d个订单/5秒", 
                                        currentCount, 
                                        maxOrders);
            return new RiskCheckResult(false, reason, "ORDER_FREQUENCY_LIMIT");
        }
        
        // 3. 添加新订单 (无阻塞添加)
        recentOrders.offer(now);
        
        return new RiskCheckResult(true, "订单频率检查通过", "ORDER_FREQUENCY_LIMIT");
    }
    
    /**
     * 检查最大亏损限制
     */
    private RiskCheckResult checkLossLimit(BigDecimal realizedPnL) {
        if (realizedPnL != null && realizedPnL.compareTo(riskConfig.getMaxLossLimit()) < 0) {
            String reason = String.format("实际盈亏%.2f低于最大亏损限制%.2f", 
                                        realizedPnL.doubleValue(), 
                                        riskConfig.getMaxLossLimit().doubleValue());
            return new RiskCheckResult(false, reason, "LOSS_LIMIT");
        }
        return new RiskCheckResult(true, "亏损限制检查通过", "LOSS_LIMIT");
    }
    
    /**
     * 添加风控日志
     */
    public void logRiskCheck(RiskAuditLog log) {
        auditLogs.add(log);
    }
    
    /**
     * 获取风控配置
     */
    public RiskConfig getRiskConfig() {
        return riskConfig;
    }
    
    /**
     * 更新风控配置
     */
    public void updateRiskConfig(RiskConfig newConfig) {
        this.riskConfig = newConfig;
    }
    
    /**
     * 获取风控日志列表
     */
    public List<RiskAuditLog> getAuditLogs() {
        return auditLogs;
    }
    
    /**
     * 风控检查结果内部类
     */
    public static class RiskCheckResult {
        private boolean passed;
        private String message;
        private String ruleType;
        
        public RiskCheckResult(boolean passed, String message, String ruleType) {
            this.passed = passed;
            this.message = message;
            this.ruleType = ruleType;
        }
        
        public boolean isPassed() {
            return passed;
        }
        
        public void setPassed(boolean passed) {
            this.passed = passed;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public String getRuleType() {
            return ruleType;
        }
        
        public void setRuleType(String ruleType) {
            this.ruleType = ruleType;
        }
    }
}