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
import io.deepcover.brain.dal.entity.SceneRiskLevel;
import org.apache.ibatis.annotations.Select;

/**
 * 场景风险等级Mapper接口
 * <p>
 * 提供场景风险等级的数据访问操作，负责管理服务的风险等级配置信息
 * </p>
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
public interface SceneRiskLevelMapper extends BaseMapper<SceneRiskLevel> {

    /**
     * 根据服务名称查询场景风险等级配置
     *
     * @param serviceName 服务名称
     * @return 场景风险等级实体，如果不存在则返回null
     */
    @Select("select * from scene_risk_level where service_name = #{serviceName}")
    SceneRiskLevel queryObject(String serviceName);
}
