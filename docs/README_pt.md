[Chinese](../README.md) | [English](README_en.md) | [Japanese](README_ja.md) | [French](README_fr.md) | **Portuguese** | [Russian](README_ru.md)

# DeepCover Brain
<img src="assets/logo.svg" alt="DeepCover Logo" width="128" height="128" align="right">

[![Java 8](https://img.shields.io/badge/Java-8-green.svg)](https://www.oracle.com/java/technologies/javase/javase8-archive-downloads.html)
[![Maven](https://img.shields.io/badge/Maven-3.5+-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Spring Boot 2.6.13](https://img.shields.io/badge/Spring%20Boot-2.6.13-brightgreen.svg)](https://spring.io/projects/spring-boot)

DeepCover Brain e o modulo do centro de analise da [plataforma de analise de precisao DeepCover](https://github.com/deepcover), responsavel pela analise de diferencas de codigo, modelagem de cenarios, analise de rastreamento e reproducao de dados. Ele trabalha em conjunto com o [DeepCover Agent](https://github.com/deepcover/deepcover-agent) (coleta de dados de precisao) e o [DeepCover DataCenter](https://github.com/deepcover/deepcover-datacenter) (centro de processamento de dados) para fornecer uma solucao completa de analise de precisao.

## Funcionalidades

- **Analise de Diferencas de Codigo (DiffAnalyse)** - Analisa diferencas entre versoes de codigo e combina avaliacoes de risco para auxiliar na decisao de testes
- **Modelagem e Gerenciamento de Cenarios** - Criar e gerenciar cenarios de teste, associar dados de analise de precisao
- **Analise de Rastreamento** - Rastreamento de links baseado em traces e visualizacao de cadeias de chamadas
- **Consumo de Filas de Mensagens (RocketMQ)** - Receber e processar assincronamente os dados coletados do Agent
- **Arquitetura Multi-Fonte de Dados** - Separacao do banco de dados principal e do banco de analise de complexidade para gerenciamento e escalabilidade independentes
- **Tarefas Agendadas Distribuidas** - Locks distribuidos baseados em ShedLock para garantir execucao unica de tarefas
- **Documentacao de API** - Swagger integrado para geracao automatica de documentacao REST API

## Arquitetura

```
+---------------------------+
|       DeepCover Agent     |  (Coleta JVM Sandbox)
+---------------------------+
            | HTTP / Kafka
            v
+---------------------------+
|    DeepCover DataCenter   |  (Recepcao e Armazenamento de Dados)
+---------------------------+
            | HTTP / RocketMQ
            v
+---------------------------+
|      DeepCover Brain      |  <-- Este Projeto
|  +---------------------+  |
|  |       facade        |  |  Definicoes de Interfaces API e DTOs
|  +---------------------+  |
|  |       model         |  |  Modelos de Dominio
|  +---------------------+  |
|  |        dal          |  |  Camada de Acesso a Dados (MyBatis Plus)
|  +---------------------+  |
|  |       service       |  |  Camada de Logica de Negocio (Controller + Service)
|  +---------------------+  |
|  |       deploy        |  |  Ponto de Entrada e Configuracao da Aplicacao
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

## Stack Tecnologica

| Componente | Versao | Descricao |
|------------|--------|-----------|
| Spring Boot | 2.6.13 | Framework de Aplicacao |
| MyBatis Plus | 3.4.1 | Framework ORM |
| Redis | - | Cache |
| RocketMQ | 2.2.3 (spring starter) | Fila de Mensagens |
| MySQL | 8.0+ | Banco de Dados |
| Druid | 1.2.8 | Pool de Conexoes |
| ShedLock | 2.3.0 | Lock de Tarefas Agendadas Distribuidas |
| Swagger | 2.9.2 | Documentacao de API |

## Inicio Rapido

### Requisitos

- **Java 8+** (JDK 8 recomendado)
- **Maven 3.5+**
- **MySQL 8.0+**
- **Redis** (opcional, para cache)
- **RocketMQ** (opcional, para consumo de mensagens)

### Compilacao

```bash
# Compilar o projeto
mvn clean compile

# Executar testes
mvn test

# Empacotamento (ignorar testes)
mvn clean package -Dmaven.test.skip=true
```

### Configuracao

1. Edite `deploy/src/main/resources/application.properties` para selecionar o perfil de ambiente ativo
2. Edite o perfil de ambiente correspondente `application-{env}.properties` e configure os seguintes itens:

```properties
# Configuracao do Banco de Dados
spring.datasource.url=jdbc:mysql://YOUR_DB_HOST:3306/deepcover_brain?useSSL=false&characterEncoding=utf8
spring.datasource.username=YOUR_DB_USER
spring.datasource.password=YOUR_DB_PASSWORD

# Configuracao Redis (se habilitado)
spring.redis.host=YOUR_REDIS_HOST
spring.redis.port=6379

# Configuracao RocketMQ (se habilitado)
rocketmq.name-server=YOUR_ROCKETMQ_NAMESERVER:9876
```

### Execucao

```bash
# Executar apos empacotamento
java -jar deploy/target/deepcover-brain-deploy.jar

# Executar com ambiente especifico
java -jar deploy/target/deepcover-brain-deploy.jar --spring.profiles.active=test
```

## Documentacao da API

Apos iniciar a aplicacao, acesse o Swagger UI para visualizar a documentacao da API:

```
http://localhost:8080/swagger-ui.html
```

## Referencia de Configuracao

| Configuracao | Descricao | Valor Padrao |
|--------------|-----------|-------------|
| `spring.profiles.active` | Identificador de ambiente (test/pre/prod) | test |
| `spring.datasource.url` | URL de conexao do banco de dados principal | - |
| `spring.datasource.complexity.url` | URL de conexao do banco de analise de complexidade | - |
| `rocketmq.name-server` | Endereco do RocketMQ NameServer | - |
| `spring.redis.host` | Endereco do host Redis | - |
| `codediff.url` | URL do servico de analise de diferencas de codigo | - |
| `datacenter.url` | URL do servico do centro de dados | - |

## Contribuicao

Contribuicoes sao bem-vindas! Consulte [CONTRIBUTING.md](../CONTRIBUTING.md) para configuracao do ambiente de desenvolvimento e processo de submissao.

## Licenca

Este projeto esta licenciado sob a [Apache License 2.0](../LICENSE).

Copyright 2024-2026 DeepCover
