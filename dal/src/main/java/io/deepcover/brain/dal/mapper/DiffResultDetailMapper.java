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
import io.deepcover.brain.dal.entity.DiffResultDetailEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectKey;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

/**
 * 代码差异结果详情Mapper接口
 * <p>
 * 提供代码差异结果详情的数据访问操作，负责管理代码比对的具体结果、统计信息和详细内容
 * </p>
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
@Repository
public interface DiffResultDetailMapper extends BaseMapper<DiffResultDetailEntity> {

    /**
     * 插入新的代码差异结果详情记录
     *
     * @param resultDetailEntity 差异结果详情实体，包含代码差异、统计信息、详细内容等
     * @return 影响的行数
     */
    @Insert("insert into code_diff_result_detail " +
            "(id,code_diff,result_stats,result_detail) " +
            "values (#{id},#{codeDiff},#{resultStats},#{resultDetail})")
    @SelectKey(statement="select LAST_INSERT_ID()",keyProperty = "id",before = false,resultType = Long.class)
    int insert(DiffResultDetailEntity resultDetailEntity);

    /**
     * 更新代码差异结果详情记录的所有字段
     *
     * @param resultDetailEntity 包含更新信息的差异结果详情实体
     * @return 影响的行数
     */
    @Update("<script>" +
            "UPDATE code_diff_result_detail " +
            "<set>" +
            "    <if test=\"codeDiff != null\">`code_diff` = #{codeDiff},</if>" +
            "    <if test=\"resultStats != null\">`result_stats` = #{resultStats},</if>" +
            "    <if test=\"resultDetail != null\">`result_detail` = #{resultDetail},</if>" +
            "    <if test=\"resultDetailEpaas != null\">`result_detail_epaas` = #{resultDetailEpaas},</if>" +
            "    <if test=\"resultStatsEpaas != null\">`result_stats_epaas` = #{resultStatsEpaas}</if>" +
            "</set>" +
            "WHERE id = #{id}" +
            "</script>")
    int updateById(DiffResultDetailEntity resultDetailEntity);

    /**
     * 更新代码差异结果详情记录的EPAAS相关字段
     *
     * @param resultDetailEntity 包含EPAAS更新信息的差异结果详情实体
     * @return 影响的行数
     */
    @Update("<script>" +
            "UPDATE code_diff_result_detail " +
            "<set>" +
            "    <if test=\"resultDetailEpaas != null\">`result_detail_epaas` = #{resultDetailEpaas},</if>" +
            "    <if test=\"resultStatsEpaas != null\">`result_stats_epaas` = #{resultStatsEpaas}</if>" +
            "</set>" +
            "WHERE id = #{id}" +
            "</script>")
    int updateByIdEpaas(DiffResultDetailEntity resultDetailEntity);

    /**
     * 根据ID查询代码差异结果详情
     *
     * @param id 记录ID
     * @return 代码差异结果详情实体
     */
    @Select("select * from code_diff_result_detail where id = #{id}")
    DiffResultDetailEntity queryObjectById(Long id);
}
