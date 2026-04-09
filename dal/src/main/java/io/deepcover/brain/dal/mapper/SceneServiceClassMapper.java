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

package io.deepcover.brain.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.deepcover.brain.dal.entity.SceneServiceClass;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 场景服务类Mapper接口
 * <p>
 * 提供场景服务类的数据访问操作，负责管理服务中的类信息，支持按服务和类名查询功能
 * </p>
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
public interface SceneServiceClassMapper extends BaseMapper<SceneServiceClass> {

    /**
     * 根据服务名称和类名查询场景服务类信息
     *
     * @param serviceName 服务名称
     * @param className 类名
     * @return 场景服务类实体列表
     */
    @Select("select * from scene_service_class where service_name = #{serviceName} and class_name=#{className}")
    List<SceneServiceClass> findByServiceNameAndClassName(@Param("serviceName") String serviceName, @Param("className") String className);

    /**
     * 获取所有不重复的服务名称
     *
     * @return 服务名称列表
     */
    @Select("SELECT DISTINCT service_name FROM scene_service_class")
    List<String> getServiceName();

    /**
     * 根据服务名称获取所有不重复的类名
     *
     * @param serviceName 服务名称
     * @return 类名列表
     */
    @Select("SELECT DISTINCT class_name FROM scene_service_class where service_name = #{serviceName}")
    List<String> getClassName(@Param("serviceName") String serviceName);
}
