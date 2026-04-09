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

package io.deepcover.brain.dal.entity;

import com.alibaba.fastjson.JSONArray;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 代理上报信息实体类
 * 用于记录Ares代理向服务端上报的心跳和状态信息，包括服务IP、环境、版本、模块信息等
 * 支持实时监控代理的运行状态和上报频率，确保代理服务的正常运行和数据采集的稳定性
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
@Data
public class ReportAgentEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 服务id
     */
    private Long id;

    /**
     * 服务本身ip
     */
    private String ip;

    /**
     * 环境
     */
    private String envCode;
    /**
     * 配置版本号
     */
    private Integer version;

    /**
     * 服务名
     */
    private String serviceName;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 模块信息（JSON格式）
     */
    private JSONArray moduleInfo;

    /**
     * 上报次数
     */
    private Long reportCount;

    /**
     * 创建时间
     */
    private Date createdTime;

    /**
     * 更新时间
     */
    private Date modifiedTime;

}
