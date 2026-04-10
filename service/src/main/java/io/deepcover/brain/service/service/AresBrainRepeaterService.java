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
import io.deepcover.brain.dal.entity.AresAgentEntity;
import io.deepcover.brain.dal.mapper.SceneTraceIdMapper;
import io.deepcover.brain.model.*;
import io.deepcover.brain.service.util.ObjectConverterUtil;
import io.deepcover.brain.service.util.client.HttpClientUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ares大脑重放服务
 *
 * <p>该服务负责管理流量重放相关的功能，为分布式系统提供流量回放和测试能力。主要功能包括：</p>
 * <ul>
 *   <li>支持场景和链路的批量流量重放</li>
 *   <li>管理重放任务的生命周期，包括创建、执行和结果查询</li>
 *   <li>提供重放结果的详细查询和统计信息</li>
 *   <li>支持动态调整流量采集的采样率配置</li>
 *   <li>维护场景ID与套件ID的关联关系</li>
 *   <li>集成自动化测试用例生成功能</li>
 *   <li>提供异步任务执行支持，提高系统性能</li>
 * </ul>
 *
 * <p>该服务通过与流量回放平台的集成，为系统提供了强大的流量回放测试能力，
 * 支持基于真实生产流量的回归测试、性能测试和故障模拟，是Ares大脑系统质量保障的重要组成部分。</p>
 *
 * @author system
 * @version 1.0
 * @since 2023-11-21
 */
@Slf4j
@Service
public class AresBrainRepeaterService {

    @Autowired
    SceneTraceIdMapper sceneTraceIdMapper;

    @Autowired
    AresBrainSceneService aresBrainSceneService;
    @Autowired
    AresBrainLinkService aresBrainLinkService;

    private static String ereplayFrontUrl;

    @Value("${ereplay.front.url}")
    public void setEreplayFrontUrl(String ereplayFrontUrl) {
        AresBrainRepeaterService.ereplayFrontUrl = ereplayFrontUrl;
    }

  /**
   * 异步批量启动流量重放
   *
   * <p>根据重放模型配置，异步启动批量流量重放任务。该方法支持两种类型的重放：</p>
   * <ul>
   *   <li>场景重放：基于场景ID进行流量重放</li>
   *   <li>链路重放：基于链路ID进行流量重放</li>
   * </ul>
   *
   * <p>批量重放流程包括：根据重放类型查询对应的场景/链路列表，然后启动重放任务。
   * 使用@Async注解确保异步执行，不阻塞主线程。</p>
   *
   * @param repeaterModel 重放模型对象，包含重放类型、服务名、环境等配置信息
   */
    @SneakyThrows
    @Async
    public void startReplayBatch(RepeaterModel repeaterModel) {
        QuerySceneBO querySceneBO = repeaterModel;
        List<TraceIdListVo> traceIdListVo = new ArrayList<>();
        if ("scene".equals(repeaterModel.getSuiteType())) {
            traceIdListVo = aresBrainSceneService.querySceneBatch(querySceneBO);
        } else if ("link".equals(repeaterModel.getSuiteType())) {
            traceIdListVo = aresBrainLinkService.queryLinkDetailBatch(querySceneBO);
        }
        repeaterModel.setTraceIdListVo(traceIdListVo);
        startReplay(repeaterModel);
    }

    /**
   * 启动流量重放任务
   *
   * <p>根据重放模型配置，启动单次流量重放任务。该方法负责：</p>
   * <ul>
   *   <li>构建重放套件和追踪数据</li>
   *   <li>获取场景关联的追踪ID列表</li>
   *   <li>调用流量回放平台创建重放任务</li>
   *   <li>维护场景ID与套件ID的关联关系</li>
   *   <li>返回重放任务的执行结果</li>
   * </ul>
   *
   * <p>重放任务创建成功后会保存场景与套件的关联关系，支持后续查询重放结果。</p>
   *
   * @param repeaterModel 重放模型对象，包含服务名、环境、套件名、追踪ID列表等信息
   * @return AresBrainResult 返回重放任务的执行结果，包含套件ID、状态码和相关信息
   */
    @SneakyThrows
    public AresBrainResult startReplay(RepeaterModel repeaterModel) {
        AresBrainResult result = new AresBrainResult();
        SuiteTraceVo suiteTraceVo = new SuiteTraceVo();
        suiteTraceVo.setSuiteType(repeaterModel.getSuiteType());
        suiteTraceVo.setIfNew(repeaterModel.getIfNew());

        SuiteVo suiteVo = new SuiteVo();
        suiteVo.setAppName(repeaterModel.getServiceName());
        suiteVo.setSuiteName(repeaterModel.getSuiteName());
        suiteVo.setEnv(repeaterModel.getEnv());
        suiteVo.setOperator(repeaterModel.getOperator());
        suiteTraceVo.setSuiteVo(suiteVo);

        TraceVo traceVo = new TraceVo();
        traceVo.setAppName(repeaterModel.getServiceName());
        traceVo.setEnvironment(repeaterModel.getEnv());
        suiteTraceVo.setTraceVo(traceVo);

        List<TraceIdListVo> sceneIdList = repeaterModel.getTraceIdListVo();
//        suiteTraceVo.setTraceIdListVo(sceneIdList);
        int sceneIdList_size = sceneIdList.size();

        List<Long> sceneidList = new ArrayList<>();
        for (int m = 0; m < sceneIdList_size; m++) {
            Long sceneId = sceneIdList.get(m).getSceneId();
            sceneidList.add(sceneId);
            JSONArray TraceIds = HttpClientUtil.httpClient("/sceneTraceIdModel/getTraceId?sceneId=" + sceneId, 1, new HashMap<>());
//            List<String> TraceIds = sceneTraceIdMapper.getTraceIds(sceneId.toString());
            sceneIdList.get(m).setTraceArray(TraceIds.toList(String.class));
        }
        suiteTraceVo.setTraceIdListVo(sceneIdList);
        String msgmodel = "startReplay方法，suiteTraceVo:" + suiteTraceVo;
        log.info(msgmodel);

        Map<String, Object> body = ObjectConverterUtil.toMap(suiteTraceVo);
        JSONObject suiteResult = HttpClientUtil.httpClientR("/suite/api/addSuiteAndaddTracelist", 2, body, "repeater");
        if (suiteResult.getBoolean("success")) {
            Long suiteId = suiteResult.getLong("data");
            SceneIdSuiteIdModel sceneIdSuiteIdModel = new SceneIdSuiteIdModel();
            sceneIdSuiteIdModel.setSuiteId(suiteId);
            sceneIdSuiteIdModel.setSceneId(sceneidList);
            sceneIdSuiteIdModel.setSuiteName(repeaterModel.getSuiteName());
            Boolean insertSceneSuiteId = insertSceneSuiteId(sceneIdSuiteIdModel);
            if (insertSceneSuiteId) {
                result.setMessage(suiteResult.getString("message"));
                result.setData(suiteResult);
                result.setCode(0);
            } else {
                String msg = suiteResult.getString("message") + ",场景id和集合id关联失败，只能在流量回放平台查看回放详情";
                result.setMessage(msg);
                result.setData(suiteResult);
                result.setCode(500);
            }
        } else {
            result.setMessage(suiteResult.getString("message"));
            result.setData(suiteResult);
            result.setCode(500);
        }
        return result;
    }

    /**
   * 查询场景重放结果
   *
   * <p>根据场景ID查询相关的重放任务结果。该方法执行以下操作：</p>
   * <ul>
   *   <li>根据场景ID查询关联的套件ID列表</li>
   *   <li>获取每个套件的最新批量重放记录</li>
   *   <li>构建重放结果的前端访问URL</li>
   *   <li>返回包含套件信息和访问链接的结果列表</li>
   * </ul>
   *
   * <p>查询结果包含套件名称、批量重放ID和前端访问链接，便于用户查看详细的重放结果。</p>
   *
   * @param sceneId 场景ID，用于查询该场景相关的重放任务
   * @return List&lt;SuiteIdRepeaterModel&gt; 返回重放结果列表，包含套件ID、套件名称和重放访问链接
   */
    @SneakyThrows
    public List<SuiteIdRepeaterModel> ReplayResult(long sceneId) {
        //场景Id查询suitId
        //suitId查询batch_repeat_id
        Map<String, Object> body = new HashMap<>();
        body.put("sceneId", sceneId);
        JSONArray suiteResult = HttpClientUtil.httpClient("/suiteIdModel/searchSuiteId", 2, body);
        int suite_size = suiteResult.size();
        List<SuiteIdRepeaterModel> list = new ArrayList<>();
        for (int m = 0; m < suite_size; m++) {
            JSONObject suiteId = (JSONObject) suiteResult.get(m);
            SuiteIdRepeaterModel suiteIdRepeaterModel = new SuiteIdRepeaterModel();
            suiteIdRepeaterModel.setSuiteId(suiteId.getLong("suiteId"));
            suiteIdRepeaterModel.setSceneId(sceneId);
            Map<String, Object> BatchRepeatBody = new HashMap<>();
            BatchRepeatBody.put("currentPage", 1);
            BatchRepeatBody.put("pageSize", 1);
            BatchRepeatBody.put("suiteId", suiteId.getLong("suiteId"));
            JSONObject BatchRepeat = HttpClientUtil.httpClientR("/result/api/getBatchRepeatId", 2, BatchRepeatBody, "repeater");
            suiteIdRepeaterModel.setSuiteName(suiteId.getString("suiteName"));
            if (BatchRepeat.getBoolean("success")) {
                JSONObject data = BatchRepeat.getJSONArray("data").getJSONObject(0);
                suiteIdRepeaterModel.setBatchRepeatId(data.getString("batch_repeat_id"));
                suiteIdRepeaterModel.setSuiteUrl(ereplayFrontUrl + "#/replayresult?batchRepeatId=" + data.getString("batch_repeat_id"));
            } else {
                suiteIdRepeaterModel.setSuiteUrl(ereplayFrontUrl + "#/TrafficCollection");
            }
            list.add(suiteIdRepeaterModel);
        }
        return list;

    }

    /**
   * 异步更新流量采集采样率
   *
   * <p>根据Ares Agent实体的配置，异步更新流量回放平台的流量采集采样率。该方法负责：</p>
   * <ul>
   *   <li>构建采样率配置请求参数</li>
   *   <li>调用流量回放平台的配置更新接口</li>
   *   <li>支持指定环境的采样率调整</li>
   *   <li>使用专用线程池执行异步任务</li>
   * </ul>
   *
   * <p>采样率配置调整后会影响该服务的流量采集频率，用于控制回放数据的采集量。</p>
   *
   * @param aresAgentEntity Ares Agent实体，包含服务名和采样率配置信息
   */
    @Async("httpExecutor")
    public void updateReplayRate(AresAgentEntity aresAgentEntity) {
        Map<String, Object> body = new HashMap<>();
        body.put("appName", aresAgentEntity.getServiceName());
        body.put("environment", "test");
        JSONObject object = new JSONObject();
        object.put("sampleRate", aresAgentEntity.getSampleRate());
        body.put("config", object.toString());
        try {
            JSONObject updateRate = HttpClientUtil.httpClientR("facade/api/config/updateRate", 2, body, "repeater");
            log.info("updateReplayRate success.", updateRate);
        } catch (Exception e) {
            log.error("updateReplayRate failed.", e);
        }
    }

    /**
   * 异步插入场景与套件关联关系
   *
   * <p>批量插入场景ID与套件ID的关联关系，用于维护重放任务的映射关系。该方法负责：</p>
   * <ul>
   *   <li>遍历场景ID列表，为每个场景创建关联记录</li>
   *   <li>调用远程接口保存场景-套件关联数据</li>
   *   <li>记录套件名称便于后续查询和展示</li>
   *   <li>使用专用线程池执行异步任务</li>
   *   <li>返回批量操作的整体执行结果</li>
   * </ul>
   *
   * <p>关联关系建立后，可以通过场景ID快速查询对应的重放套件信息。</p>
   *
   * @param sceneIdSuiteIdModel 场景套件ID关联模型，包含场景ID列表、套件ID和套件名称
   * @return Boolean 返回批量插入的执行结果，true表示所有场景都关联成功，false表示存在失败的关联
   */
    @Async("httpExecutor")
    public Boolean insertSceneSuiteId(SceneIdSuiteIdModel sceneIdSuiteIdModel) {
        List<Long> sceneidList = sceneIdSuiteIdModel.getSceneId();
        int size = sceneidList.size();
        Boolean SceneSuiteId = true;
        for (int m = 0; m < size; m++) {
            Long sceneId = sceneidList.get(m);
            Map<String, Object> body = new HashMap<>();
            body.put("sceneId", sceneId);
            body.put("suiteId", sceneIdSuiteIdModel.getSuiteId());
            body.put("suiteName", sceneIdSuiteIdModel.getSuiteName());
            try {
                JSONArray insertSceneSuiteId = HttpClientUtil.httpClient("/suiteIdModel/insertSceneSuiteId", 2, body);
                log.info("insertSceneSuiteId success.", insertSceneSuiteId);
            } catch (Exception e) {
                SceneSuiteId = false;
                log.error("insertSceneSuiteId failed.", e);
            }

        }
        return SceneSuiteId;

    }

    /**
   * 创建自动化测试用例
   *
   * <p>根据API请求信息，调用自动化测试平台生成测试用例脚本。该方法负责：</p>
   * <ul>
   *   <li>构建测试用例生成请求参数</li>
   *   <li>设置基础URL、服务名和账户信息</li>
   *   <li>传递API选择列表信息</li>
   *   <li>调用自动化测试平台的生成接口</li>
   *   <li>处理生成结果并返回统一响应</li>
   * </ul>
   *
   * <p>成功生成的测试脚本可以用于自动化回归测试，提高测试效率和质量。</p>
   *
   * @param apiRequestVo API请求对象，包含服务名、账户信息和API选择列表
   * @return AresBrainResult 返回测试用例生成结果，包含状态码、消息和生成的测试脚本数据
   */
    @SneakyThrows
    public AresBrainResult aresCreateTestCase(ApiRequestAresVo apiRequestVo) {
        AresBrainResult result = new AresBrainResult();
        Map<String, Object> Body = new HashMap<>();
        Body.put("baseUrl", "${ENV(baseUrl)}");
        Body.put("serviceName", apiRequestVo.getServiceName());
        Body.put("account", apiRequestVo.getAccount());
        Body.put("selectList", apiRequestVo.getSelectList());
        JSONObject re = HttpClientUtil.httpClientR("wanderearth/aresCreateTestCase", 2, Body, "earth");
        int code = re.getIntValue("code");
        if (code == 10000000) {
            result.setCode(0);
            result.setMessage("脚本生成成功");
            JSONArray data = re.getJSONArray("data");
            result.setData(data);
        } else {
            result.setCode(400);
            result.setMessage("脚本生成失败，请联系效能组");
            JSONArray data = re.getJSONArray("data");
            result.setData(data);
        }
        return result;

    }
}