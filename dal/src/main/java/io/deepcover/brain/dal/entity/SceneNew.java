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

/**
 * 场景新增记录实体类
 * 用于记录系统中新增的场景信息，包括场景ID、日期等基本信息
 * 支持对场景数据进行追踪和统计，为业务场景分析提供数据支持
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
@Setter
@Getter
@EqualsAndHashCode(callSuper = true)
public class SceneNew extends Model {

    private static final long serialVersionUID = 1L;
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 场景ID
     */
    private Long sceneId;

    /**
     * 日期
     */
    private String date;


}
