[Chinese](../README.md) | [English](README_en.md) | **Japanese** | [French](README_fr.md) | [Portuguese](README_pt.md) | [Russian](README_ru.md)

# DeepCover Brain

[![Java 8](https://img.shields.io/badge/Java-8-green.svg)](https://www.oracle.com/java/technologies/javase/javase8-archive-downloads.html)
[![Maven](https://img.shields.io/badge/Maven-3.5+-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Spring Boot 2.6.13](https://img.shields.io/badge/Spring%20Boot-2.6.13-brightgreen.svg)](https://spring.io/projects/spring-boot)

DeepCover Brain は [DeepCover プレシジョンテストプラットフォーム](https://github.com/deepcover) の分析センターモジュールであり、コード差分分析、シナリオモデリング、トレース分析、データリプレイを担当します。[DeepCover Agent](https://github.com/deepcover/deepcover-agent)（コードカバレッジ収集）および [DeepCover DataCenter](https://github.com/deepcover/deepcover-datacenter)（データ処理センター）と連携し、完全なプレシジョンテストソリューションを提供します。

## 機能概要

- **コード差分分析 (DiffAnalyse)** - コードバージョン間の差分を分析し、リスク評価と組み合わせてテスト意思決定を支援
- **シナリオモデリングと管理** - テストシナリオの作成と管理、コードカバレッジデータとの関連付け
- **トレース分析** - トレース情報に基づくリンクトラッキングとコールチェーンの可視化
- **メッセージキュー消費 (RocketMQ)** - Agent からの収集データを非同期で受信・処理
- **マルチデータソースアーキテクチャ** - プライマリデータベースと複雑度分析データベースの分離による独立した管理とスケーリング
- **分散スケジュールタスク** - ShedLock に基づく分散ロックにより、タスクの一意実行を保証
- **API ドキュメント** - Swagger を統合し、REST API ドキュメントを自動生成

## アーキテクチャ

```
+---------------------------+
|       DeepCover Agent     |  (JVM Sandbox 収集)
+---------------------------+
            | HTTP / Kafka
            v
+---------------------------+
|    DeepCover DataCenter   |  (データ受信と保存)
+---------------------------+
            | HTTP / RocketMQ
            v
+---------------------------+
|      DeepCover Brain      |  <-- 本プロジェクト
|  +---------------------+  |
|  |       facade        |  |  API インターフェース定義と DTO
|  +---------------------+  |
|  |       model         |  |  ドメインモデル
|  +---------------------+  |
|  |        dal          |  |  データアクセス層 (MyBatis Plus)
|  +---------------------+  |
|  |       service       |  |  ビジネスロジック層 (Controller + Service)
|  +---------------------+  |
|  |       deploy        |  |  アプリケーションエントリと設定
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

## 技術スタック

| コンポーネント | バージョン | 説明 |
|---------------|-----------|------|
| Spring Boot | 2.6.13 | アプリケーションフレームワーク |
| MyBatis Plus | 3.4.1 | ORM フレームワーク |
| Redis | - | キャッシュ |
| RocketMQ | 2.2.3 (spring starter) | メッセージキュー |
| MySQL | 8.0+ | データベース |
| Druid | 1.2.8 | コネクションプール |
| ShedLock | 2.3.0 | 分散スケジュールタスクロック |
| Swagger | 2.9.2 | API ドキュメント |

## クイックスタート

### 前提条件

- **Java 8+** (JDK 8 を推奨)
- **Maven 3.5+**
- **MySQL 8.0+**
- **Redis** (オプション、キャッシュ用)
- **RocketMQ** (オプション、メッセージ消費用)

### ビルド

```bash
# プロジェクトのコンパイル
mvn clean compile

# テストの実行
mvn test

# パッケージング (テストをスキップ)
mvn clean package -Dmaven.test.skip=true
```

### 設定

1. `deploy/src/main/resources/application.properties` を編集し、アクティブな環境プロファイルを選択
2. 対応する環境プロファイル `application-{env}.properties` を編集し、以下の内容を設定:

```properties
# データベース設定
spring.datasource.url=jdbc:mysql://YOUR_DB_HOST:3306/deepcover_brain?useSSL=false&characterEncoding=utf8
spring.datasource.username=YOUR_DB_USER
spring.datasource.password=YOUR_DB_PASSWORD

# Redis 設定 (有効な場合)
spring.redis.host=YOUR_REDIS_HOST
spring.redis.port=6379

# RocketMQ 設定 (有効な場合)
rocketmq.name-server=YOUR_ROCKETMQ_NAMESERVER:9876
```

### 実行

```bash
# パッケージング後に実行
java -jar deploy/target/deepcover-brain-deploy.jar

# 環境を指定して実行
java -jar deploy/target/deepcover-brain-deploy.jar --spring.profiles.active=test
```

## API ドキュメント

アプリケーション起動後、Swagger UI にアクセスして API ドキュメントを確認:

```
http://localhost:8080/swagger-ui.html
```

## 設定リファレンス

| 設定項目 | 説明 | デフォルト値 |
|---------|------|------------|
| `spring.profiles.active` | 環境識別子 (test/pre/prod) | test |
| `spring.datasource.url` | プライマリデータベース接続 URL | - |
| `spring.datasource.complexity.url` | 複雑度分析データベース接続 URL | - |
| `rocketmq.name-server` | RocketMQ NameServer アドレス | - |
| `spring.redis.host` | Redis ホストアドレス | - |
| `codediff.url` | コード差分分析サービス URL | - |
| `datacenter.url` | データセンターサービス URL | - |

## コントリビュート

コントリビュートを歓迎します！開発環境のセットアップとサブミッションプロセスについては [CONTRIBUTING.md](../CONTRIBUTING.md) をご参照ください。

## ライセンス

本プロジェクトは [Apache License 2.0](../LICENSE) の下でライセンスされています。

Copyright 2024-2026 DeepCover
