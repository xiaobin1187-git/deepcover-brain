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
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * 场景追踪ID实体类
 * 用于记录和追踪场景中的Trace ID信息，包括服务信息、环境、分支、版本等关联数据
 * 支持对场景执行过程进行链路追踪，为业务场景的调试和监控提供数据支撑
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
@Setter
@Getter
@EqualsAndHashCode(callSuper = true)
@TableName("scene_traceid") // 指定表名
public class SceneTraceid extends Model<SceneTraceid> {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联的Trace ID，用于链路追踪
     */
    private String traceid;

    /**
     * 状态
     */
    private Integer status = 0;

    /**
     * 操作用户
     */
    private String operator;
    
    /**
     * 服务名称
     */
    @TableField("service_name")
    private String serviceName;

    /**
     * 环境编码
     */
    @TableField("env_code")
    private String envCode;

    /**
     * 分支名称
     */
    @TableField("branch_name")
    private String branchName;

    /**
     * 基线版本
     */
    @TableField("base_version")
    private String baseVersion;

    /**
     * 当前版本
     */
    @TableField("now_version")
    private String nowVersion;

    /**
     * 用于存储去重查询结果的记录数（非数据库字段）
     */
    @TableField(exist = false)
    private Integer recordCount;

    /**
     * 用于存储去重查询结果的Trace ID列表（非数据库字段）
     */
    @TableField(exist = false)
    private String traceIds;
}