package com.quant.making.trade;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeReportRepository extends JpaRepository<TradeReport, Long> {
    
    /**
     * 根据报价ID查找成交回报
     */
    List<TradeReport> findByQuoteId(String quoteId);
    
    /**
     * 根据交易符号查找成交回报
     */
    List<TradeReport> findBySymbol(String symbol);
    
    /**
     * 根据交易符号和状态查找成交回报
     */
    List<TradeReport> findBySymbolAndStatus(String symbol, TradeStatus status);
    
    /**
     * 根据状态查找成交回报
     */
    List<TradeReport> findByStatus(TradeStatus status);
}