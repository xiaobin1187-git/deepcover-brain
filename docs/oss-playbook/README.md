# 开源准备操作手册 (OSS Playbook)

> 基于 DeepCover 采集器 Agent 开源实践总结，供数据中心、分析中心等后续模块复用。

## 适用范围

本文档适用于将内部 Java 项目开源化的全流程，涵盖：
- 信息安全审计与脱敏
- 品牌与包名重构
- Git 历史清理
- 文档体系建设
- CI/CD 与代码质量
- 社区规范与发布流程

## 模块清单与进度

| 模块 | 状态 | 说明 |
|------|------|------|
| 采集器 Agent (DeepCover) | 已完成 | 本手册的经验来源 |
| 数据中心 | 待启动 | 代码覆盖率数据的接收、存储与查询 |
| 分析中心 | 待启动 | 覆盖率计算、差分分析、报告生成 |

## 文档索引

| 文档 | 用途 |
|------|------|
| [01-preparation-checklist.md](01-preparation-checklist.md) | 开源前准备检查清单 |
| [02-security-audit.md](02-security-audit.md) | 信息安全审计与脱敏指南 |
| [03-brand-and-package.md](03-brand-and-package.md) | 品牌重命名与包重构 |
| [04-git-history.md](04-git-history.md) | Git 历史清理策略 |
| [05-documentation.md](05-documentation.md) | 文档、README、多语言 |
| [06-ci-and-quality.md](06-ci-and-quality.md) | CI、测试、代码质量 |
| [07-community-and-release.md](07-community-and-release.md) | 社区规范与发布流程 |
| [08-lessons-learned.md](08-lessons-learned.md) | 踩坑记录与经验总结 |

## 自动化 Skills

配合本手册，提供以下 Claude Code Skills 用于自动化执行：

| Skill | 用途 | 调用方式 |
|-------|------|----------|
| oss-security-scan | 信息安全扫描 | `/oss-security-scan` |
| oss-rebrand | 品牌与包名替换 | `/oss-rebrand` |
| oss-git-audit | Git 历史泄露检查 | `/oss-git-audit` |
| oss-license-header | License 头批量注入 | `/oss-license-header` |
| oss-i18n-docs | 多语言文档生成 | `/oss-i18n-docs` |
| oss-community-init | 社区规范文件生成 | `/oss-community-init` |
| oss-release | GitHub Release 创建 | `/oss-release` |

## 推荐执行顺序

```
第一阶段: 审计与规划
  ├─ 运行 /oss-security-scan 扫描安全风险
  ├─ 运行 /oss-git-audit 检查历史泄露
  └─ 确定开源范围和品牌名称

第二阶段: 代码重构
  ├─ 运行 /oss-rebrand 执行品牌替换
  ├─ 脱敏处理 (IP/域名/密钥)
  └─ 移除内部依赖

第三阶段: Git 清理
  ├─ 选择清理策略 (squash/filter)
  └─ 执行历史清理并验证

第四阶段: 文档与质量
  ├─ 编写 README.md
  ├─ 运行 /oss-i18n-docs 生成多语言文档
  ├─ 运行 /oss-license-header 添加 License 头
  ├─ 配置 CI
  └─ 确保测试通过

第五阶段: 发布
  ├─ 运行 /oss-community-init 生成社区文件
  ├─ 推送到 GitHub
  └─ 运行 /oss-release 创建 Release
```

## 注意事项

- 本文档为内部使用，**不应开源到 GitHub**
- 每个模块完成后，用对应的检查清单逐项验收
- 踩坑记录持续更新，每个模块的经验都应追加到 08-lessons-learned.md
