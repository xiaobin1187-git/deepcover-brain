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

import java.util.List;

/**
 * Ares代理批量配置实体类
 * 用于批量管理多个Ares代理的配置参数，包括采样率、上报周期、异常阈值等
 * 支持批量更新代理的监控配置，提高配置管理效率
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AresAgentBatchEntity extends Model {

    private static final long serialVersionUID = 1L;

    /**
     * 批量操作的Ares代理ID列表
     */
    @TableId(value = "ids", type = IdType.AUTO)
    private List<Long> ids;

    /**
     * 采样率
     */
    private Long sampleRate;

    /**
     * 服务状态上报的频率，单位是秒
     */
    private Integer reportPeriod;

    /**
     * 异常阈值，超过则熔断不采集
     */
    private Long exceptionThreshold;

    /**
     * 本地化标识：0-非本地化，1-本地化
     */
    private Integer local;
}
