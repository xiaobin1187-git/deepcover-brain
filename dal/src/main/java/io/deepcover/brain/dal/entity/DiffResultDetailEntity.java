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
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 代码差异分析详情实体类
 * 用于存储代码差异分析的具体结果，包括差异内容、统计信息、详细分析结果等
 * 支持保存Epaas和非Epaas系统的差异分析结果，为代码质量评估提供详细的数据支持
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DiffResultDetailEntity extends Model {

    private static final long serialVersionUID = 1L;

    /**
     * 主键自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 代码差异
     */
    private String codeDiff;

    /**
     * 统计结果
     */
    private String resultStats;

    /**
     * Epaas系统统计结果
     */
    private String resultStatsEpaas;

    /**
     * 详细结果
     */
    private String resultDetail;

    /**
     * Epaas系统详细结果
     */
    private String resultDetailEpaas;

    /**
     * 测试结果详情（JSON格式）
     */
    private JSONArray resultDetailTest;

    /**
     * 修改时间
     */
    private Date modifiedTime;
}
