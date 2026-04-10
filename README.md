<p align="center"><img src="docs/assets/logo.svg" alt="DeepCover" width="96" height="96"></p>
# DeepCover Brain

[English](docs/README_en.md) | [日本語](docs/README_ja.md) | [Français](docs/README_fr.md) | [Português](docs/README_pt.md) | [Русский](docs/README_ru.md)

[![Java 8](https://img.shields.io/badge/Java-8-green.svg)](https://www.oracle.com/java/technologies/javase/javase8-archive-downloads.html)
[![Maven](https://img.shields.io/badge/Maven-3.5+-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Spring Boot 2.6.13](https://img.shields.io/badge/Spring%20Boot-2.6.13-brightgreen.svg)](https://spring.io/projects/spring-boot)

DeepCover Brain 是 [DeepCover 精准分析平台](https://github.com/deepcover) 的分析中心模块，负责代码差异分析、场景建模、链路追踪分析和数据回放。它与 [DeepCover Agent](https://github.com/deepcover/deepcover-agent)（精准数据采集）和 [DeepCover DataCenter](https://github.com/deepcover/deepcover-datacenter)（数据处理中心）协同工作，提供完整的精准分析解决方案。

## 功能特性

- **代码差异分析 (DiffAnalyse)** - 分析代码版本间的差异，结合风险评级辅助测试决策
- **场景建模与管理** - 创建和管理测试场景，关联精准分析数据
- **链路追踪分析** - 基于 trace 信息的链路追踪和调用链路可视化
- **消息队列消费 (RocketMQ)** - 异步接收和处理来自 Agent 的采集数据
- **多数据源架构** - 主库与复杂度分析库分离，独立管理和扩展
- **分布式定时任务** - 基于 ShedLock 的分布式锁，确保任务唯一执行
- **API 文档** - 集成 Swagger，自动生成 REST API 文档

## 架构

```
+---------------------------+
|       DeepCover Agent     |  (JVM Sandbox 采集)
+---------------------------+
            | HTTP / Kafka
            v
+---------------------------+
|    DeepCover DataCenter   |  (数据接收与存储)
+---------------------------+
            | HTTP / RocketMQ
            v
+---------------------------+
|      DeepCover Brain      |  <-- 本项目
|  +---------------------+  |
|  |       facade        |  |  API 接口定义与 DTO
|  +---------------------+  |
|  |       model         |  |  领域模型
|  +---------------------+  |
|  |        dal          |  |  数据访问层 (MyBatis Plus)
|  +---------------------+  |
|  |       service       |  |  业务逻辑层 (Controller + Service)
|  +---------------------+  |
|  |       deploy        |  |  应用入口与配置
|  +---------------------+  |
+---------------------------+
            |
    +-------+-------+
    |               |
    v               v
+--------+    +---------+
| MySQL  |    |  Redis  |
+--------+    +---------+
```

## 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 2.6.13 | 应用框架 |
| MyBatis Plus | 3.4.1 | ORM 框架 |
| Redis | - | 缓存 |
| RocketMQ | 2.2.3 (spring starter) | 消息队列 |
| MySQL | 8.0+ | 数据库 |
| Druid | 1.2.8 | 连接池 |
| ShedLock | 2.3.0 | 分布式定时任务锁 |
| Swagger | 2.9.2 | API 文档 |

## 快速开始

### 环境要求

- **Java 8+** (推荐 JDK 8)
- **Maven 3.5+**
- **MySQL 8.0+**
- **Redis** (可选，用于缓存)
- **RocketMQ** (可选，用于消息消费)

### 构建

```bash
# 编译项目
mvn clean compile

# 运行测试
mvn test

# 打包（跳过测试）
mvn clean package -Dmaven.test.skip=true
```

### 配置

1. 编辑 `deploy/src/main/resources/application.properties`，选择激活的环境配置文件
2. 编辑对应环境配置文件 `application-{env}.properties`，配置以下内容：

```properties
# 数据库配置
spring.datasource.url=jdbc:mysql://YOUR_DB_HOST:3306/deepcover_brain?useSSL=false&characterEncoding=utf8
spring.datasource.username=YOUR_DB_USER
spring.datasource.password=YOUR_DB_PASSWORD

# Redis 配置（如启用）
spring.redis.host=YOUR_REDIS_HOST
spring.redis.port=6379

# RocketMQ 配置（如启用）
rocketmq.name-server=YOUR_ROCKETMQ_NAMESERVER:9876
```

### 运行

```bash
# 打包后运行
java -jar deploy/target/deepcover-brain-deploy.jar

# 指定环境运行
java -jar deploy/target/deepcover-brain-deploy.jar --spring.profiles.active=test
```

## API 文档

启动应用后，访问 Swagger UI 查看 API 文档：

```
http://localhost:8080/swagger-ui.html
```

## 配置说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `spring.profiles.active` | 环境标识 (test/pre/prod) | test |
| `spring.datasource.url` | 主数据库连接地址 | - |
| `spring.datasource.complexity.url` | 复杂度分析库连接地址 | - |
| `rocketmq.name-server` | RocketMQ NameServer 地址 | - |
| `spring.redis.host` | Redis 主机地址 | - |
| `codediff.url` | 代码差异分析服务地址 | - |
| `datacenter.url` | 数据中心服务地址 | - |

## 贡献指南

欢迎贡献代码！请参阅 [CONTRIBUTING.md](CONTRIBUTING.md) 了解开发环境配置和提交流程。

## 许可证

本项目基于 [Apache License 2.0](LICENSE) 许可证开源。

Copyright 2024-2026 DeepCover
