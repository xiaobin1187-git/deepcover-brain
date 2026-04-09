# 品牌重命名与包重构

## 概述

开源项目需要一个全新的品牌标识，包括项目名称、包名、配置前缀等。此步骤需确保全量替换，不留死角。

## 1. 确定新品牌

| 旧名称 | 新名称 | 说明 |
|--------|--------|------|
| Ares | DeepCover | 项目/模块名 |
| ares | deepcover | 配置前缀、目录名 |
| com.timevale.ares.moudle | io.deepcover.agent | Java 包名 |
| AresConfig | DeepCoverConfig | 核心配置类 |
| ares-brain | deepcover-brain | 配置中心路径 |
| ares.properties | deepcover.properties | 配置文件名 |

## 2. 替换范围

### Java 源码
- 包声明 (`package` 和 `import`)
- 类名引用
- 注解值（如 `@Information(id = "deepcover")`）
- 字符串常量
- 日志中的模块名
- 注释中的品牌名

### 配置文件
- properties 文件中的键名
- logback.xml 中的 logger name
- sandbox.properties 引用

### 构建配置
- pom.xml 中的 groupId/artifactId
- Maven 插件配置中的路径引用

### 文档
- 所有 Markdown 文件
- 代码注释

## 3. 执行步骤

### 步骤 1: 包名重构

```bash
# 1. 创建新包目录结构
mkdir -p src/main/java/io/deepcover/agent/{config,entity,ext,util}

# 2. 移动文件
mv src/main/java/com/timevale/ares/moudle/* src/main/java/io/deepcover/agent/

# 3. 批量替换包声明
find src/ -name "*.java" -exec sed -i 's/com\.timevale\.ares\.moudle/io.deepcover.agent/g' {} +
```

### 步骤 2: 类名替换

逐个检查并替换关键类名：
- `AresConfig` → `DeepCoverConfig`
- `AresModule` → `DeepCoverModule`
- 其他包含旧品牌名的类

### 步骤 3: 配置替换

```bash
# 重命名配置文件
mv src/main/resources/ares.properties src/main/resources/deepcover.properties
mv src/main/resources/ares.properties.example src/main/resources/deepcover.properties.example
```

### 步骤 4: 字符串替换

全量搜索以下关键词，确认全部替换：
- `ares` → `deepcover`（注意区分大小写和上下文）
- `Ares` → `DeepCover`
- `ARES` → `DEEPCOVER`

### 步骤 5: pom.xml 更新

```xml
<!-- 旧 -->
<groupId>com.timevale</groupId>
<artifactId>ares</artifactId>

<!-- 新 -->
<groupId>io.deepcover</groupId>
<artifactId>deepcover-agent</artifactId>
```

同时更新所有内部路径引用（sandbox 目录名等）。

## 4. 验证

```bash
# 全量扫描，确保无遗漏
grep -rn "timevale\|\.ares\.\|AresConfig\|ares-brain\|ares\.properties" src/ pom.xml
# 预期输出：空
```

## 5. 注意事项

- 替换前务必备份或创建新分支
- 注意不要误替换通用的英文单词（如 "shares" 中的 "ares"）
- 类名替换后需同步更新所有 import 语句
- 替换后立即编译验证，确保无语法错误
- 日志中的模块名也需要同步更新
