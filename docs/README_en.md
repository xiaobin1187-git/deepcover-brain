<div align="center"><img src="docs/assets/logo.svg" alt="DeepCover" width="96" height="96"></div>
[Chinese](../README.md) | **English** | [Japanese](README_ja.md) | [French](README_fr.md) | [Portuguese](README_pt.md) | [Russian](README_ru.md)

# DeepCover Brain

[![Java 8](https://img.shields.io/badge/Java-8-green.svg)](https://www.oracle.com/java/technologies/javase/javase8-archive-downloads.html)
[![Maven](https://img.shields.io/badge/Maven-3.5+-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Spring Boot 2.6.13](https://img.shields.io/badge/Spring%20Boot-2.6.13-brightgreen.svg)](https://spring.io/projects/spring-boot)

DeepCover Brain is the analysis center module of the [DeepCover Precision Analysis Platform](https://github.com/deepcover), responsible for code diff analysis, scenario modeling, trace analysis, and data replay. It works in conjunction with [DeepCover Agent](https://github.com/deepcover/deepcover-agent) (precision data collection) and [DeepCover DataCenter](https://github.com/deepcover/deepcover-datacenter) (data processing center) to provide a complete precision analysis solution.

## Features

- **Code Diff Analysis (DiffAnalyse)** - Analyzes differences between code versions and combines risk ratings to assist testing decisions
- **Scenario Modeling & Management** - Create and manage test scenarios, associate precision analysis data
- **Trace Analysis** - Trace-based link tracking and call chain visualization
- **Message Queue Consumption (RocketMQ)** - Asynchronously receive and process collected data from Agent
- **Multi-Datasource Architecture** - Separate primary database and complexity analysis database for independent management and scaling
- **Distributed Scheduled Tasks** - Distributed locks based on ShedLock to ensure unique task execution
- **API Documentation** - Integrated Swagger for automatic REST API documentation generation

## Architecture

```
+---------------------------+
|       DeepCover Agent     |  (JVM Sandbox Collection)
+---------------------------+
            | HTTP / Kafka
            v
+---------------------------+
|    DeepCover DataCenter   |  (Data Reception & Storage)
+---------------------------+
            | HTTP / RocketMQ
            v
+---------------------------+
|      DeepCover Brain      |  <-- This Project
|  +---------------------+  |
|  |       facade        |  |  API Interface Definitions & DTOs
|  +---------------------+  |
|  |       model         |  |  Domain Models
|  +---------------------+  |
|  |        dal          |  |  Data Access Layer (MyBatis Plus)
|  +---------------------+  |
|  |       service       |  |  Business Logic Layer (Controller + Service)
|  +---------------------+  |
|  |       deploy        |  |  Application Entry & Configuration
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

## Technology Stack

| Component | Version | Description |
|-----------|---------|-------------|
| Spring Boot | 2.6.13 | Application Framework |
| MyBatis Plus | 3.4.1 | ORM Framework |
| Redis | - | Cache |
| RocketMQ | 2.2.3 (spring starter) | Message Queue |
| MySQL | 8.0+ | Database |
| Druid | 1.2.8 | Connection Pool |
| ShedLock | 2.3.0 | Distributed Scheduled Task Lock |
| Swagger | 2.9.2 | API Documentation |

## Quick Start

### Prerequisites

- **Java 8+** (JDK 8 recommended)
- **Maven 3.5+**
- **MySQL 8.0+**
- **Redis** (optional, for caching)
- **RocketMQ** (optional, for message consumption)

### Build

```bash
# Compile the project
mvn clean compile

# Run tests
mvn test

# Package (skip tests)
mvn clean package -Dmaven.test.skip=true
```

### Configuration

1. Edit `deploy/src/main/resources/application.properties` to select the active environment profile
2. Edit the corresponding environment profile `application-{env}.properties` and configure the following:

```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://YOUR_DB_HOST:3306/deepcover_brain?useSSL=false&characterEncoding=utf8
spring.datasource.username=YOUR_DB_USER
spring.datasource.password=YOUR_DB_PASSWORD

# Redis Configuration (if enabled)
spring.redis.host=YOUR_REDIS_HOST
spring.redis.port=6379

# RocketMQ Configuration (if enabled)
rocketmq.name-server=YOUR_ROCKETMQ_NAMESERVER:9876
```

### Run

```bash
# Run after packaging
java -jar deploy/target/deepcover-brain-deploy.jar

# Run with specific environment
java -jar deploy/target/deepcover-brain-deploy.jar --spring.profiles.active=test
```

## API Documentation

After starting the application, access the Swagger UI to view API documentation:

```
http://localhost:8080/swagger-ui.html
```

## Configuration Reference

| Configuration | Description | Default |
|---------------|-------------|---------|
| `spring.profiles.active` | Environment identifier (test/pre/prod) | test |
| `spring.datasource.url` | Primary database connection URL | - |
| `spring.datasource.complexity.url` | Complexity analysis database connection URL | - |
| `rocketmq.name-server` | RocketMQ NameServer address | - |
| `spring.redis.host` | Redis host address | - |
| `codediff.url` | Code diff analysis service URL | - |
| `datacenter.url` | Data center service URL | - |

## Contributing

Contributions are welcome! Please refer to [CONTRIBUTING.md](../CONTRIBUTING.md) for development environment setup and submission process.

## License

This project is licensed under the [Apache License 2.0](../LICENSE).

Copyright 2024-2026 DeepCover
