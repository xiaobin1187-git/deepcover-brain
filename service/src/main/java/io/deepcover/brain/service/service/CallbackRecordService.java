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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * 回调记录服务
 *
 * <p>该服务负责处理系统中的回调记录和数据分析任务，作为HBase数据分析服务的入口点。主要功能包括：</p>
 * <ul>
 *   <li>接收和处理链路追踪ID(traceId)的分析请求</li>
 *   <li>协调HBase数据分析服务的执行</li>
 *   <li>提供简化的数据分析接口</li>
 *   <li>管理数据异步处理流程</li>
 *   <li>支持数据的批量分析和回调通知</li>
 * </ul>
 *
 * <p>该服务作为数据处理的门户，简化了外部系统对HBase数据分析能力的调用。
 * 通过封装复杂的数据处理逻辑，为上层应用提供简洁易用的数据服务接口。</p>
 *
 * @author system
 * @version 1.0
 * @since 2023-11-21
 */
@Slf4j
@Service
public class CallbackRecordService {

    @Autowired
    HbaseDataAnalysisService hbaseDataAnalysisService;

    /**
     * 分析HBase数据
     *
     * <p>作为HBase数据分析服务的入口点，该方法负责：</p>
     * <ul>
     *   <li>接收链路追踪ID(traceId)的分析请求</li>
     *   <li>委托给HBase数据分析服务执行具体的数据处理</li>
     *   <li>返回分析结果的状态码</li>
     *   <li>提供简化的数据分析接口</li>
     * </ul>
     *
     * <p>该方法封装了复杂的数据处理逻辑，为外部系统提供简洁易用的数据分析入口。</p>
     *
     * @param traceId 链路追踪ID，用于标识唯一的分布式调用链路
     * @return int 返回数据分析结果状态码，0表示成功，负数表示失败
     */
    public int analysisHbaseData(String traceId) {

        return hbaseDataAnalysisService.analysisHbaseData(traceId);
    }
}
