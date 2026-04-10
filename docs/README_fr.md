[Chinese](../README.md) | [English](README_en.md) | [Japanese](README_ja.md) | **French** | [Portuguese](README_pt.md) | [Russian](README_ru.md)

# DeepCover Brain
<img src="assets/logo.svg" alt="DeepCover Logo" width="128" height="128" align="right">

[![Java 8](https://img.shields.io/badge/Java-8-green.svg)](https://www.oracle.com/java/technologies/javase/javase8-archive-downloads.html)
[![Maven](https://img.shields.io/badge/Maven-3.5+-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Spring Boot 2.6.13](https://img.shields.io/badge/Spring%20Boot-2.6.13-brightgreen.svg)](https://spring.io/projects/spring-boot)

DeepCover Brain est le module du centre d'analyse de la [plateforme d'analyse de precision DeepCover](https://github.com/deepcover), responsable de l'analyse des differences de code, de la modelisation des scenarios, de l'analyse des traces et de la relecture des donnees. Il fonctionne conjointement avec [DeepCover Agent](https://github.com/deepcover/deepcover-agent) (collecte de donnees de precision) et [DeepCover DataCenter](https://github.com/deepcover/deepcover-datacenter) (centre de traitement des donnees) pour fournir une solution complete d'analyse de precision.

## Fonctionnalites

- **Analyse des differences de code (DiffAnalyse)** - Analyse les differences entre les versions de code et combine les evaluations de risque pour aider a la decision de test
- **Modelisation et gestion des scenarios** - Creer et gerer des scenarios de test, associer les donnees d'analyse de precision
- **Analyse des traces** - Suivi des liens base sur les traces et visualisation des chaines d'appels
- **Consommation de files de messages (RocketMQ)** - Reception et traitement asynchrones des donnees collectees depuis l'Agent
- **Architecture multi-sources de donnees** - Separation de la base de donnees principale et de la base d'analyse de complexite pour une gestion et une mise a l'echelle independantes
- **Taches planifiees distribuees** - Verrous distribues bases sur ShedLock pour garantir l'execution unique des taches
- **Documentation API** - Swagger integre pour la generation automatique de la documentation REST API

## Architecture

```
+---------------------------+
|       DeepCover Agent     |  (Collecte JVM Sandbox)
+---------------------------+
            | HTTP / Kafka
            v
+---------------------------+
|    DeepCover DataCenter   |  (Reception et stockage des donnees)
+---------------------------+
            | HTTP / RocketMQ
            v
+---------------------------+
|      DeepCover Brain      |  <-- Ce projet
|  +---------------------+  |
|  |       facade        |  |  Definitions d'interfaces API et DTO
|  +---------------------+  |
|  |       model         |  |  Modeles de domaine
|  +---------------------+  |
|  |        dal          |  |  Couche d'acces aux donnees (MyBatis Plus)
|  +---------------------+  |
|  |       service       |  |  Couche logique metier (Controller + Service)
|  +---------------------+  |
|  |       deploy        |  |  Point d'entree et configuration de l'application
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

## Stack Technologique

| Composant | Version | Description |
|-----------|---------|-------------|
| Spring Boot | 2.6.13 | Framework d'application |
| MyBatis Plus | 3.4.1 | Framework ORM |
| Redis | - | Cache |
| RocketMQ | 2.2.3 (spring starter) | File de messages |
| MySQL | 8.0+ | Base de donnees |
| Druid | 1.2.8 | Pool de connexions |
| ShedLock | 2.3.0 | Verrou de tache planifiee distribuee |
| Swagger | 2.9.2 | Documentation API |

## Demarrage Rapide

### Prerequis

- **Java 8+** (JDK 8 recommande)
- **Maven 3.5+**
- **MySQL 8.0+**
- **Redis** (optionnel, pour le cache)
- **RocketMQ** (optionnel, pour la consommation de messages)

### Compilation

```bash
# Compiler le projet
mvn clean compile

# Executer les tests
mvn test

# Packaging (ignorer les tests)
mvn clean package -Dmaven.test.skip=true
```

### Configuration

1. Editez `deploy/src/main/resources/application.properties` pour selectionner le profil d'environnement actif
2. Editez le profil d'environnement correspondant `application-{env}.properties` et configurez les elements suivants :

```properties
# Configuration de la base de donnees
spring.datasource.url=jdbc:mysql://YOUR_DB_HOST:3306/deepcover_brain?useSSL=false&characterEncoding=utf8
spring.datasource.username=YOUR_DB_USER
spring.datasource.password=YOUR_DB_PASSWORD

# Configuration Redis (si activee)
spring.redis.host=YOUR_REDIS_HOST
spring.redis.port=6379

# Configuration RocketMQ (si activee)
rocketmq.name-server=YOUR_ROCKETMQ_NAMESERVER:9876
```

### Execution

```bash
# Executer apres le packaging
java -jar deploy/target/deepcover-brain-deploy.jar

# Executer avec un environnement specifique
java -jar deploy/target/deepcover-brain-deploy.jar --spring.profiles.active=test
```

## Documentation API

Apres le demarrage de l'application, accedez a Swagger UI pour consulter la documentation API :

```
http://localhost:8080/swagger-ui.html
```

## Reference de Configuration

| Configuration | Description | Valeur par defaut |
|---------------|-------------|-------------------|
| `spring.profiles.active` | Identifiant d'environnement (test/pre/prod) | test |
| `spring.datasource.url` | URL de connexion a la base de donnees principale | - |
| `spring.datasource.complexity.url` | URL de connexion a la base d'analyse de complexite | - |
| `rocketmq.name-server` | Adresse RocketMQ NameServer | - |
| `spring.redis.host` | Adresse de l'hote Redis | - |
| `codediff.url` | URL du service d'analyse des differences de code | - |
| `datacenter.url` | URL du service du centre de donnees | - |

## Contribuer

Les contributions sont les bienvenues ! Veuillez consulter [CONTRIBUTING.md](../CONTRIBUTING.md) pour la configuration de l'environnement de developpement et le processus de soumission.

## Licence

Ce projet est sous licence [Apache License 2.0](../LICENSE).

Copyright 2024-2026 DeepCover
