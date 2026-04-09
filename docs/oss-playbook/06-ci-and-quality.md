# CI、测试、代码质量

## GitHub Actions CI

### 基础配置

创建 `.github/workflows/ci.yml`，支持多 Java 版本矩阵构建：

```yaml
name: CI
on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]
jobs:
  build:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        java: [ '8', '11', '17' ]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: ${{ matrix.java }}
          distribution: 'zulu'
      - uses: actions/cache@v4
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
      - run: mvn clean test -Dmaven.javadoc.skip=true
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: test-results-java-${{ matrix.java }}
          path: target/surefire-reports/
```

### 关键点

- **Java 版本矩阵**：至少覆盖项目支持的最低版本和 LTS 版本
- **Maven 缓存**：加速 CI 构建
- **测试报告上传**：`if: always()` 确保失败时也能获取报告
- **分支保护**：main 分支设置 PR 必须通过 CI 才能合并

## Maven Wrapper

添加 Maven Wrapper 让贡献者无需预装 Maven：

```bash
mvn -N io.takari:maven:wrapper -Dmaven=3.5.2
```

生成文件：
- `mvnw` (Linux/Mac)
- `mvnw.cmd` (Windows)
- `.mvn/wrapper/` (Wrapper JAR 和配置)

README 中应提供两种构建命令：
```bash
# 有 Maven
mvn clean package

# 无 Maven（使用 Wrapper）
./mvnw clean package
```

## License 头

所有 Java 源文件应添加 Apache 2.0 License 头：

```java
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```

可使用脚本批量添加，自动跳过已有 License 头的文件。

## 测试要求

### 最低标准

- 所有核心工具类必须有单元测试
- Entity/DTO 类测试 equals/hashCode/toString/getter/setter
- 关键业务逻辑需要正常和异常两条路径测试
- 线程安全类需要并发测试

### 测试依赖

```xml
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.13.2</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>3.12.4</version>
    <scope>test</scope>
</dependency>
```

### CI 徽章

在 README 中添加 CI 状态徽章：
```markdown
![CI](https://img.shields.io/github/actions/workflow/status/{owner}/{repo}/ci.yml?branch=main)
```

## 代码质量检查清单

- [ ] 无 System.out.println，使用日志框架
- [ ] 日志级别正确（error/warn/info/debug）
- [ ] 无未使用的 import
- [ ] 无硬编码的敏感信息
- [ ] 异常处理完整（无空 catch）
- [ ] 资源关闭（Stream、Connection、HttpClient）
- [ ] ThreadLocal 正确清理
