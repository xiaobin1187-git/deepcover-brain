# 信息安全审计与脱敏指南

## 扫描范围

### 1. 内部域名

**常见模式：**
```
tsign.cn
esign.cn
timevale.cn
*.tsign.cn
*.esign.cn
*.timevale.cn
```

**扫描命令：**
```bash
# 扫描源码
grep -rn "tsign\.cn\|esign\.cn\|timevale\.cn" src/ pom.xml

# 扫描配置文件
grep -rn "tsign\.cn\|esign\.cn\|timevale\.cn" src/main/resources/
```

**替换策略：**
- 外部服务域名 → `example.com`
- 配置中心地址 → `127.0.0.1` + 注释说明
- Kafka/数据中心的内部地址 → `YOUR_KAFKA_SERVERS` 占位符

### 2. 内部 IP 地址

**常见模式：**
```
172.20.*.*
10.*.*.*
192.168.*.*
```

**扫描命令：**
```bash
grep -rn "172\.20\.\|10\.\d\+\.\|192\.168\." src/ --include="*.java" --include="*.properties" --include="*.xml" --include="*.yml"
```

**替换策略：**
- 保留 `127.0.0.1` 和 `localhost`（本地开发用）
- 其他内网 IP → `YOUR_SERVER_IP` 或 `127.0.0.1`

### 3. 内部邮箱

**常见模式：**
```
*@tsign.cn
*@timevale.cn
*@esign.cn
```

**扫描命令：**
```bash
# 扫描 Git 历史
git log --all --format='%ae' | sort -u
git log --all --format='%ce' | sort -u
```

**处理策略：**
- 必须通过 Git 历史清理解决（见 04-git-history.md）
- Java 源码中的邮箱注释也需清理

### 4. 硬编码密钥/Token

**常见模式：**
```
password=xxx
secret=xxx
token=xxx
apiKey=xxx
accessKey=xxx
```

**扫描命令：**
```bash
grep -rni "password\s*=\|secret\s*=\|token\s*=\|apiKey\s*=\|accessKey\s*=" src/ --include="*.properties" --include="*.java" --include="*.xml" --include="*.yml"
```

**替换策略：**
- 全部替换为 `YOUR_*` 占位符
- 敏感配置文件加入 .gitignore
- 提供 `.example` 模板文件

### 5. 内部 API 路径

**常见模式：**
```
/ares-brain/
/tsign-xxx/
/esign-xxx/
```

**扫描命令：**
```bash
grep -rn "ares-brain\|tsign-\|esign-" src/ --include="*.java" --include="*.properties"
```

**替换策略：**
- 统一替换为新的品牌路径（如 `deepcover-brain`）

### 6. 内部 Maven 仓库

**扫描位置：**
- pom.xml 中的 `<repository>` 和 `<distributionManagement>`
- settings.xml 引用

**处理策略：**
- 移除内部仓库配置
- 确保所有依赖可从 Maven Central 获取
- 特殊依赖（如 JVM Sandbox）说明获取方式

## 脱敏验证

完成所有替换后，执行全量扫描确认：

```bash
# 一键验证脚本
grep -rn "tsign\|esign\|timevale\|172\.20\|@tsign\|@timevale\|@esign" src/ pom.xml
# 预期输出：空
```

如果输出不为空，说明还有遗漏，需要继续处理。
