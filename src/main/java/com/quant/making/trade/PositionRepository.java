package com.quant.making.trade;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {
    
    /**
     * 根据交易符号查找持仓
     */
    Optional<Position> findBySymbol(String symbol);
    
    /**
     * 根据交易符号查找持仓，如果不存在则创建新的
     */
    default Position findOrCreateBySymbol(String symbol) {
        Optional<Position> positionOpt = findBySymbol(symbol);
        if (positionOpt.isPresent()) {
            return positionOpt.get();
        } else {
            Position newPosition = new Position(symbol);
            return save(newPosition);
        }
    }
}