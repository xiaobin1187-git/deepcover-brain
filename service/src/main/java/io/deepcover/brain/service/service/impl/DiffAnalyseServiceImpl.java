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

package io.deepcover.brain.service.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.common.base.Strings;
import io.deepcover.brain.dal.entity.AresAgentEntity;
import io.deepcover.brain.dal.entity.DiffRecordEntity;
import io.deepcover.brain.dal.entity.DiffResultDetailEntity;
import io.deepcover.brain.dal.entity.SceneTraceid;
import io.deepcover.brain.dal.mapper.AresServiceMapper;
import io.deepcover.brain.dal.mapper.DiffRecordMapper;
import io.deepcover.brain.dal.mapper.DiffResultDetailMapper;
import io.deepcover.brain.dal.mapper.SceneTraceIdMapper;
import io.deepcover.brain.model.*;
import io.deepcover.brain.service.exception.RRException;
import io.deepcover.brain.service.service.AresBrainLinkService;
import io.deepcover.brain.service.service.AresBrainSceneService;
import io.deepcover.brain.service.service.DiffAnalyseService;
import io.deepcover.brain.service.service.SceneRiskLevelService;
import io.deepcover.brain.service.util.DingNotifyUtils;
import io.deepcover.brain.service.util.PageUtils;
import io.deepcover.brain.service.util.Query;
import io.deepcover.brain.service.util.client.HttpClientRequest;
import io.deepcover.brain.service.util.client.HttpClientResponse;
import io.deepcover.brain.service.util.client.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileNotFoundException;
import java.util.*;
import java.util.Objects;



/**
 * 差异分析服务实现类
 *
 * 提供代码差异分析的核心功能实现，包括代码变更分析、风险评估、
 * 场景关联分析、测试结果统计等功能
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
@Service
@Slf4j
public class DiffAnalyseServiceImpl implements DiffAnalyseService {

    @Value("${git.local.base.dir}")
    private String gitLocalBaseRepoDir;

    @Value("${ereplay.front.url}")
    private String ereplayFrontUrl;

    @Value(value = "${dingTalk.need.send}")
    private Boolean dingTalkNeedSend;
    @Value("${epaasapi:/v1/signflows/{flowId}/getShowUrl,/v2/processes/start,/v2/processes/startByFlowTemplate}")
    private String TARGET_API;
    @Autowired
    private DiffRecordMapper diffRecordMapper;

    @Autowired
    private AresServiceMapper aresAgentDao;

    @Autowired
    private DiffResultDetailMapper resultDetailMapper;

    @Autowired
    private AresBrainLinkService aresBrainLinkService;

    @Autowired
    SceneRiskLevelService riskLevelService;

    @Autowired
    AresBrainSceneService brainSceneService;

    @Autowired
    private SceneTraceIdMapper sceneTraceIdMapper;

    /**
     * 预处理差异分析记录
     *
     * 验证服务配置、检查重复记录、初始化分析状态，并确定是否需要发送钉钉通知
     *
     * @param recordEntity 差异分析实体，包含服务名称、环境代码、版本信息等
     * @return Boolean 返回是否需要发送钉钉通知，true表示需要发送
     * @throws RRException 当服务未在ares控制台配置时抛出异常
     */
    @Override
    public Boolean preAdd(DiffRecordEntity recordEntity) {
        if (recordEntity == null) {
            throw new RRException("参数recordEntity不能为空");
        }
        AresAgentEntity aresAgentEntity = aresAgentDao.queryObject(recordEntity.getServiceName());
        if (aresAgentEntity == null) {
            log.warn("服务名：{}，没有在ares控制台配置，不进行比对", recordEntity.getServiceName());
            throw new RRException(recordEntity.getServiceName() + ":没有在ares控制台配置，不进行比对");
        }

        DiffRecordEntity diffExist = diffRecordMapper.queryObjectByUnique(recordEntity);
        if (diffExist == null) {
            recordEntity.setStatus(1); // 1-分析中
            recordEntity.setLastStartDiffTime(new Date());
            diffRecordMapper.insert(recordEntity);

        } else {
            recordEntity.setDiffDetailId(diffExist.getDiffDetailId());
            recordEntity.setId(diffExist.getId());
            recordEntity.setLastStartDiffTime(new Date());
            recordEntity.setStatus(1); // 1-分析中
            diffRecordMapper.updateStatus(recordEntity);
        }

        log.info("服务:{},envCode:{},变更分析中", recordEntity.getServiceName(), recordEntity.getEnvCode());
        String sendMessageEnv = "sml";
        if (aresAgentEntity.getLocal() == 1) {
            //公有云 只有模拟环境发送钉钉通知
            //混合云 只有稳定环境发送钉钉通知
            sendMessageEnv = "test";
        }
        return null == diffExist && (null == recordEntity.getEnvCode() ?
                false : recordEntity.getEnvCode().toLowerCase().contains(sendMessageEnv));
    }
    /**
     * 新版本差异数据分析
     *
     * 基于traceId进行过滤的代码差异分析，支持epaas场景关联分析，
     * 包括风险等级评估、统计计算等功能
     *
     * @param recordEntity 差异分析实体，包含版本信息和对比配置
     * @param dingMsgExist 是否需要发送钉钉消息通知
     * @param traceId 用于过滤场景的跟踪ID
     */
    @Override
//    @Async("newAsyncServiceExecutor")
    @Transactional
    public void newDiffAnalyseData(DiffRecordEntity recordEntity, Boolean dingMsgExist, String traceId) {
        if (recordEntity == null) {
            throw new RRException("参数recordEntity不能为空");
        }
        long startTime = System.currentTimeMillis();
        log.info("newDiffAnalyseData,getNowVersion={},getNowBranch={},traceId={},serviceName={}", recordEntity.getNowVersion(), recordEntity.getNowBranch(), traceId,recordEntity.getServiceName());

        try {
            DiffResultDetailEntity resultDetailEntity = initDiffResultDetail(recordEntity);
            JSONArray resData = getDiffResultData(recordEntity, resultDetailEntity);
            String rule = riskLevelService.getServiceRule(recordEntity.getServiceName());

            // 如果有traceId，则先进行过滤处理
            if (traceId != null) {
                // 先填充api数据，确保processTraceIdMatching能正确处理
                Map<String, String> sceneLinkMap = new HashMap<>();
                this.caclResultStats(
                        resData == null ? new JSONArray() : resData,
                        recordEntity.getServiceName(),
                        sceneLinkMap);

                JSONArray epaasJsonArray = processTraceIdMatching(resData, traceId, recordEntity);
                if (epaasJsonArray.size() > 0) {
                    recordEntity.setIsEpaas(1);
                    
                    // 检查是否已有resultDetailEpaas数据，如果有则追加而不是替换
                    JSONArray finalEpaasArray; // 用于统计的最终epaas数据数组
                
                    // 获取现有的epaas数据
                    String existingEpaasData = resultDetailEntity.getResultDetailEpaas();
                    if (existingEpaasData != null && !existingEpaasData.isEmpty()) {
                        try {
                            // 解析现有的epaas数据
                            JSONArray existingEpaasArray = JSONArray.parseArray(existingEpaasData);

                            // 将新数据追加到现有数据中，但需要处理不同traceId的情况
                            mergeEpaasData(existingEpaasArray, epaasJsonArray);

                            // 去重处理，确保不会有重复的数据，但保留不同traceId的数据
                            JSONArray deduplicatedArray = deduplicateEpaasData(existingEpaasArray);
                            log.info("去重后epaas数据条数: {}", deduplicatedArray.size());
                            
                            resultDetailEntity.setResultDetailEpaas(JSONObject.toJSONString(deduplicatedArray));
                            finalEpaasArray = deduplicatedArray; // 使用合并并去重后的数据进行统计
                        } catch (Exception e) {
                            log.warn("解析现有resultDetailEpaas数据失败，使用新数据替换", e);
                            resultDetailEntity.setResultDetailEpaas(JSONObject.toJSONString(epaasJsonArray));
                            finalEpaasArray = epaasJsonArray; // 使用新数据进行统计
                        }
                    } else {
                        // 如果没有现有数据，直接设置新数据
                        log.info("未找到现有epaas数据，直接使用新数据，条数: {}", epaasJsonArray.size());
                        resultDetailEntity.setResultDetailEpaas(JSONObject.toJSONString(epaasJsonArray));
                        finalEpaasArray = epaasJsonArray; // 使用新数据进行统计
                    }

                    // 只使用过滤后的数据进行统计计算
                    log.info("开始计算epaas统计数据，数据条数: {}", finalEpaasArray.size());
                    JSONObject resultEpaasStats =
                            this.caclResultStatsForEpaas(
                                    finalEpaasArray == null ? new JSONArray() : finalEpaasArray);
                    resultEpaasStats.put("rule", rule);
                    resultDetailEntity.setResultStatsEpaas(JSON.toJSONString(resultEpaasStats));
                    // 如果有traceId匹配的数据，则只保存匹配的数据相关统计信息
                    resultDetailMapper.updateByIdEpaas(resultDetailEntity);
                    diffRecordMapper.updateRestResultEpaas(recordEntity);
                    if (dingMsgExist) {
                        DingNotifyUtils.sendEpassAnalyseWorkMessage(recordEntity, traceId);
                        // 保存epaas分析报告参数到SceneTraceid记录
                        saveReportParamsToSceneTraceId(recordEntity, traceId);
                    }
                }

            }
            // 如果traceId为空，则不进行任何操作

        } catch (Exception e) {
            log.error("newDiffAnalyseData code-diff分析失败={}",e.getMessage());

        }

        log.info("newDiffAnalyseData code-diff分析耗时={},serviceName={}", System.currentTimeMillis() - startTime, recordEntity.getServiceName());
    }


    /**
     * 异步添加差异分析数据
     *
     * 使用HTTP专用线程池异步执行代码差异分析，包括获取代码差异结果、
     * 风险等级评估、场景关联分析、统计计算等功能
     *
     * @param recordEntity 差异分析实体，包含版本信息和对比配置
     * @param dingMsgExist 是否需要发送钉钉消息通知
     */
    @Override
    @Async("httpExecutor")
    public void addDiffAnalyseData(DiffRecordEntity recordEntity, Boolean dingMsgExist) {
        if (recordEntity == null) {
            throw new RRException("参数recordEntity不能为空");
        }
        long startTime = System.currentTimeMillis();
        JSONArray resData=null;
        DiffResultDetailEntity resultDetailEntity = null;
        if(recordEntity.getDiffDetailId()!=null){
            resultDetailEntity = resultDetailMapper.queryObjectById(recordEntity.getDiffDetailId());
            if(resultDetailEntity!=null && !StringUtils.isEmpty(resultDetailEntity.getCodeDiff())){
                resData=JSONArray.parseArray(resultDetailEntity.getCodeDiff());
            }
        }

        if(resData==null){
            HttpClientResponse response = this.getCodeDiffResult(recordEntity);
            if (response.getResponseBody() == null) {
                throw new RRException("调用服务code_diff异常，分析失败");
            }
            JSONObject json = JSONObject.parseObject(response.getResponseBody().toString());
            if (json.getIntValue("code") != 10000) {
                throw new RRException("代码变更分析失败:" + json.getString("msg"));
            }
            resData = json.getJSONArray("data");
        }
        if(resultDetailEntity==null){
            resultDetailEntity = new DiffResultDetailEntity();
        }

        resultDetailEntity.setCodeDiff(JSONArray.toJSONString(resData));
        resultDetailEntity.setResultStats(null);
        resultDetailEntity.setResultDetail(null);
//        JSONObject json = null;
        HttpClientResponse response = null;
//        DiffResultDetailEntity resultDetailEntity = new DiffResultDetailEntity();

        try {
//            response = this.getCodeDiffResult(recordEntity);
//            if (response.getResponseBody() == null) {
//                throw new RRException("调用服务code_diff异常，分析失败");
//            }
//            json = JSONObject.parseObject(response.getResponseBody().toString());
//            if (json.getIntValue("code") != 10000) {
//                throw new RRException("代码变更分析失败:" + json.getString("msg"));
//            }
//            JSONArray resData = json.getJSONArray("data");
            Map<String, String> sceneLinkMap = new HashMap<>();
            JSONObject resultStats =
                    this.caclResultStats(
                            resData == null ? new JSONArray() : resData,
                            recordEntity.getServiceName(),
                            sceneLinkMap);
            List<String> testResult = new ArrayList<>();
            for (String key : sceneLinkMap.keySet()) {
                String value = sceneLinkMap.get(key);
                testResult.add(value);
            }
            //modify by 黄台 2024-03-01 添加服务中高风险定义规则
            String rule = riskLevelService.getServiceRule(recordEntity.getServiceName());
            resultStats.put("rule", rule);

            // modify by 黄台 2024-03-01 按照风险等级从高到低排序
//            JSONArray resultDetail = json.getJSONArray("data");
            if(resData!=null){
                resData.sort(Comparator.comparing(obj -> ((JSONObject) obj).getJSONObject("classRiskLevel").getInteger("code")));
            }

            resultDetailEntity.setResultDetail(
                    JSONObject.toJSONString(null == resData ? new JSONArray() : resData));
            resultDetailEntity.setResultStats(resultStats.toJSONString());

            if (recordEntity.getDiffDetailId() == null || recordEntity.getDiffDetailId() == 0) {
                resultDetailMapper.insert(resultDetailEntity);
            } else {
//                DiffResultDetailEntity exitDetail =
//                        resultDetailMapper.queryObjectById(recordEntity.getDiffDetailId());
                if (resultDetailEntity.getId()== null) {
                    resultDetailEntity.setId(recordEntity.getDiffDetailId());
                    resultDetailMapper.insert(resultDetailEntity);

                } else {
//                    resultDetailEntity.setId(exitDetail.getId());
                    resultDetailMapper.updateById(resultDetailEntity);
                }
            }

            recordEntity.setDiffDetailId(resultDetailEntity.getId());
            recordEntity.setStatus(2); // 2-分析成功
            recordEntity.setTestResult(testResult.toString());
            diffRecordMapper.updateRestResult(recordEntity);
        } catch (Exception e) {
            if (recordEntity.getDiffDetailId() == null || recordEntity.getDiffDetailId() == 0) {
                resultDetailEntity.setResultStats("{}");
                resultDetailEntity.setResultDetail("[]");
                resultDetailMapper.insert(resultDetailEntity);
                recordEntity.setDiffDetailId(resultDetailEntity.getId());
            }
            recordEntity.setStatus(3); // 3-比对失败
            recordEntity.setStatusDescrip(e.getMessage());
            diffRecordMapper.updateRestResult(recordEntity);

            if (dingMsgExist && dingTalkNeedSend) {
                DingNotifyUtils.sendDiffAnalyseWorkMessage("变更分析失败,请打开链接重新比对,并刷新页面", recordEntity);
            }
            log.error(
                    "服务:{},envCode:{},变更比对失败,比对内容：{}",
                    recordEntity.getServiceName(),
                    recordEntity.getEnvCode(),
                    response.getResponseBody(),
                    e);
            throw e;
        }
        if (dingMsgExist && dingTalkNeedSend) {
            String action = recordEntity.getPublishTime() == null ? "【手工触发】" : "【发布触发】";
            DingNotifyUtils.sendDiffAnalyseWorkMessage(action + "代码变更分析", recordEntity);
        }
        log.info("code-diff分析耗时={},serviceName={}",System.currentTimeMillis()-startTime,recordEntity.getServiceName());
    }
    
    /**
     * 保存报告参数到SceneTraceid记录
     */
    private void saveReportParamsToSceneTraceId(DiffRecordEntity recordEntity, String traceId) {
        try {
            // 使用实际的traceId
            if (traceId != null && !traceId.isEmpty()) {
                // 检查是否已存在相同的traceId和服务参数组合
                List<SceneTraceid> existRecords = sceneTraceIdMapper.getSceneTraceIdsByServiceParams(
                    traceId,
                    recordEntity.getServiceName(),
                    recordEntity.getEnvCode(),
                    recordEntity.getNowBranch(),
                    recordEntity.getBaseVersion(),
                    recordEntity.getNowVersion()
                );
                
                if (existRecords == null || existRecords.isEmpty()) {
                    // 检查是否已存在相同的traceId
                    List<SceneTraceid> traceIdRecords = sceneTraceIdMapper.getSceneTraceIds(traceId);
                    
                    if (traceIdRecords == null || traceIdRecords.isEmpty()) {
                        // 创建新记录
                        SceneTraceid sceneTraceid = new SceneTraceid();
                        sceneTraceid.setTraceid(traceId);
                        sceneTraceid.setStatus(0);
                        // 设置单独的字段
                        sceneTraceid.setServiceName(recordEntity.getServiceName());
                        sceneTraceid.setEnvCode(recordEntity.getEnvCode());
                        sceneTraceid.setBranchName(recordEntity.getNowBranch());
                        sceneTraceid.setBaseVersion(recordEntity.getBaseVersion());
                        sceneTraceid.setNowVersion(recordEntity.getNowVersion());
                        sceneTraceIdMapper.insert(sceneTraceid);
                        log.info("创建SceneTraceid记录成功，traceId: {}", traceId);
                    } else {
                        // 更新现有记录
                        SceneTraceid existingRecord = traceIdRecords.get(0);
                        existingRecord.setServiceName(recordEntity.getServiceName());
                        existingRecord.setEnvCode(recordEntity.getEnvCode());
                        existingRecord.setBranchName(recordEntity.getNowBranch());
                        existingRecord.setBaseVersion(recordEntity.getBaseVersion());
                        existingRecord.setNowVersion(recordEntity.getNowVersion());
                        sceneTraceIdMapper.updateById(existingRecord);
                        log.info("更新SceneTraceid记录成功，traceId: {}", traceId);
                    }
                } else {
                    log.info("已存在相同的traceId和服务参数组合，traceId: {}", traceId);
                }
            }
        } catch (Exception e) {
            log.error("保存报告参数到SceneTraceid记录异常", e);
        }
    }

    // 公共方法 - 处理基础数据获取和初始化
    private DiffResultDetailEntity initDiffResultDetail(DiffRecordEntity recordEntity) {
        DiffResultDetailEntity resultDetailEntity = null;
        if (recordEntity.getDiffDetailId() != null) {
            try {
                resultDetailEntity = resultDetailMapper.queryObjectById(recordEntity.getDiffDetailId());
                if (resultDetailEntity != null && !StringUtils.isEmpty(resultDetailEntity.getCodeDiff())) {
                    return resultDetailEntity;
                }
            } catch (Exception e) {
                log.error("查询结果详情失败", e);
            }
        }
        return new DiffResultDetailEntity();
    }

    // 公共方法 - 获取代码差异结果
    private JSONArray getDiffResultData(DiffRecordEntity recordEntity, DiffResultDetailEntity resultDetailEntity) {
        if (resultDetailEntity.getCodeDiff() != null) {
            return JSONArray.parseArray(resultDetailEntity.getCodeDiff());
        }

        HttpClientResponse response = this.getCodeDiffResult(recordEntity);
        if (response.getResponseBody() == null) {
            throw new RRException("调用服务code_diff异常，分析失败");
        }
        JSONObject json = JSONObject.parseObject(response.getResponseBody().toString());
        if (json.getIntValue("code") != 10000) {
            throw new RRException("代码变更分析失败:" + json.getString("msg"));
        }
        return json.getJSONArray("data");
    }

    /**
     * 根据ID添加差异分析记录
     *
     * 根据传入的ID查询并处理差异分析记录
     *
     * @param id 差异分析记录的ID
     */
    @Override
    public void addById(Long id) {
        DiffRecordEntity diffExist = diffRecordMapper.queryObjectById(id);
        if (diffExist == null) {
        }
    }

    private HttpClientResponse getCodeDiffResult(DiffRecordEntity recordEntity) {
        HttpClientRequest request = new HttpClientRequest();
        request.setType(1);
        request.setUrl(
                HttpClientUtil.codediffUrl
                        + "/v2/api/code/diff/gitUrl/list"
                        + "?baseGitUrl="
                        + recordEntity.getBaseGitUrl()
                        + "&baseVersion="
                        + recordEntity.getBaseVersion()
                        + "&nowGitUrl="
                        + recordEntity.getNowGitUrl()
                        + "&nowVersion="
                        + recordEntity.getNowVersion());
        HttpClientResponse response = HttpClientUtil.sendRequest(request);
        return response;
    }

    /**
     * 专门用于epaas数据的统计计算
     * @param array epaas数据数组
     * @return 统计结果
     */
    private JSONObject caclResultStatsForEpaas(JSONArray array) {
        String[] numType = {"file", "line", "method", "scene", "api"};
        String[] nums = {"num", "add", "update", "delete"};
        JSONObject resultStats = new JSONObject();
        for (String type : numType) {
            resultStats.put(type, new JSONObject());
            for (String n : nums) {
                resultStats.getJSONObject(type).put(n, 0);
            }
        }

        JSONObject file = resultStats.getJSONObject("file");
        JSONObject line = resultStats.getJSONObject("line");
        JSONObject method = resultStats.getJSONObject("method");
        JSONObject api = resultStats.getJSONObject("api");
        JSONObject scene = resultStats.getJSONObject("scene");

        // 用于存储唯一的API URL
        Set<String> uniqueApiUrls = new HashSet<>();
        // 用于存储唯一的场景ID
        Set<String> uniqueSceneIds = new HashSet<>();
        
        int classHighRiskNum = 0, classMediumRiskNum = 0, classLowRiskNum = 0;
        int methodHighRiskNum = 0, methodMediumRiskNum = 0, methodLowRiskNum = 0;
        for (int i = 0; i < array.size(); i++) {
            JSONObject o = array.getJSONObject(i);
            // 统计文件
            file.put("num", file.getInteger("num") + 1);
            if ("ADD".equals(o.getString("type"))) {
                file.put("add", file.getInteger("add") + 1);
            } else if ("MODIFY".equals(o.getString("type"))) {
                file.put("update", file.getInteger("update") + 1);
            } else {
                file.put("delete", file.getInteger("delete") + 1);
            }
            // 统计行号
            JSONArray lineArr = o.getJSONArray("lines");
            for (int j = 0; j < lineArr.size(); j++) {
                JSONObject lineItem = lineArr.getJSONObject(j);
                if ("INSERT".equals(lineItem.getString("type"))) {
                    int addNum = lineItem.getIntValue("endLineNum") - lineItem.getIntValue("startLineNum");
                    line.put("num", line.getInteger("num") + addNum);
                    line.put("add", line.getInteger("add") + addNum);
                } else if ("REPLACE".equals(lineItem.getString("type"))) {
                    int updateNum = lineItem.getIntValue("endLineNum") - lineItem.getIntValue("startLineNum");
                    line.put("num", line.getInteger("num") + updateNum);
                    line.put("update", line.getInteger("update") + updateNum);
                } else {
                    int deleteNum =
                            lineItem.getIntValue("endLineNum") - lineItem.getIntValue("startLineNum") + 1;
                    line.put("num", line.getInteger("num") + deleteNum);
                    line.put("delete", line.getInteger("delete") + deleteNum);
                }
            }

            // 统计方法
            JSONArray methodInfos = o.getJSONArray("methodInfos");
            //类风险等级
            RiskEnum classRiskLevel = RiskEnum.NULL;
            for (int k = 0; k < methodInfos.size(); k++) {
                JSONObject mItem = methodInfos.getJSONObject(k);
                //判断方法是否修改过
                boolean isModify = false, isOnlyAdd = false;
                RiskLevelVO riskLevelVO = new RiskLevelVO();
                RiskEnum riskEnum = RiskEnum.NULL;
                if ("ADD".equals(mItem.getString("type"))) {
                    method.put("num", method.getInteger("num") + 1);
                    method.put("add", method.getInteger("add") + 1);
                    isOnlyAdd = true;
                } else if ("MODIFY".equals(mItem.getString("type"))) {
                    method.put("num", method.getInteger("num") + 1);
                    method.put("update", method.getInteger("update") + 1);
                    isModify = true;
                } else {
                    method.put("num", method.getInteger("num") + 1);
                    method.put("delete", method.getInteger("delete") + 1);
                    isModify = true;
                }
                //如果代码没有改动，则无风险
                if (!isOnlyAdd && !isModify) {
                    riskEnum = RiskEnum.NORISK;
                }
                
                // 获取风险等级信息
                if (mItem.containsKey("riskLevel")) {
                    try {
                        // 使用更安全的方式解析riskLevel对象
                        Object riskLevelObj = mItem.get("riskLevel");
                        if (riskLevelObj != null) {
                            if (riskLevelObj instanceof JSONObject) {
                                // 如果是JSONObject，直接转换
                                JSONObject riskLevelJson = (JSONObject) riskLevelObj;
                                riskLevelVO = new RiskLevelVO();
                                if (riskLevelJson.containsKey("riskEnum")) {
                                    // 处理riskEnum字段
                                    Object riskEnumObj = riskLevelJson.get("riskEnum");
                                    if (riskEnumObj instanceof JSONObject) {
                                        JSONObject riskEnumJson = (JSONObject) riskEnumObj;
                                        if (riskEnumJson.containsKey("code")) {
                                            int code = riskEnumJson.getIntValue("code");
                                            for (RiskEnum re : RiskEnum.values()) {
                                                if (re.getCode() == code) {
                                                    riskLevelVO.setRiskEnum(re);
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                                // 处理其他字段
                                if (riskLevelJson.containsKey("decision")) {
                                    riskLevelVO.setDecision(riskLevelJson.getString("decision"));
                                }
                                if (riskLevelJson.containsKey("flowNum")) {
                                    riskLevelVO.setFlowNum(riskLevelJson.getIntValue("flowNum"));
                                }
                                if (riskLevelJson.containsKey("complexityValue")) {
                                    riskLevelVO.setComplexityValue(riskLevelJson.getIntValue("complexityValue"));
                                }
                            } else if (riskLevelObj instanceof String) {
                                // 如果是字符串，尝试解析JSON
                                String riskLevelStr = (String) riskLevelObj;
                                JSONObject riskLevelJson = JSONObject.parseObject(riskLevelStr);
                                riskLevelVO = new RiskLevelVO();
                                if (riskLevelJson.containsKey("riskEnum")) {
                                    JSONObject riskEnumJson = riskLevelJson.getJSONObject("riskEnum");
                                    if (riskEnumJson.containsKey("code")) {
                                        int code = riskEnumJson.getIntValue("code");
                                        for (RiskEnum re : RiskEnum.values()) {
                                            if (re.getCode() == code) {
                                                riskLevelVO.setRiskEnum(re);
                                                break;
                                            }
                                        }
                                    }
                                }
                                // 处理其他字段
                                if (riskLevelJson.containsKey("decision")) {
                                    riskLevelVO.setDecision(riskLevelJson.getString("decision"));
                                }
                                if (riskLevelJson.containsKey("flowNum")) {
                                    riskLevelVO.setFlowNum(riskLevelJson.getIntValue("flowNum"));
                                }
                                if (riskLevelJson.containsKey("complexityValue")) {
                                    riskLevelVO.setComplexityValue(riskLevelJson.getIntValue("complexityValue"));
                                }
                            }
                        }
                        if (riskLevelVO != null && riskLevelVO.getRiskEnum() != null) {
                            riskEnum = riskLevelVO.getRiskEnum();
                        }
                    } catch (Exception e) {
                        log.warn("解析riskLevel对象失败，使用默认值", e);
                        riskLevelVO = new RiskLevelVO();
                        riskEnum = RiskEnum.NORISK;
                    }
                }

                // 统计接口和场景
                JSONArray apiArr = mItem.getJSONArray("api");
                if (apiArr != null) {
                    for (int apiIndex = 0; apiIndex < apiArr.size(); apiIndex++) {
                        JSONObject apiItem = apiArr.getJSONObject(apiIndex);
                        String apiUrl = apiItem.getString("url");
                        // 添加唯一的API URL
                        if (apiUrl != null && !uniqueApiUrls.contains(apiUrl)) {
                            uniqueApiUrls.add(apiUrl);
                        }
                        
                        // 统计场景
                        JSONArray sceneArr = apiItem.getJSONArray("scene");
                        if (sceneArr != null) {
                            for (int sceneIndex = 0; sceneIndex < sceneArr.size(); sceneIndex++) {
                                String sceneStr = sceneArr.getString(sceneIndex);
                                try {
                                    if (sceneStr != null && !sceneStr.isEmpty()) {
                                        JSONObject sceneJson = JSONObject.parseObject(sceneStr);
                                        String sceneId = sceneJson.getString("sceneId");
                                        // 添加唯一的场景ID
                                        if (sceneId != null && !uniqueSceneIds.contains(sceneId)) {
                                            uniqueSceneIds.add(sceneId);
                                        }
                                    }
                                } catch (Exception e) {
                                    log.warn("解析场景JSON失败: {}", sceneStr, e);
                                }
                            }
                        }
                    }
                }

                //类风险取方法最高的风险等级
                classRiskLevel = riskEnum.getCode() < classRiskLevel.getCode() ? riskEnum : classRiskLevel;

                //计算中高风险方法总数
                if (riskEnum.getCode() == 1) {
                    methodHighRiskNum++;
                } else if (riskEnum.getCode() == 2) {
                    methodMediumRiskNum++;
                } else if (riskEnum.getCode() == 3) {
                    methodLowRiskNum++;
                }
            }
            o.put("classRiskLevel", classRiskLevel);
            //计算中高风险方法总数
            if (classRiskLevel.getCode() == 1) {
                classHighRiskNum++;
            } else if (classRiskLevel.getCode() == 2) {
                classMediumRiskNum++;
            } else if (classRiskLevel.getCode() == 3) {
                classLowRiskNum++;
            }
        }
    
        // 统计唯一API数量
        api.put("num", uniqueApiUrls.size());
        // 统计唯一场景数量
        scene.put("num", uniqueSceneIds.size());

        //把方法和类的中高风险总汇放到结果集中
        JSONObject classRisk = new JSONObject();
        classRisk.put("high", classHighRiskNum);
        classRisk.put("medium", classMediumRiskNum);
        classRisk.put("low", classLowRiskNum);
        resultStats.put("classRisk", classRisk);
        JSONObject methodRisk = new JSONObject();
        methodRisk.put("high", methodHighRiskNum);
        methodRisk.put("medium", methodMediumRiskNum);
        methodRisk.put("low", methodLowRiskNum);
        resultStats.put("methodRisk", methodRisk);
        return resultStats;
    }

    private JSONObject caclResultStats(
            JSONArray array, String serviceName, Map<String, String> sceneLinkMap) {
        String[] numType = {"file", "line", "method", "scene", "api"};
        String[] nums = {"num", "add", "update", "delete"};
        JSONObject resultStats = new JSONObject();
        for (String type : numType) {
            resultStats.put(type, new JSONObject());
            for (String n : nums) {
                resultStats.getJSONObject(type).put(n, 0);
            }
        }

        JSONObject file = resultStats.getJSONObject("file");
        JSONObject line = resultStats.getJSONObject("line");
        JSONObject method = resultStats.getJSONObject("method");
        JSONObject api = resultStats.getJSONObject("api");
        JSONObject scene = resultStats.getJSONObject("scene");

        Map<String, Object> apiLinkMap = new HashMap<>();
        int classHighRiskNum = 0, classMediumRiskNum = 0, classLowRiskNum = 0;
        int methodHighRiskNum = 0, methodMediumRiskNum = 0, methodLowRiskNum = 0;
        for (int i = 0; i < array.size(); i++) {
            JSONObject o = array.getJSONObject(i);
            // 统计文件
            file.put("num", file.getInteger("num") + 1);
            if ("ADD".equals(o.getString("type"))) {
                file.put("add", file.getInteger("add") + 1);
            } else if ("MODIFY".equals(o.getString("type"))) {
                file.put("update", file.getInteger("update") + 1);
            } else {
                file.put("delete", file.getInteger("delete") + 1);
            }
            // 统计行号
            JSONArray lineArr = o.getJSONArray("lines");
            for (int j = 0; j < lineArr.size(); j++) {
                JSONObject lineItem = lineArr.getJSONObject(j);
                if ("INSERT".equals(lineItem.getString("type"))) {
                    int addNum = lineItem.getIntValue("endLineNum") - lineItem.getIntValue("startLineNum");
                    line.put("num", line.getInteger("num") + addNum);
                    line.put("add", line.getInteger("add") + addNum);
                } else if ("REPLACE".equals(lineItem.getString("type"))) {
                    int updateNum = lineItem.getIntValue("endLineNum") - lineItem.getIntValue("startLineNum");
                    line.put("num", line.getInteger("num") + updateNum);
                    line.put("update", line.getInteger("update") + updateNum);
                } else {
                    int deleteNum =
                            lineItem.getIntValue("endLineNum") - lineItem.getIntValue("startLineNum") + 1;
                    line.put("num", line.getInteger("num") + deleteNum);
                    line.put("delete", line.getInteger("delete") + deleteNum);
                }
            }

            // 统计方法
            JSONArray methodInfos = o.getJSONArray("methodInfos");
            //类风险等级
            RiskEnum classRiskLevel = RiskEnum.NULL;
            for (int k = 0; k < methodInfos.size(); k++) {
                JSONObject mItem = methodInfos.getJSONObject(k);
                //判断方法是否修改过
                boolean isModify = false, isOnlyAdd = false;
                RiskLevelVO riskLevelVO = new RiskLevelVO();
                RiskEnum riskEnum = RiskEnum.NULL;
                if ("ADD".equals(mItem.getString("type"))) {
                    method.put("num", method.getInteger("num") + 1);
                    method.put("add", method.getInteger("add") + 1);
                    isOnlyAdd = true;
                } else if ("MODIFY".equals(mItem.getString("type"))) {
                    method.put("num", method.getInteger("num") + 1);
                    method.put("update", method.getInteger("update") + 1);
                    isModify = true;
                } else {
                    method.put("num", method.getInteger("num") + 1);
                    method.put("delete", method.getInteger("delete") + 1);
                    isModify = true;
                }
                //如果代码没有改动，则无风险
                if (!isOnlyAdd && !isModify) {
                    riskEnum = RiskEnum.NORISK;
                }
                // 统计接口
                JSONArray apiArr = new JSONArray();
                mItem.put("api", apiArr);
                if (!"ADD".equals(o.getString("type")) && !"ADD".equals(mItem.getString("type"))) {
                    Map<String, String> params = new HashMap<>();
                    SearchModel searchModel = new SearchModel();
                    params.put("serviceName", serviceName);
                    searchModel.setServiceName(serviceName);
                    String className = o.getString("classFile").replace("/", ".");
                    params.put("className", className);
                    searchModel.setClassName(className);
                    String methodName = mItem.getString("methodName");
                    params.put("methodName", methodName);
                    searchModel.setMethodName(methodName);
                    String queryParameters = "";
                    String parameters = mItem.getString("parameters");
                    if (mItem.containsKey("parameters")) {
                        //方法参数信息
                        queryParameters = getParams(searchModel, parameters);
                    }
                    //添加参数类型作为查询条件
                    params.put("parameters", queryParameters);
                    //获取流量值和是否核心场景
                    List<Integer> flowAndCore = querySceneByMethod(params, apiArr, apiLinkMap, sceneLinkMap);
                    String methodNameParams = methodName + "()";
                    if (mItem.containsKey("parameters")) {
                        //查询圈复杂度的参数类型
                        methodNameParams = methodName + getParams(parameters);
                    }
                    if (flowAndCore != null && flowAndCore.size() >= 2) {
                        riskLevelVO = riskLevelService.getModifyMethodRisk(serviceName, methodNameParams, flowAndCore.get(0), flowAndCore.get(1));
                        riskEnum = riskLevelVO.getRiskEnum();
                    }

                }
                //类风险取方法最高的风险等级
                classRiskLevel = riskEnum.getCode() < classRiskLevel.getCode() ? riskEnum : classRiskLevel;
                riskLevelVO.setRiskEnum(riskEnum);
                mItem.put("riskLevel", riskLevelVO);

                //计算中高风险方法总数
                if (riskEnum.getCode() == 1) {
                    methodHighRiskNum++;
                } else if (riskEnum.getCode() == 2) {
                    methodMediumRiskNum++;
                } else if (riskEnum.getCode() == 3) {
                    methodLowRiskNum++;
                }
            }
            o.put("classRiskLevel", classRiskLevel);
            //计算中高风险方法总数
            if (classRiskLevel.getCode() == 1) {
                classHighRiskNum++;
            } else if (classRiskLevel.getCode() == 2) {
                classMediumRiskNum++;
            } else if (classRiskLevel.getCode() == 3) {
                classLowRiskNum++;
            }
        }
        api.put("num", apiLinkMap.size());
        scene.put("num", sceneLinkMap.size());

        //把方法和类的中高风险总汇放到结果集中
        JSONObject classRisk = new JSONObject();
        classRisk.put("high", classHighRiskNum);
        classRisk.put("medium", classMediumRiskNum);
        classRisk.put("low", classLowRiskNum);
        resultStats.put("classRisk", classRisk);
        JSONObject methodRisk = new JSONObject();
        methodRisk.put("high", methodHighRiskNum);
        methodRisk.put("medium", methodMediumRiskNum);
        methodRisk.put("low", methodLowRiskNum);
        resultStats.put("methodRisk", methodRisk);
        return resultStats;
    }

    private List<Integer> querySceneByMethod(
            Map<String, String> params,
            JSONArray apiArr,
            Map<String, Object> apiLinkMap,
            Map<String, String> sceneLinkMap) {
        QuerySceneBO querySceneBO = new QuerySceneBO();
        querySceneBO.setServiceName(params.get("serviceName"));
        querySceneBO.setClassName(params.get("className"));
        querySceneBO.setMethodName(params.get("methodName"));
        querySceneBO.setParameters(params.get("parameters"));
        querySceneBO.setPage(1);
        querySceneBO.setPageSize(2000);
        AresBrainResult<QueryLinkVO> result = null;
        List<Integer> riskResult = new ArrayList<>();
        int flowNum = 0, isCore = 1;
        try {
            result = aresBrainLinkService.queryLinkDetail(querySceneBO);
        } catch (Exception e) {
            log.error("调用分析中心查询链路详情失败", e);
            return riskResult;
        }

        if (result.getCode() != 0) {
            log.error("调用分析中心查询链路详情失败：" + result.getMessage());
            return riskResult;
        } else {
            if (result != null && null != result.getData() && result.getData().getTotalNum() != 0) {
                QueryLinkVO queryLinkVO = result.getData();
                Map<String, JSONObject> apiMap = new HashMap<>();
                List<SceneVOModel> lists = queryLinkVO.getLinkAnalysisVOModels();
                //影响场景总流量
                flowNum = queryLinkVO.getQueryLinkTitleModel().getFlowNum();
                for (SceneVOModel sceneVOModel : lists) {
                    //如果是核心场景，则标记为核心
                    if (0 == sceneVOModel.getIsCore()) {
                        isCore = 0;
                    }
                    String url = sceneVOModel.getMethod() + " " + sceneVOModel.getApi();
                    if (!apiLinkMap.containsKey(url)) {
                        apiLinkMap.put(url, sceneVOModel);
                    }
                    if (!sceneLinkMap.containsKey(sceneVOModel.getStrId())) {
                        sceneLinkMap.put(sceneVOModel.getStrId(), JSON.toJSONString(sceneVOModel));
                    }
                    if (apiMap.containsKey(url)) {
                        JSONObject exist = apiMap.get(url);
                        JSONArray sceneExist = exist.getJSONArray("scene");

                        JSONObject sceneItem = new JSONObject();
                        sceneItem.put("traceId", sceneVOModel.getSceneId());
                        sceneItem.put("strId", sceneVOModel.getStrId());
                        sceneItem.put("name", sceneVOModel.getRequestBody());
                        sceneItem.put("dependency", sceneVOModel.getServiceOrder());
                        //                        sceneExist.add(sceneItem);
                        sceneExist.add(JSON.toJSONString(sceneVOModel));

                    } else {
                        JSONObject apiJson = new JSONObject();
                        apiJson.put("url", url);

                        JSONArray sceneArr = new JSONArray();
                        apiJson.put("scene", sceneArr);

                        JSONObject sceneItem = new JSONObject();
                        sceneItem.put("traceId", sceneVOModel.getSceneId());
                        sceneItem.put("strId", sceneVOModel.getStrId());
                        sceneItem.put("name", sceneVOModel.getRequestBody());
                        sceneItem.put("dependency", sceneVOModel.getServiceOrder());
                        //                        sceneArr.add(sceneItem);
                        sceneArr.add(JSON.toJSONString(sceneVOModel));

                        apiMap.put(url, apiJson);
                    }
                }

                for (JSONObject json : apiMap.values()) {
                    apiArr.add(json);
                }
            }
        }
        riskResult.add(flowNum);
        riskResult.add(isCore);
        return riskResult;
    }

    private void querySceneByMethod(
            Map<String, String> params, Map<String, String> sceneLinkMap, JSONArray api) {
        QuerySceneBO querySceneBO = new QuerySceneBO();
        querySceneBO.setServiceName(params.get("serviceName"));
        querySceneBO.setClassName(params.get("className"));
        querySceneBO.setMethodName(params.get("methodName"));
        querySceneBO.setPage(1);
        querySceneBO.setPageSize(1000);
        AresBrainResult<QueryLinkVO> result = aresBrainLinkService.queryLinkDetail(querySceneBO);
        if (result.getCode() != 0) {
            throw new RRException("调用分析中心查询链路详情失败：" + result.getMessage());
        } else {
            if (result.getData().getTotalNum() != 0) {
                List<SceneVOModel> lists = result.getData().getLinkAnalysisVOModels();
                for (SceneVOModel sceneVOModel : lists) {
                    if (!sceneLinkMap.containsKey(sceneVOModel.getStrId())) {
                        sceneLinkMap.put(sceneVOModel.getStrId(), JSON.toJSONString(sceneVOModel));
                    }
                }
            }
        }

        for (int k = 0; k < api.size(); k++) {
            JSONArray scene = api.getJSONObject(k).getJSONArray("scene");
            for (int m = 0; m < scene.size(); m++) {
                JSONObject strIdJson = JSONObject.parseObject(scene.getString(m));

                if (!sceneLinkMap.containsKey(strIdJson.getString("strId"))) {
                    strIdJson.put("isExist", false);
                    sceneLinkMap.put(strIdJson.getString("strId"), JSON.toJSONString(strIdJson));
                }
            }
        }

        Map<String, JSONArray> apiMapNew = new HashMap<>();
        for (String key : sceneLinkMap.keySet()) {
            JSONObject o = JSONObject.parseObject(sceneLinkMap.get(key));
            String url = o.getString("method") + " " + o.getString("api");
            if (!apiMapNew.containsKey(url)) {
                apiMapNew.put(url, new JSONArray());
            }
            apiMapNew.get(url).add(sceneLinkMap.get(key));
        }

        JSONArray resultNew = new JSONArray();

        for (String key : apiMapNew.keySet()) {
            JSONObject o = new JSONObject();
            o.put("url", key);
            o.put("scene", apiMapNew.get(key));
            resultNew.add(o);
        }
        api = resultNew;
    }

    /**
     * 查询服务差异统计列表
     *
     * 根据查询条件分页查询各服务的差异分析统计数据，包括服务名称、
     * 分析次数、成功率、API数量、场景数量等统计信息
     *
     * @param query 查询条件对象，包含分页参数、过滤条件等
     * @return PageUtils 分页结果对象，包含统计数据列表和总数信息
     */
    @Override
    public PageUtils queryServiceList(Query query) {
        List<Map<String, Object>> array = diffRecordMapper.queryDiffStats(query);
        int total = diffRecordMapper.queryDiffStatsTotal(query);
        return new PageUtils(array, total, query.getLimit(), query.getPage());
    }

    /**
     * 查询最新差异分析记录列表
     *
     * 根据查询条件获取各服务最新的差异分析记录，通常用于展示每个服务
     * 最近一次的差异分析结果
     *
     * @param query 查询条件对象，包含分页参数、服务过滤条件等
     * @return PageUtils 分页结果对象，包含最新差异分析记录列表
     */
    @Override
    public PageUtils queryLatestList(Query query) {
        List<DiffRecordEntity> list = diffRecordMapper.queryLatestList(query);
        return new PageUtils(list, 0, query.getLimit(), query.getPage());
    }

    /**
     * 查询差异分析记录列表
     *
     * 根据查询条件获取差异分析记录的完整列表，支持按服务名称、
     * 时间范围等条件进行过滤和分页查询
     *
     * @param query 查询条件对象，包含分页参数、服务名称、时间范围等过滤条件
     * @return PageUtils 分页结果对象，包含差异分析记录列表
     */
    @Override
    public PageUtils queryList(Query query) {
//        DiffRecordEntity recordEntity = new DiffRecordEntity();
//        recordEntity.setId(1L);
//        recordEntity.setIsEpaas(1);
//        diffRecordMapper.updateRestResult(recordEntity);
        List<DiffRecordEntity> list = diffRecordMapper.queryList(query);
        return new PageUtils(list, 0, query.getLimit(), query.getPage());
    }


    /**
     * 根据ID查询差异分析详情
     *
     * 查询指定ID的差异分析详细结果，包括代码变更详情、API影响分析、
     * 场景关联信息等。对于epaas类型的数据，会自动处理status字段
     * 的注入和过滤
     *
     * 处理步骤：
     * 1. 查询基础详情数据
     * 2. 处理epaas数据，注入status字段
     * 3. 过滤不匹配的场景数据
     *
     * @param id 差异分析结果详情的ID
     * @return DiffResultDetailEntity 差异分析详情实体，包含完整的分析结果数据
     */
    @Override
    public DiffResultDetailEntity queryDetailById(Long id) {
        DiffResultDetailEntity detailEntity = resultDetailMapper.queryObjectById(id);
        
        // 处理resultDetailEpaas字段，确保包含status字段
        if (detailEntity != null && detailEntity.getResultDetailEpaas() != null) {
            try {
                JSONArray epaasArray = JSONArray.parseArray(detailEntity.getResultDetailEpaas());
                if (epaasArray != null && !epaasArray.isEmpty()) {
                    // 遍历epaas数据，为每个场景注入status字段
                    for (int i = 0; i < epaasArray.size(); i++) {
                        JSONObject epaasObj = epaasArray.getJSONObject(i);
                        JSONArray methodInfos = epaasObj.getJSONArray("methodInfos");
                        
                        if (methodInfos != null && !methodInfos.isEmpty()) {
                            for (int j = 0; j < methodInfos.size(); j++) {
                                JSONObject methodInfo = methodInfos.getJSONObject(j);
                                JSONArray apiArr = methodInfo.getJSONArray("api");
                                
                                if (apiArr != null && !apiArr.isEmpty()) {
                                    for (int k = 0; k < apiArr.size(); k++) {
                                        JSONObject apiObj = apiArr.getJSONObject(k);
                                        JSONArray sceneArr = apiObj.getJSONArray("scene");
                                        
                                        if (sceneArr != null && !sceneArr.isEmpty()) {
                                            boolean sceneArrModified = false;
                                            for (int l = 0; l < sceneArr.size(); l++) {
                                                String sceneStr = sceneArr.getString(l);
                                                JSONObject sceneJson = JSONObject.parseObject(sceneStr);
                                                
                                                // 获取并设置查看状态
                                                try {
                                                    String sceneId = sceneJson.getString("sceneId");
                                                    if (sceneId != null) {
                                                        Integer status = sceneTraceIdMapper.getStatusByTraceId(sceneId);
                                                        if (status != null) {
                                                            sceneJson.put("status", status);
                                                        } else {
                                                            sceneJson.put("status", 0); // 默认未查看状态
                                                        }
                                                    } else {
                                                        sceneJson.put("status", 0); // 默认未查看状态
                                                    }
                                                } catch (Exception e) {
                                                    log.error("获取sceneId {} 的查看状态失败", sceneJson.getString("sceneId"), e);
                                                    sceneJson.put("status", 0); // 默认未查看状态
                                                }
                                                
                                                // 更新场景字符串
                                                sceneArr.set(l, sceneJson.toJSONString());
                                                sceneArrModified = true;
                                            }
                                            
                                            // 如果sceneArr被修改了，需要更新apiObj中的scene字段
                                            if (sceneArrModified) {
                                                apiObj.put("scene", sceneArr);
                                            }
                                        }
                                    }
                                }
                                
                                // 更新methodInfo中的api字段
                                methodInfo.put("api", apiArr);
                            }
                        }
                        
                        // 更新epaasObj中的methodInfos字段
                        epaasObj.put("methodInfos", methodInfos);
                    }
                    
                    // 更新resultDetailEpaas字段
                    detailEntity.setResultDetailEpaas(epaasArray.toJSONString());
                }
            } catch (Exception e) {
                log.error("处理resultDetailEpaas字段时发生错误", e);
            }
        }
        
        return detailEntity;
    }

    /**
     * 刷新差异分析结果中的场景关联信息
     *
     * 重新查询和更新指定差异分析结果中的场景关联数据，包括：
     * 1. 根据服务名称和方法信息查询最新的场景关联
     * 2. 统计API数量和场景数量
     * 3. 更新差异分析结果和统计数据
     *
     * 处理步骤：
     * 1. 查询差异分析结果详情
     * 2. 解析结果详情中的方法信息
     * 3. 根据服务名称和方法信息查询场景关联
     * 4. 统计API和场景数量
     * 5. 更新比对结果和统计数据
     *
     * @param resultId 差异分析结果详情ID
     * @param serviceName 服务名称，用于查询场景关联
     */
    @Override
    public void refreshScene(Long resultId, String serviceName) {
        DiffResultDetailEntity diffResultEntity = resultDetailMapper.queryObjectById(resultId);

        Map<String, String> existSceneLinkMap = new HashMap<>();
        JSONArray array = JSONArray.parseArray(diffResultEntity.getResultDetail());
        for (int i = 0; i < array.size(); i++) {
            JSONArray methodInfos = array.getJSONObject(i).getJSONArray("methodInfos");
            if (methodInfos != null && methodInfos.size() > 0) {
                for (int j = 0; j < methodInfos.size(); j++) {
                    Map<String, String> params = new HashMap<>();
                    params.put("serviceName", serviceName);
                    params.put("className", array.getJSONObject(i).getString("classFile").replace("/", "."));
                    params.put("methodName", methodInfos.getJSONObject(j).getString("methodName"));

                    Map<String, String> sceneLinkMap = new HashMap<>();
                    JSONArray api = methodInfos.getJSONObject(j).getJSONArray("api");
                    this.querySceneByMethod(params, sceneLinkMap, api);

                    existSceneLinkMap.putAll(sceneLinkMap);
                }
            }
        }
        List<String> apiUrl = new ArrayList<>();
        for (String key : existSceneLinkMap.keySet()) {
            JSONObject o = JSONObject.parseObject(existSceneLinkMap.get(key));
            String urlPath = o.getString("method") + " " + o.getString("api");
            if (apiUrl.indexOf(urlPath) < 0) {
                apiUrl.add(urlPath);
            }
        }

        // 更新比对结果
        JSONObject resultStats = JSONObject.parseObject(diffResultEntity.getResultStats());
        resultStats.getJSONObject("api").put("num", apiUrl.size());
        resultStats.getJSONObject("scene").put("num", existSceneLinkMap.size());
        diffResultEntity.setResultDetail(JSON.toJSONString(array));
        diffResultEntity.setResultStats(JSON.toJSONString(resultStats));
        resultDetailMapper.updateById(diffResultEntity);

        // 更新关联case

    }

    /**
     * 获取用于比较的源代码文件内容
     *
     * 根据差异分析记录ID和文件路径，获取基础版本和当前版本的源代码文件内容，
     * 用于代码差异对比展示。支持处理文件新增、删除、修改等不同类型
     *
     * 处理逻辑：
     * - ADD类型：只获取当前版本文件
     * - DELETE类型：只获取基础版本文件
     * - MODIFY类型：同时获取两个版本文件
     *
     * @param params 参数Map，包含：
     *               - id: 差异分析记录ID
     *               - filePrefix: 文件路径前缀
     *               - classFile: 类文件名
     *               - type: 变更类型（ADD/DELETE/MODIFY）
     * @return Map<String, String> 包含两个文件内容的Map：
     *         - "baseFile": 基础版本文件内容
     *         - "nowFile": 当前版本文件内容
     * @throws FileNotFoundException 当文件不存在时抛出异常
     */
    @Override
    public Map<String, String> getCompareFile(Map<String, Object> params) throws FileNotFoundException {
        DiffRecordEntity diff =
                diffRecordMapper.queryObjectById(Long.parseLong(params.get("id").toString()));
        String baseUrl =
                getHttpGitUrl(diff.getBaseGitUrl(), "", diff.getBaseVersion())
                        + "/"
                        + params.get("filePrefix") + params.get("classFile") + ".java";
        String nowUrl =
                getHttpGitUrl(diff.getNowGitUrl(), "", diff.getNowVersion())
                        + "/" + params.get("filePrefix") + params.get("classFile") + ".java";
        HttpClientRequest request = new HttpClientRequest();
        request.setType(1);
        Map<String, String> headers = new HashMap<>();
        headers.put("private-token", "ixNR5EheGrM-FK6vZfhS");
        request.setHeaders(headers);
        Map<String, String> map = new HashMap<>();
        if (!"DELETE".equals(params.get("type"))) {
            request.setUrl(nowUrl);
            HttpClientResponse response = HttpClientUtil.sendRequest(request);
            if ("200".equals(response.getStateCode())) {
                map.put("nowFile", response.getResponseBody().toString());
            } else if ("404".equals(response.getStateCode())) {
                throw new FileNotFoundException();
            } else {
                throw new RRException("调用服务异常:" + HttpClientUtil.codediffUrl);
            }
        }

        if (!"ADD".equals(params.get("type"))) {
            request.setUrl(baseUrl);
            HttpClientResponse response = HttpClientUtil.sendRequest(request);
            if ("200".equals(response.getStateCode())) {
                map.put("baseFile", response.getResponseBody().toString());
            } else if ("404".equals(response.getStateCode())) {
                throw new FileNotFoundException();
            } else {
                throw new RRException("调用服务异常:" + HttpClientUtil.codediffUrl);
            }
        }
        return map;
    }

    /**
     * 重新下载代码差异分析结果
     *
     * 根据指定的差异分析记录ID，重新执行代码差异分析过程。
     * 通常用于分析失败后重新尝试，或者需要更新分析结果时使用
     *
     * 处理步骤：
     * 1. 根据ID查询差异分析记录
     * 2. 重新调用代码差异分析接口获取结果
     * 3. 更新数据库中的分析结果
     *
     * @param id 差异分析记录的ID
     */
    @Override
    public void reDownloadCodeDiff(Long id) {
        DiffRecordEntity diff = diffRecordMapper.queryObjectById(id);
        this.getCodeDiffResult(diff);
    }

    /**
     * 查询测试相关的差异分析数据
     *
     * 根据差异分析记录实体查询测试相关数据，区分普通分析和epaas分析。
     * 提取场景关联信息、流程统计等数据，用于测试执行和结果展示
     *
     * 处理逻辑：
     * 1. 根据isEpaas标识选择不同的数据源
     * 2. 解析差异分析结果数据
     * 3. 提取场景关联和流程信息
     * 4. 统计相关数据并返回结果
     *
     * @param recordEntity 差异分析记录实体，包含ID和epaas标识等信息
     * @return JSONObject 包含测试相关数据的JSON对象，包括场景统计、流程信息等
     */
    @Override
    public JSONObject queryTest(DiffRecordEntity recordEntity) {
        if (recordEntity == null) {
            throw new RRException("参数recordEntity不能为空");
        }
        JSONObject result = new JSONObject();
        DiffRecordEntity diffRecordEntity;
        DiffResultDetailEntity diffResultEntity;
        JSONArray array;

        if (recordEntity.getIsEpaas() != null && recordEntity.getIsEpaas() == 1) {
            diffRecordEntity = diffRecordMapper.queryEpaasObjectById(recordEntity.getId(), 1);
            diffResultEntity = resultDetailMapper.queryObjectById(diffRecordEntity.getDiffDetailId());
            array = JSONArray.parseArray(diffResultEntity.getResultDetailEpaas());

        } else {
            diffRecordEntity = diffRecordMapper.queryObjectById(recordEntity.getId());
            diffResultEntity = resultDetailMapper.queryObjectById(diffRecordEntity.getDiffDetailId());
            array = JSONArray.parseArray(diffResultEntity.getResultDetail());

        }

        Map<String, String> existSceneLinkMap = new HashMap<>();
        List<Long> ids = new ArrayList<>();
        Map<Long, Integer> sceneFlows = new HashMap<>();
        for (int i = 0; i < array.size(); i++) {

            JSONArray methodInfos = array.getJSONObject(i).getJSONArray("methodInfos");

            if (methodInfos != null && methodInfos.size() > 0) {
                for (int j = 0; j < methodInfos.size(); j++) {
                    Map<String, String> sceneLinkMap = new HashMap<>();
                    existSceneLinkMap.putAll(sceneLinkMap);

                    JSONArray api = methodInfos.getJSONObject(j).getJSONArray("api");
                    if (api != null && api.size() > 0) {

                        for (int k = 0; k < api.size(); k++) {
                            JSONArray scene = api.getJSONObject(k).getJSONArray("scene");
                            for (int m = 0; m < scene.size(); m++) {
                                JSONObject strIdJson = JSONObject.parseObject(scene.getString(m));
                                Long id = strIdJson.getLong("strId");
                                if (ids.indexOf(id) == -1) {
                                    ids.add(id);
                                    sceneFlows.put(id, strIdJson.getIntValue("flow"));
                                }
                            }
                        }
                    }
                }
            }
        }
        Map<String, Object> params = new HashMap<>();
        params.put("id", ids);
        JSONArray testResult = new JSONArray();
        List<SuiteIdRepeaterModel> list = new ArrayList<>();
        try {
            if (ids.size() > 0) {
                testResult = HttpClientUtil.httpClient("sceneModel/findByIds", 2, params);
                JSONArray suitResult =
                        HttpClientUtil.httpClient("suiteIdModel/searchSuiteByIds", 2, params);

                for (int m = 0; m < suitResult.size(); m++) {
                    JSONObject suiteId = (JSONObject) suitResult.get(m);
                    SuiteIdRepeaterModel suiteIdRepeaterModel = new SuiteIdRepeaterModel();
                    suiteIdRepeaterModel.setSuiteId(suiteId.getLong("suiteId"));
                    //                    suiteIdRepeaterModel.setSceneId(sceneId);
                    Map<String, Object> BatchRepeatBody = new HashMap<>();
                    BatchRepeatBody.put("currentPage", 1);
                    BatchRepeatBody.put("pageSize", 1);
                    BatchRepeatBody.put("suiteId", suiteId.getLong("suiteId"));
                    JSONObject BatchRepeat =
                            HttpClientUtil.httpClientR(
                                    "/result/api/getBatchRepeatId", 2, BatchRepeatBody, "repeater");
                    suiteIdRepeaterModel.setSuiteName(suiteId.getString("suiteName"));
                    if (BatchRepeat.getBoolean("success")) {
                        JSONObject data = BatchRepeat.getJSONArray("data").getJSONObject(0);
                        suiteIdRepeaterModel.setBatchRepeatId(data.getString("batch_repeat_id"));
                        suiteIdRepeaterModel.setSuiteUrl(
                                ereplayFrontUrl
                                        + "#/replayresult?batchRepeatId="
                                        + data.getString("batch_repeat_id"));
                    } else {
                        suiteIdRepeaterModel.setSuiteUrl(ereplayFrontUrl + "#/TrafficCollection");
                    }
                    list.add(suiteIdRepeaterModel);
                }
            }

        } catch (Exception e) {
            log.error("查询场景链路失败", e);
        }

        JSONObject allCov =
                this.queryCovResult(
                        HttpClientUtil.ejacocoUrl + "?appName=" + recordEntity.getServiceName() + "&type=1",
                        diffRecordEntity);

        JSONObject incrementalCov =
                this.queryCovResult(
                        HttpClientUtil.ejacocoUrl + "?appName=" + recordEntity.getServiceName() + "&type=2",
                        diffRecordEntity);
        result.put("allCov", allCov);
        result.put("incrementalCov", incrementalCov);
        // 更新统计数据
        JSONArray sceneReuslt = new JSONArray();
        for (int i = 0; i < testResult.size(); i++) {
            JSONObject jsonObject = testResult.getJSONObject(i);
            jsonObject.put("flow", sceneFlows.get(jsonObject.getLong("strId")));
            sceneReuslt.add(jsonObject);
        }
        result.put("test", sceneReuslt);
        result.put("suite", list);
        return result;
    }

    private JSONObject queryCovResult(String url, DiffRecordEntity diff) {
        HttpClientRequest request = new HttpClientRequest();
        request.setType(1);
        request.setUrl(url);
        HttpClientResponse response = HttpClientUtil.sendRequest(request);
        if (response.getResponseBody() == null) {
            throw new RRException("调用服务ejacoco异常");
        }
        JSONObject json = JSONObject.parseObject(response.getResponseBody().toString());
        if (json.getIntValue("code") != 200) {
            throw new RRException("访问ejacoco异常:" + json.getString("msg"));
        }
        JSONArray data = json.getJSONArray("data");

        for (int i = 0; i < data.size(); i++) {
            JSONObject jsonItem = data.getJSONObject(i);
            if (diff.getBaseVersion().equals(jsonItem.getString("onlineCommitId"))
                    && diff.getNowVersion().equals(jsonItem.getString("commitId"))
                    && diff.getEnvCode().equals(jsonItem.getString("envCode"))) {
                return jsonItem;
            }
        }
        return new JSONObject();
    }

    private String getHttpLabJavaUrl(String repoUrl, String commitId, Map<String, Object> params) {
        StringBuilder httpUrl = new StringBuilder("http://YOUR_GIT_SERVER:8081");
        if (Strings.isNullOrEmpty(repoUrl)) {
            return "";
        }
        String repoName = repoUrl.split(":")[1].split(".git")[0];
        httpUrl.append("/").append(repoName).append("/blob/").append(commitId);

        String filePrefix = params.get("filePrefix").toString();
        httpUrl
                .append("/")
                .append(filePrefix)
                .append(params.get("classFile").toString())
                .append(".java");
        return httpUrl.toString();
    }

    public static String getUrlLocalDir(String repoUrl, String localBaseRepoDir, String version) {
        StringBuilder localDir = new StringBuilder(localBaseRepoDir);
        if (Strings.isNullOrEmpty(repoUrl)) {
            return "";
        }
        localDir.append("/");
        String repoName = repoUrl.split(":")[1].split(".git")[0];
        localDir.append(repoName);
        if (!StringUtils.isEmpty(version)) {
            localDir.append("/");
            localDir.append(version);
        }
        return localDir.toString();
    }

    /**
     * 取远程代码本地存储路径
     *
     * @param repoUrl
     * @param localBaseRepoDir
     * @param version
     * @return
     */
    public static String getLocalDir(String repoUrl, String localBaseRepoDir, String version) {
        StringBuilder localDir = new StringBuilder(localBaseRepoDir);
        if (Strings.isNullOrEmpty(repoUrl)) {
            return "";
        }
        localDir.append("/");
        String repoName = repoUrl.split("/")[1].split(".git")[0];
        localDir.append(repoName);
        if (!StringUtils.isEmpty(version)) {
            localDir.append("/");
            localDir.append(version);
        }
        return localDir.toString();
    }

    /**
     * 取远程代码http请求url
     *
     * @param repoUrl
     * @param localBaseRepoDir
     * @param version
     * @return
     */
    public static String getHttpGitUrl(String repoUrl, String localBaseRepoDir, String version) {
        StringBuilder gitHttpUrl = new StringBuilder(localBaseRepoDir);
        if (Strings.isNullOrEmpty(repoUrl)) {
            return "";
        }
        String repoName = repoUrl.split("\\.git")[0].replace("git@YOUR_GIT_SERVER:", "http://YOUR_GIT_SERVER:8081/");
        gitHttpUrl.append(repoName).append("/raw");
        if (!StringUtils.isEmpty(version)) {
            gitHttpUrl.append("/");
            gitHttpUrl.append(version);
        }
        return gitHttpUrl.toString();
    }

    /**
     * 获取参数
     *
     * @param searchModel
     * @param parameters
     * @return
     */
    private String getParams(SearchModel searchModel, String parameters) {
        //根据服务 类 方法获取参数类型
        List<MethodParametersModel> parametersModels = brainSceneService.queryMehtodParameters(searchModel);
        String[] paramesDiff = StringUtils.split(parameters, "&");
        for (MethodParametersModel parametersModel : parametersModels) {
            String[] paramsModel = StringUtils.split(parametersModel.getParameters(), ",");
            if (paramsModel.length == paramesDiff.length) {
                //个数相等才做判断
                boolean flag = true;
                for (int index = 0; index < paramesDiff.length; index++) {
                    //只获取类型，比如List<BizApprovalFlowBaseInfoListOutput>，获取取List
                    String paramesDiffTemp = StringUtils.substringBefore(paramesDiff[index], "<");
                    if ((index == paramesDiff.length - 1) ? !paramsModel[index].endsWith("." + paramesDiffTemp + "\"]")
                            : !paramsModel[index].endsWith("." + paramesDiffTemp + "\"")) {
                        //如果相同顺序的参数不一致，则跳出
                        flag = false;
                        break;
                    }
                }
                if (flag) {
                    //参数顺序和值都一样则返回参数类型
                    return parametersModel.getParameters();
                }
            }
        }
        return "";
    }

    /**
     * 参数类型，用于圈复杂度查询
     *
     * @param parameters
     * @return
     */
    private String getParams(String parameters) {
        String result = "(";
        String[] parametersArr = StringUtils.split(parameters, "&");
        for (int i = 0; i < parametersArr.length; i++) {
            if (i == parametersArr.length - 1) {
                result += StringUtils.substringBefore(parametersArr[i], "<") + ")";
            } else {
                result += StringUtils.substringBefore(parametersArr[i], "<") + ", ";
            }
        }
        return result;
    }
    /**
     * 检查给定的API路径是否在TARGET_API配置中
     * @param apiPath 要检查的API路径
     * @return 如果在TARGET_API配置中返回true，否则返回false
     */
    private boolean isTargetApi(String apiPath) {
        if (TARGET_API == null || TARGET_API.isEmpty() || apiPath == null) {
            return false;
        }

        String[] apis = TARGET_API.split(",");
        for (String api : apis) {
            // 提取API路径部分（去除可能的HTTP方法前缀）
            String targetApiPath = api.trim();
            if (targetApiPath.contains(" ")) {
                targetApiPath = targetApiPath.substring(targetApiPath.indexOf(" ") + 1);
            }

            // 完全匹配检查
            if (targetApiPath.equals(apiPath)) {
                return true;
            }
        }
        return false;
    }
    /**
     * 对epaas数据进行去重处理
     * @param epaasArray 包含重复数据的epaas数组
     * @return 去重后的epaas数组
     */
    private JSONArray deduplicateEpaasData(JSONArray epaasArray) {
        if (epaasArray == null || epaasArray.isEmpty()) {
            return epaasArray;
        }
        
        log.info("开始对epaas数据进行去重处理，原始数据条数: {}", epaasArray.size());
        
        JSONArray deduplicatedArray = new JSONArray();
        
        // 为每个epaas对象创建唯一标识，包括所有traceId信息
        for (int i = 0; i < epaasArray.size(); i++) {
            JSONObject epaasObj = epaasArray.getJSONObject(i);
            
            // 收集该对象的所有traceId
            Set<String> traceIds = new HashSet<>();
            JSONArray methodInfos = epaasObj.getJSONArray("methodInfos");
            if (methodInfos != null) {
                for (int j = 0; j < methodInfos.size(); j++) {
                    JSONObject methodInfo = methodInfos.getJSONObject(j);
                    JSONArray apiArr = methodInfo.getJSONArray("api");
                    if (apiArr != null) {
                        for (int k = 0; k < apiArr.size(); k++) {
                            JSONObject apiInfo = apiArr.getJSONObject(k);
                            JSONArray sceneArr = apiInfo.getJSONArray("scene");
                            if (sceneArr != null) {
                                for (int l = 0; l < sceneArr.size(); l++) {
                                    String sceneStr = sceneArr.getString(l);
                                    try {
                                        JSONObject sceneJson = JSONObject.parseObject(sceneStr);
                                        String traceId = sceneJson.getString("sceneId");
                                        if (traceId != null) {
                                            traceIds.add(traceId);
                                        }
                                    } catch (Exception e) {
                                        log.warn("解析scene JSON失败: {}", sceneStr, e);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // 检查是否已存在相同的对象（基于classFile、moduleName、lines和traceIds）
            boolean isDuplicate = false;
            for (int idx = 0; idx < deduplicatedArray.size(); idx++) {
                JSONObject existingObj = deduplicatedArray.getJSONObject(idx);
                
                // 比较基本属性
                if (Objects.equals(epaasObj.getString("classFile"), existingObj.getString("classFile")) &&
                    Objects.equals(epaasObj.getString("moduleName"), existingObj.getString("moduleName"))) {
                    
                    // 比较lines数组
                    JSONArray lines1 = epaasObj.getJSONArray("lines");
                    JSONArray lines2 = existingObj.getJSONArray("lines");
                    if (linesAreEqual(lines1, lines2)) {
                        // 比较traceIds
                        Set<String> existingTraceIds = extractTraceIds(existingObj);
                        if (traceIds.equals(existingTraceIds)) {
                            isDuplicate = true;
                            log.info("发现重复的epaas对象，classFile: {}, traceIds: {}", epaasObj.getString("classFile"), traceIds);
                            break;
                        }
                    }
                }
            }
            
            // 如果不是重复的，则添加到结果数组中
            if (!isDuplicate) {
                deduplicatedArray.add(epaasObj);
                log.info("添加了epaas对象，classFile: {}, traceIds: {}", epaasObj.getString("classFile"), traceIds);
            }
        }
        
        log.info("去重处理完成，去重后数据条数: {}", deduplicatedArray.size());
        return deduplicatedArray;
    }

    /**
     * 比较两个lines数组是否相等
     */
    private boolean linesAreEqual(JSONArray lines1, JSONArray lines2) {
        if (lines1 == null && lines2 == null) return true;
        if (lines1 == null || lines2 == null) return false;
        if (lines1.size() != lines2.size()) return false;
        
        // 对lines进行排序以确保一致性
        JSONArray sortedLines1 = new JSONArray();
        JSONArray sortedLines2 = new JSONArray();
        
        for (int i = 0; i < lines1.size(); i++) {
            sortedLines1.add(lines1.getJSONObject(i));
        }
        for (int i = 0; i < lines2.size(); i++) {
            sortedLines2.add(lines2.getJSONObject(i));
        }
        
        sortedLines1.sort(Comparator.comparing(obj -> ((JSONObject) obj).getIntValue("startLineNum")));
        sortedLines2.sort(Comparator.comparing(obj -> ((JSONObject) obj).getIntValue("startLineNum")));
        
        // 比较每个元素
        for (int i = 0; i < sortedLines1.size(); i++) {
            JSONObject line1 = sortedLines1.getJSONObject(i);
            JSONObject line2 = sortedLines2.getJSONObject(i);
            
            if (!Objects.equals(line1.getIntValue("startLineNum"), line2.getIntValue("startLineNum")) ||
                !Objects.equals(line1.getIntValue("endLineNum"), line2.getIntValue("endLineNum")) ||
                !Objects.equals(line1.getString("type"), line2.getString("type"))) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * 从epaas对象中提取所有traceId
     */
    private Set<String> extractTraceIds(JSONObject epaasObj) {
        Set<String> traceIds = new HashSet<>();
        JSONArray methodInfos = epaasObj.getJSONArray("methodInfos");
        if (methodInfos != null) {
            for (int j = 0; j < methodInfos.size(); j++) {
                JSONObject methodInfo = methodInfos.getJSONObject(j);
                JSONArray apiArr = methodInfo.getJSONArray("api");
                if (apiArr != null) {
                    for (int k = 0; k < apiArr.size(); k++) {
                        JSONObject apiInfo = apiArr.getJSONObject(k);
                        JSONArray sceneArr = apiInfo.getJSONArray("scene");
                        if (sceneArr != null) {
                            for (int l = 0; l < sceneArr.size(); l++) {
                                String sceneStr = sceneArr.getString(l);
                                try {
                                    JSONObject sceneJson = JSONObject.parseObject(sceneStr);
                                    String traceId = sceneJson.getString("sceneId");
                                    if (traceId != null) {
                                        traceIds.add(traceId);
                                    }
                                } catch (Exception e) {
                                    log.warn("解析scene JSON失败: {}", sceneStr, e);
                                }
                            }
                        }
                    }
                }
            }
        }
        return traceIds;
    }

    private JSONArray processTraceIdMatching(JSONArray resData, String traceId, DiffRecordEntity recordEntity) {
        JSONArray epaasJsonArray = new JSONArray();

        for (int i = 0; i < resData.size(); i++) {
            JSONObject o = resData.getJSONObject(i);
            JSONArray methodInfos = o.getJSONArray("methodInfos");
            if (methodInfos != null && methodInfos.size() > 0) {
                // 为当前类预先创建epaas对象
                JSONObject epaasObj = new JSONObject();
                epaasObj.put("classFile", o.getString("classFile"));
                epaasObj.put("filePrefix", o.getString("filePrefix"));
                epaasObj.put("lines", o.getJSONArray("lines"));
                epaasObj.put("moduleName", o.getString("moduleName"));
                epaasObj.put("type", o.getString("type"));
                epaasObj.put("classRiskLevel", o.getJSONObject("classRiskLevel"));
                
                // 存储过滤后的方法信息
                JSONArray filteredMethodInfos = new JSONArray();
                
                // 遍历类中的所有方法
                for (int j = 0; j < methodInfos.size(); j++) {
                    JSONObject mItem = methodInfos.getJSONObject(j);
                    JSONArray apiArr = mItem.getJSONArray("api");
                    
                    if (apiArr != null && apiArr.size() > 0) {
                        // 存储过滤后的API信息
                        JSONArray filteredApis = new JSONArray();
                        
                        // 遍历方法中的所有API
                        for (int k = 0; k < apiArr.size(); k++) {
                            JSONObject apiItem = apiArr.getJSONObject(k);
                            String apiUrl = apiItem.getString("url");
                            String apiPath = apiUrl;
                            
                            // 提取API路径部分（去除HTTP方法前缀）
                            if (apiPath != null && apiPath.contains(" ")) {
                                apiPath = apiPath.substring(apiPath.indexOf(" ") + 1);
                            }

                            // 检查当前API是否在TARGET_API配置中
                            boolean isEpaasApi = isTargetApi(apiPath);

                            if (isEpaasApi) {
                                JSONArray sceneArr = apiItem.getJSONArray("scene");
                                
                                if (sceneArr != null && sceneArr.size() > 0) {
                                    // 存储匹配traceId的场景
                                    JSONArray matchedScenes = new JSONArray();
                                    JSONArray matchedSceneStrings = new JSONArray(); // 用于存储原始JSON字符串
                                    
                                    // 遍历API中的所有场景，只保留匹配traceId的场景
                                    for (int l = 0; l < sceneArr.size(); l++) {
                                        String sceneStr = sceneArr.getString(l);
                                        try {
                                            JSONObject sceneJson = JSONObject.parseObject(sceneStr);
                                            String sceneId = sceneJson.getString("sceneId");

                                            // 只有场景ID匹配时才添加
                                            if (traceId != null && traceId.equals(sceneId)) {
                                                matchedScenes.add(sceneJson);
                                                matchedSceneStrings.add(sceneStr); // 保持原始JSON字符串格式
                                            }
                                        } catch (Exception e) {
                                            log.warn("解析scene JSON失败: {}", sceneStr, e);
                                            continue;
                                        }
                                    }
                                    
                                    // 只有当存在匹配的场景时才添加此API
                                    if (matchedScenes.size() > 0) {
                                        JSONObject filteredApi = new JSONObject();
                                        filteredApi.put("url", apiUrl);
                                        // 保持原始JSON字符串格式，而不是解析后的对象
                                        filteredApi.put("scene", matchedSceneStrings);
                                        filteredApis.add(filteredApi);
                                        log.info("为API {} 添加了 {} 个匹配的场景", apiUrl, matchedScenes.size());
                                    }
                                }
                            }
                        }
                        
                        // 只有当方法中存在匹配的API时才添加此方法
                        if (filteredApis.size() > 0) {
                            JSONObject filteredMethod = new JSONObject();
                            filteredMethod.put("methodName", mItem.getString("methodName"));
                            filteredMethod.put("parameters", mItem.getString("parameters"));
                            filteredMethod.put("type", mItem.getString("type"));
                            filteredMethod.put("riskLevel", mItem.getJSONObject("riskLevel"));
                            filteredMethod.put("api", filteredApis);
                            filteredMethodInfos.add(filteredMethod);
                        }
                    }
                }
                
                // 只有当类中存在匹配的方法时才添加此类
                if (filteredMethodInfos.size() > 0) {
                    epaasObj.put("methodInfos", filteredMethodInfos);
                    epaasJsonArray.add(epaasObj);
                }
            }
        }

        log.info("处理完成，总共返回 {} 个epaas对象", epaasJsonArray.size());
        return epaasJsonArray;
    }


    /**
     * 更新traceId的状态
     *
     * 更新指定traceId的查看状态，用于追踪场景执行的查看记录。
     * 如果traceId不存在则创建新记录，存在则更新状态和操作人信息
     *
     * 处理逻辑：
     * 1. 检查traceId记录是否存在
     * 2. 不存在时创建新记录
     * 3. 存在时更新状态和操作人
     *
     * @param traceId 场景执行的traceId标识
     * @param status 状态值（0:未查看, 1:已查看）
     * @param operator 操作人员标识
     * @return boolean 更新是否成功，true表示成功
     */
    @Override
    public boolean updateTraceIdStatus(String traceId, int status, String operator) {
        try {
            // 检查是否存在该traceId的记录
            List<SceneTraceid> sceneTraceids = sceneTraceIdMapper.getSceneTraceIds(traceId);
            if (sceneTraceids == null || sceneTraceids.isEmpty()) {
                // 如果不存在，创建新记录
                SceneTraceid newRecord = new SceneTraceid();
                newRecord.setTraceid(traceId);
                newRecord.setStatus(status);
                newRecord.setOperator(operator);
                int result = sceneTraceIdMapper.insert(newRecord);
                log.info("创建新的traceId记录 {}，状态: {}，影响行数: {}", traceId, status, result);
                return result > 0;
            } else {
                // 更新数据库中的状态和操作用户
                int result = sceneTraceIdMapper.updateStatus(traceId, status,operator);
                log.info("更新traceId {} 的状态为 {}，操作用户: {}，影响行数: {}", traceId, status, operator, result);
                return result > 0;
            }
        } catch (Exception e) {
            log.error("更新traceId {} 的状态失败", traceId, e);
            return false;
        }
    }
    


    /**
     * 获取traceId的查看状态
     *
     * 查询指定traceId的查看状态，用于判断场景执行结果是否已被查看。
     * 支持状态为1表示已查看，其他状态或不存在记录表示未查看
     *
     * @param traceId 场景执行的traceId标识
     * @return boolean 是否已查看，true表示已查看，false表示未查看或查询失败
     */
    @Override
    public boolean getViewedStatus(String traceId) {
        try {
            Integer status = sceneTraceIdMapper.getStatusByTraceId(traceId);
            // 状态为1或2表示已查看
            return status != null && (status == 1);
        } catch (Exception e) {
            log.error("获取traceId {} 的查看状态失败", traceId, e);
            return false;
        }
    }
    
    /**
     * 合并epaas数据，保留不同traceId的数据
     * @param existingEpaasArray 现有的epaas数据数组
     * @param newEpaasArray 新的epaas数据数组
     */
    private void mergeEpaasData(JSONArray existingEpaasArray, JSONArray newEpaasArray) {
        log.info("开始合并epaas数据，现有数据条数: {}, 新数据条数: {}", existingEpaasArray.size(), newEpaasArray.size());
        
        // 遍历新数据
        for (int i = 0; i < newEpaasArray.size(); i++) {
            JSONObject newEpaasObj = newEpaasArray.getJSONObject(i);
            boolean found = false;
            
            // 在现有数据中查找相同classFile的对象
            for (int j = 0; j < existingEpaasArray.size(); j++) {
                JSONObject existingEpaasObj = existingEpaasArray.getJSONObject(j);
                
                // 检查是否为同一类文件
                if (Objects.equals(newEpaasObj.getString("classFile"), existingEpaasObj.getString("classFile"))) {
                    found = true;
                    
                    // 合并methodInfos
                    JSONArray existingMethodInfos = existingEpaasObj.getJSONArray("methodInfos");
                    JSONArray newMethodInfos = newEpaasObj.getJSONArray("methodInfos");
                    
                    if (newMethodInfos != null) {
                        if (existingMethodInfos == null) {
                            existingEpaasObj.put("methodInfos", newMethodInfos);
                            log.info("为类 {} 添加了新的methodInfos", newEpaasObj.getString("classFile"));
                        } else {
                            // 合并方法信息，但保留不同traceId的场景
                            mergeMethodInfos(existingMethodInfos, newMethodInfos);
                        }
                    }
                    break;
                }
            }
            
            // 如果没找到相同的类，则直接添加
            if (!found) {
                existingEpaasArray.add(newEpaasObj);
                log.info("添加了新的类: {}", newEpaasObj.getString("classFile"));
            }
        }
        log.info("合并epaas数据完成，总条数: {}", existingEpaasArray.size());
    }
    
    /**
     * 合并方法信息，保留不同traceId的场景
     * @param existingMethodInfos 现有的方法信息数组
     * @param newMethodInfos 新的方法信息数组
     */
    private void mergeMethodInfos(JSONArray existingMethodInfos, JSONArray newMethodInfos) {

        // 遍历新方法信息
        for (int i = 0; i < newMethodInfos.size(); i++) {
            JSONObject newMethodInfo = newMethodInfos.getJSONObject(i);
            boolean found = false;
            
            // 在现有方法信息中查找相同方法
            for (int j = 0; j < existingMethodInfos.size(); j++) {
                JSONObject existingMethodInfo = existingMethodInfos.getJSONObject(j);
                
                // 检查是否为同一方法
                if (Objects.equals(newMethodInfo.getString("methodName"), existingMethodInfo.getString("methodName")) &&
                    Objects.equals(newMethodInfo.getString("parameters"), existingMethodInfo.getString("parameters"))) {
                    found = true;
                    
                    // 合并API信息
                    JSONArray existingApis = existingMethodInfo.getJSONArray("api");
                    JSONArray newApis = newMethodInfo.getJSONArray("api");
                    
                    if (newApis != null) {
                        if (existingApis == null) {
                            existingMethodInfo.put("api", newApis);
                            log.info("为方法 {} 添加了新的API信息", newMethodInfo.getString("methodName"));
                        } else {
                            // 合并API信息，保留不同traceId的场景
                            mergeApiInfos(existingApis, newApis);
                        }
                    }
                    break;
                }
            }
            
            // 如果没找到相同的方法，则直接添加
            if (!found) {
                existingMethodInfos.add(newMethodInfo);
                log.info("添加了新的方法: {}", newMethodInfo.getString("methodName"));
            }
        }
    }
    
    /**
     * 合并API信息，保留不同traceId的场景
     * @param existingApis 现有的API信息数组
     * @param newApis 新的API信息数组
     */
    private void mergeApiInfos(JSONArray existingApis, JSONArray newApis) {

        // 遍历新API信息
        for (int i = 0; i < newApis.size(); i++) {
            JSONObject newApi = newApis.getJSONObject(i);
            boolean found = false;
            
            // 在现有API信息中查找相同URL的API
            for (int j = 0; j < existingApis.size(); j++) {
                JSONObject existingApi = existingApis.getJSONObject(j);
                
                // 检查是否为同一API URL
                if (Objects.equals(newApi.getString("url"), existingApi.getString("url"))) {
                    found = true;
                    
                    // 合并场景信息，保留不同traceId的场景
                    JSONArray existingScenes = existingApi.getJSONArray("scene");
                    JSONArray newScenes = newApi.getJSONArray("scene");
                    
                    if (newScenes != null) {
                        if (existingScenes == null) {
                            existingApi.put("scene", newScenes);
                        } else {
                            // 合并场景，保留不同traceId的场景
                            mergeScenes(existingScenes, newScenes);
                        }
                    }
                    break;
                }
            }
            
            // 如果没找到相同的API，则直接添加
            if (!found) {
                existingApis.add(newApi);
            }
        }
    }
    
    /**
     * 合并场景信息，保留不同traceId的场景
     * @param existingScenes 现有的场景数组
     * @param newScenes 新的场景数组
     */
    private void mergeScenes(JSONArray existingScenes, JSONArray newScenes) {
        log.info("开始合并场景信息，现有场景数: {}, 新场景数: {}", existingScenes.size(), newScenes.size());
        
        // 遍历新场景
        for (int i = 0; i < newScenes.size(); i++) {
            String newSceneStr = newScenes.getString(i);
            boolean found = false;
            
            try {
                JSONObject newScene = JSONObject.parseObject(newSceneStr);
                String newSceneId = newScene.getString("sceneId");
                
                // 在现有场景中查找相同traceId的场景
                for (int j = 0; j < existingScenes.size(); j++) {
                    String existingSceneStr = existingScenes.getString(j);
                    JSONObject existingScene = JSONObject.parseObject(existingSceneStr);
                    String existingSceneId = existingScene.getString("sceneId");
                    
                    // 检查是否为同一traceId
                    if (Objects.equals(newSceneId, existingSceneId)) {
                        found = true;
                        // 如果是同一traceId，则替换现有场景
                        existingScenes.set(j, newSceneStr);
                        break;
                    }
                }
                
                // 如果没找到相同的traceId，则添加新场景
                // 这里是关键修改：即使是相同信息但不同traceId的场景，也应该添加而不是替换
                if (!found) {
                    existingScenes.add(newSceneStr);
                }
            } catch (Exception e) {
                log.warn("解析场景JSON失败，直接添加场景字符串", e);
                // 如果解析失败，直接添加场景字符串
                existingScenes.add(newSceneStr);
                log.info("直接添加了场景字符串");
            }
        }
    }

}