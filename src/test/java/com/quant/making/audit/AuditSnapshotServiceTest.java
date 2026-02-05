package com.quant.making.audit;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password="
})
class AuditSnapshotServiceTest {

    @Autowired
    private AuditSnapshotService auditSnapshotService;

    @MockBean
    private AuditService auditService;

    @MockBean
    private com.quant.making.book.OrderBookService orderBookService;

    @MockBean
    private com.quant.making.quote.QuoteService quoteService;

    @MockBean
    private com.quant.making.risk.RiskControlService riskControlService;

    @Test
    void testGenerateSystemSnapshot() {
        // 测试生成系统快照
        assertDoesNotThrow(() -> auditSnapshotService.generateSystemSnapshot());
        
        // 验证相关方法被调用
        Mockito.verify(auditService, Mockito.atLeastOnce()).logEvent(
            Mockito.any(AuditEventType.class),
            Mockito.anyString(),
            Mockito.anyString()
        );
    }

    @Test
    void testRecordOrderBookSnapshot() {
        // 由于recordOrderBookSnapshot是私有方法，我们通过generateSystemSnapshot来间接测试
        assertDoesNotThrow(() -> auditSnapshotService.generateSystemSnapshot());
    }

    @Test
    void testRecordQuoteSnapshot() {
        // 测试记录报价快照
        assertDoesNotThrow(() -> auditSnapshotService.generateSystemSnapshot());
    }

    @Test
    void testRecordRiskSnapshot() {
        // 测试记录风控快照
        assertDoesNotThrow(() -> auditSnapshotService.generateSystemSnapshot());
    }

    @Test
    void testGenerateDetailedSnapshot() {
        // 测试生成详细快照
        assertDoesNotThrow(() -> auditSnapshotService.generateDetailedSnapshot());
        
        Mockito.verify(auditService, Mockito.atLeastOnce()).logEvent(
            Mockito.eq(AuditEventType.SYSTEM_SNAPSHOT),
            Mockito.eq("SYSTEM"),
            Mockito.anyString()
        );
    }

    @Test
    void testInit() {
        // 测试初始化方法
        // 这会在Spring上下文加载时自动调用
        assertNotNull(auditSnapshotService);
    }
}