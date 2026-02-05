package com.quant.making.trade;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 持仓服务类
 */
@Service
@Transactional
public class PositionService {

    @Autowired
    private PositionRepository positionRepository;

    // 内存中的持仓缓存，提高访问性能
    private final ConcurrentHashMap<String, Position> positionCache = new ConcurrentHashMap<>();

    /**
     * 获取持仓信息
     */
    public synchronized Position getPosition(String symbol) {
        // 先从缓存获取
        Position cachedPosition = positionCache.get(symbol);
        if (cachedPosition != null) {
            return cachedPosition;
        }

        // 从数据库获取
        Optional<Position> positionOpt = positionRepository.findBySymbol(symbol);
        Position position;
        if (positionOpt.isPresent()) {
            position = positionOpt.get();
        } else {
            position = new Position(symbol);
            position = positionRepository.save(position);
        }

        // 加入缓存
        positionCache.put(symbol, position);
        return position;
    }

    /**
     * 更新持仓信息
     */
    public synchronized Position updatePosition(String symbol, BigDecimal quantity, BigDecimal price) {
        Position position = getPosition(symbol);

        // 更新持仓数量和平均价格
        position.increaseQuantity(quantity, price);

        // 保存到数据库
        position = positionRepository.save(position);

        // 更新缓存
        positionCache.put(symbol, position);

        return position;
    }

    /**
     * 减持或平仓
     */
    public synchronized Position decreasePosition(String symbol, BigDecimal quantity) {
        Position position = getPosition(symbol);

        // 减少持仓数量
        position.decreaseQuantity(quantity);

        // 保存到数据库
        position = positionRepository.save(position);

        // 更新缓存
        positionCache.put(symbol, position);

        return position;
    }

    /**
     * 冻结持仓数量
     */
    public synchronized boolean freezePosition(String symbol, BigDecimal quantity) {
        Position position = getPosition(symbol);

        boolean success = position.freezeQuantity(quantity);
        if (success) {
            // 保存到数据库
            position = positionRepository.save(position);

            // 更新缓存
            positionCache.put(symbol, position);
        }

        return success;
    }

    /**
     * 解冻持仓数量
     */
    public synchronized boolean unfreezePosition(String symbol, BigDecimal quantity) {
        Position position = getPosition(symbol);

        boolean success = position.unfreezeQuantity(quantity);
        if (success) {
            // 保存到数据库
            position = positionRepository.save(position);

            // 更新缓存
            positionCache.put(symbol, position);
        }

        return success;
    }

    /**
     * 获取所有持仓
     */
    public List<Position> getAllPositions() {
        return positionRepository.findAll();
    }

    /**
     * 根据交易符号列表获取持仓
     */
    public List<Position> getPositionsBySymbols(List<String> symbols) {
        return positionRepository.findAll();
    }

    /**
     * 清除缓存（通常在系统重启或需要强制刷新时使用）
     */
    public void clearCache() {
        positionCache.clear();
    }

    /**
     * 刷新特定符号的缓存
     */
    public void refreshCache(String symbol) {
        Optional<Position> positionOpt = positionRepository.findBySymbol(symbol);
        if (positionOpt.isPresent()) {
            positionCache.put(symbol, positionOpt.get());
        } else {
            positionCache.remove(symbol);
        }
    }
}