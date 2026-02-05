package com.quant.making.trade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PositionServiceTest {

    @Mock
    private PositionRepository positionRepository;

    @InjectMocks
    private PositionService positionService;

    private String testSymbol;

    @BeforeEach
    void setUp() {
        testSymbol = "BTC-USDT";
        // 清除缓存以确保测试独立性
        positionService.clearCache();
    }

    @Test
    void testGetPosition_Existing() {
        // 设置模拟行为
        Position existingPosition = new Position(testSymbol);
        existingPosition.setPositionId(1L);
        existingPosition.setQuantity(new BigDecimal("1.5"));
        
        when(positionRepository.findBySymbol(testSymbol))
                .thenReturn(Optional.of(existingPosition));

        // 执行测试
        Position result = positionService.getPosition(testSymbol);

        // 验证结果
        assertNotNull(result);
        assertEquals(testSymbol, result.getSymbol());
        assertEquals(new BigDecimal("1.5"), result.getQuantity());
        verify(positionRepository, times(1)).findBySymbol(testSymbol);
    }

    @Test
    void testGetPosition_New() {
        // 设置模拟行为
        when(positionRepository.findBySymbol(testSymbol))
                .thenReturn(Optional.empty());
        
        Position newPosition = new Position(testSymbol);
        newPosition.setPositionId(2L);
        when(positionRepository.save(any(Position.class)))
                .thenReturn(newPosition);

        // 执行测试
        Position result = positionService.getPosition(testSymbol);

        // 验证结果
        assertNotNull(result);
        assertEquals(testSymbol, result.getSymbol());
        assertEquals(BigDecimal.ZERO, result.getQuantity());
        verify(positionRepository, times(1)).findBySymbol(testSymbol);
        verify(positionRepository, times(1)).save(any(Position.class));
    }

    @Test
    void testUpdatePosition() {
        // 设置模拟行为
        Position existingPosition = new Position(testSymbol);
        existingPosition.setPositionId(1L);
        existingPosition.setQuantity(BigDecimal.ZERO);
        existingPosition.setAvgPrice(BigDecimal.ZERO);
        
        when(positionRepository.findBySymbol(testSymbol))
                .thenReturn(Optional.of(existingPosition));
        when(positionRepository.save(any(Position.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BigDecimal quantityToAdd = new BigDecimal("0.5");
        BigDecimal price = new BigDecimal("40000.00");

        // 执行测试
        Position result = positionService.updatePosition(testSymbol, quantityToAdd, price);

        // 验证结果
        assertNotNull(result);
        assertEquals(testSymbol, result.getSymbol());
        assertEquals(quantityToAdd, result.getQuantity());
        assertEquals(price, result.getAvgPrice()); // 平均价格应该等于新价格，因为之前是0
        
        verify(positionRepository, times(1)).findBySymbol(testSymbol);
        verify(positionRepository, times(1)).save(any(Position.class));
    }

    @Test
    void testDecreasePosition() {
        // 设置模拟行为
        Position existingPosition = new Position(testSymbol);
        existingPosition.setPositionId(1L);
        existingPosition.setQuantity(new BigDecimal("1.0"));
        
        when(positionRepository.findBySymbol(testSymbol))
                .thenReturn(Optional.of(existingPosition));
        when(positionRepository.save(any(Position.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BigDecimal quantityToSubtract = new BigDecimal("0.3");

        // 执行测试
        Position result = positionService.decreasePosition(testSymbol, quantityToSubtract);

        // 验证结果
        assertNotNull(result);
        assertEquals(testSymbol, result.getSymbol());
        assertEquals(new BigDecimal("0.7"), result.getQuantity()); // 1.0 - 0.3 = 0.7
        
        verify(positionRepository, times(1)).findBySymbol(testSymbol);
        verify(positionRepository, times(1)).save(any(Position.class));
    }

    @Test
    void testFreezePosition_Success() {
        // 设置模拟行为
        Position existingPosition = new Position(testSymbol);
        existingPosition.setPositionId(1L);
        existingPosition.setQuantity(new BigDecimal("1.0"));
        existingPosition.setFrozenQuantity(BigDecimal.ZERO);
        
        when(positionRepository.findBySymbol(testSymbol))
                .thenReturn(Optional.of(existingPosition));
        when(positionRepository.save(any(Position.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BigDecimal quantityToFreeze = new BigDecimal("0.5");

        // 执行测试
        boolean result = positionService.freezePosition(testSymbol, quantityToFreeze);

        // 验证结果
        assertTrue(result);
        
        verify(positionRepository, times(1)).findBySymbol(testSymbol);
        verify(positionRepository, times(1)).save(any(Position.class));
    }

    @Test
    void testFreezePosition_Failure() {
        // 设置模拟行为
        Position existingPosition = new Position(testSymbol);
        existingPosition.setPositionId(1L);
        existingPosition.setQuantity(new BigDecimal("0.3")); // 总共只有0.3
        existingPosition.setFrozenQuantity(BigDecimal.ZERO);
        
        when(positionRepository.findBySymbol(testSymbol))
                .thenReturn(Optional.of(existingPosition));

        BigDecimal quantityToFreeze = new BigDecimal("0.5"); // 尝试冻结0.5，但只有0.3可用

        // 执行测试
        boolean result = positionService.freezePosition(testSymbol, quantityToFreeze);

        // 验证结果
        assertFalse(result);
        
        verify(positionRepository, times(1)).findBySymbol(testSymbol);
        verify(positionRepository, never()).save(any(Position.class)); // 不应该保存，因为冻结失败
    }

    @Test
    void testUnfreezePosition() {
        // 设置模拟行为
        Position existingPosition = new Position(testSymbol);
        existingPosition.setPositionId(1L);
        existingPosition.setQuantity(new BigDecimal("1.0"));
        existingPosition.setFrozenQuantity(new BigDecimal("0.5"));
        
        when(positionRepository.findBySymbol(testSymbol))
                .thenReturn(Optional.of(existingPosition));
        when(positionRepository.save(any(Position.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BigDecimal quantityToUnfreeze = new BigDecimal("0.2");

        // 执行测试
        boolean result = positionService.unfreezePosition(testSymbol, quantityToUnfreeze);

        // 验证结果
        assertTrue(result);
        
        verify(positionRepository, times(1)).findBySymbol(testSymbol);
        verify(positionRepository, times(1)).save(any(Position.class));
    }

    @Test
    void testGetAllPositions() {
        // 设置模拟行为
        Position position1 = new Position("BTC-USDT");
        Position position2 = new Position("ETH-USDT");
        List<Position> allPositions = List.of(position1, position2);
        
        when(positionRepository.findAll())
                .thenReturn(allPositions);

        // 执行测试
        List<Position> result = positionService.getAllPositions();

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        
        verify(positionRepository, times(1)).findAll();
    }

    @Test
    void testGetPositionUsesCache() {
        // 设置模拟行为
        Position existingPosition = new Position(testSymbol);
        existingPosition.setPositionId(1L);
        existingPosition.setQuantity(new BigDecimal("1.5"));
        
        when(positionRepository.findBySymbol(testSymbol))
                .thenReturn(Optional.of(existingPosition));

        // 第一次调用，会查询数据库并放入缓存
        Position result1 = positionService.getPosition(testSymbol);
        
        // 第二次调用，应该使用缓存
        Position result2 = positionService.getPosition(testSymbol);

        // 验证结果
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(result1, result2); // 应该是同一个对象（来自缓存）
        
        // 只应查询数据库一次
        verify(positionRepository, times(1)).findBySymbol(testSymbol);
    }

    @Test
    void testRefreshCache() {
        // 设置模拟行为
        Position existingPosition = new Position(testSymbol);
        existingPosition.setPositionId(1L);
        existingPosition.setQuantity(new BigDecimal("1.5"));
        
        when(positionRepository.findBySymbol(testSymbol))
                .thenReturn(Optional.of(existingPosition));

        // 先获取位置，这会将其放入缓存
        Position result1 = positionService.getPosition(testSymbol);
        
        // 清空模拟的调用次数
        reset(positionRepository);
        
        // 设置新的返回值
        Position updatedPosition = new Position(testSymbol);
        updatedPosition.setPositionId(1L);
        updatedPosition.setQuantity(new BigDecimal("2.0"));
        when(positionRepository.findBySymbol(testSymbol))
                .thenReturn(Optional.of(updatedPosition));
        
        // 刷新缓存
        positionService.refreshCache(testSymbol);
        
        // 再次获取，应该得到更新后的对象
        Position result2 = positionService.getPosition(testSymbol);

        // 验证结果
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotEquals(result1.getQuantity(), result2.getQuantity()); // 数量应该不同
        
        // 应该再次查询数据库
        verify(positionRepository, times(1)).findBySymbol(testSymbol);
    }
}