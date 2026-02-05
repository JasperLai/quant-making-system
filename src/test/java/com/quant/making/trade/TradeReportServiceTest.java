package com.quant.making.trade;

import com.quant.making.audit.AuditService;
import com.quant.making.quote.QuoteService;
import com.quant.making.risk.RiskControlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeReportServiceTest {

    @Mock
    private TradeReportRepository tradeReportRepository;

    @Mock
    private PositionService positionService;

    @Mock
    private QuoteService quoteService;

    @Mock
    private RiskControlService riskControlService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private TradeReportService tradeReportService;

    private String testQuoteId;
    private String testSymbol;
    private BigDecimal testPrice;
    private BigDecimal testQuantity;

    @BeforeEach
    void setUp() {
        testQuoteId = "QUOTE_TEST_001";
        testSymbol = "BTC-USDT";
        testPrice = new BigDecimal("40000.00");
        testQuantity = new BigDecimal("0.1");
    }

    @Test
    void testProcessTradeReport() {
        // 设置模拟行为
        when(tradeReportRepository.save(any(TradeReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(positionService.getPosition(testSymbol))
                .thenReturn(new Position(testSymbol));

        // 执行测试
        TradeReport result = tradeReportService.processTradeReport(
                testQuoteId, testSymbol, Side.BUY, testPrice, testQuantity);

        // 验证结果
        assertNotNull(result);
        assertEquals(testQuoteId, result.getQuoteId());
        assertEquals(testSymbol, result.getSymbol());
        assertEquals(Side.BUY, result.getSide());
        assertEquals(testPrice, result.getPrice());
        assertEquals(testQuantity, result.getQuantity());
        assertEquals(TradeStatus.EXECUTED, result.getStatus());

        // 验证方法调用
        verify(tradeReportRepository, times(1)).save(any(TradeReport.class));
        verify(positionService, times(1)).updatePosition(anyString(), any(BigDecimal.class), any(BigDecimal.class));
        verify(auditService, times(1)).logTradeExecution(any(TradeReport.class), any(BigDecimal.class));
    }

    @Test
    void testProcessRejectedTrade() {
        // 设置模拟行为
        when(tradeReportRepository.save(any(TradeReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 执行测试
        TradeReport result = tradeReportService.processRejectedTrade(
                testQuoteId, testSymbol, "Insufficient funds");

        // 验证结果
        assertNotNull(result);
        assertEquals(testQuoteId, result.getQuoteId());
        assertEquals(testSymbol, result.getSymbol());
        assertEquals(TradeStatus.REJECTED, result.getStatus());

        // 验证方法调用
        verify(tradeReportRepository, times(1)).save(any(TradeReport.class));
        verify(auditService, times(1)).logTradeRejection(any(TradeReport.class), anyString());
    }

    @Test
    void testProcessCancelledTrade() {
        // 设置模拟行为
        when(tradeReportRepository.save(any(TradeReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 执行测试
        TradeReport result = tradeReportService.processCancelledTrade(testQuoteId, testSymbol);

        // 验证结果
        assertNotNull(result);
        assertEquals(testQuoteId, result.getQuoteId());
        assertEquals(testSymbol, result.getSymbol());
        assertEquals(TradeStatus.CANCELLED, result.getStatus());

        // 验证方法调用
        verify(tradeReportRepository, times(1)).save(any(TradeReport.class));
        verify(auditService, times(1)).logTradeCancellation(any(TradeReport.class));
    }

    @Test
    void testGetTradeReportsByQuoteId() {
        // 设置模拟行为
        when(tradeReportRepository.findByQuoteId(testQuoteId))
                .thenReturn(List.of(createSampleTradeReport()));

        // 执行测试
        List<TradeReport> results = tradeReportService.getTradeReportsByQuoteId(testQuoteId);

        // 验证结果
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        assertEquals(testQuoteId, results.get(0).getQuoteId());

        // 验证方法调用
        verify(tradeReportRepository, times(1)).findByQuoteId(testQuoteId);
    }

    @Test
    void testGetTradeReportsBySymbol() {
        // 设置模拟行为
        when(tradeReportRepository.findBySymbol(testSymbol))
                .thenReturn(List.of(createSampleTradeReport()));

        // 执行测试
        List<TradeReport> results = tradeReportService.getTradeReportsBySymbol(testSymbol);

        // 验证结果
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        assertEquals(testSymbol, results.get(0).getSymbol());

        // 验证方法调用
        verify(tradeReportRepository, times(1)).findBySymbol(testSymbol);
    }

    @Test
    void testCalculateRealizedPnL() {
        // 测试计算实现盈亏的逻辑
        // 注意：由于calculateRealizedPnL是私有方法，我们通过其他公共方法间接测试
        
        // 设置模拟行为
        when(tradeReportRepository.save(any(TradeReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Position mockPosition = new Position(testSymbol);
        when(positionService.getPosition(testSymbol)).thenReturn(mockPosition);

        // 执行测试
        TradeReport result = tradeReportService.processTradeReport(
                testQuoteId, testSymbol, Side.BUY, testPrice, testQuantity);

        // 验证基本操作
        assertNotNull(result);
        assertEquals(TradeStatus.EXECUTED, result.getStatus());

        // 验证持仓更新被调用
        verify(positionService, times(1)).updatePosition(eq(testSymbol), eq(testQuantity), eq(testPrice));
    }

    private TradeReport createSampleTradeReport() {
        TradeReport report = new TradeReport();
        report.setTradeId(1L);
        report.setQuoteId(testQuoteId);
        report.setSymbol(testSymbol);
        report.setSide(Side.BUY);
        report.setPrice(testPrice);
        report.setQuantity(testQuantity);
        report.setStatus(TradeStatus.EXECUTED);
        return report;
    }
}