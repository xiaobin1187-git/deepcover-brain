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

package io.deepcover.brain.dal.complexitymapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.deepcover.brain.dal.entity.MethodComplexity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * 方法的圈复杂度 Mapper 接口
 * </p>
 *
 * @author mybatis-plus-generator
 * @since 2024-02-23
 */
public interface MethodComplexityMapper extends BaseMapper<MethodComplexity> {

    @Select("SELECT * FROM method_complexity WHERE app=#{app} AND NAME=#{methodName} ORDER BY id DESC LIMIT 1")
    MethodComplexity getValueByMethodName(@Param("app") String app, @Param("methodName") String methodName);
}
