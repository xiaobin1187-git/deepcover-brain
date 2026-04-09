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

import com.alibaba.fastjson.JSONObject;
import io.deepcover.brain.dal.entity.AresAgentBatchEntity;
import io.deepcover.brain.dal.entity.AresAgentEntity;
import io.deepcover.brain.dal.entity.ReportAgentEntity;
import io.deepcover.brain.model.ServiceModel;

import java.util.List;
import java.util.Map;

/**
 * Ares代理服务接口
 *
 * 提供Ares代理的管理功能，包括代理的增删改查、服务信息同步、
 * 配置差异分析等核心业务功能
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
public interface AresAgentService {

    /**
     * 添加Ares代理配置
     *
     * @param aresAgentEntity Ares代理实体，包含应用名、包名、配置信息等
     */
    void add(AresAgentEntity aresAgentEntity);

    /**
     * 更新Ares代理配置
     *
     * @param aresAgentEntity Ares代理实体，包含需要更新的配置信息
     */
    void update(AresAgentEntity aresAgentEntity);

    /**
     * 批量更新Ares代理配置
     *
     * @param aresAgentBatchEntity 批量更新实体，包含多个代理的ID和配置
     */
    void batchUpdate(AresAgentBatchEntity aresAgentBatchEntity);

    /**
     * 查询Ares代理列表
     *
     * @param map 查询参数，支持按应用名、本地化标志等条件过滤
     * @return List<AresAgentEntity> 返回符合条件的代理列表
     */
    List<AresAgentEntity> queryList(Map<String, Object> map);

    /**
     * 查询配置差异列表
     *
     * @return JSONObject 返回包含采样率、数据中心类型、异常阈值等配置差异信息
     */
    JSONObject queryDiffList();

    /**
     * 获取所有服务名称列表
     *
     * @return List<String> 返回所有已配置的服务名称
     */
    List<String> getAllServiceName();

    /**
     * 查询Ares代理总数
     *
     * @param map 查询参数
     * @return int 返回符合条件的代理总数
     */
    int queryTotal(Map<String, Object> map);

    /**
     * 根据服务名称查询Ares代理配置
     *
     * @param serviceName 服务名称
     * @return AresAgentEntity 返回对应的代理配置信息
     */
    AresAgentEntity queryObject(String serviceName);

    /**
     * 同步服务器信息
     *
     * 异步处理上报的服务器信息，更新或插入到数据库
     *
     * @param serverEntity 服务器上报实体，包含IP、版本、状态等信息
     */
    void syncServerInfo(ReportAgentEntity serverEntity);

    /**
     * 查询上报的服务器信息列表
     *
     * @param serviceName 服务名称
     * @return List<ReportAgentEntity> 返回该服务上报的服务器信息列表
     */
    List<ReportAgentEntity> listReportServerInfo(String serviceName);

    /**
     * 查询服务列表
     *
     * @return List<ServiceModel> 返回所有服务的基本信息模型
     */
    List<ServiceModel> queryService();
}
