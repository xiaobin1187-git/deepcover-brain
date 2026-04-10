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
import io.deepcover.brain.dal.entity.SceneTraceid;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 场景追踪ID Mapper接口
 * <p>
 * 提供场景追踪ID的数据访问操作，负责管理场景追踪记录，支持状态跟踪、查询统计和报告生成功能
 * </p>
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
@Repository
public interface SceneTraceIdMapper extends BaseMapper<SceneTraceid> {

    /**
     * 根据追踪ID查询场景追踪记录
     *
     * @param traceId 追踪ID
     * @return 场景追踪记录列表
     */
    @Select("SELECT id,traceid,status,operator,service_name,env_code,branch_name,base_version,now_version FROM scene_traceid where traceid=#{traceId}")
    List<SceneTraceid> getSceneTraceIds(@Param("traceId") String traceId);

    /**
     * 更新场景追踪状态和操作用户
     *
     * @param traceId 追踪ID
     * @param status 状态值
     * @param operator 操作用户
     * @return 影响的行数
     */
    @Update("UPDATE scene_traceid SET status = #{status},operator = #{operator} WHERE traceid = #{traceId}")
    int updateStatus(@Param("traceId") String traceId, @Param("status") int status,@Param("operator") String operator);

    /**
     * 根据追踪ID查询状态
     *
     * @param traceId 追踪ID
     * @return 状态值，如果不存在则返回null
     */
    @Select("SELECT status FROM scene_traceid WHERE traceid = #{traceId} LIMIT 1")
    Integer getStatusByTraceId(@Param("traceId") String traceId);

    /**
     * 根据追踪ID更新场景追踪记录的完整信息
     *
     * @param traceId 追踪ID
     * @param record 包含更新信息的场景追踪记录实体
     * @return 影响的行数
     */
    @Update("UPDATE scene_traceid SET status = #{record.status}, operator = #{record.operator}, service_name = #{record.serviceName}, env_code = #{record.envCode}, branch_name = #{record.branchName}, base_version = #{record.baseVersion}, now_version = #{record.nowVersion} WHERE traceid = #{traceId}")
    int update(@Param("traceId") String traceId, @Param("record") SceneTraceid record);
    
    /**
     * 根据服务参数查询记录（支持参数为空的情况）
     *
     * @param traceId 追踪ID（可选）
     * @param serviceName 服务名称（可选）
     * @param envCode 环境编码（可选）
     * @param branchName 分支名称（可选）
     * @param baseVersion 基础版本（可选）
     * @param nowVersion 当前版本（可选）
     * @return 场景追踪记录列表
     */
    @Select({
        "<script>",
        "SELECT id,traceid,status,operator,service_name,env_code,branch_name,base_version,now_version FROM scene_traceid WHERE 1=1",
        "<if test='traceId != null and traceId != \"\"'>AND traceid = #{traceId}</if>",
        "<if test='serviceName != null and serviceName != \"\"'>AND service_name = #{serviceName}</if>",
        "<if test='envCode != null and envCode != \"\"'>AND env_code = #{envCode}</if>",
        "<if test='branchName != null and branchName != \"\"'>AND branch_name = #{branchName}</if>",
        "<if test='baseVersion != null and baseVersion != \"\"'>AND base_version = #{baseVersion}</if>",
        "<if test='nowVersion != null and nowVersion != \"\"'>AND now_version = #{nowVersion}</if>",
        "</script>"
    })
    List<SceneTraceid> getSceneTraceIdsByServiceParams(
        @Param("traceId") String traceId,
        @Param("serviceName") String serviceName,
        @Param("envCode") String envCode,
        @Param("branchName") String branchName,
        @Param("baseVersion") String baseVersion,
        @Param("nowVersion") String nowVersion
    );

    /**
     * 根据服务参数查询追踪ID列表（支持参数为空的情况）
     *
     * @param serviceName 服务名称（可选）
     * @param envCode 环境编码（可选）
     * @param branchName 分支名称（可选）
     * @return 场景追踪记录列表
     */
    @Select({
            "<script>",
            "SELECT id,traceid,status,service_name,env_code,branch_name,base_version,now_version FROM scene_traceid WHERE 1=1",
            "<if test='serviceName != null and serviceName != \"\"'>AND service_name = #{serviceName}</if>",
            "<if test='envCode != null and envCode != \"\"'>AND env_code = #{envCode}</if>",
            "<if test='branchName != null and branchName != \"\"'>AND branch_name = #{branchName}</if>",
            "</script>"
    })
    List<SceneTraceid> getTraceIdsByServiceParams(
            @Param("serviceName") String serviceName,
            @Param("envCode") String envCode,
            @Param("branchName") String branchName
    );
    
    /**
     * 通过SQL实现去重和分页的报告列表查询
     * 根据服务参数分组统计，返回去重后的报告记录
     *
     * @param serviceName 服务名称（可选）
     * @param envCode 环境编码（可选）
     * @param branchName 分支名称（可选）
     * @param offset 分页偏移量
     * @param limit 分页大小
     * @return 去重后的报告记录列表，包含统计信息
     */
    @Select({
        "<script>",
        "SELECT service_name, env_code, branch_name, base_version, now_version,",
        "COUNT(*) as record_count,",
        "GROUP_CONCAT(traceid) as trace_ids",
        "FROM scene_traceid",
        "WHERE 1=1",
        "<if test='serviceName != null and serviceName != \"\"'>AND service_name = #{serviceName}</if>",
        "<if test='envCode != null and envCode != \"\"'>AND env_code = #{envCode}</if>",
        "<if test='branchName != null and branchName != \"\"'>AND branch_name = #{branchName}</if>",
        "GROUP BY service_name, env_code, branch_name, base_version, now_version",
        "ORDER BY service_name, env_code, branch_name, base_version, now_version",
        "LIMIT #{offset}, #{limit}",
        "</script>"
    })
    List<SceneTraceid> getDistinctReportList(
        @Param("serviceName") String serviceName,
        @Param("envCode") String envCode,
        @Param("branchName") String branchName,
        @Param("offset") int offset,
        @Param("limit") int limit
    );

    /**
     * 获取去重后的报告总数
     * 根据服务参数分组统计，返回去重后的报告记录总数
     *
     * @param serviceName 服务名称（可选）
     * @param envCode 环境编码（可选）
     * @param branchName 分支名称（可选）
     * @return 去重后的报告记录总数
     */
    @Select({
        "<script>",
        "SELECT COUNT(*) FROM (",
        "SELECT 1",
        "FROM scene_traceid",
        "WHERE 1=1",
        "<if test='serviceName != null and serviceName != \"\"'>AND service_name = #{serviceName}</if>",
        "<if test='envCode != null and envCode != \"\"'>AND env_code = #{envCode}</if>",
        "<if test='branchName != null and branchName != \"\"'>AND branch_name = #{branchName}</if>",
        "GROUP BY service_name, env_code, branch_name, base_version, now_version",
        ") as distinct_reports",
        "</script>"
    })
    int getDistinctReportCount(
        @Param("serviceName") String serviceName,
        @Param("envCode") String envCode,
        @Param("branchName") String branchName
    );
}