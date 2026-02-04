-- 量化做市系统 - 订单簿模块数据库表
-- 创建时间: 2024

-- 订单簿档位表
CREATE TABLE IF NOT EXISTS `order_book_entry` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `symbol` VARCHAR(32) NOT NULL COMMENT '品种代码',
    `market_type` TINYINT NOT NULL COMMENT '市场类型: 1=境内黄金, 2=境内外汇, 3=境外',
    `source` VARCHAR(32) NOT NULL COMMENT '价源标识',
    `side` TINYINT NOT NULL COMMENT '方向: 1=BUY, 2=SELL',
    `price_level` INT NOT NULL COMMENT '价格档位序号',
    `price` DECIMAL(18, 8) NOT NULL COMMENT '分档价格',
    `quantity` DECIMAL(18, 8) NOT NULL COMMENT '数量',
    `snapshot_time` DATETIME(3) NOT NULL COMMENT '快照时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_symbol` (`symbol`),
    INDEX `idx_snapshot_time` (`snapshot_time`),
    INDEX `idx_symbol_market` (`symbol`, `market_type`),
    INDEX `idx_symbol_time` (`symbol`, `snapshot_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单簿档位表';

-- 订单表（预留）
CREATE TABLE IF NOT EXISTS `quote_order` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '订单ID',
    `order_id` VARCHAR(64) NOT NULL COMMENT '外部订单号',
    `symbol` VARCHAR(32) NOT NULL COMMENT '品种代码',
    `side` TINYINT NOT NULL COMMENT '方向: 1=BUY, 2=SELL',
    `quantity` DECIMAL(18, 8) NOT NULL COMMENT '委托数量',
    `price` DECIMAL(18, 8) NOT NULL COMMENT '委托价格',
    `filled_qty` DECIMAL(18, 8) DEFAULT 0 COMMENT '成交数量',
    `status` VARCHAR(20) NOT NULL COMMENT '订单状态',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_order_id` (`order_id`),
    INDEX `idx_symbol_status` (`symbol`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报价订单表';

-- 审计日志表（预留）
CREATE TABLE IF NOT EXISTS `audit_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `event_type` VARCHAR(50) NOT NULL COMMENT '事件类型',
    `symbol` VARCHAR(32) COMMENT '品种代码',
    `order_id` VARCHAR(64) COMMENT '关联订单ID',
    `detail` JSON COMMENT '详细信息',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_event_type` (`event_type`),
    INDEX `idx_symbol` (`symbol`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';

-- 风控参数表（预留）
CREATE TABLE IF NOT EXISTS `risk_config` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `symbol` VARCHAR(32) COMMENT '品种代码, 空表示全局配置',
    `param_name` VARCHAR(50) NOT NULL COMMENT '参数名称',
    `param_value` DECIMAL(18, 8) NOT NULL COMMENT '参数值',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_symbol_param` (`symbol`, `param_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='风控参数配置表';
