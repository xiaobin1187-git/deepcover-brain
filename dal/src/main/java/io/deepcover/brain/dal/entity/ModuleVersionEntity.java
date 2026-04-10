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
 * 模块版本管理实体类
 * 用于管理系统中各种模块的版本信息，包括版本号、发布环境、Git信息等
 * 支持对Ares平台中不同组件模块进行版本追踪和管理，确保系统版本的一致性和可追溯性
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ModuleVersionEntity extends Model {

    private static final long serialVersionUID = 1L;

    /**
     * 主键自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 模块类型名称
     */
    private String typeName;

    /**
     * 发布的环境标识
     */
    private String env;

    /**
     * OSS存储地址
     */
    private String ossUrl;

    /**
     * Git分支名称
     */
    private String branch;

    /**
     * Git提交记录ID
     */
    private String commitId;

    /**
     * 模块类型
     */
    private Integer type;
    /**
     * 版本号
     */
    private Integer version;

    /**
     * 版本描述信息
     */
    private String description;

    /**
     * 状态：0-初始化，1-采集中，2-停止采集
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createdTime;
}
