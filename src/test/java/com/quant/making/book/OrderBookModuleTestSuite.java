package com.quant.making.book;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * 订单簿模块测试套件
 * 运行所有订单簿模块的单元测试
 */
@Suite
@SelectClasses({
    OrderBookEntryTest.class,
    OrderBookTest.class,
    OrderBookServiceTest.class,
    OrderBookSnapshotTest.class,
    OrderBookRepositoryTest.class,
    OrderBookTestDataValidationTest.class
})
public class OrderBookModuleTestSuite {
    // 测试套件，运行所有订单簿模块的测试
}