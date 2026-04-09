# 文档、README、多语言

## 文档体系结构

```
项目根目录/
├── README.md                    # 项目主文档（中文）
├── README_EN.md                 # 英文版
├── README_JA.md                 # 日文版
├── README_FR.md                 # 法文版
├── README_PT.md                 # 葡萄牙文版
├── README_RU.md                 # 俄文版
├── CONTRIBUTING.md              # 贡献指南
├── CHANGELOG.md                 # 变更日志
├── LICENSE                      # 许可证
├── CODE_OF_CONDUCT.md           # 行为准则
├── SECURITY.md                  # 安全策略
├── docs/
│   ├── deployment-guide.md      # 部署指南（+ 多语言）
│   └── configuration-guide.md   # 配置指南（+ 多语言）
└── examples/
    └── demo-servlet/            # 代码示例
        └── README.md            # 示例文档（+ 多语言）
```

## README 必备内容

### 1. 标题与徽章

```markdown
# 项目名称 - 一句话描述

[中文](README.md) | [English](README_EN.md) | ...

<div align="center">

![CI](https://img.shields.io/github/actions/workflow/status/...)
![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)
![Java](https://img.shields.io/badge/Java-1.8-orange)
![Tests](https://img.shields.io/badge/Tests-52_passed-brightgreen)

</div>
```

### 2. 项目简介

一段话说明项目是什么、做什么。

### 3. 核心特性

用列表展示，每项一句话。

### 4. 架构图

ASCII 图展示系统架构、数据流。这是用户理解项目的关键。

### 5. 快速开始

可复制的命令行步骤：
- 编译
- 配置
- 部署
- 验证

### 6. 配置说明

参数表格，包含：参数名、说明、默认值。

### 7. 项目结构

目录树展示源码组织。

### 8. 依赖列表

表格展示所有外部依赖及用途。

### 9. 许可证

Apache 2.0 标准声明。

## 多语言文档规范

### 语言切换器

每个文档顶部必须有语言切换链接：
```markdown
[中文](README.md) | **English** | [日本語](README_JA.md)
```

当前语言用 `**粗体**`，其他语言为超链接。

### 翻译要求

- 技术术语保持英文（如 JVM、Servlet、Kafka）
- 代码块和命令不翻译
- 配置参数名不翻译
- 保持相同的文档结构

### 文件命名

- 主文档：`README.md`
- 翻译文档：`README_EN.md`, `README_JA.md`, `README_FR.md`, `README_PT.md`, `README_RU.md`
- 用户手册同理：`deployment-guide.md`, `deployment-guide_en.md`, ...

## 配置文件模板

提供 `.example` 文件，使用 `YOUR_*` 占位符：

```properties
# 模板文件: deepcover.properties.example
test.dataCenterAddr=YOUR_DATACENTER_ADDR
test.KAFKA_BOOTSTRAP_SERVERS=YOUR_KAFKA_SERVERS
test.KAFKA_TOPIC=deepcover-collection-code-info
```

同时确保 `.gitignore` 中包含实际配置文件。
