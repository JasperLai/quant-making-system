package com.quant.making.risk;

import com.quant.making.quote.Quote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 风控规则引擎测试类
 */
public class RiskRuleEngineTest {
    
    private RiskConfig riskConfig;
    private RiskRuleEngine riskRuleEngine;
    
    @BeforeEach
    void setUp() {
        riskConfig = new RiskConfig();
        riskConfig.setMaxSingleTradeAmount(new BigDecimal("1000"));
        riskConfig.setMaxDailyTradeAmount(new BigDecimal("5000"));
        riskConfig.setMaxPositionPerSymbol(new BigDecimal("2000"));
        riskConfig.setMaxSpreadLimit(new BigDecimal("0.05")); // 5%
        riskConfig.setMaxLevelDeviation(3);
        riskConfig.setBlacklist(Arrays.asList("BLOCKED_SYMBOL"));
        riskConfig.setWhitelist(Arrays.asList("ALLOWED_SYMBOL"));
        riskConfig.setMaxOrdersPerSecond(5);
        riskConfig.setMaxLossLimit(new BigDecimal("-500")); // -500
        
        riskRuleEngine = new RiskRuleEngine(riskConfig);
    }
    
    @Test
    void testPreTradeCheckPass() {
        // 创建一个符合所有风控条件的报价
        Quote validQuote = new Quote("VALID_SYMBOL", 1, Quote.BUY, 
                                    new BigDecimal("100"), new BigDecimal("5"));
        validQuote.setSpread(new BigDecimal("0.02")); // 2% spread
        validQuote.setLevel(1);
        
        RiskRuleEngine.RiskCheckResult result = riskRuleEngine.preTradeCheck(validQuote);
        
        assertTrue(result.isPassed());
        assertEquals("前置风控检查通过", result.getMessage());
    }
    
    @Test
    void testSingleTradeAmountLimit() {
        // 创建一个超出单笔交易金额限制的报价
        Quote invalidQuote = new Quote("TEST_SYMBOL", 1, Quote.BUY, 
                                      new BigDecimal("1500"), new BigDecimal("2"));
        
        RiskRuleEngine.RiskCheckResult result = riskRuleEngine.preTradeCheck(invalidQuote);
        
        assertFalse(result.isPassed());
        assertTrue(result.getMessage().contains("单笔交易金额"));
    }
    
    @Test
    void testBlacklistCheck() {
        // 创建一个在黑名单中的报价
        Quote blacklistedQuote = new Quote("BLOCKED_SYMBOL", 1, Quote.BUY, 
                                          new BigDecimal("100"), new BigDecimal("1"));
        
        RiskRuleEngine.RiskCheckResult result = riskRuleEngine.preTradeCheck(blacklistedQuote);
        
        assertFalse(result.isPassed());
        assertTrue(result.getMessage().contains("黑名单"));
    }
    
    @Test
    void testWhitelistCheck() {
        // 创建一个不在白名单中的报价
        Quote nonWhitelistedQuote = new Quote("NON_ALLOWED_SYMBOL", 1, Quote.BUY, 
                                             new BigDecimal("100"), new BigDecimal("1"));
        
        RiskRuleEngine.RiskCheckResult result = riskRuleEngine.preTradeCheck(nonWhitelistedQuote);
        
        assertFalse(result.isPassed());
        assertTrue(result.getMessage().contains("白名单"));
    }
    
    @Test
    void testWhitelistWithAllowedSymbol() {
        // 创建一个在白名单中的报价
        Quote whitelistedQuote = new Quote("ALLOWED_SYMBOL", 1, Quote.BUY, 
                                          new BigDecimal("100"), new BigDecimal("1"));
        
        RiskRuleEngine.RiskCheckResult result = riskRuleEngine.preTradeCheck(whitelistedQuote);
        
        assertTrue(result.isPassed());
    }
    
    @Test
    void testLevelDeviationLimit() {
        // 创建一个档位偏离超标的报价
        Quote highLevelQuote = new Quote("TEST_SYMBOL", 1, Quote.BUY, 
                                        new BigDecimal("100"), new BigDecimal("1"));
        highLevelQuote.setLevel(5); // 超过最大3档
        
        RiskRuleEngine.RiskCheckResult result = riskRuleEngine.preTradeCheck(highLevelQuote);
        
        assertFalse(result.isPassed());
        assertTrue(result.getMessage().contains("档位"));
    }
    
    @Test
    void testSpreadLimit() {
        // 创建一个点差超标的报价
        Quote highSpreadQuote = new Quote("TEST_SYMBOL", 1, Quote.BUY, 
                                         new BigDecimal("100"), new BigDecimal("1"));
        highSpreadQuote.setSpread(new BigDecimal("0.1")); // 10% spread, 超过5%限制
        
        RiskRuleEngine.RiskCheckResult result = riskRuleEngine.preTradeCheck(highSpreadQuote);
        
        assertFalse(result.isPassed());
        assertTrue(result.getMessage().contains("点差"));
    }
    
    @Test
    void testPostTradeCheckPass() {
        // 创建一个符合所有事后风控条件的成交
        Quote validExecutedQuote = new Quote("TEST_SYMBOL", 1, Quote.BUY, 
                                            new BigDecimal("100"), new BigDecimal("5"));
        BigDecimal pnl = new BigDecimal("100"); // 盈利100
        
        RiskRuleEngine.RiskCheckResult result = riskRuleEngine.postTradeCheck(validExecutedQuote, pnl);
        
        assertTrue(result.isPassed());
        assertEquals("事后风控检查通过", result.getMessage());
    }
    
    @Test
    void testDailyTradeAmountLimit() {
        Quote quote = new Quote("TEST_SYMBOL", 1, Quote.BUY, 
                               new BigDecimal("1000"), new BigDecimal("6")); // 6000 > 5000 limit
        
        RiskRuleEngine.RiskCheckResult result = riskRuleEngine.postTradeCheck(quote, new BigDecimal("100"));
        
        assertFalse(result.isPassed());
        assertTrue(result.getMessage().contains("单日交易金额"));
    }
    
    @Test
    void testLossLimit() {
        Quote quote = new Quote("TEST_SYMBOL", 1, Quote.BUY, 
                               new BigDecimal("100"), new BigDecimal("5"));
        BigDecimal largeLoss = new BigDecimal("-600"); // 超过-500的亏损限制
        
        RiskRuleEngine.RiskCheckResult result = riskRuleEngine.postTradeCheck(quote, largeLoss);
        
        assertFalse(result.isPassed());
        assertTrue(result.getMessage().contains("亏损"));
    }
    
    @Test
    void testUpdateRiskConfig() {
        RiskConfig newConfig = new RiskConfig();
        newConfig.setMaxSingleTradeAmount(new BigDecimal("5000"));
        newConfig.setEnabled(true);
        
        riskRuleEngine.updateRiskConfig(newConfig);
        
        assertEquals(new BigDecimal("5000"), riskRuleEngine.getRiskConfig().getMaxSingleTradeAmount());
    }
    
    @Test
    void testDisabledRiskControl() {
        riskConfig.setEnabled(false);
        Quote quote = new Quote("TEST_SYMBOL", 1, Quote.BUY, 
                               new BigDecimal("10000"), new BigDecimal("10")); // 远超限制
        
        RiskRuleEngine.RiskCheckResult result = riskRuleEngine.preTradeCheck(quote);
        
        assertTrue(result.isPassed());
        assertEquals("风控未启用", result.getMessage());
    }
}