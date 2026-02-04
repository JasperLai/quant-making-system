package com.quant.making.risk;

import com.quant.making.quote.Quote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 风控服务测试类
 */
public class RiskControlServiceTest {
    
    private RiskConfig riskConfig;
    private RiskControlService riskControlService;
    
    @BeforeEach
    void setUp() {
        riskConfig = new RiskConfig();
        riskConfig.setMaxSingleTradeAmount(new BigDecimal("1000"));
        riskConfig.setMaxDailyTradeAmount(new BigDecimal("5000"));
        riskConfig.setMaxPositionPerSymbol(new BigDecimal("2000"));
        riskConfig.setMaxSpreadLimit(new BigDecimal("0.05")); // 5%
        riskConfig.setMaxLevelDeviation(3);
        riskConfig.setMaxLossLimit(new BigDecimal("-500")); // -500
        
        riskControlService = new RiskControlService(riskConfig);
    }
    
    @Test
    void testPreTradeCheckPass() {
        Quote validQuote = new Quote("TEST_SYMBOL", 1, Quote.BUY, 
                                   new BigDecimal("100"), new BigDecimal("5"));
        validQuote.setSpread(new BigDecimal("0.02"));
        validQuote.setLevel(1);
        
        RiskRuleEngine.RiskCheckResult result = riskControlService.preTradeCheck(validQuote);
        
        assertTrue(result.isPassed());
        assertEquals("前置风控检查通过", result.getMessage());
    }
    
    @Test
    void testPreTradeCheckFail() {
        Quote invalidQuote = new Quote("TEST_SYMBOL", 1, Quote.BUY, 
                                     new BigDecimal("2000"), new BigDecimal("2")); // 4000 > 1000 limit
        
        RiskRuleEngine.RiskCheckResult result = riskControlService.preTradeCheck(invalidQuote);
        
        assertFalse(result.isPassed());
        assertTrue(result.getMessage().contains("单笔交易金额"));
    }
    
    @Test
    void testPostTradeCheckPass() {
        Quote validQuote = new Quote("TEST_SYMBOL", 1, Quote.BUY, 
                                   new BigDecimal("100"), new BigDecimal("5"));
        BigDecimal profit = new BigDecimal("100");
        
        RiskRuleEngine.RiskCheckResult result = riskControlService.postTradeCheck(validQuote, profit);
        
        assertTrue(result.isPassed());
        assertEquals("事后风控检查通过", result.getMessage());
    }
    
    @Test
    void testPostTradeCheckFail() {
        Quote quote = new Quote("TEST_SYMBOL", 1, Quote.BUY, 
                              new BigDecimal("100"), new BigDecimal("5"));
        BigDecimal loss = new BigDecimal("-600"); // 超过-500限制
        
        RiskRuleEngine.RiskCheckResult result = riskControlService.postTradeCheck(quote, loss);
        
        assertFalse(result.isPassed());
        assertTrue(result.getMessage().contains("亏损"));
    }
    
    @Test
    void testBatchPreTradeCheck() {
        Quote validQuote = new Quote("TEST_SYMBOL1", 1, Quote.BUY, 
                                   new BigDecimal("100"), new BigDecimal("5"));
        Quote invalidQuote = new Quote("TEST_SYMBOL2", 1, Quote.BUY, 
                                     new BigDecimal("2000"), new BigDecimal("2")); // Exceeds limit
        
        List<Quote> quotes = Arrays.asList(validQuote, invalidQuote);
        List<RiskRuleEngine.RiskCheckResult> results = riskControlService.batchPreTradeCheck(quotes);
        
        assertEquals(2, results.size());
        assertTrue(results.get(0).isPassed()); // Valid quote passes
        assertFalse(results.get(1).isPassed()); // Invalid quote fails
    }
    
    @Test
    void testUpdateRiskParameters() {
        RiskConfig newConfig = new RiskConfig();
        newConfig.setMaxSingleTradeAmount(new BigDecimal("5000"));
        newConfig.setMaxSpreadLimit(new BigDecimal("0.1")); // 10%
        
        riskControlService.updateRiskParameters(newConfig);
        
        RiskConfig currentConfig = riskControlService.getCurrentRiskConfig();
        assertEquals(new BigDecimal("5000"), currentConfig.getMaxSingleTradeAmount());
        assertEquals(new BigDecimal("0.1"), currentConfig.getMaxSpreadLimit());
    }
    
    @Test
    void testGetCurrentRiskConfig() {
        RiskConfig currentConfig = riskControlService.getCurrentRiskConfig();
        assertEquals(new BigDecimal("1000"), currentConfig.getMaxSingleTradeAmount());
        assertEquals(new BigDecimal("5000"), currentConfig.getMaxDailyTradeAmount());
    }
    
    @Test
    void testRiskControlEnableDisable() {
        assertTrue(riskControlService.isRiskControlEnabled());
        
        riskControlService.setRiskControlEnabled(false);
        assertFalse(riskControlService.isRiskControlEnabled());
        
        riskControlService.setRiskControlEnabled(true);
        assertTrue(riskControlService.isRiskControlEnabled());
    }
    
    @Test
    void testQueryRiskLogs() {
        Quote quote = new Quote("TEST_SYMBOL", 1, Quote.BUY, 
                              new BigDecimal("100"), new BigDecimal("5"));
        
        // 执行一次风控检查
        riskControlService.preTradeCheck(quote);
        
        // 查询日志
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        List<RiskAuditLog> logs = riskControlService.queryRiskLogs(oneHourAgo, now.plusHours(1));
        
        assertEquals(1, logs.size());
        assertEquals("TEST_SYMBOL", logs.get(0).getSymbol());
        assertEquals(quote.getQuoteId(), logs.get(0).getTradeId());
    }
    
    @Test
    void testQueryRiskLogsBySymbol() {
        Quote quote1 = new Quote("AAPL", 1, Quote.BUY, 
                                new BigDecimal("150"), new BigDecimal("10"));
        Quote quote2 = new Quote("GOOGL", 1, Quote.BUY, 
                                new BigDecimal("2500"), new BigDecimal("5"));
        
        riskControlService.preTradeCheck(quote1);
        riskControlService.preTradeCheck(quote2);
        
        List<RiskAuditLog> aaplLogs = riskControlService.queryRiskLogsBySymbol("AAPL");
        List<RiskAuditLog> googlLogs = riskControlService.queryRiskLogsBySymbol("GOOGL");
        
        assertEquals(1, aaplLogs.size());
        assertEquals(1, googlLogs.size());
        assertEquals("AAPL", aaplLogs.get(0).getSymbol());
        assertEquals("GOOGL", googlLogs.get(0).getSymbol());
    }
    
    @Test
    void testQueryRiskLogsByResult() {
        Quote validQuote = new Quote("VALID_SYMBOL", 1, Quote.BUY, 
                                   new BigDecimal("100"), new BigDecimal("5"));
        Quote invalidQuote = new Quote("INVALID_SYMBOL", 1, Quote.BUY, 
                                     new BigDecimal("2000"), new BigDecimal("2")); // Exceeds limit
        
        riskControlService.preTradeCheck(validQuote);  // Should pass
        riskControlService.preTradeCheck(invalidQuote); // Should fail
        
        List<RiskAuditLog> passedLogs = riskControlService.queryRiskLogsByResult(true);
        List<RiskAuditLog> failedLogs = riskControlService.queryRiskLogsByResult(false);
        
        // 验证至少有一个通过和一个失败的日志
        assertTrue(passedLogs.size() >= 1);
        assertTrue(failedLogs.size() >= 1);
        
        // 检查通过的日志确实标记为通过
        for (RiskAuditLog log : passedLogs) {
            assertTrue(log.getPassed());
        }
        
        // 检查失败的日志确实标记为失败
        for (RiskAuditLog log : failedLogs) {
            assertFalse(log.getPassed());
        }
    }
    
    @Test
    void testGetRiskRuleEngine() {
        assertNotNull(riskControlService.getRiskRuleEngine());
        
        // 确保获取的是同一个实例
        RiskRuleEngine engine1 = riskControlService.getRiskRuleEngine();
        RiskRuleEngine engine2 = riskControlService.getRiskRuleEngine();
        
        assertSame(engine1, engine2);
    }
    
    @Test
    void testResetRiskStatistics() {
        Quote quote = new Quote("TEST_SYMBOL", 1, Quote.BUY, 
                              new BigDecimal("100"), new BigDecimal("5"));
        
        // 执行几次风控检查
        riskControlService.preTradeCheck(quote);
        riskControlService.preTradeCheck(quote);
        
        // 获取日志数量
        List<RiskAuditLog> logsBeforeReset = riskControlService.getRiskRuleEngine().getAuditLogs();
        int countBeforeReset = logsBeforeReset.size();
        
        // 重置统计
        riskControlService.resetRiskStatistics();
        
        // 检查日志是否被清空或重置
        List<RiskAuditLog> logsAfterReset = riskControlService.getRiskRuleEngine().getAuditLogs();
        // 注意：resetRiskStatistics()方法会重新创建RiskRuleEngine实例，所以日志会被清空
        assertTrue(logsAfterReset.isEmpty());
    }
}