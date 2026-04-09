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

import com.alibaba.fastjson2.JSONObject;
import io.deepcover.brain.dal.entity.*;
import io.deepcover.brain.model.SceneModel;
import io.deepcover.brain.service.util.PageUtils;
import io.deepcover.brain.service.util.Query;

import java.io.FileNotFoundException;
import java.util.Map;

/**
 * 代码差异分析服务接口
 *
 * 提供代码差异分析的核心功能，包括差异记录管理、分析结果查询、
 * 预处理等功能
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
public interface DiffAnalyseService {

    /**
     * 预添加差异分析记录
     *
     * @param recordEntity 差异记录实体
     * @return Boolean 返回预处理结果
     */
    Boolean preAdd(DiffRecordEntity recordEntity);

    /**
     * 添加差异分析数据
     *
     * 异步处理差异分析数据，生成分析报告
     *
     * @param recordEntity 差异记录实体
     * @param dingMsgExist 是否存在钉钉消息
     */
    void addDiffAnalyseData(DiffRecordEntity recordEntity, Boolean dingMsgExist);

    /**
     * 新增差异分析数据
     *
     * 处理新的差异分析数据，支持链路追踪
     *
     * @param recordEntity 差异记录实体
     * @param dingMsgExist 是否存在钉钉消息
     * @param traceId 链路追踪ID
     */
    void newDiffAnalyseData(DiffRecordEntity recordEntity, Boolean dingMsgExist, String traceId);

    /**
     * 根据ID添加差异分析
     *
     * @param id 差异记录ID
     */
    void addById(Long id);

    /**
     * 查询服务差异列表
     *
     * @param query 查询条件
     * @return PageUtils 返回分页结果
     */
    PageUtils queryServiceList(Query query);

    /**
     * 查询最新差异列表
     *
     * @param query 查询条件
     * @return PageUtils 返回分页结果
     */
    PageUtils queryLatestList(Query query);

    /**
     * 查询差异列表
     *
     * @param query 查询条件
     * @return PageUtils 返回分页结果
     */
    PageUtils queryList(Query query);

    /**
     * 根据ID查询差异详情
     *
     * @param id 差异记录ID
     * @return DiffResultDetailEntity 返回差异详情实体
     */
    DiffResultDetailEntity queryDetailById(Long id);

    /**
     * 刷新场景信息
     *
     * <p>根据差异记录ID和服务名，刷新相关的场景信息数据。该方法负责：</p>
     * <ul>
     *   <li>更新场景的最新状态信息</li>
     *   <li>同步差异分析结果到场景数据</li>
     *   <li>维护数据的一致性和完整性</li>
     * </ul>
     *
     * @param id 差异记录ID，标识需要刷新的差异分析记录
     * @param serviceName 服务名称，指定需要刷新的服务
     */
    void refreshScene(Long id,String serviceName) ;

    /**
     * 获取文件比较内容
     *
     * <p>根据指定的参数，获取文件差异比较的详细内容。该方法负责：</p>
     * <ul>
     *   <li>根据参数条件定位需要比较的文件</li>
     *   <li>获取文件的原始版本和修改版本内容</li>
     *   <li>返回文件差异的详细信息</li>
     *   <li>支持多种格式的文件比较</li>
     * </ul>
     *
     * @param params 查询参数，包含文件路径、版本信息等比较条件
     * @return Map&lt;String, String&gt; 返回文件比较结果的键值对映射
     * @throws FileNotFoundException 当指定文件不存在时抛出文件未找到异常
     */
    Map<String, String> getCompareFile(Map<String, Object> params) throws FileNotFoundException;

    /**
     * 查询测试信息
     *
     * <p>根据差异记录实体，查询相关的测试信息和数据。该方法负责：</p>
     * <ul>
     *   <li>根据差异记录获取关联的测试数据</li>
     *   <li>构建测试查询的请求参数</li>
     *   <li>调用测试服务获取相关信息</li>
     *   <li>返回格式化的测试数据结果</li>
     * </ul>
     *
     * @param recordEntity 差异记录实体，包含用于查询测试信息的相关字段
     * @return JSONObject 返回包含测试信息的JSON对象
     */
    JSONObject queryTest(DiffRecordEntity recordEntity);

    /**
     * 重新下载代码差异
     *
     * <p>根据差异记录ID，重新下载和生成代码差异分析数据。该方法负责：</p>
     * <ul>
     *   <li>根据ID查找对应的差异记录</li>
     *   <li>重新从代码仓库获取代码差异数据</li>
     *   <li>更新差异分析的结果信息</li>
     *   <li>处理下载过程中的异常情况</li>
     * </ul>
     *
     * <p>该方法通常用于修复之前下载失败的差异记录或更新过期的差异数据。</p>
     *
     * @param id 差异记录ID，指定需要重新下载差异数据的记录
     */
    void reDownloadCodeDiff(Long id);
    
    /**
     * 标记traceId
     * @param traceId traceId
     * @param status 标记状态
     * @return 是否成功
     */
    boolean updateTraceIdStatus(String traceId, int status,String operator);


    /**
     * 获取traceId的查看状态
     * @param traceId traceId
     * @return 是否已查看
     */
    boolean getViewedStatus(String traceId);
}