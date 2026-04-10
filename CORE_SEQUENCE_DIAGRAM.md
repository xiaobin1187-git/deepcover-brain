# AresBrain系统核心链路时序图

## 概述

本文档展示AresBrain系统中几个核心业务流程的时序图，包括代码差异分析、Ares代理管理、模块版本管理等核心功能。

## 1. 代码差异分析流程

### 1.1 标准差异分析流程

```mermaid
sequenceDiagram
    participant Client as 客户端
    participant DAC as DiffAnalyseController
    participant DAS as DiffAnalyseService
    participant DASI as DiffAnalyseServiceImpl
    participant DRM as DiffRecordMapper
    participant RDM as ResultDetailMapper
    participant ExternalAPI as 外部代码差异API
    participant DB as 数据库

    Client->>DAC: POST /codeDiff/add (差异分析请求)
    DAC->>DAS: preAdd(recordEntity)
    DAS->>DASI: preAdd(recordEntity)

    Note over DASI: 1. 验证服务配置
    DASI->>DRM: queryObject(serviceName)
    DRM-->>DASI: AresAgentEntity

    Note over DASI: 2. 检查重复记录
    DASI->>DRM: queryObjectByUnique(recordEntity)
    DRM-->>DASI: DiffRecordEntity/null

    Note over DASI: 3. 插入或更新记录
    alt 无重复记录
        DASI->>DRM: insert(recordEntity)
    else 有重复记录
        DASI->>DRM: updateStatus(recordEntity)
    end

    DASI-->>DAS: Boolean (是否发送钉钉通知)
    DAS-->>DAC: success response
    DAC-->>Client: 返回成功

    Note over DASI: 4. 异步执行差异分析
    DASI->>DASI: @Async("httpExecutor") addDiffAnalyseData(recordEntity, dingMsgExist)

    Note over DASI: 5. 获取代码差异数据
    alt 无缓存数据
        DASI->>ExternalAPI: HTTP GET /code/diff/gitUrl/list
        ExternalAPI-->>DASI: JSON差异数据
    end

    Note over DASI: 6. 计算统计信息
    DASI->>DASI: caclResultStats(resData, serviceName, sceneLinkMap)

    Note over DASI: 7. 风险等级评估
    DASI->>DASI: 获取服务规则，评估方法风险

    Note over DASI: 8. 保存分析结果
    DASI->>RDM: insert(resultDetailEntity)
    DASI->>DRM: updateRestResult(recordEntity)

    Note over DASI: 9. 发送钉钉通知
    alt 需要发送通知且分析成功
        DASI->>DASI: DingNotifyUtils.sendDiffAnalyseWorkMessage()
    end
```

### 1.2 EPAAS差异分析流程（基于TraceId过滤）

```mermaid
sequenceDiagram
    participant Client as 客户端
    participant DAC as DiffAnalyseController
    participant DAS as DiffAnalyseService
    participant DASI as DiffAnalyseServiceImpl
    participant STM as SceneTraceIdMapper
    participant RDM as ResultDetailMapper
    participant DRM as DiffRecordMapper
    participant DB as 数据库

    Client->>DAC: POST /codeDiff/add (带traceId的请求)
    DAC->>DAS: preAdd(recordEntity)
    DAS->>DASI: preAdd(recordEntity)
    DASI-->>DAS: Boolean

    Note over DASI: 异步执行EPAAS分析
    DASI->>DASI: @Transactional newDiffAnalyseData(recordEntity, dingMsgExist, traceId)

    Note over DASI: 初始化差异结果详情
    DASI->>DASI: initDiffResultDetail(recordEntity)

    Note over DASI: 获取代码差异数据
    DASI->>DASI: getDiffResultData(recordEntity, resultDetailEntity)

    Note over DASI: 处理TraceId匹配过滤
    DASI->>DASI: processTraceIdMatching(resData, traceId, recordEntity)

    Note over DASI: 过滤逻辑：
    Note over DASI: 1. 遍历所有差异数据
    Note over DASI: 2. 检查API是否在TARGET_API中
    Note over DASI: 3. 只保留匹配指定traceId的场景
    Note over DASI: 4. 构建过滤后的epaas数据

    alt 有匹配的epaas数据
        Note over DASI: 合并现有epaas数据
        DASI->>DASI: mergeEpaasData(existing, new)

        Note over DASI: 数据去重处理
        DASI->>DASI: deduplicateEpaasData(epaasArray)

        Note over DASI: 计算EPAAS统计数据
        DASI->>DASI: caclResultStatsForEpaas(epaasArray)

        Note over DASI: 保存EPAAS结果
        DASI->>RDM: updateByIdEpaas(resultDetailEntity)
        DASI->>DRM: updateRestResultEpaas(recordEntity)

        Note over DASI: 保存报告参数到SceneTraceid
        DASI->>DASI: saveReportParamsToSceneTraceId(recordEntity, traceId)

        Note over DASI: 发送钉钉通知
        DASI->>DASI: DingNotifyUtils.sendEpassAnalyseWorkMessage()
    end
```

## 2. Ares代理管理流程

### 2.1 Ares代理配置管理

```mermaid
sequenceDiagram
    participant Client as 客户端
    participant AAC as AresAgentController
    participant AAS as AresAgentService
    participant AASI as AresAgentServiceImpl
    participant ASM as AresServiceMapper
    participant ACR as AresConfigMapper
    participant MVM as ModuleVersionMapper
    participant DB as 数据库

    Client->>AAC: POST /agent/add (添加Ares代理配置)
    AAC->>AAS: add(aresAgentEntity)
    AAS->>AASI: add(aresAgentEntity)

    Note over AASI: 1. 检查服务是否已存在
    AASI->>ASM: queryObject(serviceName)
    ASM-->>AASI: AresAgentEntity/null

    alt 服务不存在
        Note over AASI: 2. 加载默认配置参数
        AASI->>ACR: queryObject("ignore_urls")
        ACR-->>AASI: ConfigEntity
        AASI->>ACR: queryObject("sampleRate")
        ACR-->>AASI: ConfigEntity
        AASI->>ACR: queryObject("limitCodeMethodSize")
        ACR-->>AASI: ConfigEntity
        Note over AASI: ... 加载其他配置参数

        Note over AASI: 3. 设置代理配置
        AASI->>AASI: 设置采样率、忽略URL、异常阈值等

        Note over AASI: 4. 插入代理配置
        AASI->>ASM: insert(aresAgentEntity)

        Note over AASI: 5. 初始化模块版本配置
        AASI->>AASI: 创建ServiceModuleVersionEntity
        AASI->>AASI: 设置各模块启用状态和版本
        AASI->>MVM: insertModuleVersion(serviceModuleVersionEntity)

        AASI-->>AAS: success
    else 服务已存在
        AASI-->>AAS: 抛出RRException("应用名已存在")
    end

    AAS-->>AAC: success response
    AAC-->>Client: 返回成功
```

### 2.2 服务器信息同步流程

```mermaid
sequenceDiagram
    participant Agent as Ares Agent
    participant AAC as AresAgentController
    participant AAS as AresAgentService
    participant AASI as AresAgentServiceImpl
    participant ARM as AresReportMapper
    participant Sandbox as Sandbox模块
    participant DB as 数据库

    Note over Agent: Agent启动或心跳时上报信息
    Agent->>AAC: POST /aresbrain/ares/report/server/info
    AAC->>AAS: syncServerInfo(reportAgentEntity)
    AAS->>AASI: @Async("dbExecutor") syncServerInfo(reportAgentEntity)

    Note over AASI: 使用数据库专用线程池异步执行
    AASI->>ARM: insertOrUpdate(reportAgentEntity)
    ARM-->>AASI: success
    AASI-->>AAS: 异步执行完成
    AAS-->>AAC: success response
    AAC-->>Agent: 返回成功
```

### 2.3 模块状态查询流程

```mermaid
sequenceDiagram
    participant Client as 客户端
    participant AAC as AresAgentController
    participant AAS as AresAgentService
    participant AASI as AresAgentServiceImpl
    participant ARM as AresReportMapper
    participant Sandbox as 各服务器Sandbox
    participant DB as 数据库

    Client->>AAC: GET /agent/listReportServerInfo?serviceName=xxx
    AAC->>AAS: listReportServerInfo(serviceName)
    AAS->>AASI: listReportServerInfo(serviceName)

    Note over AASI: 1. 查询服务下的活跃服务器
    AASI->>ARM: queryActiveList(serviceName)
    ARM-->>AASI: List<ReportAgentEntity>

    Note over AASI: 2. 遍历每个服务器查询模块状态
    loop 每个服务器
        Note over AASI: 检查服务器状态
        alt 服务器状态正常
            Note over AASI: 调用Sandbox API查询模块列表
            AASI->>Sandbox: HTTP GET /sandbox/default/module/http/sandbox-module-mgr/list
            Sandbox-->>AASI: 模块列表响应

            Note over AASI: 解析模块信息
            AASI->>AASI: 过滤系统模块(Login-Filter等)
            AASI->>AASI: 统计活跃模块数量
            AASI->>AASI: 构建模块信息JSON
        else 服务器状态异常
            AASI->>AASI: 记录警告日志
        end
    end

    AASI-->>AAS: 带模块信息的服务器列表
    AAS-->>AAC: 响应结果
    AAC-->>Client: 返回服务器列表及模块状态
```

## 3. 模块版本管理流程

### 3.1 模块版本添加流程

```mermaid
sequenceDiagram
    participant Client as 客户端
    participant MVC as ModuleVersionController
    participant MVS as ModuleVersionService
    participant MVSI as ModuleVersionServiceImpl
    participant MVM as ModuleVersionMapper
    participant DB as 数据库

    Client->>MVC: POST /module/version/add
    MVC->>MVS: add(moduleVersionEntity)
    MVS->>MVSI: add(moduleVersionEntity)

    Note over MVSI: 1. 根据类型设置模块名称
    alt type == 1
        MVSI->>MVSI: setTypeName("ares")
    else type == 2
        MVSI->>MVSI: setTypeName("repeater")
    else type == 3
        MVSI->>MVSI: setTypeName("chaosblade")
    else type == 4
        MVSI->>MVSI: setTypeName("emock")
    else 其他类型
        MVSI->>MVSI: 抛出RRException
    end

    Note over MVSI: 2. 生成版本号
    MVSI->>MVM: queryLatestVersion(type)
    MVM-->>MVSI: 最新版本/null
    MVSI->>MVSI: version = latestVersion == null ? 1 : latestVersion + 1

    Note over MVSI: 3. 构建OSS URL
    MVSI->>MVSI: ossUrl = baseOssUrl + typeName + "_" + version + ".jar"

    Note over MVSI: 4. 保存模块版本信息
    MVSI->>MVM: insert(moduleVersionEntity)
    MVM-->>MVSI: success

    MVSI-->>MVS: success
    MVS-->>MVC: 返回添加的模块版本信息
    MVC-->>Client: success response
```

### 3.2 模块激活/冻结流程

```mermaid
sequenceDiagram
    participant Client as 客户端
    participant MVC as ModuleVersionController
    participant Sandbox as 目标服务器Sandbox
    participant DB as 数据库

    Note over Client: 模块激活
    Client->>MVC: POST /module/version/active/{moduleName}
    MVC->>Sandbox: HTTP GET http://ip:4769/sandbox/default/module/http/sandbox-module-mgr/active?ids={moduleName}
    Sandbox-->>MVC: 激活结果
    MVC-->>Client: 返回激活结果

    Note over Client: 模块冻结
    Client->>MVC: POST /module/version/frozen/{moduleName}
    MVC->>Sandbox: HTTP GET http://ip:4769/sandbox/default/module/http/sandbox-module-mgr/frozen?ids={moduleName}
    Sandbox-->>MVC: 冻结结果
    MVC-->>Client: 返回冻结结果
```

## 4. 场景管理流程

### 4.1 场景详情查询流程

```mermaid
sequenceDiagram
    participant Client as 客户端
    participant ASC as AresBrainSceneController
    participant ABS as AresBrainSceneService
    participant ABSI as 业务实现类
    participant HTTP as 外部HTTP服务
    participant DB as 数据库

    Client->>ASC: POST /querySceneDetail
    ASC->>ABS: querySceneDetail(searchModel)
    ABS->>ABSI: querySceneDetail(searchModel)

    Note over ABSI: 1. 构建查询参数
    ABSI->>ABSI: 构建HTTP请求参数

    Note over ABSI: 2. 调用外部服务查询场景详情
    ABSI->>HTTP: HTTP POST /queryLinkDetail
    HTTP-->>ABSI: JSON响应数据

    Note over ABSI: 3. 处理和转换数据
    ABSI->>ABSI: 解析JSON响应
    ABSI->>ABSI: 转换为FrontModel对象
    ABSI->>ABSI: 处理分页信息
    ABSI->>ABSI: 处理统计数据

    ABSI-->>ABS: FrontModel
    ABS-->>ASC: AresBrainResult<FrontModel>
    ASC-->>Client: 返回场景详情
```

### 4.2 批量场景分析流程

```mermaid
sequenceDiagram
    participant Client as 客户端
    participant ASC as AresBrainSceneController
    participant ABS as AresBrainSceneService
    participant HDAS as HbaseDataAnalysisService

    Client->>ASC: POST /querySceneBatch
    ASC->>ABS: querySceneBatch(querySceneBO)

    Note over ABS: 异步执行批量分析
    ABS->>ABS: @Async 方法处理

    Note over ABS: 1. 分析HBase数据
    ABS->>HDAS: analysisHbaseData(traceId)
    HDAS->>HDAS: 查询和排序HBase数据
    HDAS->>HDAS: distinctNode() 去重处理
    HDAS->>HDAS: sceneProcess() 场景处理

    Note over HDAS: 2. 保存场景数据
    HDAS->>HDAS: insertSceneTraceIdModel()
    HDAS->>HDAS: insertSceneBranch()

    Note over ABS: 3. 自动触发差异分析
    ABS->>ABS: autoAnalyseForScene()

    ABS-->>ASC: 异步处理已启动
    ASC-->>Client: 返回处理启动确认
```

## 5. 异步处理和监控流程

### 5.1 线程池监控流程

```mermaid
sequenceDiagram
    participant ScheduledTask as 定时任务
    participant TPM as ThreadPoolMonitor
    participant SPM as SimpleThreadPoolMonitor
    participant Executor as ThreadPoolExecutor
    participant Log as 日志系统

    Note over ScheduledTask: 每30秒执行一次
    ScheduledTask->>TPM: monitor()
    TPM->>SPM: monitorAllThreadPools()

    Note over SPM: 监控dbExecutor
    SPM->>SPM: printThreadPoolStatus("dbExecutor", dbExecutor)
    SPM->>Executor: getCorePoolSize()
    Executor-->>SPM: 核心线程数
    SPM->>Executor: getActiveCount()
    Executor-->>SPM: 活跃线程数
    SPM->>Executor: getQueue().size()
    Executor-->>SPM: 队列任务数
    SPM->>Executor: getCompletedTaskCount()
    Executor-->>SPM: 已完成任务数

    Note over SPM: 检查告警阈值
    alt 活跃线程数 >= 8
        SPM->>Log: WARN: dbExecutor活跃线程数过高
    else 队列任务数 >= 400
        SPM->>Log: WARN: dbExecutor队列积压严重
    else 活跃线程数 >= 9
        SPM->>Log: ERROR: dbExecutor线程池接近饱和
    end

    Note over SPM: 监控httpExecutor (类似逻辑)
    SPM->>SPM: printThreadPoolStatus("httpExecutor", httpExecutor)

    SPM-->>TPM: 监控完成
    TPM-->>ScheduledTask: 监控结果
```

### 5.2 HTTP客户端超时处理流程

```mermaid
sequenceDiagram
    participant Service as 业务服务
    participant HC as HttpClientUtil
    participant HCC as HttpClientConfig
    participant HTTP as HTTP客户端
    participant Target as 目标服务

    Service->>HC: sendRequest(request)

    Note over HC: 获取配置的HTTP客户端
    HC->>HCC: httpClientWithTimeout()

    Note over HCC: 客户端配置包含：
    Note over HCC: - 连接超时: 5秒
    Note over HCC: - Socket超时: 10秒
    Note over HCC: - 连接请求超时: 3秒

    HCC-->>HC: CloseableHttpClient
    HC->>HTTP: 执行HTTP请求

    alt 请求成功
        HTTP-->>HC: HttpResponse
        HC-->>Service: 成功响应
    else 连接超时
        HTTP--xHC: ConnectTimeoutException
        HC-->>Service: 超时异常
    else Socket超时
        HTTP--xHC: SocketTimeoutException
        HC-->>Service: 超时异常
    else 其他异常
        HTTP--xHC: 其他IOException
        HC-->>Service: IO异常
    end
```

## 总结

这些时序图展示了AresBrain系统的核心业务流程：

1. **代码差异分析**：支持标准和EPAAS两种模式，包含异步处理、风险等级评估、统计计算等功能
2. **Ares代理管理**：完整的代理配置管理、服务器信息同步、模块状态查询功能
3. **模块版本管理**：模块版本添加、激活/冻结管理等核心功能
4. **场景管理**：场景详情查询、批量分析等复杂业务流程
5. **异步处理和监控**：线程池监控、HTTP客户端超时处理等基础设施功能

系统采用了异步处理、多线程池、超时控制等技术手段，确保了系统的稳定性和高性能。同时通过完整的监控和告警机制，保证了系统的可观测性。