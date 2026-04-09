# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-04-09

### 首次开源发布

#### 新增
- 完全开源发布，采用 Apache 2.0 许可证
- 完整文档: README.md、CONTRIBUTING.md、CHANGELOG.md、LICENSE
- GitHub Issue 和 PR 模板
- GitHub Actions CI 自动构建测试

#### 安全修复 (CRITICAL)
- 明文凭据脱敏: 所有配置文件中的密码/密钥替换为占位符
- 内部 IP/域名替换为 YOUR_* 占位符

#### 代码优化
- 内部依赖替换: eapr/mandarin/unified-cache/unified-mq/encrypt-component 替换为 Spring Boot 标准依赖
- CacheUtil: unified-cache API 替换为 Spring Data Redis RedisTemplate
- MQConsumer: 统一 MQ 框架替换为 RocketMQ Spring Boot Starter
- MyBatis 加密拦截器移除
- MD5Utils 替换为标准 MessageDigest
- 包名: com.timevale.aresbrain -> io.deepcover.brain
- 代码质量修复: 未使用 import 清理

#### 品牌替换
- 包名: com.timevale.aresbrain -> io.deepcover.brain
- 标识符: aresbrain -> deepcover-brain

## [Unreleased]

### 计划中
- 更多单元测试覆盖
- 性能优化
- 支持更多消息队列
