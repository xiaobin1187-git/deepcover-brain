# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

### Maven Build Commands
```bash
# Build all modules
mvn clean install

# Compile without running tests
mvn compile -Dmaven.test.skip=true

# Build specific module
mvn compile -pl aresbrain-service -Dmaven.test.skip=true

# Run tests
mvn test

# Package the application
mvn package -Dmaven.test.skip=true

# Run the application (requires active profile configuration)
java -jar deploy/target/aresbrain-deploy.jar
```

### Environment Configuration
- Application uses Spring Boot with multi-environment support
- Configuration files located in `deploy/src/main/resources/`
- Available environments: `application-{env}.properties` where env ∈ {test, pre, prod}
- Default environment: TEST (configured in `application.properties`)
- Most configuration properties are managed via Puppeteer configuration system (`@EnablePuppeteerConfig`)

### Running the Application
- Main class: `io.deepcover.brain.deploy.Application`
- Uses TimeVale's custom `@UniversalService` annotation (extends `@SpringBootApplication`)
- Package: `aresbrain-parent`
- Final artifact: `deploy/target/aresbrain-deploy.jar`

## Project Architecture

### Module Structure
Multi-module Maven project with the following modules:
- **dal**: Data Access Layer (MyBatis mappers, entities)
- **facade**: External API interfaces and DTOs
- **model**: Domain models and data transfer objects
- **service**: Business logic layer (services, controllers)
- **deploy**: Application entry point, configuration, and deployment resources

### Key Technologies
- **Spring Boot 1.5.x** with Feign clients
- **MyBatis Plus 3.4.1** for database operations
- **Custom Framework**: TimeVale's internal microservice framework (`mandarin-microservice`)
- **Configuration Management**: Puppeteer configuration system
- **Scheduling**: ShedLock for distributed task locking

### Thread Pool Architecture (Recently Optimized)
The application uses a sophisticated thread pool strategy to prevent task rejection:

1. **asyncServiceExecutor** (Legacy): 5-10 threads, 999 queue - still exists for backward compatibility
2. **dbExecutor** (New): 5-15 threads, 500 queue - for database operations
3. **httpExecutor** (New): 15-50 threads, 1000 queue - for external HTTP calls

Each thread pool has built-in monitoring and alerting via `SimpleThreadPoolMonitor`.

### Database Layer
- Uses MyBatis Plus with custom mapper scanning
- Mapper scan base package: `io.deepcover.brain.dal`
- **Multi-datasource architecture**:
  - Default datasource: Main operational database
  - Complexity datasource: Separate database for complexity analysis data
- **Data encryption**: Encrypt/Decrypt interceptors for sensitive fields
- **Pagination**: MyBatis Plus with PostgreSQL dialect, max limit 10,000 records
- HBase integration for specific data storage requirements (trace data analysis)

### Asynchronous Processing
- Multiple `@Async` annotated methods using different thread pools
- HTTP calls include timeout configurations via `HttpClientConfig`
- Separate pools for database vs HTTP operations to prevent interference

### External Services Integration
- **Feign clients**: For service-to-service communication within the microservice ecosystem
- **Custom HTTP client**: With configurable timeouts (see `HttpClientConfig`)
- **External service URLs** (environment-specific):
  - `codediff.url`: Code difference analysis service
  - `datacenter.url`: Ares datacenter service
  - `ereplay.console.url`: EReplay console service
  - `dingtalk.url`: DingTalk notification service
  - `ejacoco.url`: Code coverage service
- **Message Queue**: RocketMQ integration for async event processing

## Configuration Management

### Puppeteer Integration
- Uses custom `@EnablePuppeteerConfig` annotation
- Configuration namespaces: `application`, `JSBZ.SOA_PUBLIC`
- Dynamic configuration reload capability
- Most database and service URLs are managed through Puppeteer

### Environment-Specific Properties
Each environment has its own property file with:
- Database connections (multi-datasource configuration)
- External service URLs
- Thread pool configurations
- Monitoring settings
- Message queue settings (RocketMQ)
- Cache configuration (Redis - when enabled)

### Data Source Configuration
- **Primary datasource**: `MybatisDefaultDataSourceConfig` (marked with `@Primary`)
- **Secondary datasource**: `MybatisComplexityDataSourceConfig` for complexity data
- Connection pooling via Druid
- Transaction managers configured separately for each datasource

## Development Guidelines

### Thread Pool Usage
When adding new `@Async` methods:
- Use `@Async("dbExecutor")` for database operations
- Use `@Async("httpExecutor")` for HTTP calls
- Use `@Async("asyncServiceExecutor")` only for backward compatibility

### HTTP Client Usage
Prefer configured HTTP clients:
- `httpClientWithTimeout`: Standard 10s read timeout
- `httpClientForLongRunningTasks`: 30s read timeout for long operations

### Database Operations
- Use MyBatis Plus for standard CRUD operations
- Follow existing mapper patterns in `dal/src/main/java/io/deepcover/brain/dal/mapper/`
- Entities are in `dal/src/main/java/io/deepcover/brain/dal/entity/`
- For complex queries, use XML mapper files in `dal/src/main/resources/mapper/`
- When working with complexity data, use the `@Qualifier("complexitySqlSessionFactory")` annotation

### Adding New Controllers
- Controllers should be placed in `service/src/main/java/io/deepcover/brain/service/controller/`
- Group controllers by domain (e.g., `agent/`, `codeDiff/`, `repeater/`)
- Use `@RestController` with appropriate request mapping
- Inject services using `@Autowired`
- Consider adding Swagger annotations for API documentation

### Adding New Services
- Service interfaces go in `service/src/main/java/io/deepcover/brain/service/service/`
- Implementations go in `service/src/main/java/io/deepcover/brain/service/service/impl/`
- Use `@Service` annotation on implementations
- Follow naming convention: `{Name}Service` interface, `{Name}ServiceImpl` implementation
- For async operations, specify appropriate thread pool (see Thread Pool Usage below)

### Monitoring and Debugging
Thread pool status is automatically logged every 30 seconds:
- Check logs for `ThreadPool Status Monitor` entries
- Watch for ⚠️ warnings and 🚨 critical alerts
- Monitor queue sizes and active thread counts

## Known Issues and Solutions

### Thread Pool Exhaustion
Previously experienced `TaskRejectedException` due to thread pool queue saturation. This has been resolved through:
- Thread pool separation by operation type
- Increased pool sizes and queue capacities
- Better rejection strategies (`CallerRunsPolicy`)

### Performance Considerations
- Database operations are now isolated from HTTP call latency
- HTTP calls have configurable timeouts
- Thread pool monitoring provides early warning of capacity issues

## Code Organization

### Module Dependencies
```
deploy (executable module)
  ├── depends on: service
  └── provides: Application main, configs, resources

service (business logic)
  ├── depends on: dal, model, facade
  └── provides: controllers, services, utils

dal (data access)
  ├── depends on: model
  └── provides: entities, mappers, datasource config

facade (external API contracts)
  ├── depends on: model
  └── provides: API interfaces, DTOs

model (domain models)
  ├── depends on: none
  └── provides: entities, DTOs, enums
```

### Key Packages
- `controller/`: REST API endpoints organized by domain
- `service/`: Business logic interfaces and implementations
- `dal/mapper/`: MyBatis mapper interfaces
- `dal/entity/`: JPA/MyBatis entities
- `dal/config/`: Data layer configuration (datasources, MyBatis)
- `deploy/config/`: Application configuration (thread pools, HTTP, monitoring)
- `service/util/`: Utility classes and helpers
- `service/exception/`: Custom exceptions and global exception handling

## Core Business Domains

1. **Code Difference Analysis** (`codeDiff/`): Analyzes code changes between versions with risk assessment
2. **Ares Agent Management** (`agent/`): Manages Ares agent configurations and server information
3. **Module Version Management** (`agent/`): Handles versioning and activation/deactivation of modules
4. **Scene Management** (`AresBrainSceneController`): Manages test scenes and trace analysis
5. **Repeater Integration** (`repeater/`): Integrates with EReplay for traffic replay

## Important Notes

- **No Test Suite**: The project currently has no test files in `src/test/`
- **Documentation**: Refer to `CORE_SEQUENCE_DIAGRAM.md` and `SYSTEM_UML_CLASS_DIAGRAM.md` for detailed architectural documentation
- **Thread Pool Monitoring**: Logs every 30 seconds - check for warnings about queue saturation
- **Data Encryption**: Sensitive fields are encrypted/decrypted automatically via interceptors
- **Multi-datasource**: Be careful to use the correct datasource for your operation