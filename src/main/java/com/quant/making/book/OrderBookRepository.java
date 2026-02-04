package com.quant.making.book;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单簿数据访问接口
 */
@Repository
public interface OrderBookRepository extends JpaRepository<OrderBookEntry, Long> {
    
    /**
     * 根据品种代码和时间范围查询快照
     */
    @Query("SELECT e FROM OrderBookEntry e WHERE e.symbol = :symbol " +
           "AND e.snapshotTime >= :startTime AND e.snapshotTime <= :endTime " +
           "ORDER BY e.priceLevel, e.side")
    List<OrderBookEntry> findBySymbolAndSnapshotTimeBetween(
            @Param("symbol") String symbol,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 删除指定时间之前的旧快照
     */
    @Modifying
    @Query("DELETE FROM OrderBookEntry e WHERE e.snapshotTime < :cutoffTime")
    int deleteOldSnapshots(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * 根据品种和市场类型查询最新快照
     */
    @Query("SELECT e FROM OrderBookEntry e WHERE e.symbol = :symbol " +
           "AND e.marketType = :marketType " +
           "AND e.snapshotTime = (SELECT MAX(e2.snapshotTime) FROM OrderBookEntry e2 " +
           "WHERE e2.symbol = :symbol AND e2.marketType = :marketType)")
    List<OrderBookEntry> findLatestSnapshot(
            @Param("symbol") String symbol,
            @Param("marketType") Integer marketType);
    
    /**
     * 根据品种代码查询所有相关价源
     */
    @Query("SELECT DISTINCT e.source FROM OrderBookEntry e WHERE e.symbol = :symbol")
    List<String> findDistinctSourcesBySymbol(@Param("symbol") String symbol);
}
