# AGENTS.md

This file provides guidance to agentic coding agents working in this repository.

## Build and Development Commands

### Maven Build Commands (Use local Maven at D:\tools\apache-maven-3.5.2\conf\settings.xml)
```bash
# Build all modules
mvn clean install

# Compile without running tests
mvn compile -Dmaven.test.skip=true

# Build specific module
mvn compile -pl aresbrain-service -Dmaven.test.skip=true

# Run tests (if available)
mvn test

# Run a single test class (replace with actual test class)
mvn test -Dtest=ClassName

# Run a single test method
mvn test -Dtest=ClassName#methodName

# Package the application
mvn package -Dmaven.test.skip=true

# Run the application
java -jar deploy/target/aresbrain-deploy.jar
```

## Project Structure

Multi-module Maven project:
- **dal**: Data Access Layer (MyBatis mappers, entities, datasource config)
- **facade**: External API interfaces and DTOs
- **model**: Domain models and DTOs
- **service**: Business logic (services, controllers, utils, exceptions)
- **deploy**: Application entry point, configuration, deployment resources

## Code Style Guidelines

### General Conventions
- **Language**: Java 8+, Spring Boot 1.5.x
- **Framework**: MyBatis Plus 3.4.1 for database operations
- **Naming**: CamelCase for variables/classes, UPPER_CASE for constants
- **Comments**: Chinese JavaDoc comments encouraged for business logic

### Controller Layer (`service/src/main/java/io/deepcover/brain/service/controller/`)
- Use `@RestController` for API controllers
- Use `@RequestMapping` for base path, `@GetMapping`/`@PostMapping` for specific methods
- Inject services with `@Autowired` on private fields
- Always return `R` wrapper (custom response utility)
- Method naming: `add()`, `update()`, `delete()`, `list()`, `info()`, `query*()`
- Parameter validation: check null/empty and throw `RRException` with Chinese message
- Example:
  ```java
  @RequestMapping("/add")
  public R add(@RequestBody Entity entity) {
      if (entity == null || StringUtils.isEmpty(entity.getName())) {
          throw new RRException("参数中名称不能为空");
      }
      service.add(entity);
      return R.ok();
  }
  ```

### Service Layer (`service/src/main/java/io/deepcover/brain/service/service/`)
- Interfaces in `service/` package, implementations in `service/impl/`
- Use `@Service` on implementation classes
- Use `@Autowired` to inject mappers and other services
- Use `@Transactional` for methods that modify data
- Use `@Async("dbExecutor")` for async database operations
- Use `@Async("httpExecutor")` for async HTTP calls
- Legacy `@Async` without parameter uses `asyncServiceExecutor` (avoid for new code)
- Return types: Entities for queries, void/boolean for mutations
- Thread Pool Selection Rules:
  - **dbExecutor**: Database operations (insert, update, delete, query)
  - **httpExecutor**: External HTTP calls
  - **asyncServiceExecutor**: Legacy tasks (avoid for new code)

### Data Access Layer (`dal/src/main/java/io/deepcover/brain/dal/`)
- **Entities**: In `entity/` package, use `@Data`, `@EqualsAndHashCode(callSuper = true)`, extend `Model<T>`
- **Mappers**: In `mapper/` package, use `@Repository`, extend `BaseMapper<T>`
- Use `@TableId(type = IdType.AUTO)` for primary keys
- Use `@Select`, `@Insert`, `@Update`, `@Delete` for custom queries
- For complexity data, use `@Qualifier("complexitySqlSessionFactory")` on mapper injection
- Example:
  ```java
  @Repository
  public interface AresConfigMapper extends BaseMapper<AresConfigEntity> {
      @Select("select * from agent_config a where a.key = #{key} and status=1")
      AresConfigEntity queryObject(String key);
  }
  ```

### Exception Handling
- Use custom `RRException` for business errors (extends RuntimeException)
- Constructor patterns: `RRException(String msg)`, `RRException(String msg, int code)`
- Throw with Chinese error messages for better user experience
- Do NOT suppress exceptions with empty catch blocks

### Imports and Dependencies
- Use `lombok.Data` and `lombok.extern.slf4j.Slf4j` for entities and logging
- Use `org.apache.commons.lang.StringUtils` for string operations
- Use `com.alibaba.fastjson.JSONObject`/`JSONArray` for JSON
- Use `com.baomidou.mybatisplus.*` for MyBatis Plus
- Keep imports organized (java.*, javax.*, org.*, com.*)

### HTTP Client Usage
- Use configured HTTP clients: `httpClientWithTimeout` (10s timeout) or `httpClientForLongRunningTasks` (30s timeout)
- Use `HttpClientUtil.sendRequest(HttpClientRequest)` for HTTP calls
- Check response state code and body before processing

### Response Patterns
- **Success**: `R.ok()` or `R.ok().put("key", value)`
- **Error**: `R.error(msg)` or throw `RRException(msg)`
- Paginated: Use `Query` params, wrap with `PageUtils`, return as `R.ok().put("page", pageUtil)`

### Configuration
- Main class: `io.deepcover.brain.deploy.Application`
- Uses `@UniversalService` (TimeVale custom annotation)
- Mapper scan: `io.deepcover.brain.dal`
- Multi-datasource: Default datasource is primary, use `@Qualifier` for complexity datasource

### Git Commit Messages
- Use Chinese commit messages (per project preference)
- NO emojis or special symbols in commit messages
- Conventional format: `type: description` (e.g., `fix: 修复AresAgent服务配置更新问题`)
- Types: `feat`, `fix`, `refactor`, `docs`, `style`, `test`, `chore`

### Important Notes
- Project has **no test suite** currently (no `src/test/` files)
- Thread pool monitoring runs every 30 seconds - check logs for warnings
- Pagination max limit: 10,000 records
- Multi-datasource: Be careful to use correct datasource for operations
- Data encryption is handled automatically via interceptors
