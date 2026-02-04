# 订单簿模块测试用例补充（深度报价场景）

## TC-OB-001-Enhanced: 真实深度报价聚合测试

### 目的
验证真实场景下的深度报价（Depth of Market）正确聚合，包含完整的报价字段。

### 完整报价数据结构
每个深度报价档位包含以下6个字段：

| 字段 | 类型 | 说明 | 示例值 |
|------|------|------|--------|
| priceLevel | int | 价格档位序号 | 1, 2, 3... |
| source | String | 价源标识 | DIMPLE, CFETS, FOREIGN_BANK |
| marketType | int | 市场类型 | 1=境内黄金, 2=境内外汇, 3=境外 |
| side | int | 买卖方向 | 1=BUY, 2=SELL |
| price | BigDecimal | 档位价格 | 2000.00, 380.50 |
| quantity | BigDecimal | 档位数量 | 100.00, 500000 |

### 真实场景数据示例

#### 示例1: Dimple境内黄金深度报价
```json
{
  "symbol": "XAUUSD",
  "quotes": [
    {
      "priceLevel": 1,
      "source": "DIMPLE",
      "marketType": 1,
      "side": "BUY",
      "price": "2000.00",
      "quantity": "100.00"
    },
    {
      "priceLevel": 2,
      "source": "DIMPLE",
      "marketType": 1,
      "side": "BUY",
      "price": "1999.50",
      "quantity": "150.00"
    },
    {
      "priceLevel": 3,
      "source": "DIMPLE",
      "marketType": 1,
      "side": "BUY",
      "price": "1999.00",
      "quantity": "200.00"
    },
    {
      "priceLevel": 1,
      "source": "DIMPLE",
      "marketType": 1,
      "side": "SELL",
      "price": "2000.50",
      "quantity": "80.00"
    },
    {
      "priceLevel": 2,
      "source": "DIMPLE",
      "marketType": 1,
      "side": "SELL",
      "price": "2001.00",
      "quantity": "120.00"
    }
  ]
}
```

#### 示例2: CFETS境内外汇深度报价
```json
{
  "symbol": "USDJPY",
  "quotes": [
    {
      "priceLevel": 1,
      "source": "CFETS",
      "marketType": 2,
      "side": "BUY",
      "price": "7.2500",
      "quantity": "1000000"
    },
    {
      "priceLevel": 2,
      "source": "CFETS",
      "marketType": 2,
      "side": "BUY",
      "price": "7.2450",
      "quantity": "1500000"
    },
    {
      "priceLevel": 1,
      "source": "CFETS",
      "marketType": 2,
      "side": "SELL",
      "price": "7.2550",
      "quantity": "800000"
    }
  ]
}
```

#### 示例3: 外资行境外外汇深度报价
```json
{
  "symbol": "EURUSD",
  "quotes": [
    {
      "priceLevel": 1,
      "source": "FOREIGN_BANK",
      "marketType": 3,
      "side": "BUY",
      "price": "1.0800",
      "quantity": "500000"
    },
    {
      "priceLevel": 1,
      "source": "FOREIGN_BANK",
      "marketType": 3,
      "side": "SELL",
      "price": "1.0850",
      "quantity": "400000"
    }
  ]
}
```

### 测试验证点

1. **数据结构完整性**
   - ✅ 每个档位包含6个字段
   - ✅ 字段类型正确
   - ✅ 数值精度正确

2. **聚合正确性**
   - ✅ 相同价格的多笔订单正确聚合
   - ✅ 买卖方向正确区分
   - ✅ 价源标识正确记录

3. **最优价格提取**
   - ✅ Best Bid = 最高买价
   - ✅ Best Ask = 最低卖价
   - ✅ Spread 计算正确

4. **市场隔离**
   - ✅ 不同市场类型订单簿独立
   - ✅ 不同价源数据不混淆
   - ✅ 多品种数据隔离

### 预期结果

| 品种 | 市场类型 | 价源 | Best Bid | Best Ask | Spread |
|------|----------|------|----------|----------|--------|
| XAUUSD | 境内黄金(1) | DIMPLE | 2000.00 × 100 | 2000.50 × 80 | 0.50 |
| USDJPY | 境内外汇(2) | CFETS | 7.2500 × 1M | 7.2550 × 0.8M | 0.0050 |
| EURUSD | 境外(3) | FOREIGN_BANK | 1.0800 × 0.5M | 1.0850 × 0.4M | 0.0050 |

### 代码示例

```java
// 发送完整结构的深度报价
orderBookService.updateQuote(
    "XAUUSD",                          // symbol
    OrderBook.MARKET_DOMESTIC_GOLD,    // marketType
    "DIMPLE",                          // source
    OrderBook.BUY,                     // side
    new BigDecimal("2000.00"),         // price
    new BigDecimal("100.00")           // quantity
);
```

### 测试用例ID映射

| 原测试用例 | 增强版测试用例 | 说明 |
|-----------|---------------|------|
| TC-OB-001 | TC-OB-001-Enhanced | 深度报价聚合测试 |
| TC-OB-002 | TC-OB-002-Enhanced | 深度报价快照测试 |
| TC-OB-003 | TC-OB-003-Enhanced | 深度报价初始化测试 |
