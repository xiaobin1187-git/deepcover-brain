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

package io.deepcover.brain.service.service;

import io.deepcover.brain.dal.entity.*;
import io.deepcover.brain.model.ServiceModel;

import java.util.List;
import java.util.Map;

/**
 * 模块版本管理服务接口
 *
 * 提供模块版本管理的核心功能，包括版本的增删改查、
 * 服务版本管理等
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
public interface ModuleVersionService {

    /**
     * 查询服务模块版本列表
     *
     * 查询所有服务的模块版本信息，包含模块状态和版本号
     *
     * @param map 查询参数，支持分页和过滤条件
     * @return List<ServiceModuleVersionEntity> 返回服务版本列表
     */
    List<ServiceModuleVersionEntity> queryServiceList(Map<String, Object> map);

    /**
     * 查询服务总数
     *
     * @param map 查询参数
     * @return int 返回符合条件的记录总数
     */
    int queryServiceTotal(Map<String, Object> map);

    /**
     * 新增模块版本信息
     *
     * 添加新的模块版本记录，自动处理版本号和文件命名
     *
     * @param moduleVersionEntity 模块版本实体，包含类型、分支、提交ID等信息
     */
    void add(ModuleVersionEntity moduleVersionEntity);

    /**
     * 根据ID删除模块版本
     *
     * 软删除：将状态设置为无效，而不是物理删除
     *
     * @param id 模块版本ID
     */
    void deleteById(Long id);

    /**
     * 更新服务版本信息
     *
     * 更新指定服务的版本配置信息
     *
     * @param entity 服务版本实体，包含更新后的版本信息
     */
    void updateServiceVersion(ServiceModuleVersionEntity entity);

    /**
     * 查询服务的默认版本信息
     *
     * 获取指定服务配置的默认模块版本
     *
     * @param serviceName 服务名称
     * @return ServiceModuleVersionEntity 返回默认版本信息
     */
    ServiceModuleVersionEntity queryDefault(String serviceName);

    /**
     * 查询模块版本列表
     *
     * 根据查询条件获取模块版本列表
     *
     * @param map 查询参数，支持按类型、分支等条件过滤
     * @return List<ModuleVersionEntity> 返回模块版本列表
     */
    List<ModuleVersionEntity> queryList(Map<String, Object> map);

    /**
     * 查询模块版本总数
     *
     * @param map 查询参数
     * @return int 返回符合条件的记录总数
     */
    int queryTotal(Map<String, Object> map);
}
