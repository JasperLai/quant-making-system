package com.quant.making.risk;

import com.quant.making.quote.Quote;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 风控服务
 * 提供风控检查的核心服务接口
 */
public class RiskControlService {
    
    private RiskRuleEngine riskRuleEngine;
    private RiskConfig riskConfig;
    
    public RiskControlService(RiskConfig riskConfig) {
        this.riskConfig = riskConfig;
        this.riskRuleEngine = new RiskRuleEngine(riskConfig);
    }
    
    /**
     * 报价前风控检查
     */
    public RiskRuleEngine.RiskCheckResult preTradeCheck(Quote quote) {
        RiskRuleEngine.RiskCheckResult result = riskRuleEngine.preTradeCheck(quote);
        
        // 记录风控日志
        RiskAuditLog log = new RiskAuditLog(
            quote.getQuoteId(),
            quote.getSymbol(),
            quote.getSide(),
            quote.getPrice(),
            quote.getQuantity(),
            result.getRuleType(),
            result.isPassed(),
            result.getMessage()
        );
        log.setLogId(java.util.UUID.randomUUID().toString());
        riskRuleEngine.logRiskCheck(log);
        
        return result;
    }
    
    /**
     * 成交后风控检查
     */
    public RiskRuleEngine.RiskCheckResult postTradeCheck(Quote executedQuote, BigDecimal realizedPnL) {
        RiskRuleEngine.RiskCheckResult result = riskRuleEngine.postTradeCheck(executedQuote, realizedPnL);
        
        // 记录风控日志
        RiskAuditLog log = new RiskAuditLog(
            executedQuote.getQuoteId(),
            executedQuote.getSymbol(),
            executedQuote.getSide(),
            executedQuote.getPrice(),
            executedQuote.getQuantity(),
            result.getRuleType(),
            result.isPassed(),
            result.getMessage()
        );
        log.setLogId(java.util.UUID.randomUUID().toString());
        riskRuleEngine.logRiskCheck(log);
        
        return result;
    }
    
    /**
     * 批量报价前风控检查
     */
    public List<RiskRuleEngine.RiskCheckResult> batchPreTradeCheck(List<Quote> quotes) {
        return quotes.stream()
                    .map(this::preTradeCheck)
                    .toList();
    }
    
    /**
     * 动态更新风控参数
     */
    public void updateRiskParameters(RiskConfig newConfig) {
        this.riskConfig = newConfig;
        this.riskRuleEngine.updateRiskConfig(newConfig);
    }
    
    /**
     * 获取当前风控配置
     */
    public RiskConfig getCurrentRiskConfig() {
        return this.riskConfig;
    }
    
    /**
     * 查询风控日志
     */
    public List<RiskAuditLog> queryRiskLogs(LocalDateTime startTime, LocalDateTime endTime) {
        return riskRuleEngine.getAuditLogs().stream()
                .filter(log -> log.getCheckTime().isAfter(startTime) && log.getCheckTime().isBefore(endTime))
                .toList();
    }
    
    /**
     * 查询风控日志（按交易符号）
     */
    public List<RiskAuditLog> queryRiskLogsBySymbol(String symbol) {
        return riskRuleEngine.getAuditLogs().stream()
                .filter(log -> symbol.equals(log.getSymbol()))
                .toList();
    }
    
    /**
     * 查询风控日志（按结果）
     */
    public List<RiskAuditLog> queryRiskLogsByResult(boolean passed) {
        return riskRuleEngine.getAuditLogs().stream()
                .filter(log -> log.getPassed().equals(passed))
                .toList();
    }
    
    /**
     * 获取风控规则引擎实例
     */
    public RiskRuleEngine getRiskRuleEngine() {
        return riskRuleEngine;
    }
    
    /**
     * 重置风控统计数据（如日交易量、持仓等）
     */
    public void resetRiskStatistics() {
        // 重新初始化风控规则引擎以清除统计数据
        this.riskRuleEngine = new RiskRuleEngine(this.riskConfig);
    }
    
    /**
     * 启用/禁用风控
     */
    public void setRiskControlEnabled(boolean enabled) {
        this.riskConfig.setEnabled(enabled);
    }
    
    /**
     * 检查风控是否启用
     */
    public boolean isRiskControlEnabled() {
        return this.riskConfig.isEnabled();
    }
}