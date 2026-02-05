package com.quant.making.trade;

import com.quant.making.audit.AuditService;
import com.quant.making.quote.Quote;
import com.quant.making.quote.QuoteService;
import com.quant.making.risk.RiskControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 成交回报服务类
 */
@Service
@Transactional
public class TradeReportService {

    @Autowired
    private TradeReportRepository tradeReportRepository;

    @Autowired
    private PositionService positionService;

    @Autowired
    private QuoteService quoteService;

    @Autowired
    private RiskControlService riskControlService;

    @Autowired
    private AuditService auditService;

    // 用于确保同一交易符号的交易处理是线程安全的
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * 处理成交回报
     */
    public TradeReport processTradeReport(String quoteId, String symbol, Side side, 
                                         BigDecimal price, BigDecimal quantity) {
        lock.lock();
        try {
            // 创建成交回报对象
            TradeReport tradeReport = new TradeReport(quoteId, symbol, side, price, quantity);
            tradeReport.setTimestamp(LocalDateTime.now());
            tradeReport.setStatus(TradeStatus.EXECUTED);

            // 保存成交回报
            tradeReport = tradeReportRepository.save(tradeReport);

            // 更新报价状态
            updateQuoteStatus(quoteId, TradeStatus.EXECUTED);

            // 更新持仓信息
            updatePosition(symbol, side, quantity, price);

            // 计算实现盈亏
            BigDecimal realizedPnL = calculateRealizedPnL(symbol, side, price);

            // 触发事后风控检查
            performPostTradeRiskCheck(tradeReport, realizedPnL);

            // 记录审计日志
            auditService.logTradeExecution(tradeReport, realizedPnL);

            return tradeReport;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 处理被拒绝的成交
     */
    public TradeReport processRejectedTrade(String quoteId, String symbol, String reason) {
        lock.lock();
        try {
            TradeReport tradeReport = new TradeReport(quoteId, symbol, null, BigDecimal.ZERO, BigDecimal.ZERO);
            tradeReport.setTimestamp(LocalDateTime.now());
            tradeReport.setStatus(TradeStatus.REJECTED);

            // 保存成交回报
            tradeReport = tradeReportRepository.save(tradeReport);

            // 更新报价状态
            updateQuoteStatus(quoteId, TradeStatus.REJECTED);

            // 记录审计日志
            auditService.logTradeRejection(tradeReport, reason);

            return tradeReport;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 处理被取消的成交
     */
    public TradeReport processCancelledTrade(String quoteId, String symbol) {
        lock.lock();
        try {
            TradeReport tradeReport = new TradeReport(quoteId, symbol, null, BigDecimal.ZERO, BigDecimal.ZERO);
            tradeReport.setTimestamp(LocalDateTime.now());
            tradeReport.setStatus(TradeStatus.CANCELLED);

            // 保存成交回报
            tradeReport = tradeReportRepository.save(tradeReport);

            // 更新报价状态
            updateQuoteStatus(quoteId, TradeStatus.CANCELLED);

            // 记录审计日志
            auditService.logTradeCancellation(tradeReport);

            return tradeReport;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 根据报价ID查找成交回报
     */
    public List<TradeReport> getTradeReportsByQuoteId(String quoteId) {
        return tradeReportRepository.findByQuoteId(quoteId);
    }

    /**
     * 根据交易符号查找成交回报
     */
    public List<TradeReport> getTradeReportsBySymbol(String symbol) {
        return tradeReportRepository.findBySymbol(symbol);
    }

    /**
     * 根据状态查找成交回报
     */
    public List<TradeReport> getTradeReportsByStatus(TradeStatus status) {
        return tradeReportRepository.findByStatus(status);
    }

    /**
     * 更新报价状态
     */
    private void updateQuoteStatus(String quoteId, TradeStatus status) {
        // 这里调用报价服务来更新报价状态
        // 实际实现中可能需要根据状态映射到对应的QuoteStatus
        // quoteService.updateQuoteStatus(quoteId, mapTradeStatusToQuoteStatus(status));
    }

    /**
     * 更新持仓信息
     */
    private void updatePosition(String symbol, Side side, BigDecimal quantity, BigDecimal price) {
        if (side == Side.BUY) {
            // 买入增加多头持仓
            positionService.updatePosition(symbol, quantity, price);
        } else if (side == Side.SELL) {
            // 卖出减少持仓（可能是平多或开空）
            positionService.decreasePosition(symbol, quantity.abs());
        }
    }

    /**
     * 计算实现盈亏
     */
    private BigDecimal calculateRealizedPnL(String symbol, Side side, BigDecimal price) {
        // 获取当前持仓
        Position position = positionService.getPosition(symbol);

        // 这里简化实现，实际的盈亏计算可能更复杂
        // 基于平均持仓成本和当前成交价格计算盈亏
        if (position.getAvgPrice().compareTo(BigDecimal.ZERO) == 0) {
            // 如果没有持仓，则不计算盈亏
            return BigDecimal.ZERO;
        }

        BigDecimal pnl = BigDecimal.ZERO;
        if (side == Side.SELL && position.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
            // 卖出平多仓，计算盈亏
            BigDecimal priceDiff = price.subtract(position.getAvgPrice());
            pnl = priceDiff.multiply(position.getQuantity());
        } else if (side == Side.BUY && position.getQuantity().compareTo(BigDecimal.ZERO) < 0) {
            // 买入平空仓，计算盈亏
            BigDecimal priceDiff = position.getAvgPrice().subtract(price);
            pnl = priceDiff.multiply(position.getQuantity().abs());
        }

        return pnl;
    }

    /**
     * 执行事后风控检查
     */
    private void performPostTradeRiskCheck(TradeReport tradeReport, BigDecimal realizedPnL) {
        // 使用风控服务进行事后检查
        // 这里可以检查单日累计盈亏、单品种持仓等
        riskControlService.performPostTradeCheck(tradeReport, realizedPnL);
    }

    /**
     * 获取某个交易符号的总成交量
     */
    public BigDecimal getTotalVolumeBySymbol(String symbol) {
        List<TradeReport> reports = tradeReportRepository.findBySymbol(symbol);
        return reports.stream()
                .map(TradeReport::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 获取某个交易符号的成交金额
     */
    public BigDecimal getTotalTurnoverBySymbol(String symbol) {
        List<TradeReport> reports = tradeReportRepository.findBySymbol(symbol);
        return reports.stream()
                .map(report -> report.getPrice().multiply(report.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}