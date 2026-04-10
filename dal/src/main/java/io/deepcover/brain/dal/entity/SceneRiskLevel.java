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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 场景风险等级实体类
 * 用于记录服务的场景风险等级配置，包括低分位数和高分位数设置
 * 支持对服务的风险等级进行量化评估，为场景分析和风险管控提供数据基础
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
@Setter
@Getter
@EqualsAndHashCode(callSuper = true)
public class SceneRiskLevel extends Model {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 低分位数阈值
     */
    private int lowerQuantile;

    /**
     * 高分位数阈值
     */
    private int higherQuantile;

    /**
     * 修改时间
     */
    private Date modifyDate;


}
