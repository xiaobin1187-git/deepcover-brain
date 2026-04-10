/*
 * Copyright 2024-2026 DeepCover
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.deepcover.brain.service.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.deepcover.brain.dal.entity.DiffRecordEntity;
import io.deepcover.brain.dal.entity.SceneNew;
import io.deepcover.brain.dal.entity.SceneServiceClass;
import io.deepcover.brain.dal.mapper.DiffRecordMapper;
import io.deepcover.brain.dal.mapper.SceneNewMapper;
import io.deepcover.brain.dal.mapper.SceneServiceClassMapper;
import io.deepcover.brain.model.*;
import io.deepcover.brain.service.util.DateTimeUtil;
import io.deepcover.brain.service.util.ObjectConverterUtil;
import io.deepcover.brain.service.util.client.HttpClientUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletionException;

/**
 * HBase数据分析服务
 *
 * <p>该服务负责从HBase中获取追踪数据，并进行深度分析。主要功能包括：</p>
 * <ul>
 *   <li>从HBase获取原始追踪数据并按时间排序</li>
 *   <li>分析应用热度、API热度、方法热度等指标</li>
 *   <li>构建场景节点和相互关系图</li>
 *   <li>处理服务链路调用关系</li>
 *   <li>生成场景模型和链路分析数据</li>
 *   <li>支持场景数据的自动分析和差异比对</li>
 * </ul>
 *
 * <p>该服务是Ares大脑系统的核心组件，用于处理分布式系统中的链路追踪数据，
 * 将原始数据转换为可分析的场景信息，为系统监控、性能优化和故障排查提供数据支持。</p>
 *
 * @author huangtai
 * @version 1.0
 * @since 2023-04-20
 */
@Slf4j
@Service
public class HbaseDataAnalysisService {

//    @Value("${redis.close}")
//    private boolean redisClose;

    //private final CacheUtil cacheUtil;
    @Autowired
    SceneNewMapper sceneNewMapper;
    @Autowired
    SceneServiceClassMapper serviceClassMapper;

    @Autowired
    private DiffRecordMapper diffRecordMapper;
    @Autowired
    private DiffAnalyseService diffAnalyseService;
    @Value("${epaasapi:/v1/signflows/{flowId}/getShowUrl,/v2/processes/start,/v2/processes/startByFlowTemplate}")
    private String TARGET_API;
    @Autowired
    private Environment env;
    @Autowired
    @Qualifier("newAsyncServiceExecutor")
    private Executor newAsyncServiceExecutor;
//    public HbaseDataAnalysisService(CacheUtil cacheUtil) {
//
//        this.cacheUtil = cacheUtil;
//    }

    /**
     * 从HBase获取数据并按照代码块进行排序
     *
     * <p>根据链路追踪ID从HBase获取原始追踪数据，并对数据进行分析和排序。该方法负责：</p>
     * <ul>
     *   <li>验证traceId的有效性，清理已存在的数据</li>
     *   <li>从HBase获取完整的链路追踪数据</li>
     *   <li>解析和构建HbaseDataModel对象列表</li>
     *   <li>按照代码块的执行时间进行正序排序</li>
     *   <li>调用后续的数据分析方法</li>
     * </ul>
     *
     * <p>该方法获取的数据包含服务名、方法名、代码行号、执行时间等关键信息，用于后续的场景分析。</p>
     *
     * @param traceId 链路追踪ID，用于标识唯一的分布式调用链路
     * @return int 返回处理结果状态码，0表示成功，-1表示失败
     */
    @SneakyThrows
    public int analysisHbaseData(String traceId) {
        JSONArray checkResult = HttpClientUtil.httpClient("sceneTraceIdModel/checkTraceId?traceId=" + traceId, 1, "");
        if (!checkResult.getBoolean(0)) {
            log.info("删除traceId已有数据失败");
            return -1;
        }
        //根据traceId从HBase获取原始数据
        JSONArray jsonArray = HttpClientUtil.httpClient("trace/get?traceId=" + traceId, 1, "");
        if (jsonArray.size() == 0) {
            return 0;
        }
        List<HbaseDataModel> hbaseDataModels = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            //解析代码信息
            JSONObject serviceData = jsonArray.getJSONObject(i);
            if (serviceData.getString("method") == null) {
                continue;
            }
            String method = serviceData.getString("method").toLowerCase();
            String processId = serviceData.getString("processId");
            String serviceName = serviceData.getString("serviceName");
            String url = serviceData.getString("url");
            String branch = serviceData.getString("branch");
            JSONArray codeInfo = serviceData.getJSONArray("codeInfo");
            if (codeInfo == null || codeInfo.size() == 0) {
                return 0;
            }
            for (int j = 0; j < codeInfo.size(); j++) {
                //设置每一个代码块的信息，并需要按照代码块的时间排序
                HbaseDataModel hbaseDataModel = new HbaseDataModel();
                hbaseDataModel.setMethod(method);
                hbaseDataModel.setProcessId(processId);
                hbaseDataModel.setServiceName(serviceName);
                hbaseDataModel.setUrl(formatUrl(url, serviceName));
                hbaseDataModel.setBranch(branch);
                JSONObject code = codeInfo.getJSONObject(j);
                hbaseDataModel.setInvokeId(code.getIntValue("invokeId"));
                hbaseDataModel.setMethodName(code.getString("methodName"));
                hbaseDataModel.setLineNums(code.getList("lineNum", Integer.class));
                hbaseDataModel.setClassName(code.getString("className"));
                hbaseDataModel.setBeginTime(code.getString("beginTime"));
                hbaseDataModel.setParameters(code.getList("parameters", String.class));
                hbaseDataModels.add(hbaseDataModel);
            }
        }
        //按照方法的时间正序排列
        if (hbaseDataModels.size() > 1) {
            Collections.sort(hbaseDataModels, new Comparator<HbaseDataModel>() {
                @Override
                public int compare(HbaseDataModel o1, HbaseDataModel o2) {
                    return Long.valueOf(o1.getBeginTime()).compareTo(Long.valueOf(o2.getBeginTime()));
                }
            });
        }
        analysisHbaseData(hbaseDataModels, traceId);
        return 0;
    }

    /**
     * 分析原始数据，构建应用热度、API热度、方法热度、场景节点和相互关系
     *
     * <p>对排序后的HBase数据模型列表进行深度分析，构建完整的调用关系图。该方法负责：</p>
     * <ul>
     *   <li>分析服务调用顺序和应用热度统计</li>
     *   <li>计算API接口的访问热度</li>
     *   <li>统计方法的调用频率和内外部调用关系</li>
     *   <li>构建方法调用的关联关系图</li>
     *   <li>创建场景节点和父子节点关系</li>
     *   <li>处理循环调用和重复节点的情况</li>
     * </ul>
     *
     * <p>分析结果将用于场景建模、关系图谱构建和热度统计，为系统监控和优化提供数据支持。</p>
     *
     * @param hbaseDataModels 已排序的HBase数据模型列表，包含链路追踪的详细信息
     * @param traceId 链路追踪ID，用于标识唯一的分布式调用链路
     */
    @SneakyThrows
    public void analysisHbaseData(List<HbaseDataModel> hbaseDataModels, String traceId) {

        //变量定义
        //应用热度
//        Map<String, Integer> serviceHeatModels = new HashMap<>();
//        //api热度
//        Map<String, Integer> apiHeatModels = new HashMap<>();
//        //方法热度
//        Map<String, Integer> methodHeatModels = new HashMap<>();
        //设置好调用关系的数据
        Map<String, HbaseDataModel> methodDataOrder = new LinkedHashMap<>();
        //应用顺序
        List<String> serviceOrder = new ArrayList<>();
        //上一个代码块的唯一键
        String beforeMethodKey = "";
        //应用ID，processId标识应用被调用一次，新调用会发生变化
        List<String> processIds = new ArrayList<>();
        //方法ID，invokeId标识方法被调用一次，新调用会发生变化
        List<Integer> invokeIds = new ArrayList<>();
        List<String> serviceNodes = new ArrayList<>();
        for (HbaseDataModel hbaseDataModel : hbaseDataModels) {

            //服务的调用顺序
            String serviceName = hbaseDataModel.getServiceName();
            if (!serviceOrder.contains(serviceName)) {
                serviceOrder.add(serviceName);
            }
            //服务的热度
            String processId = hbaseDataModel.getProcessId();
            //   String serviceKey = serviceName;
//            int serviceHeat = 1;
//            if (!processIds.contains(processId)) {
//                if (serviceHeatModels.containsKey(serviceKey)) {
//                    //服务已经被调用则热度+1，否则初始热度为1
//                    serviceHeat = serviceHeatModels.get(serviceKey) + 1;
//                }
//                //添加到总数据中
//                serviceHeatModels.put(serviceKey, serviceHeat);
//            }

            //api的热度
            String apiName = hbaseDataModel.getUrl();
            String apiMethod = hbaseDataModel.getMethod();
//            String apiKey = serviceName + "&" + apiName + "&" + apiMethod;
//            int apiHeat = 1;
//            if (!processIds.contains(processId)) {
//                if (apiHeatModels.containsKey(apiKey)) {
//                    //接口已经被调用则热度+1，否则初始热度为1
//                    apiHeat = apiHeatModels.get(apiKey) + 1;
//                }
//                //添加到总数据中
//                apiHeatModels.put(apiKey, apiHeat);
//            }

            //方法热度
            String className = hbaseDataModel.getClassName();
            String methodName = hbaseDataModel.getMethodName();
            String parameters = hbaseDataModel.getParameters().toString();
            //方法的唯一标识
            String methodKey = serviceName + "&" + className + "&" + methodName + "&" + parameters.toString();
            int invokeId = hbaseDataModel.getInvokeId();
            // int outerHeat = 1, innerHeat = 1;
//            if (methodHeatModels.containsKey(methodKey)) {
//                outerHeat = methodHeatModels.get(methodKey) + 1;
//                innerHeat = methodHeatModels.get(methodKey) + 1;
//            }
//            //如果processId不包含方法所在的processId，则说明是其他系统调用了该方法
//            if (!processIds.contains(processId)) {
//                methodHeatModels.put(methodKey + "&outerHeat", outerHeat);
//            } else if (!invokeIds.contains(invokeId)) {
//                //如果processId已经存在，如果invokeIds不包含方法所在的invokeId，则说明是应用内部调用
//                methodHeatModels.put(methodKey + "&innerHeat", innerHeat);
//            }
            processIds.add(processId);
            invokeIds.add(invokeId);

            //方法关联关系设定
            String orderKey = processId + "&" + invokeId + "&" + methodKey;
            String serviceNodesKey = serviceName + "&" + apiName + "&" + apiMethod;
            if (methodDataOrder.containsKey(orderKey)) {
                //包含说明方法之前存在，只是从其它方法回到该方法，则只需要把覆盖行数汇总
                HbaseDataModel hbaseDataModelTemp = methodDataOrder.get(orderKey);
                List<Integer> lineNumsTemp = hbaseDataModelTemp.getLineNums();
                lineNumsTemp.addAll(hbaseDataModel.getLineNums());
                hbaseDataModelTemp.setLineNums(lineNumsTemp);
            } else if ("".equals(beforeMethodKey)) {

                HbaseDataModel firstNode = new HbaseDataModel();
                firstNode.setLinkId(UUID.randomUUID().toString());
                firstNode.setParentLinkId("0");
                firstNode.setServiceName(serviceName);
                firstNode.setUrl(apiName);
                firstNode.setMethod(apiMethod);
                firstNode.setBeginTime(hbaseDataModel.getBeginTime());
                firstNode.setType(1);
                methodDataOrder.put(serviceNodesKey + firstNode.getLinkId(), firstNode);
                serviceNodes.add(serviceNodesKey);
                //该方法是第一个方法
                hbaseDataModel.setLinkId(UUID.randomUUID().toString());
                hbaseDataModel.setParentLinkId(firstNode.getLinkId());
                methodDataOrder.put(orderKey, hbaseDataModel);
            } else {
                HbaseDataModel beforeDataModel = methodDataOrder.get(beforeMethodKey);
                String parentNodeLinkId = beforeDataModel.getLinkId();
                if (!serviceNodes.contains(serviceNodesKey) ||
                        (serviceNodes.contains(serviceNodesKey) && !processIds.contains(processId))) {
                    HbaseDataModel firstNode = new HbaseDataModel();
                    firstNode.setLinkId(UUID.randomUUID().toString());
                    firstNode.setParentLinkId(parentNodeLinkId);
                    firstNode.setServiceName(serviceName);
                    firstNode.setUrl(apiName);
                    firstNode.setMethod(apiMethod);
                    firstNode.setBeginTime(hbaseDataModel.getBeginTime());
                    firstNode.setType(1);
                    methodDataOrder.put(serviceNodesKey + firstNode.getLinkId(), firstNode);
                    parentNodeLinkId = firstNode.getLinkId();
                    serviceNodes.add(serviceNodesKey);
                }
                //获取前一个方法的信息，主要是获取前一个方法的linkId作为该方法的parentLineId
                hbaseDataModel.setParentLinkId(parentNodeLinkId);
                hbaseDataModel.setLinkId(UUID.randomUUID().toString());
                methodDataOrder.put(orderKey, hbaseDataModel);
            }
            beforeMethodKey = orderKey;
        }

        //场景处理
        distinctNode(methodDataOrder, traceId, serviceOrder);
//        if (0 != sceneId && !redisClose) {
//            //setRedisDate(serviceHeatModels, apiHeatModels, methodHeatModels, sceneId);
//        }
    }

    /**
     * 去除重复节点并构建场景节点模型
     *
     * <p>对方法调用数据进行去重处理，合并相同的方法调用记录，主要处理循环调用的情况。该方法负责：</p>
     * <ul>
     *   <li>根据方法+类名+方法名+参数+行号的组合识别重复节点</li>
     *   <li>合并重复节点，汇总行号信息和父子关系</li>
     *   <li>处理循环调用中的节点引用关系</li>
     *   <li>调整子节点的父节点引用，维护调用关系的完整性</li>
     *   <li>生成场景的唯一哈希码用于标识</li>
     *   <li>调用后续的场景处理方法</li>
     * </ul>
     *
     * <p>去重后的节点数据将用于构建清晰准确的服务调用链路图。</p>
     *
     * @param methodDataOrder 方法数据映射，包含排序后的方法调用关系数据
     * @param traceId 链路追踪ID，用于标识唯一的分布式调用链路
     * @param serviceOrder 服务调用顺序列表，记录服务被调用的顺序
     */
    public void distinctNode(Map<String, HbaseDataModel> methodDataOrder, String traceId, List<String> serviceOrder) {

        Map<String, HbaseDataModel> distinctNode = new LinkedHashMap<>();
        List<HbaseDataModel> hbaseDataModels = methodDataOrder.values().stream().collect(Collectors.toList());
        String hashCode = "";
        for (int i = 0; i < hbaseDataModels.size(); i++) {
            HbaseDataModel hbaseDataModel = hbaseDataModels.get(i);
            String methodKey = "";
            if (hbaseDataModel.getType() == 0) {
                methodKey = hbaseDataModel.getServiceName() + "&" + hbaseDataModel.getClassName() +
                        "&" + hbaseDataModel.getMethodName() + "&" + hbaseDataModel.getParameters() + "&" + hbaseDataModel.getLineNums();
            } else {
                methodKey = hbaseDataModel.getServiceName() + "&" + hbaseDataModel.getUrl() + "&" + hbaseDataModel.getMethod();
            }

            if (distinctNode.containsKey(methodKey)) {
                HbaseDataModel temp = distinctNode.get(methodKey);
                //如果方法重复，则增加设置父节点，父节点如果有循环去除重复的数据
                if (!StringUtils.contains(temp.getParentLinkId(), hbaseDataModel.getParentLinkId())) {
                    temp.setParentLinkId(temp.getParentLinkId() + "," + hbaseDataModel.getParentLinkId());
                    temp.setBeginTime(temp.getBeginTime() + "," + hbaseDataModel.getBeginTime());
                }
                //把该节点的子节点挂载到重复的节点上
                for (int j = i + 1; j < hbaseDataModels.size(); j++) {
                    HbaseDataModel nextHbaseDateModel = hbaseDataModels.get(j);
                    if (hbaseDataModel.getLinkId().equals(nextHbaseDateModel.getParentLinkId())) {
                        nextHbaseDateModel.setParentLinkId(temp.getLinkId());
                    }
                }
            } else {
                distinctNode.put(methodKey, hbaseDataModel);
                if (hbaseDataModel.getType() == 0) {
                    hashCode = md5(hashCode + "&" + methodKey);
                }
            }
        }
        sceneProcess(distinctNode.values().stream().collect(Collectors.toList()), traceId, serviceOrder, hashCode);
    }

    /**
     * 处理场景信息并保存到数据库
     *
     * <p>对去重后的场景节点数据进行处理，创建或更新场景模型。该方法负责：</p>
     * <ul>
     *   <li>查询服务是否为本地服务，区分处理逻辑</li>
     *   <li>根据本地/远程服务获取不同的场景信息来源</li>
     *   <li>检查场景是否已存在，执行新建或更新操作</li>
     *   <li>维护场景的热度统计和访问记录</li>
     *   <li>创建网关节点和场景节点关系</li>
     *   <li>记录场景的分支信息和链路追踪关系</li>
     *   <li>触发自动分析流程</li>
     * </ul>
     *
     * <p>场景信息处理完成后，支持场景查询、分析和管理功能。</p>
     *
     * @param sceneNodeModels 场景节点模型列表，包含场景的详细信息
     * @param traceId 链路追踪ID，用于标识唯一的分布式调用链路
     * @param serviceOrder 服务调用顺序列表，记录服务被调用的顺序
     * @param hashCode 场景哈希码，用于唯一标识场景内容
     */
    @SneakyThrows
    public void sceneProcess(List<Object> sceneNodeModels, String traceId, List<String> serviceOrder, String hashCode) {

        //查看服务是否本地服务
        JSONArray jsonArray = HttpClientUtil.httpClient("agentService/getLocalByServiceName?serviceName=" + serviceOrder.get(0), 1, "");
        int local = jsonArray.getIntValue(0);
        String branch = ((HbaseDataModel) sceneNodeModels.get(1)).getBranch();
        SceneModel sceneModel = sceneModel(traceId, serviceOrder, local);
        //查询场景是否存在
        long sceneId = 0, isExist = 0;
        Map<String, Object> findMapBody = new HashMap<>();
        findMapBody.put("hashCode", hashCode);
        JSONArray findData = HttpClientUtil.httpClient("sceneModel/find", 2, findMapBody);
        if (findData.size() > 0) {
            JSONObject jsonObject = findData.getJSONObject(0);
            sceneId = jsonObject.getLongValue("id");
            String isITCov = "否";
            if (jsonObject.containsKey("isITCov") && null != jsonObject.getString("isITCov")) {
                isITCov = jsonObject.getString("isITCov");
            }
            isExist = 1;
            findMapBody.put("parentId", sceneId);
            findMapBody.put("date", DateTimeUtil.getYMDByDate());
            findMapBody.remove("hashCode");
            JSONArray findDataDate = HttpClientUtil.httpClient("sceneModel/find", 2, findMapBody);
            if (findDataDate.size() > 0) {
                JSONObject sceneModelTemp = findDataDate.getJSONObject(0);
                Map<String, Object> setMap = new HashMap<>();
                setMap.put("id", sceneModelTemp.getLong("id"));
                setMap.put("heat", sceneModelTemp.getLong("heat") + 1);
                HttpClientUtil.httpClient("sceneModel/set", 2, setMap);
            } else {
                findMapBody.put("heat", 1);
                findMapBody.put("id", sceneId);
                HttpClientUtil.httpClient("sceneModel/addSceneTraceId", 2, findMapBody);
            }

            if ("是".equals(sceneModel.getIsITCov()) && "否".equals(isITCov)) {
                Map<String, Object> setMap = new HashMap<>();
                setMap.put("id", sceneId);
                setMap.put("isITCov", "是");
                HttpClientUtil.httpClient("sceneModel/set", 2, setMap);
            }
        } else {
            //获取场景信息和网关信息
            if (StringUtils.isBlank(sceneModel.getServiceId())) {
                return;
            }
            if ("是".equals(sceneModel.getIsITCov())) {
                return;
            }
            sceneModel.setHashCode(hashCode);
            //场景信息保存到数据库
            List<Object> objects = new ArrayList<>();
            objects.add(sceneModel);
            JSONArray createResult = HttpClientUtil.httpClient("sceneModel/create", 2, sceneModel);
            sceneId = createResult.getLong(0);
            if (sceneId == -1000009) {
                return;
            }
            sceneModel.setId(sceneId);
            //添加网关节点
            HbaseDataModel gatewayNode = new HbaseDataModel();
            gatewayNode.setSceneId(sceneId);
            gatewayNode.setUrl(sceneModel.getApi());
            gatewayNode.setMethod(sceneModel.getMethod());
            gatewayNode.setServiceName(sceneModel.getSource());
            gatewayNode.setParentLinkId("0");
            gatewayNode.setLinkId(sceneId + "");
            gatewayNode.setType(2);
            HbaseDataModel firstNode = (HbaseDataModel) sceneNodeModels.get(0);
            firstNode.setParentLinkId(gatewayNode.getLinkId());
            sceneNodeModels.add(0, gatewayNode);
            //场景不存在，则场景节点保存到数据库

            HttpClientUtil.httpClient("sceneNodeModel/create", 2, sceneNodeModels);
            methodProcess(sceneNodeModels, sceneModel);

            //设置为新场景
            SceneNew sceneNew = new SceneNew();
            sceneNew.setSceneId(sceneId);
            sceneNew.setDate(DateTimeUtil.getYMDByDate());
            sceneNewMapper.insert(sceneNew);
            autoAnalyseForScene(sceneModel,local); // 新增调用

        }
        if (local == 1) {
            insertSceneBranch(sceneId, branch);
        }

        insertSceneTraceIdModel(traceId, sceneId, isExist);
    }

    /**
     * 构建场景模型对象
     *
     * <p>根据链路追踪ID和服务调用顺序，构建完整的场景模型。该方法负责：</p>
     * <ul>
     *   <li>区分本地服务和远程服务，采用不同的数据获取策略</li>
     *   <li>本地服务：从流量回放平台获取输入参数信息</li>
     *   <li>远程服务：从网关审计日志获取请求信息</li>
     *   <li>格式化URL路径，替换数字参数为占位符</li>
     *   <li>设置场景的基本属性和元数据信息</li>
     *   <li>判断是否为IT自动化覆盖的场景</li>
     *   <li>构建完整的服务调用链路信息</li>
     * </ul>
     *
     * <p>场景模型是Ares大脑系统的核心数据结构，用于描述完整的业务调用链路。</p>
     *
     * @param traceId 链路追踪ID，用于标识唯一的分布式调用链路
     * @param serviceOrder 服务调用顺序列表，记录服务被调用的顺序
     * @param int 本地服务标识，1表示本地服务，0表示远程服务
     * @return SceneModel 返回构建完成的场景模型对象，包含场景的完整信息
     */
    @SneakyThrows
    public SceneModel sceneModel(String traceId, List<String> serviceOrder, int local) {

        SceneModel sceneModel = new SceneModel();
        //把网关信息放到场景模型中
        if (1 == local) {
            //本地服务从流量回放获取信息
            JSONObject jsonObject = HttpClientUtil.httpClientR("online/getInputParameters?appName=" + serviceOrder.get(0) + "&traceId=" + traceId, 1, "", "repeater");
            if (jsonObject != null) {
                sceneModel = JSONObject.parseObject(jsonObject.getString("data"), SceneModel.class);
                sceneModel.setUrl(sceneModel.getApi());
                sceneModel.setSource(sceneModel.getServiceId());
            }
        } else {
            //从网关获取信息
            JSONArray auditLog = HttpClientUtil.httpClient("trace/getAuditLog?traceId=" + traceId, 1, "");
            if (auditLog.size() > 0) {
                //赋值网关数据
                JSONObject data = auditLog.getJSONObject(0);
                sceneModel = data.toJavaObject(SceneModel.class);
                sceneModel.setSource(sceneModel.getSource());
                sceneModel.setMethod(sceneModel.getMethod().toLowerCase());
                sceneModel.setApi(sceneModel.getApi());
            }
        }
        if (StringUtils.isNotBlank(sceneModel.getServiceId())) {
            sceneModel.setServiceId(sceneModel.getServiceId().toLowerCase());
            sceneModel.setApi(formatUrl(sceneModel.getApi(), sceneModel.getServiceId()));
        }
        sceneModel.setSceneId(traceId);
        sceneModel.setServiceOrder(String.join(",", serviceOrder));
        sceneModel.setIsCore(1); //0核心 1非核心
        sceneModel.setIsDelete(0);//0 有用 1无用
        sceneModel.setDepth(0);
        sceneModel.setIsNew(0); //0新增
        sceneModel.setLength(serviceOrder.size());
        sceneModel.setIsITCov("否");
        if (StringUtils.isNotEmpty(sceneModel.getRequestHeader()) && sceneModel.getRequestHeader().contains("python-requests")) {
            sceneModel.setIsITCov("是");
        }
        return sceneModel;
    }

    @SneakyThrows
    private void methodProcess(List<Object> methodDetailModels, SceneModel sceneModel) {
        //忽略上下游
        Map<String, LinkAnalysisModel> methodCollect = new HashMap<>();
        for (Object object : methodDetailModels) {
            HbaseDataModel methodDetailModel = (HbaseDataModel) object;
            if (methodDetailModel.getType() != 0) {
                continue;
            }
            String methodKey = methodDetailModel.getServiceName() + "&" + methodDetailModel.getClassName() + "&" +
                    methodDetailModel.getMethodName() + "&" + methodDetailModel.getParameters();
            if (methodCollect.containsKey(methodKey)) {
                LinkAnalysisModel tempModel = methodCollect.get(methodKey);
                List<Integer> lineNums = tempModel.getLineNums();
                lineNums.addAll(methodDetailModel.getLineNums());
                tempModel.setLineNums(lineNums);
                String hashCode = md5(methodKey + "&" + tempModel.getServiceOrder() + "&" + tempModel.getLineNums());
                tempModel.setHashCode(hashCode);
            } else {
                LinkAnalysisModel linkAnalysisModel = new LinkAnalysisModel();
                ObjectConverterUtil.convert(methodDetailModel, linkAnalysisModel);
                linkAnalysisModel.setServiceOrder(StringUtils.substringBefore(sceneModel.getServiceOrder(), linkAnalysisModel.getServiceName()) +
                        linkAnalysisModel.getServiceName());
                linkAnalysisModel.setServiceName(linkAnalysisModel.getServiceName());
                linkAnalysisModel.setSceneId(sceneModel.getId());
                String hashCode = md5(methodKey + "&" + linkAnalysisModel.getServiceOrder() + "&" + linkAnalysisModel.getLineNums());
                linkAnalysisModel.setHashCode(hashCode);
                methodCollect.put(methodKey, linkAnalysisModel);

                List<SceneServiceClass> serviceClasses = serviceClassMapper.findByServiceNameAndClassName(linkAnalysisModel.getServiceName(), linkAnalysisModel.getClassName());
                if (null == serviceClasses || serviceClasses.size() == 0) {
                    SceneServiceClass sceneServiceClass = new SceneServiceClass();
                    sceneServiceClass.setServiceName(linkAnalysisModel.getServiceName());
                    sceneServiceClass.setClassName(linkAnalysisModel.getClassName());
                    serviceClassMapper.insert(sceneServiceClass);
                }
            }
        }
        HttpClientUtil.httpClient("linkAnalysisModel/create", 2,
                methodCollect.values().stream().collect(Collectors.toList()));
    }


//    /**
//     * 应用热度
//     *
//     * @param serviceHeatModelMap
//     */
//    @SneakyThrows
//    public void setRedisDate(Map<String, Integer> serviceHeatModelMap, Map<String, Integer> apiHeatModels,
//                             Map<String, Integer> methodHeatModels, long sceneId) {
//
//        String date = DateTimeUtil.getYMDByDate();
////        List<Object> serviceDateKeys = getDateKeyValues("service" + date);
////        for (String serviceKey : serviceHeatModelMap.keySet()) {
////            String serviceDateKey = serviceKey + "&" + date;
////            setRedisDate(serviceDateKey, serviceHeatModelMap.get(serviceKey));
////            if (!serviceDateKeys.contains(serviceDateKey)) {
////                serviceDateKeys.add(serviceDateKey);
////            }
////        }
////        setDateKey("service" + date, serviceDateKeys);
////
////        List<Object> apiDateKeys = getDateKeyValues("api" + date);
////        for (String apiKey : apiHeatModels.keySet()) {
////            String apiDateKey = apiKey + "&" + date;
////            setRedisDate(apiDateKey, apiHeatModels.get(apiKey));
////            if (!apiDateKeys.contains(apiDateKey)) {
////                apiDateKeys.add(apiDateKey);
////            }
////        }
////        setDateKey("api" + date, apiDateKeys);
////
////        List<Object> methodDateKeys = getDateKeyValues("method" + date);
////        for (String methodKey : methodHeatModels.keySet()) {
////            setRedisDate(methodKey + "&" + date, methodHeatModels.get(methodKey));
////            String temp = StringUtils.substringBeforeLast(methodKey, "&");
////            if (!methodDateKeys.contains(temp)) {
////                methodDateKeys.add(StringUtils.substringBeforeLast(methodKey, "&"));
////            }
////        }
////        setDateKey("method" + date, methodDateKeys);
//
//        List<Object> sceneIdKey = getDateKeyValues("sceneId" + date);
//        sceneIdKey.add(sceneId);
//        setDateKey("sceneId" + date, sceneIdKey);
//    }
//
//    /**
//     * 应用热度
//     */
//    @SneakyThrows
//    public void updateSceneNew(String date) {
//
//        Object result = cacheUtil.get("sceneId" + date, String.class);
//        if (result != null) {
//            List<Object> sceneIds = ObjectConverterUtil.jsonToList(result);
//            HttpClientUtil.httpClient("sceneModel/updateSceneByIds", 2, sceneIds);
//            cacheUtil.delete("sceneId" + date);
//        }
//    }
//
//    /**
//     * 应用热度
//     */
//    @SneakyThrows
//    public void serviceHeat(String date) {
//
//        Object result = cacheUtil.get("service" + date, String.class);
//        if (result != null) {
//            List<Object> serviceHeatResult = ObjectConverterUtil.jsonToList(result);
//            List<JSONObject> serviceHeats = new ArrayList<>();
//            for (Object serviceHeat : serviceHeatResult) {
//                String[] serviceHeatMessage = StringUtils.split(serviceHeat.toString(), "&");
//                Integer heat = cacheUtil.get(serviceHeat.toString(), Integer.class);
//                JSONObject jsonObject = new JSONObject();
//                jsonObject.put("heat", heat == null ? 0 : heat);
//                jsonObject.put("serviceName", serviceHeatMessage[0]);
//                jsonObject.put("date", date);
//                serviceHeats.add(jsonObject);
//                cacheUtil.delete(serviceHeat.toString());
//            }
//            HttpClientUtil.httpClient("serviceHeatModel/create", 2, serviceHeats);
//        }
//        cacheUtil.delete("service" + date);
//    }
//
//    /**
//     * api热度
//     */
//    @SneakyThrows
//    public void apiHeat(String date) {
//
//        Object result = cacheUtil.get("api" + date, String.class);
//        if (result != null) {
//            List<Object> apiHeatResult = ObjectConverterUtil.jsonToList(result);
//            List<JSONObject> apiHeatModels = new ArrayList<>();
//            for (Object apiHeat : apiHeatResult) {
//                String[] apiHeatMessage = StringUtils.split(apiHeat.toString(), "&");
//                Integer heat = cacheUtil.get(apiHeat.toString(), Integer.class);
//                JSONObject jsonObject = new JSONObject();
//                jsonObject.put("heat", heat == null ? 0 : heat);
//                jsonObject.put("serviceName", apiHeatMessage[0]);
//                jsonObject.put("apiName", apiHeatMessage[1]);
//                jsonObject.put("method", apiHeatMessage[2]);
//                jsonObject.put("date", date);
//                apiHeatModels.add(jsonObject);
//                cacheUtil.delete(apiHeat.toString());
//            }
//            HttpClientUtil.httpClient("apiHeatModel/create", 2, apiHeatModels);
//        }
//        cacheUtil.delete("api" + date);
//    }
//
//    /**
//     * 方法热度
//     **/
//    @SneakyThrows
//    public void methodHeat(String date) {
//        Object result = cacheUtil.get("method" + date, String.class);
//        if (result != null) {
//            List<Object> methodHeatResult = ObjectConverterUtil.jsonToList(result);
//            List<JSONObject> methodHeats = new ArrayList<>();
//            for (Object methodHeat : methodHeatResult) {
//                String[] methodHeatMessage = StringUtils.split(methodHeat.toString(), "&");
//                Integer outerHeat = cacheUtil.get(methodHeat.toString() + "&outerHeat&" + date, Integer.class);
//                Integer innerHeat = cacheUtil.get(methodHeat.toString() + "&innerHeat&" + date, Integer.class);
//                JSONObject jsonObject = new JSONObject();
//                jsonObject.put("outerHeat", outerHeat == null ? 0 : outerHeat);
//                jsonObject.put("innerHeat", innerHeat == null ? 0 : innerHeat);
//                jsonObject.put("serviceName", methodHeatMessage[0]);
//                jsonObject.put("className", methodHeatMessage[1]);
//                jsonObject.put("methodName", methodHeatMessage[2]);
//                jsonObject.put("parameters", methodHeatMessage[3]);
//                jsonObject.put("date", date);
//                methodHeats.add(jsonObject);
//                cacheUtil.delete(methodHeat.toString() + "&outerHeat&" + date);
//                cacheUtil.delete(methodHeat.toString() + "&innerHeat&" + date);
//            }
//            HttpClientUtil.httpClient("methodHeatModel/create", 2, methodHeats);
//        }
//        cacheUtil.delete("method" + date);
//    }

    /**
     * 插入场景追踪关联关系
     *
     * <p>建立链路追踪ID与场景ID之间的关联关系，记录场景的创建状态。该方法负责：</p>
     * <ul>
     *   <li>保存traceId与sceneId的映射关系</li>
     *   <li>记录场景是否已存在（0存在，1新增）</li>
     *   <li>支持后续通过traceId快速查询场景信息</li>
     *   <li>维护数据关联关系的完整性</li>
     * </ul>
     *
     * <p>关联关系建立后，可以通过链路追踪ID快速定位和分析对应的业务场景。</p>
     *
     * @param traceId 链路追踪ID，用于标识唯一的分布式调用链路
     * @param sceneId 场景ID，标识唯一的业务场景
     * @param isExist 场景存在标识，0表示场景已存在，1表示场景为新增
     */
    @SneakyThrows
    private void insertSceneTraceIdModel(String traceId, long sceneId, long isExist) {

        Map<String, Object> map = new HashMap<>();
        map.put("sceneId", sceneId);
        map.put("traceId", traceId);
        map.put("isExist", isExist);
        HttpClientUtil.httpClient("sceneTraceIdModel/insertSceneTraceIdModel", 2, map);
    }

    /**
     * 插入场景分支信息
     *
     * <p>为场景添加代码分支信息，记录场景创建时的代码分支状态。该方法负责：</p>
     * <ul>
     *   <li>保存场景ID与代码分支的关联关系</li>
     *   <li>记录场景创建时所在的代码分支</li>
     *   <li>支持后续的版本对比和差异分析</li>
     *   <li>为代码变更追踪提供基础数据</li>
     * </ul>
     *
     * <p>分支信息对于理解场景的代码上下文和进行差异分析非常重要。</p>
     *
     * @param sceneId 场景ID，标识唯一的业务场景
     * @param branch 代码分支名称，标识场景创建时的代码分支
     */
    @SneakyThrows
    private void insertSceneBranch(long sceneId, String branch) {
        Map<String, Object> map = new HashMap<>();
        map.put("sceneId", sceneId);
        map.put("branch", branch);
        HttpClientUtil.httpClient("sceneBranch/insertSceneBranch", 2, map);
    }

    private static String formatUrl(String url, String serviceName) {

        List<String> searchList = new ArrayList<>();
        List<String> replaceList = new ArrayList<>();
        String[] urls = StringUtils.split(url, "?");
        String[] urlArr = urls[0].split("/");
        for (int i = 0; i < urlArr.length; i++) {
            //url中间节点大于两个数字则替换
            if (isNumeric(urlArr[i]) > 2) {
                searchList.add(urlArr[i]);
                replaceList.add("{" + serviceName + "}");
            }
        }
        if (urls.length > 1) {
            String[] afterUrls = urls[1].split("&");
            for (int i = 0; i < afterUrls.length; i++) {
                //分页替换，大于1个数字就替换
                String[] nodeUrl = afterUrls[i].split("=");
                if (nodeUrl[0].toLowerCase().contains("page")) {
                    searchList.add(afterUrls[i]);
                    replaceList.add(nodeUrl[0] + "={" + serviceName + "}");
                } else if (nodeUrl.length > 1 && isNumeric(nodeUrl[1]) > 1) {
                    searchList.add(nodeUrl[1]);
                    replaceList.add("{" + serviceName + "}");
                }
            }
        }
        return StringUtils.replaceEach(url, searchList.toArray(new String[searchList.size()]), replaceList.toArray(new String[searchList.size()]));
    }

    /**
     * 统计字符串中数字字符的个数
     *
     * <p>计算给定字符串中包含数字字符的数量，用于URL参数识别和处理。该方法负责：</p>
     * <ul>
     *   <li>遍历字符串的每个字符</li>
     *   <li>统计ASCII码在48-57范围内的字符数量</li>
     *   <li>48-57对应数字字符'0'-'9'</li>
     *   <li>用于判断URL参数是否为纯数字</li>
     * </ul>
     *
     * <p>该方法主要用于URL格式化处理，识别需要替换为占位符的数字参数。</p>
     *
     * @param str 输入字符串，通常为URL路径或参数部分
     * @return int 返回字符串中数字字符的总数
     */
    private static int isNumeric(String str) {
        //将字符串转换为字符数组
        byte[] array1 = str.getBytes();
        int count = 0;
        for (int i = 0; i < array1.length; i++) {
            //数字的ASCII码为48--57
            if (array1[i] >= 48 && array1[i] <= 57) {
                count++;
            }
        }
        return count;
    }

//    private void setRedisDate(String key, int keyValue) {
//        Integer heat = cacheUtil.get(key, Integer.class);
//        if (null == heat) {
//            cacheUtil.setHeat(key, keyValue);
//        } else {
//            cacheUtil.setHeat(key, heat + keyValue);
//        }
//    }
//
//    private List<Object> getDateKeyValues(String dateKey) {
//        Object result = cacheUtil.get(dateKey, String.class);
//        List<Object> dateKeyValues = new ArrayList<>();
//        if (result != null) {
//            dateKeyValues = ObjectConverterUtil.jsonToList(result);
//        }
//        return dateKeyValues;
//    }
//
//    private void setDateKey(String dateKey, List<Object> dateKeyValues) {
//        cacheUtil.set(dateKey, ObjectConverterUtil.toJson(dateKeyValues));
//    }

//    @SneakyThrows
//    private boolean checkTrace(String traceId) {
//
//        List<SceneTraceid> sceneTraceids = sceneTraceIdMapper.getSceneTraceIds(traceId);
//        if (sceneTraceids == null || sceneTraceids.size() == 0) {
//            return true;
//        }
//        for (SceneTraceid sceneTraceid : sceneTraceids) {
//            if (sceneTraceid.getIsexist() == 0) {
//                JSONArray checkResult = HttpClientUtil.httpClient("sceneTraceIdModel/checkTraceId?sceneId=" + sceneTraceid.getSceneid(), 1, "");
//                if (!checkResult.getBoolean(0)) {
//                    return false;
//                }
//            }
//            sceneTraceIdMapper.deleteById(sceneTraceid.getId());
//        }
//        return true;
//    }


//    private boolean isTargetApp(String serviceName) {
//        List<String> targetApps = Arrays.asList("contract-manager", "sparrow");
//        return targetApps.contains(serviceName);
//    }

    /**
     * 为场景自动触发差异分析
     *
     * <p>当检测到目标API的新场景时，自动触发差异分析流程。该方法负责：</p>
     * <ul>
     *   <li>检查场景API是否为目标API列表中的接口</li>
     *   <li>解析请求头获取环境信息和API分组</li>
     *   <li>根据环境确定环境代码（testvpc/smlvpc）</li>
     *   <li>查询场景中涉及服务的最近差异记录</li>
     *   <li>按分支分组选择最新的差异记录</li>
     *   <li>异步执行差异分析任务</li>
     *   <li>避免重复分析相同分支的差异</li>
     * </ul>
     *
     * <p>自动分析功能可以实现场景创建时的即时差异比对，提供更及时的代码变更洞察。</p>
     *
     * @param sceneModel 场景模型对象，包含场景的API、服务和请求信息
     * @param local 本地服务标识，1表示本地服务，0表示远程服务
     */
    private void autoAnalyseForScene(SceneModel sceneModel,int local) {
        try {
            List<String> apilist = Arrays.asList(StringUtils.split(TARGET_API, ","));

            // 1. 检查是否目标API
            if (!apilist.contains(sceneModel.getApi())) {
                return;
            }
            // 2. 获取最近比对记录
            Map<String, Object> params = new HashMap<>();
            params.put("serviceName", sceneModel.getServiceId());
            JSONObject requestBody = JSONObject.parseObject(sceneModel.getRequestHeader());
              String activeProfile = env.getActiveProfiles()[0]; // 获取激活的profile
            String envcode;
            if ("test".equals(activeProfile.toLowerCase())) {
                envcode = "testvpc";
            }else{
                envcode = "smlvpc";

            }

            //默认设置成测试环境
            String envCode = requestBody.keySet().stream().filter("X-Service-Group"::equalsIgnoreCase).findFirst().map(requestBody::getString).orElse(envcode);
            List<String> services = Arrays.asList(StringUtils.split(sceneModel.getServiceOrder(), ","));
            log.info("services==========" + services);
            
            // 使用传统的for循环替代forEach，避免Lambda表达式可能的问题
            for (int serviceIndex = 0; serviceIndex < services.size(); serviceIndex++) {
                String service = services.get(serviceIndex);
                log.info("开始处理服务: {}, 服务索引: {}, 当前线程: {}", service, serviceIndex, Thread.currentThread().getName());
                log.info("查询参数: service={}, envCode={}", service, envCode); // 添加查询参数日志
                List<DiffRecordEntity> records = diffRecordMapper.selectLatestByServiceNameAndEnv(service, envCode, 5);
                log.info("查询完成，服务: {}, 记录数: {}", service, (records != null ? records.size() : 0));
                
                // 添加查询结果的详细日志
                if (records != null && !records.isEmpty()) {
                    for (int i = 0; i < records.size(); i++) {
                        DiffRecordEntity record = records.get(i);
                        log.info("服务 {} 的第 {} 条记录: id={}, serviceName={}, envCode={}, nowBranch={}", 
                                service, i+1, record.getId(), record.getServiceName(), record.getEnvCode(), record.getNowBranch());
                    }
                } else {
                    log.info("服务 {} 没有找到记录", service);
                    continue; // 使用continue替代return
                }
                    
                log.info("原始记录数 for service {}: {}", service, records.size());
                
                // 检查是否所有记录的分支标识都相同
                Set<String> uniqueBranchNames = new HashSet<>();
                for (DiffRecordEntity record : records) {
                    uniqueBranchNames.add(record.getNowBranch());
                }
                boolean allBranchesSame = uniqueBranchNames.size() <= 1;
                
                // 如果所有分支标识都相同，只执行一次处理逻辑
                Collection<DiffRecordEntity> recordsToProcess;
                if (allBranchesSame) {
                    // 所有分支标识都相同，选择最新的一个记录处理
                    DiffRecordEntity latestRecord = null;
                    for (DiffRecordEntity record : records) {
                        if (latestRecord == null || record.getCreatedTime().after(latestRecord.getCreatedTime())) {
                            latestRecord = record;
                        }
                    }
                    recordsToProcess = latestRecord != null ? Collections.singletonList(latestRecord) : Collections.emptyList();
                    log.info("所有分支标识相同，只处理最新记录");
                } else {
                    // 分支标识不同，为每个不同分支选择最新的记录处理
                    Map<String, DiffRecordEntity> latestRecordsByBranch = new ConcurrentHashMap<>();
                    for (DiffRecordEntity record : records) {
                        String branchKey = record.getNowBranch();
                        if (latestRecordsByBranch.containsKey(branchKey)) {
                            // 如果已存在该分支标识的记录，比较创建时间，保留最新的
                            if (record.getCreatedTime().after(latestRecordsByBranch.get(branchKey).getCreatedTime())) {
                                latestRecordsByBranch.put(branchKey, record);
                            }
                        } else {
                            // 如果不存在该分支标识的记录，直接放入
                            latestRecordsByBranch.put(branchKey, record);
                        }
                    }
                    recordsToProcess = latestRecordsByBranch.values();
                    log.info("分支标识不同，处理 {} 个不同分支的最新记录", latestRecordsByBranch.size());
                }
                
                log.info("实际处理记录数: {}", recordsToProcess.size());

                // 处理筛选后的记录
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                for (DiffRecordEntity currentRecord : recordsToProcess) {
                    futures.add(CompletableFuture.runAsync(() -> {
                        log.info("实际执行分析 - record: {}, getNowVersion: {}, getNowBranch: {}, traceId: {}",
                                currentRecord.getId(), currentRecord.getNowVersion(), currentRecord.getNowBranch(), sceneModel.getSceneId());
                        currentRecord.setCreatedTime(new Date());
                        diffAnalyseService.newDiffAnalyseData(currentRecord, true, sceneModel.getSceneId());
                    }, newAsyncServiceExecutor));
                }

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            }


            log.info("自动触发应用{}的重新比对", sceneModel.getServiceId());
        } catch (Exception e) {
            log.error("自动分析场景异常", e);
        }
    }

    /**
     * 获取场景关联的服务列表
     *
     * <p>根据场景模型，查询该场景涉及的所有服务名称。该方法负责：</p>
     * <ul>
     *   <li>根据场景ID查询关联的场景节点数据</li>
     *   <li>提取所有节点的服务名称信息</li>
     *   <li>去重并返回服务名称集合</li>
     *   <li>用于了解场景涉及的服务范围</li>
     * </ul>
     *
     * <p>服务列表用于差异分析、风险评估和影响范围分析。</p>
     *
     * @param sceneModel 场景模型对象，包含场景的ID和基本信息
     * @return Set&lt;String&gt; 返回场景涉及的服务名称集合
     * @throws Exception 当查询服务节点数据时发生异常
     */
    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5 hash failed", e);
        }
    }

    public Set<String> getServices(SceneModel sceneModel) throws Exception {
        Map<String, Object> findMapBody = new HashMap<>();
        findMapBody.put("id", sceneModel.getId());
        JSONArray sceneArray = HttpClientUtil.httpClient("sceneNodeModel/find", 2, findMapBody);
        Set<String> serviceSet = new HashSet<>();
        for (int i = 0; i < sceneArray.size(); i++) {
            String serviceName = sceneArray.getJSONObject(i).getString("serviceName");
            serviceSet.add(serviceName);
        }
        return serviceSet;

    }

}

