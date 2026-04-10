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

import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 方法圈复杂度实体类
 * 用于记录和存储Java方法的圈复杂度分析结果，衡量代码的复杂程度和维护难度
 * 支持按应用、方法、业务域等维度进行复杂度统计和分析，为代码质量优化提供数据依据
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MethodComplexity extends Model {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 类型
     */
    private Integer type;

    /**
     * 方法唯一标识键
     */
    private String key;

    /**
     * 方法键的MD5值，用于去重
     */
    private String keyMd5;

    /**
     * 应用名称
     */
    private String app;

    /**
     * 方法名称
     */
    private String name;

    /**
     * 圈复杂度值
     */
    private Integer value;

    /**
     * 分析报告HTML内容
     */
    private String reportHtml;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 修改时间
     */
    private Date gmtModify;

    /**
     * 业务域名称
     */
    private String bizDomainName;

    /**
     * 产品线名称
     */
    private String productLineName;


}
