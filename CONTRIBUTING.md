# 贡献指南

感谢你对 DeepCover Brain 项目的关注！本文档将帮助你了解如何参与项目开发。

## 开发环境要求

- **Java 8+** (推荐 JDK 8)
- **Maven 3.5+**
- **MySQL 8.0+**
- **Redis** (可选)
- **RocketMQ** (可选)
- **IDE**: IntelliJ IDEA 或 Eclipse（推荐 IDEA）

## 构建与测试

```bash
# 编译项目
mvn clean compile

# 运行测试
mvn test

# 打包（跳过测试）
mvn clean package -Dmaven.test.skip=true

# 编译指定模块
mvn compile -pl service -Dmaven.test.skip=true
```

> 注意：请使用本地 Maven 配置进行构建。

## 代码规范

### Java 编码规范

- 遵循 [Java 编码规范](https://www.oracle.com/java/technologies/javase/codeconventions-contents.html)
- 使用 4 个空格缩进，不使用 Tab
- 类名使用 PascalCase，方法名和变量名使用 camelCase
- 常量使用 UPPER_SNAKE_CASE
- 包名全部小写

### Lombok 使用

项目使用 [Lombok](https://projectlombok.org/) 简化 Java 代码，常用注解：

- `@Data` - 自动生成 getter/setter/toString/equals/hashCode
- `@Builder` - 构建者模式
- `@Slf4j` - 日志对象
- `@NoArgsConstructor` / `@AllArgsConstructor` - 构造函数

### 其他规范

- Controller 放置在 `service` 模块的 `controller/` 包下，按领域分组
- Service 接口和实现分别放置在 `service/service/` 和 `service/service/impl/` 下
- MyBatis Mapper 放置在 `dal` 模块的 `mapper/` 包下
- 配置类放置在对应模块的 `config/` 包下
- 异步方法请指定合适的线程池：`@Async("dbExecutor")` 或 `@Async("httpExecutor")`

## 提交流程

1. **Fork** 本仓库到你的 GitHub 账号
2. 从 `master` 分支创建功能分支：
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. 编写代码并确保通过所有测试：
   ```bash
   mvn test
   ```
4. 提交代码，commit message 请遵循以下格式：
   ```
   <type>: <description>

   类型(type):
   - feat:     新功能
   - fix:      Bug 修复
   - docs:     文档更新
   - style:    代码格式调整（不影响功能）
   - refactor: 代码重构
   - test:     测试相关
   - chore:    构建/工具变更
   ```
5. 推送到你的 Fork 并创建 Pull Request
6. 等待 Code Review 和合并

### Commit Message 规范

- 使用英文编写 commit message
- 不要包含 emoji 或特殊字符
- 第一行不超过 72 个字符
- 如有必要，在空行后添加详细描述

## 问题反馈

如果你发现了 Bug 或有功能建议，请通过 [GitHub Issues](https://github.com/deepcover/deepcover-brain/issues) 提交，并使用提供的 Issue 模板。

## 许可证

提交代码即表示你同意将代码以 [Apache License 2.0](LICENSE) 许可证开源。
