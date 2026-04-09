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
import io.deepcover.brain.model.*;
import io.deepcover.brain.service.util.client.HttpClientUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ares大脑链接服务
 *
 * <p>该服务负责处理分布式系统中的链路追踪数据查询和分析。主要功能包括：</p>
 * <ul>
 *   <li>提供链路信息的详细查询功能</li>
 *   <li>支持按服务名、类名、方法名、参数等多维度条件查询</li>
 *   <li>实现链路数据的分页展示</li>
 *   <li>提供API接口的热度统计和汇总信息</li>
 *   <li>支持批量查询链路详情数据</li>
 *   <li>计算和展示链路的深度、长度等关键指标</li>
 * </ul>
 *
 * <p>该服务是Ares大脑系统的重要组成部分，专注于链路追踪数据的查询和分析，
 * 为开发者提供深入的分布式系统调用链路洞察，帮助理解系统间的调用关系和数据流向。</p>
 *
 * @author system
 * @version 1.0
 * @since 2023-11-21
 */
@Slf4j
@Service
public class AresBrainLinkService {

    /**
     * 查询链路详细信息
     *
     * <p>根据提供的查询条件，查询链路分析的详细信息。支持多种查询条件组合，包括：</p>
     * <ul>
     *   <li>服务名：指定要查询的微服务名称</li>
     *   <li>类名：指定要查询的Java类名</li>
     *   <li>方法名：指定要查询的方法名称</li>
     *   <li>参数：指定方法的参数信息</li>
     *   <li>行号：指定代码行号范围</li>
     *   <li>IT覆盖标识：查询是否为IT自动化覆盖的场景</li>
     *   <li>API接口：指定要查询的API路径</li>
     * </ul>
     *
     * <p>查询结果包含分页信息、API汇总统计和详细的场景数据，每个场景会计算热度占比。</p>
     *
     * @param querySceneBO 查询条件对象，包含各种过滤条件和分页参数
     * @return AresBrainResult 返回包含链路查询结果的统一响应对象，包含分页信息、API列表和场景数据
     */
    @SneakyThrows
    public AresBrainResult queryLinkDetail(QuerySceneBO querySceneBO) {
        AresBrainResult aresBrainResult = new AresBrainResult();
        Map<String, String> request = new HashMap<>();
        if (StringUtils.isNotBlank(querySceneBO.getServiceName())) {
            request.put("serviceName", querySceneBO.getServiceName());
        }
        if (StringUtils.isNotBlank(querySceneBO.getClassName())) {
            request.put("className", querySceneBO.getClassName());
        }
        if (StringUtils.isNotBlank(querySceneBO.getMethodName())) {
            request.put("methodName", querySceneBO.getMethodName());
        }
        if (StringUtils.isNotBlank(querySceneBO.getParameters())) {

            request.put("parameters", StringUtils.replace(querySceneBO.getParameters(), "\"", "\\\""));
        }
        if (StringUtils.isNotBlank(querySceneBO.getLineNums())) {
            request.put("lineNums", querySceneBO.getLineNums());
        }
        if (StringUtils.isNotBlank(querySceneBO.getIsITCov())) {
            request.put("isITCov", querySceneBO.getIsITCov());
        }
        if (StringUtils.isNotBlank(querySceneBO.getApi())) {
            request.put("api", querySceneBO.getApi());
        }
        request.put("page", querySceneBO.getPage() + "");
        request.put("pageSize", querySceneBO.getPageSize() + "");
        JSONArray jsonArray = HttpClientUtil.httpClient("linkAnalysisModel/getSceneIds", 2, request);
        QueryLinkVO queryLinkVO = new QueryLinkVO();
        JSONObject jsonObject = jsonArray.getJSONObject(0);
        //分页信息
        JSONObject pageDetail = jsonObject.getJSONObject("queryScenePage");
        queryLinkVO.setTotalNum(pageDetail.getIntValue("totalNum"));
        queryLinkVO.setPage(querySceneBO.getPage());
        queryLinkVO.setPageSize(querySceneBO.getPageSize());
        if (StringUtils.isEmpty(querySceneBO.getApi())) {
            queryLinkVO.setApis(jsonObject.getList("apis", String.class));
        }
        //汇总信息
        int totalHeat = pageDetail.getIntValue("totalHeat");
        QueryLinkTitleModel queryLinkTitleModel = new QueryLinkTitleModel();
        queryLinkTitleModel.setFlowNum(totalHeat);
        queryLinkTitleModel.setLinkNum(queryLinkVO.getTotalNum());
        queryLinkTitleModel.setMaxDepth(pageDetail.getIntValue("maxDepth"));
        queryLinkTitleModel.setMaxLength(pageDetail.getIntValue("maxLength"));
        queryLinkVO.setQueryLinkTitleModel(queryLinkTitleModel);

        List<SceneVOModel> sceneVOModels = jsonObject.getList("sceneModelEntitys", SceneVOModel.class);
        for (SceneVOModel sceneVOModel : sceneVOModels) {
            if (StringUtils.isBlank(sceneVOModel.getRequestBody())) {
                sceneVOModel.setRequestBody(sceneVOModel.getUrl());
            }
            sceneVOModel.setStrId(sceneVOModel.getId() + "");
            sceneVOModel.setHeatDegree(totalHeat == 0 ? 0.0f : Float.valueOf(String.format("%.2f", 1.0 * sceneVOModel.getFlow() / totalHeat * 100)));
        }
        queryLinkVO.setLinkAnalysisVOModels(sceneVOModels);
        aresBrainResult.setData(queryLinkVO);
        return aresBrainResult;
    }

    /**
     * 批量查询链路详细信息
     *
     * <p>根据提供的查询条件，批量查询链路分析的详细信息，用于批量处理和导出场景。该方法会：</p>
     * <ul>
     *   <li>支持与queryLinkDetail相同的查询条件</li>
     *   <li>自动遍历所有分页数据，无需手动处理分页</li>
     *   <li>返回简化后的TraceIdListVo列表，主要用于链路追踪</li>
     *   <li>适用于需要获取大量链路数据的批量操作场景</li>
     * </ul>
     *
     * <p>注意：该方法会持续查询直到获取所有匹配的数据，在数据量较大时可能需要较长时间。</p>
     *
     * @param querySceneBO 查询条件对象，包含服务名、类名、方法名等过滤条件和分页参数
     * @return List&lt;TraceIdListVo&gt; 返回包含场景ID和追踪ID的批量查询结果列表
     */
    @SneakyThrows
    public List<TraceIdListVo> queryLinkDetailBatch(QuerySceneBO querySceneBO) {
        List<TraceIdListVo> traceIdListVos = new ArrayList<>();
        Map<String, String> request = new HashMap<>();
        if (StringUtils.isNotBlank(querySceneBO.getServiceName())) {
            request.put("serviceName", querySceneBO.getServiceName());
        }
        if (StringUtils.isNotBlank(querySceneBO.getClassName())) {
            request.put("className", querySceneBO.getClassName());
        }
        if (StringUtils.isNotBlank(querySceneBO.getMethodName())) {
            request.put("methodName", querySceneBO.getMethodName());
        }
        if (StringUtils.isNotBlank(querySceneBO.getParameters())) {

            request.put("parameters", StringUtils.replace(querySceneBO.getParameters(), "\"", "\\\""));
        }
        if (StringUtils.isNotBlank(querySceneBO.getLineNums())) {
            request.put("lineNums", querySceneBO.getLineNums());
        }
        int page = querySceneBO.getPage();
        while (true) {
            request.put("page", page + "");
            request.put("pageSize", querySceneBO.getPageSize() + "");
            JSONArray jsonArray = HttpClientUtil.httpClient("linkAnalysisModel/getSceneIds", 2, request);
            JSONObject jsonObject = jsonArray.getJSONObject(0);
            List<SceneVOModel> sceneVOModels = jsonObject.getList("sceneModelEntitys", SceneVOModel.class);
            for (SceneVOModel sceneVOModel : sceneVOModels) {
                TraceIdListVo traceIdListVo = new TraceIdListVo();
                traceIdListVo.setTraceId(sceneVOModel.getSceneId());
                traceIdListVo.setSceneId(sceneVOModel.getId());
                traceIdListVos.add(traceIdListVo);
            }
            if (sceneVOModels.size() < querySceneBO.getPageSize()) {
                break;
            }
            page++;
        }
        return traceIdListVos;
    }
}
