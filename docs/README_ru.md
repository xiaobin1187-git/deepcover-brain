[Chinese](../README.md) | [English](README_en.md) | [Japanese](README_ja.md) | [French](README_fr.md) | [Portuguese](README_pt.md) | **Russian**

# DeepCover Brain
<img src="assets/logo.svg" alt="DeepCover Logo" width="128" height="128" align="right">

[![Java 8](https://img.shields.io/badge/Java-8-green.svg)](https://www.oracle.com/java/technologies/javase/javase8-archive-downloads.html)
[![Maven](https://img.shields.io/badge/Maven-3.5+-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Spring Boot 2.6.13](https://img.shields.io/badge/Spring%20Boot-2.6.13-brightgreen.svg)](https://spring.io/projects/spring-boot)

DeepCover Brain - модуль центра анализа [платформы точного анализа DeepCover](https://github.com/deepcover), отвечающий за анализ различий в коде, моделирование сценариев, анализ трассировок и воспроизведение данных. Он работает совместно с [DeepCover Agent](https://github.com/deepcover/deepcover-agent) (сбор данных точного анализа) и [DeepCover DataCenter](https://github.com/deepcover/deepcover-datacenter) (центр обработки данных), обеспечивая полное решение для точного анализа.

## Функциональные возможности

- **Анализ различий кода (DiffAnalyse)** - Анализ различий между версиями кода с учетом оценки рисков для помощи в принятии решений о тестировании
- **Моделирование и управление сценариями** - Создание и управление тестовыми сценариями, привязка данных точного анализа
- **Анализ трассировок** - Отслеживание связей на основе trace-информации и визуализация цепочек вызовов
- **Потребление очередей сообщений (RocketMQ)** - Асинхронный прием и обработка собранных данных от Agent
- **Мультиисточниковая архитектура данных** - Разделение основной базы данных и базы анализа сложности для независимого управления и масштабирования
- **Распределенные запланированные задачи** - Распределенные блокировки на основе ShedLock для обеспечения уникального выполнения задач
- **Документация API** - Интеграция Swagger для автоматической генерации документации REST API

## Архитектура

```
+---------------------------+
|       DeepCover Agent     |  (Сбор данных JVM Sandbox)
+---------------------------+
            | HTTP / Kafka
            v
+---------------------------+
|    DeepCover DataCenter   |  (Прием и хранение данных)
+---------------------------+
            | HTTP / RocketMQ
            v
+---------------------------+
|      DeepCover Brain      |  <-- Данный проект
|  +---------------------+  |
|  |       facade        |  |  Определения интерфейсов API и DTO
|  +---------------------+  |
|  |       model         |  |  Доменные модели
|  +---------------------+  |
|  |        dal          |  |  Уровень доступа к данным (MyBatis Plus)
|  +---------------------+  |
|  |       service       |  |  Уровень бизнес-логики (Controller + Service)
|  +---------------------+  |
|  |       deploy        |  |  Точка входа и конфигурация приложения
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

## Технологический стек

| Компонент | Версия | Описание |
|-----------|--------|----------|
| Spring Boot | 2.6.13 | Фреймворк приложений |
| MyBatis Plus | 3.4.1 | ORM фреймворк |
| Redis | - | Кэш |
| RocketMQ | 2.2.3 (spring starter) | Очередь сообщений |
| MySQL | 8.0+ | База данных |
| Druid | 1.2.8 | Пул соединений |
| ShedLock | 2.3.0 | Блокировка распределенных задач |
| Swagger | 2.9.2 | Документация API |

## Быстрый старт

### Требования

- **Java 8+** (рекомендуется JDK 8)
- **Maven 3.5+**
- **MySQL 8.0+**
- **Redis** (опционально, для кэширования)
- **RocketMQ** (опционально, для потребления сообщений)

### Сборка

```bash
# Компиляция проекта
mvn clean compile

# Запуск тестов
mvn test

# Упаковка (пропуск тестов)
mvn clean package -Dmaven.test.skip=true
```

### Конфигурация

1. Отредактируйте `deploy/src/main/resources/application.properties`, чтобы выбрать активный профиль окружения
2. Отредактируйте соответствующий профиль окружения `application-{env}.properties` и настройте следующие параметры:

```properties
# Конфигурация базы данных
spring.datasource.url=jdbc:mysql://YOUR_DB_HOST:3306/deepcover_brain?useSSL=false&characterEncoding=utf8
spring.datasource.username=YOUR_DB_USER
spring.datasource.password=YOUR_DB_PASSWORD

# Конфигурация Redis (если включено)
spring.redis.host=YOUR_REDIS_HOST
spring.redis.port=6379

# Конфигурация RocketMQ (если включено)
rocketmq.name-server=YOUR_ROCKETMQ_NAMESERVER:9876
```

### Запуск

```bash
# Запуск после упаковки
java -jar deploy/target/deepcover-brain-deploy.jar

# Запуск с указанием окружения
java -jar deploy/target/deepcover-brain-deploy.jar --spring.profiles.active=test
```

## Документация API

После запуска приложения откройте Swagger UI для просмотра документации API:

```
http://localhost:8080/swagger-ui.html
```

## Справочник конфигурации

| Параметр | Описание | Значение по умолчанию |
|----------|----------|----------------------|
| `spring.profiles.active` | Идентификатор окружения (test/pre/prod) | test |
| `spring.datasource.url` | URL подключения к основной базе данных | - |
| `spring.datasource.complexity.url` | URL подключения к базе анализа сложности | - |
| `rocketmq.name-server` | Адрес RocketMQ NameServer | - |
| `spring.redis.host` | Адрес хоста Redis | - |
| `codediff.url` | URL сервиса анализа различий кода | - |
| `datacenter.url` | URL сервиса центра данных | - |

## Участие в разработке

Приветствуются ваши вклады! Пожалуйста, ознакомьтесь с [CONTRIBUTING.md](../CONTRIBUTING.md) для настройки среды разработки и процесса отправки изменений.

## Лицензия

Этот проект лицензирован под [Apache License 2.0](../LICENSE).

Copyright 2024-2026 DeepCover
