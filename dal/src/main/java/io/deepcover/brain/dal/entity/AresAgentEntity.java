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

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * Ares代理实体类
 * 用于管理Ares代码采集代理的配置信息，包括服务名称、采集配置、过滤规则、异常处理等
 * 支持对Java服务进行代码级别的监控和数据采集，为代码质量分析提供数据基础
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AresAgentEntity extends Model {

    private static final long serialVersionUID = 1L;

    /**
     * 主键自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 在线节点数
     */
    private Integer onLineNum;

    /**
     * 映射的包名
     */
    private String packageName;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 过滤不需要采集的接口
     */
    private String ignoreUrls;

    /**
     * 过滤不需要采集的类
     */
    private String ignoreClasses;

    /**
     * 过滤不需要采集的方法
     */
    private String ignoreMethods;

    /**
     * 过滤不需要采集的注解
     */
    private String ignoreAnnos;

    /**
     * 采样率
     */
    private Long sampleRate;

    /**
     * 采集的方法数限制
     */
    private Long limitCodeMethodSize;

    /**
     * 方法行限制
     */
    private Long limitCodeMethodLineSize;

    /**
     * 服务状态上报的频率，单位是秒
     */
    private Integer reportPeriod;

    /**
     * 异常阈值，超过则熔断不采集
     */
    private Long exceptionThreshold;

    /**
     * 异常计算的时间，单位min
     */
    private Long exceptionCalcTime;

    /**
     * 熔断后暂停采集的时间
     */
    private Long exceptionPauseTime;

    /**
     * 发送到数据中心方式：0-不发送，1-http,2-kafka
     */
    private Integer sendDataCenterType;

    private Integer queueNum;

    private Integer queueSize;

    private Integer queueMsgSize;

    private Integer queueRecycleTime;

    /**
     * 状态：0-初始化，1-采集中，2-停止采集
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createdTime;

    /**
     * 更新时间
     */
    private Date modifiedTime;

    /**
     * 创建用户
     */
    private String addBy;

    /**
     * 更新用户
     */
    private String updateBy;

    /**
     * 本地化标识：0-非本地化，1-本地化
     */
    private Integer local;
}
