# AresBrain系统UML类图

## 系统架构概览

AresBrain是一个基于Spring Boot的代码分析和差异检测系统，采用经典的三层架构模式。

## 核心模块UML类图

### 1. Controller层 (表现层)

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                    Controller Package                                    │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│  ┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐       │
│  │   AresAgentController │    │ AresBrainController │    │DiffAnalyseController│       │
│  ├─────────────────────┤    ├─────────────────────┤    ├─────────────────────┤       │
│  │ +add()              │    │ +query()            │    │ +add()              │       │
│  │ +update()           │    │ +queryDetail()      │    │ +queryServiceList() │       │
│  │ +batchUpdate()      │    │ +queryTest()        │    │ +queryDetailById()  │       │
│  │ +queryList()        │    │ +create()           │    │ +refreshScene()     │       │
│  │ +queryDiffList()    │    │ +updateStatus()     │    │ +getCompareFile()   │       │
│  └─────────────────────┘    └─────────────────────┘    └─────────────────────┘       │
│                                                                                         │
│  ┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐       │
│  │ModuleVersionController│    │AresBrainLinkController│ │ AresBrainSceneController │  │
│  ├─────────────────────┤    ├─────────────────────┤    ├─────────────────────┤       │
│  │ +serviceList()      │    │ +queryLinkDetail()  │    │ +addFeedBack()     │       │
│  │ +add()              │    │                     │    │ +markCore()        │       │
│  │ +delete()           │    │                     │    │ +setScene()        │       │
│  │ +updateService()    │    │                     │    │ +queryProject()    │       │
│  │ +frozen()           │    │                     │    │ +queryClass()      │       │
│  │ +active()           │    │                     │    │ +queryMethod()     │       │
│  └─────────────────────┘    └─────────────────────┘    └─────────────────────┘       │
│                                                                                         │
│  ┌─────────────────────┐                                                                │
│  │AresRepeaterController│                                                                │
│  ├─────────────────────┤                                                                │
│  │ +startReplay()      │                                                                │
│  │ +ReplayResult()     │                                                                │
│  │ +startReplayBatch() │                                                                │
│  │ +updateReplayRate() │                                                                │
│  └─────────────────────┘                                                                │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

### 2. Service层 (业务逻辑层)

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                     Service Package                                      │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│  ┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐       │
│  │   AresAgentService  │    │DiffAnalyseService   │    │ModuleVersionService │       │
│  ├─────────────────────┤    ├─────────────────────┤    ├─────────────────────┤       │
│  │ +add()              │    │ +preAdd()           │    │ +queryServiceList() │       │
│  │ +update()           │    │ +newDiffAnalyseData()│    │ +add()              │       │
│  │ +batchUpdate()      │    │ +addDiffAnalyseData()│    │ +deleteById()       │       │
│  │ +queryList()        │    │ +queryServiceList() │    │ +updateServiceVersion()│      │
│  │ +queryDiffList()    │    │ +queryLatestList()  │    │ +queryDefault()     │       │
│  │ +getAllServiceName()│    │ +queryDetailById()  │    │ +queryList()        │       │
│  │ +queryObject()      │    │ +refreshScene()     │    │ +queryTotal()       │       │
│  │ +syncServerInfo()   │    │ +getCompareFile()   │    └─────────────────────┘       │
│  │ +listReportServer() │    │ +updateTraceIdStatus()│                                     │
│  └─────────────────────┘    │ +getViewedStatus()   │                                     │
│                            └─────────────────────┘                                     │
│                                                                                         │
│  ┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐       │
│  │AresBrainLinkService │    │AresBrainSceneService│    │AresBrainRepeaterService│    │
│  ├─────────────────────┤    ├─────────────────────┤    ├─────────────────────┤       │
│  │ +queryLinkDetail()  │    │ +addFeedBack()      │    │ +startReplayBatch() │       │
│  │ +queryLinkDetailBatch()│  │ +markCore()         │    │ +startReplay()      │       │
│  │                     │    │ +setScene()         │    │ +ReplayResult()     │       │
│  │                     │    │ +queryProject()     │    │ +updateReplayRate() │       │
│  │                     │    │ +queryClass()       │    │ +insertSceneSuiteId()│       │
│  │                     │    │ +queryMethod()      │    │ +aresCreateTestCase()│       │
│  │                     │    │ +querySceneDetail() │    └─────────────────────┘       │
│  │                     │    │ +queryScene()       │                                     │
│  │                     │    │ +querySceneBatch()  │                                     │
│  │                     │    └─────────────────────┘                                     │
│  └─────────────────────┘                                                                     │
│                                                                                         │
│  ┌─────────────────────┐    ┌─────────────────────┐                                   │
│  │SceneRiskLevelService│    │HbaseDataAnalysisService│                               │
│  ├─────────────────────┤    ├─────────────────────┤                                   │
│  │ +getModifyMethodRisk()│    │ +analysisHbaseData()│                                   │
│  │ +getSceneRisk()     │    │ +distinctNode()     │                                   │
│  │ +getComplexityRisk()│    │ +sceneProcess()     │                                   │
│  │ +getServiceRule()   │    │ +insertSceneTraceIdModel()│                           │
│  └─────────────────────┘    │ +autoAnalyseForScene()│                               │
│                            └─────────────────────┘                                   │
└─────────────────────────────────────────────────────────────────────────────────────────┘

### Service实现类关系图

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                 Service Implementation Package                          │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│  ┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐       │
│  │AresAgentServiceImpl │    │DiffAnalyseServiceImpl│    │ModuleVersionServiceImpl│    │
│  ├─────────────────────┤    ├─────────────────────┤    ├─────────────────────┤       │
│  │ 实现AresAgentService │    │ 实现DiffAnalyseService│    │ 实现ModuleVersionService│   │
│  │                     │    │                     │    │                     │       │
│  │ @Autowired          │    │ @Autowired          │    │ @Autowired          │       │
│  │ -AresServiceMapper  │    │ -DiffRecordMapper   │    │ -ModuleVersionMapper │       │
│  │ -AresReportMapper   │    │ -AresServiceMapper  │    │ -AresReportMapper   │       │
│  │ -AresConfigMapper   │    │ -ResultDetailMapper │    │                     │       │
│  │ -ModuleVersionMapper│    │ -AresBrainLinkService│                            │       │
│  │ -SceneServiceClassMapper│  │ -RiskLevelService   │                            │       │
│  │                     │    │ -BrainSceneService  │                            │       │
│  │ +add()              │    │ -SceneTraceIdMapper │                            │       │
│  │ +update()           │    │                     │                            │       │
│  │ +batchUpdate()      │    │ +preAdd()           │                            │       │
│  │ +queryList()        │    │ +newDiffAnalyseData()│                            │       │
│  │ +queryDiffList()    │    │ +addDiffAnalyseData()│                            │       │
│  │ +syncServerInfo()   │    │ +queryServiceList() │                            │       │
│  │ +listReportServer() │    │ +queryDetailById()  │                            │       │
│  │ +queryService()     │    │ +refreshScene()     │                            │       │
│  └─────────────────────┘    │ +getCompareFile()   │    │ +queryServiceList() │       │
│                            │ +updateTraceIdStatus()│    │ +add()              │       │
│                            │ +getViewedStatus()   │    │ +deleteById()       │       │
│                            │ +mergeEpaasData()    │    │ +updateServiceVersion()│      │
│                            │ +processTraceIdMatching()│  │ +queryDefault()     │       │
│                            │ +caclResultStats()   │    │ +queryList()        │       │
│                            │ +caclResultStatsForEpaas()│                        │       │
│                            └─────────────────────┘    │ +queryTotal()       │       │
│                                                            └─────────────────────┘       │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

### 3. Mapper层 (数据访问层)

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                      Mapper Package                                     │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│  ┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐       │
│  │   AresServiceMapper │    │  DiffRecordMapper   │    │ResultDetailMapper   │       │
│  ├─────────────────────┤    ├─────────────────────┤    ├─────────────────────┤       │
│  │ +insert()           │    │ +insert()           │    │ +insert()           │       │
│  │ +updateById()       │    │ +queryObjectById()  │    │ +updateById()       │       │
│  │ +batchUpdateByIds() │    │ +queryObjectByUnique()│    │ +queryObjectById()  │       │
│  │ +queryObject()      │    │ +updateRestResult() │    │ +updateByIdEpaas()  │       │
│  │ +queryList()        │    │ +queryLatestList()  │    └─────────────────────┘       │
│  │ +querySampleRateDiffList()│  │ +queryList()        │                                     │
│  │ +querySendDataCenterTypeDiffList()│  │ +queryDiffStats()   │                                     │
│  │ +queryExceptionThresholdDiffList()│  │ +queryDiffStatsTotal()│                               │
│  │ +queryTotal()       │    │ +selectLatestByServiceNameAndEnv()│                             │
│  │ +getAllServiceName()│    └─────────────────────┘                                     │
│  └─────────────────────┘                                                                         │
│                                                                                         │
│  ┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐       │
│  │ ModuleVersionMapper│    │   AresReportMapper  │    │   SceneTraceIdMapper │      │
│  ├─────────────────────┤    ├─────────────────────┤    ├─────────────────────┤       │
│  │ +queryServiceList() │    │ +queryObject()      │    │ +getSceneTraceIds() │       │
│  │ +queryServiceTotal()│    │ +queryList()        │    │ +updateStatus()     │       │
│  │ +insert()           │    │ +queryActiveList()  │    │ +getStatusByTraceId()│       │
│  │ +insertModuleVersion()│    │ +update()           │    │ +getSceneTraceIdsByServiceParams()│
│  │ +updateStatusById() │    │ +insert()           │    │ +getTraceIdsByServiceParams()│
│  │ +queryDefault()     │    │ +insertOrUpdate()   │    │ +getDistinctReportList()│       │
│  │ +updateServiceVersion()│    └─────────────────────┘    │ +getDistinctReportCount()│       │
│  │ +queryLatestVersion()│                            └─────────────────────┘       │
│  │ +queryList()        │                                                                     │
│  │ +queryTotal()       │                                                                     │
│  └─────────────────────┘                                                                     │
│                                                                                         │
│  ┌─────────────────────┐    ┌─────────────────────┐                                   │
│  │   AresConfigMapper  │    │SceneServiceClassMapper│                                   │
│  ├─────────────────────┤    ├─────────────────────┤                                   │
│  │ +queryObject()      │    │ +findByServiceNameAndClassName()│                           │
│  └─────────────────────┘    │ +getServiceName()    │                                   │
│                            │ +getClassName()      │                                   │
│                            └─────────────────────┘                                   │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

### 4. Entity层 (数据模型层)

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                      Entity Package                                     │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│  ┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐       │
│  │   AresAgentEntity   │    │   DiffRecordEntity  │    │ModuleVersionEntity  │       │
│  ├─────────────────────┤    ├─────────────────────┤    ├─────────────────────┤       │
│  │ -id: Long          │    │ -id: Long           │    │ -id: Long           │       │
│  │ -serviceName: String│    │ -serviceName: String│    │ -type: Integer      │       │
│  │ -packageName: String│    │ -baseVersion: String│    │ -typeName: String   │       │
│  │ -sampleRate: Long  │    │ -nowVersion: String │    │ -branch: String     │       │
│  │ -ignoreUrls: String│    │ -baseGitUrl: String │    │ -version: Integer   │       │
│  │ -reportPeriod: Integer│   │ -nowGitUrl: String   │    │ -commitId: String   │       │
│  │ -exceptionThreshold: Long│   │ -envCode: String    │    │ -ossUrl: String     │       │
│  │ -status: Integer   │    │ -status: Integer    │    │ -createTime: Date   │       │
│  │ -createTime: Date  │    │ -isEpaas: Integer    │    │ -updateTime: Date   │       │
│  │ -updateTime: Date  │    │ -createTime: Date    │    └─────────────────────┘       │
│  └─────────────────────┘    │ -updateTime: Date    │                                     │
│                            └─────────────────────┘                                     │
│                                                                                         │
│  ┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐       │
│  │ ReportAgentEntity   │    │DiffResultDetailEntity│    │ServiceModuleVersionEntity│   │
│  ├─────────────────────┤    ├─────────────────────┤    ├─────────────────────┤       │
│  │ -id: Long           │    │ -id: Long           │    │ -serviceName: String│       │
│  │ -serviceName: String│    │ -codeDiff: String   │    │ -aresEnabled: Integer│       │
│  │ -ip: String        │    │ -resultDetail: String│    │ -aresModuleVersion: Integer│ │
│  │ -version: String   │    │ -resultStats: String│    │ -repeaterEnabled: Integer│     │
│  │ -status: Integer   │    │ -resultDetailEpaas: String│  │ -repeaterModuleVersion: Integer││
│  │ -moduleInfo: String│    │ -createTime: Date    │    │ -chaosbladeEnabled: Integer│  │
│  │ -heartbeatTime: Date│    │ -updateTime: Date    │    │ -chaosbladeModuleVersion: Integer││
│  │ -reportTime: Date  │    └─────────────────────┘    │ -emockEnabled: Integer│       │
│  └─────────────────────┘                            │ -emockModuleVersion: Integer│  │
│                                                   └─────────────────────┘       │
│                                                                                         │
│  ┌─────────────────────┐    ┌─────────────────────┐                                   │
│  │  SceneTraceid       │    │   SceneRiskLevel   │                                   │
│  ├─────────────────────┤    ├─────────────────────┤                                   │
│  │ -id: Long           │    │ -id: Long           │                                   │
│  │ -traceid: String    │    │ -serviceName: String │                                   │
│  │ -serviceName: String│    │ -riskLevel: Integer │                                   │
│  │ -envCode: String    │    │ -rule: String       │                                   │
│  │ -branchName: String │    │ -createTime: Date    │                                   │
│  │ -baseVersion: String│    │ -updateTime: Date    │                                   │
│  │ -nowVersion: String │    └─────────────────────┘                                   │
│  │ -status: Integer   │                                                                           │
│  │ -operator: String   │                                                                           │
│  │ -createTime: Date   │                                                                           │
│  └─────────────────────┘                                                                           │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

## 核心依赖关系图

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                     系统依赖关系图                                    │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│  Controller层                                                                           │
│  ┌─────────────────────┐                                                                │
│  │ AresAgentController │                                                                │
│  └─────────┬───────────┘                                                                │
│            │                                                                               │
│            │ @Autowired                                                                   │
│            ▼                                                                               │
│  Service层                                                                               │
│  ┌─────────────────────┐                                                                │
│  │ AresAgentService    │                                                                │
│  └─────────┬───────────┘                                                                │
│            │ implements                                                                  │
│            ▼                                                                               │
│  ┌─────────────────────┐                                                                │
│  │AresAgentServiceImpl│                                                                │
│  └─────────┬───────────┘                                                                │
│            │ @Autowired                                                                   │
│            ▼                                                                               │
│  Mapper层                                                                                │
│  ┌─────────────────────┐                                                                │
│  │ AresServiceMapper   │                                                                │
│  └─────────┬───────────┘                                                                │
│            │ MyBatis                                                                   │
│            ▼                                                                               │
│  Database                                                                               │
│  ┌─────────────────────┐                                                                │
│  │   MySQL Database   │                                                                │
│  └─────────────────────┘                                                                │
│                                                                                         │
│  Entity层 (被Mapper和Service使用)                                                        │
│  ┌─────────────────────┐    ┌─────────────────────┐                                   │
│  │   AresAgentEntity   │    │ ReportAgentEntity   │                                   │
│  └─────────────────────┘    └─────────────────────┘                                   │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

## 异步处理和配置

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              异步处理和配置模块                                         │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│  ┌─────────────────────┐    @Async("dbExecutor")    @Async("httpExecutor")           │
│  │   ExecutorConfig    │ ┌─────────────────────┐  ┌─────────────────────┐           │
│  ├─────────────────────┤ │ AresAgentServiceImpl│  │DiffAnalyseServiceImpl│           │
│  │ -dbExecutor         │ ├─────────────────────┤  ├─────────────────────┤           │
│  │ -httpExecutor       │ │ +syncServerInfo()    │  │ +addDiffAnalyseData()│           │
│  │                     │ └─────────────────────┘  │ +newDiffAnalyseData()│           │
│  │ @Bean("dbExecutor") │                          └─────────────────────┘           │
│  │ ThreadPoolTaskExecutor│                                                                 │
│  │                     │    ThreadPoolMonitor                                        │
│  │ @Bean("httpExecutor")│  ┌─────────────────────┐                                     │
│  │ ThreadPoolTaskExecutor│ │ SimpleThreadPoolMonitor│                                   │
│  └─────────────────────┘ ├─────────────────────┤                                     │
│                            │ +monitor()           │                                     │
│                            │ +printThreadPoolStatus()│                                 │
│                            └─────────────────────┘                                     │
│                                                                                         │
│  HTTP客户端配置                                                                          │
│  ┌─────────────────────┐                                                                │
│  │  HttpClientConfig    │                                                                │
│  ├─────────────────────┤                                                                │
│  │ @Bean("httpClientWithTimeout")│                                                     │
│  │ CloseableHttpClient   │                                                                │
│  └─────────────────────┘                                                                │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

## 关键注解和配置

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              系统关键注解                                             │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│  Spring Boot注解:                                                                       │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │ @RestController - HTTP请求处理                                                      │   │
│  │ @Service - 业务逻辑组件                                                              │   │
│  │ @Autowired - 依赖注入                                                                │   │
│  │ @Async - 异步方法执行                                                                │   │
│  │ @Transactional - 事务管理                                                            │   │
│  │ @Value - 配置属性注入                                                                │   │
│  │ @Bean - Spring容器管理Bean                                                           │   │
│  │ @Configuration - 配置类                                                              │   │
│  │ @Mapper - MyBatis映射接口                                                            │   │
│  │ @Entity - JPA实体类                                                                   │   │
│  │ @Table - 数据库表映射                                                                 │   │
│  │ @Column - 数据库字段映射                                                               │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                         │
│  MyBatis-Plus配置:                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │ @MapperScan - Mapper接口扫描                                                        │   │
│  │ MybatisPlusConfig - 分页插件、加密解密配置                                             │   │
│  │ PaginationInterceptor - 分页拦截器                                                    │   │
│  │ CryptoInterceptor - 加密解密拦截器                                                    │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                         │
│  多数据源配置:                                                                           │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │ MybatisDefaultDataSourceConfig - 默认数据源配置                                        │   │
│  │ MybatisComplexityDataSourceConfig - 复杂数据源配置                                    │   │
│  │ @Primary - 主数据源标记                                                                │   │
│  │ @Qualifier - 限定符注解                                                                 │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

这个UML类图展示了AresBrain系统的整体架构，包括：

1. **分层架构**：Controller → Service → Mapper → Database的经典三层架构
2. **核心模块**：代码差异分析、Ares代理管理、模块版本管理、场景管理等
3. **依赖关系**：清晰的依赖注入和接口实现关系
4. **异步处理**：专门的线程池配置处理数据库操作和HTTP调用
5. **配置管理**：数据源配置、HTTP客户端配置、监控配置等

每个模块都有明确的职责分工，遵循了单一职责原则和依赖倒置原则。