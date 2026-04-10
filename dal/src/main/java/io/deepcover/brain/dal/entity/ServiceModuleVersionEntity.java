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
import java.util.Map;

/**
 * 服务模块版本实体类
 * 用于管理服务中各个模块的版本信息，包括Ares、Repeater、Chaosblade、Emock等模块的启用状态和版本
 * 支持对服务依赖的多个模块进行统一版本管理和状态监控，确保系统模块的版本兼容性和稳定性
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ServiceModuleVersionEntity extends Model {

    private static final long serialVersionUID = 1L;

    /**
     * 主键自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 名
     */
    private String serviceName;

    /**
     * 在线节点数
     */
    private Integer onLineNum;

    /**
     * 模块数量统计
     */
    private Map<String,Integer> moduleNum;

    /**
     * ares是否启动：0-禁用，1-启用
     */
    private Integer aresEnabled;
    /**
     * 版本号
     */
    private Integer aresModuleVersion;

    /**
     * 是否启动：0-禁用，1-启用
     */
    private Integer repeaterEnabled;

    /**
     * 版本号
     */
    private Integer repeaterModuleVersion;

    /**
     * 是否启动：0-禁用，1-启用
     */
    private Integer chaosbladeEnabled;

    /**
     * 版本号
     */
    private Integer chaosbladeModuleVersion;


    /**
     * 是否启动：0-禁用，1-启用
     */
    private Integer emockEnabled;

    /**
     * 版本号
     */
    private Integer emockModuleVersion;

    /**
     * 创建时间
     */
    private Date createdTime;

    /**
     * 修改时间
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
