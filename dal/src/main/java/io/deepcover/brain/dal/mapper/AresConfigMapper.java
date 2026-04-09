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
import io.deepcover.brain.dal.entity.AresAgentEntity;
import io.deepcover.brain.dal.entity.AresConfigEntity;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Ares配置信息Mapper接口
 * <p>
 * 提供Ares系统配置信息的数据访问操作，主要负责查询系统配置参数
 * </p>
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
@Repository
public interface AresConfigMapper extends BaseMapper<AresConfigEntity> {

    /**
     * 根据配置键查询有效配置信息
     *
     * @param key 配置键名
     * @return 配置实体对象，如果不存在或状态无效则返回null
     */
    @Select("select * from agent_config a where a.key = #{key} and status=1")
    AresConfigEntity queryObject(String key);
}
