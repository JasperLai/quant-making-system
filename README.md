# 量化做市系统 (Quant Making System)

## 项目简介

量化做市系统是一个专业的外汇/黄金做市交易系统，支持多市场报价数据接入、本地深度订单簿构建、智能报价生成和实时风控管理。

## 核心功能

### 📊 数据接入
- **Dimple接入**：对接上期所和金交所，境内黄金报价
- **CFETS MQ**：接入境内外汇报价
- **外资行MQ**：接入境外外汇和黄金报价

### 📈 订单簿管理
- 多价源数据聚合
- 毫秒级实时更新
- 按价格档位聚合
- 历史快照支持

### 🎯 报价引擎
- 最优价格提取
- 固定点差配置
- 智能报价生成
- 订单生命周期管理

### 🛡️ 风控系统
- 单笔限额控制（区分买卖）
- 仓位限制管理
- 前置+事后双检查
- 参数在线配置

### 📝 审计日志
- 全链路事件记录
- 订单簿快照存储
- 支持查询和导出
- 对接管理台

## 技术架构

### 技术栈
- **语言**: Java 17+
- **框架**: Spring Boot 3.x
- **数据库**: MySQL 8.0
- **消息队列**: RabbitMQ/Kafka
- **部署**: Kubernetes

### 系统架构

```
┌─────────────────────────────────────────────────────────┐
│                   量化做市系统架构                        │
├─────────────────────────────────────────────────────────┤
│  数据层  │ Dimple │ CFETS MQ │ 外资行 MQ                 │
├─────────────────────────────────────────────────────────┤
│  适配层  │         价源适配器统一转换                       │
├─────────────────────────────────────────────────────────┤
│  核心层  │ 订单簿 │ 报价引擎 │ 风控引擎 │ 审计引擎          │
├─────────────────────────────────────────────────────────┤
│  接口层  │   CFETS前置   │    管理台API                  │
└─────────────────────────────────────────────────────────┘
```

## 项目结构

```
quant-making-system/
├── docs/                    # 文档
│   ├── requirements.md     # 需求规格文档
│   ├── test-plan.md        # 测试计划文档
│   └── api/                # API文档
├── src/                    # 源代码
│   ├── main/
│   │   ├── java/
│   │   │   └── com/quant/making/book/   # 订单簿模块
│   │   │       ├── OrderBookEntry.java
│   │   │       ├── OrderBook.java
│   │   │       ├── OrderBookService.java
│   │   │       └── OrderBookRepository.java
│   │   └── resources/
│   └── test/
│       └── java/
│           └── com/quant/making/book/   # 订单簿测试
│               ├── OrderBookServiceTest.java
│               ├── OrderBookTest.java
│               └── OrderBookEntryTest.java
├── tests/                  # 测试用例
│   ├── data/              # 测试数据
│   └── scripts/           # 测试脚本
├── config/                 # 配置文件
├── scripts/                # 部署脚本
├── pom.xml                # Maven配置
├── README.md
└── LICENSE
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0
- Docker & K8s (部署环境)

### 本地开发

```bash
# 克隆项目
git clone https://github.com/JasperLai/quant-making-system.git
cd quant-making-system

# 构建项目
mvn clean install

# 运行测试
mvn test

# 启动服务
java -jar target/quant-making-system.jar
```

### 配置说明

详见 [配置文档](docs/config/README.md)

## 文档

- [需求规格](docs/requirements.md)
- [测试计划](docs/test-plan.md)
- [API接口文档](docs/api/README.md)
- [数据库设计](docs/database/README.md)
- [部署指南](docs/deployment/README.md)

## 测试

### 运行单元测试

```bash
mvn test
```

### 运行集成测试

```bash
mvn verify -P integration-test
```

### 性能测试

```bash
mvn verify -P performance-test
```

## 部署

### Docker构建

```bash
docker build -t quant-making-system:latest .
```

### K8s部署

```bash
kubectl apply -f k8s/
```

详见 [部署文档](docs/deployment/README.md)

## 版本历史

- v0.1.0 - 项目初始化，完成需求分析和架构设计
- v0.1.1 - 订单簿模块完成，实现数据模型、服务层、数据访问层和单元测试

## 模块实现状态

### 订单簿模块 (Order Book Module) - ✅ 已完成
- **实现时间**: 2026-02-04
- **核心功能**:
  - 多价源数据聚合
  - 按价格档位聚合
  - 最优价格提取 (getBestBid/getBestAsk)
  - 快照生成与恢复
  - 线程安全支持

### 报价引擎模块 (Quote Engine Module) - 🔄 开发中
- **计划功能**:
  - 最优价提取
  - 点差计算
  - 报价生成
  - 与CFETS前置对接

### 风控模块 (Risk Control Module) - ⏳ 待开发
- **计划功能**:
  - 前置风控检查
  - 事后风控检查
  - 风控参数配置

### 审计模块 (Audit Module) - ⏳ 待开发
- **计划功能**:
  - 事件记录
  - 快照存储
  - 查询接口

### 成交回报模块 (Trade Report Module) - ⏳ 待开发
- **计划功能**:
  - 回报接收
  - 订单关联
  - 持仓更新
- v0.2.0 - **订单簿模块实现完成** ✅
  - 实现 OrderBookEntry 实体（订单簿档位）
  - 实现 OrderBook 聚合视图（含 PriceLevel）
  - 实现 OrderBookService 核心服务
  - 实现 OrderBookRepository 数据访问
  - 完成 TC-OB-001 订单簿档位聚合测试
  - 完成 TC-OB-002 订单簿快照生成测试
  - 完成 TC-OB-003 订单簿初始化测试
  - 实现完整的单元测试覆盖（OrderBookEntryTest, OrderBookTest, OrderBookServiceTest, OrderBookSnapshotTest）
  - 支持多市场类型（境内黄金/境内外汇/境外）
  - 支持多价源数据聚合
  - 支持毫秒级订单簿更新
  - 支持定时快照和手动快照功能

## 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 联系

- 项目维护者：JasperLai
- GitHub：[@JasperLai](https://github.com/JasperLai)
