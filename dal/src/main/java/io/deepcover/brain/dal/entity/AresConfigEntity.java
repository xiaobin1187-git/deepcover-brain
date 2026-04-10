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
 * Ares系统配置实体类
 * 用于管理Ares代码分析平台的系统配置参数，支持键值对形式的配置存储
 * 包括系统开关、阈值设置、功能配置等，为系统运行提供灵活的参数配置能力
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AresConfigEntity extends Model {

    /**
     * 主键自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 配置项键名
     */
    private String key;

    /**
     * 配置项值
     */
    private String value;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 备注信息
     */
    private String remark;

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


}
