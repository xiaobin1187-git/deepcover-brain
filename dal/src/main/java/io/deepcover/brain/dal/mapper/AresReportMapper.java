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
import io.deepcover.brain.dal.entity.ReportAgentEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * Ares代理服务报告信息Mapper接口
 * <p>
 * 提供Ares代理服务报告信息的数据访问操作，负责管理代理服务的状态报告、心跳信息等
 * </p>
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
@Repository
public interface AresReportMapper extends BaseMapper<ReportAgentEntity> {

    /**
     * 根据服务名、IP和版本查询代理报告信息
     *
     * @param reportAgentEntity 包含服务名、IP和版本的查询条件实体
     * @return 代理报告实体对象
     */
    @Select("select * from agent_service_report_info " +
            "where service_name = #{serviceName} and ip = #{ip} and version = #{version}")
    ReportAgentEntity queryObject(ReportAgentEntity reportAgentEntity);

    /**
     * 查询指定服务的代理报告列表，按修改时间倒序排列
     *
     * @param serviceName 服务名称
     * @param limit 返回记录数限制
     * @return 代理报告实体列表，包含在线状态信息
     */
  @Select(
      "select *,if((now()-modified_time < 60),1,0) AS `status` from agent_service_report_info "
          + "where service_name = #{serviceName} order by modified_time desc  limit #{limit}")
  List<ReportAgentEntity> queryList(String serviceName, Integer limit);

    /**
     * 查询指定服务的活跃代理报告列表（最近60秒内有更新的）
     *
     * @param serviceName 服务名称
     * @return 活跃代理报告实体列表
     */
    @Select(
            "select * from agent_service_report_info "
                    + "where service_name = #{serviceName} and modified_time>now()-60 order by modified_time desc")
    List<ReportAgentEntity> queryActiveList(String serviceName);

    /**
     * 更新代理报告计数
     *
     * @param id 报告记录ID
     * @return 影响的行数
     */
    @Update("update agent_service_report_info set report_count=report_count+1 where id = #{id}")
    int update(long id);

    /**
     * 插入新的代理报告记录
     *
     * @param reportAgentEntity 代理报告实体对象
     * @return 影响的行数
     */
    @Insert("insert into agent_service_report_info (service_name,ip,version,env_code) values(#{serviceName},#{ip},#{version},#{envCode})")
    int insert(ReportAgentEntity reportAgentEntity);

    /**
     * 插入或更新代理报告记录（存在则更新计数，不存在则新增）
     *
     * @param reportAgentEntity 代理报告实体对象
     * @return 影响的行数
     */
    @Insert("insert into agent_service_report_info (service_name,ip,version,env_code) values(#{serviceName},#{ip},#{version},#{envCode}) ON DUPLICATE KEY UPDATE report_count=report_count+1")
    int insertOrUpdate(ReportAgentEntity reportAgentEntity);
}
