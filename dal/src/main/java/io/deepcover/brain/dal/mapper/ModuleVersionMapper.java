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
import io.deepcover.brain.dal.entity.ModuleVersionEntity;
import io.deepcover.brain.dal.entity.ServiceModuleVersionEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 模块版本Mapper接口
 * <p>
 * 提供模块版本信息的数据访问操作，负责管理代理服务的模块版本配置、版本发布、启用状态等功能
 * </p>
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
@Repository
public interface ModuleVersionMapper extends BaseMapper<AresAgentEntity> {

    /**
     * 分页查询代理服务模块版本列表，包含在线状态统计
     *
     * @param map 查询参数，包含服务名称、分页信息等
     * @return 服务模块版本实体列表，包含在线数量统计信息
     */
    @Select("<script>" +
            "SELECT a.* ,SUM(if(NOW()-b.modified_time &lt; 60,1,0)) as onLineNum from agent_service_module_version a\n" +
            "LEFT JOIN agent_service_report_info b ON a.service_name=b.service_name" +
            "        <where>\n" +
            "            1=1\n" +
            "            <if test=\"serviceName != null and serviceName.trim() != ''\">\n" +
            "                and a.service_name like concat('%',#{serviceName},'%')\n" +
            "            </if>\n" +
            "        </where>\n" +
            "group BY a.service_name order by onLineNum desc , a.service_name asc \n" +
            "        <if test=\"limit != null and limit > 0\">\n" +
            "            limit #{offset}, #{limit}\n" +
            "        </if>" +
            "</script>")
    List<ServiceModuleVersionEntity> queryServiceList(Map<String, Object> map);

    /**
     * 查询代理服务模块版本总数（支持按服务名称模糊查询）
     *
     * @param map 查询参数，包含服务名称等过滤条件
     * @return 满足条件的服务模块版本总数
     */
    @Select("<script>" +
            "SELECT count(1) from agent_service_module_version\n" +
            "        <where>\n" +
            "            1=1\n" +
            "            <if test=\"serviceName != null and serviceName.trim() != ''\">\n" +
            "                and `service_name` like concat('%',#{serviceName},'%')\n" +
            "            </if>\n" +
            "        </where>" +
            "</script>")
    int queryServiceTotal(Map<String, Object> map);

    /**
     * 插入新的模块版本记录
     *
     * @param moduleVersionEntity 模块版本实体，包含OSS地址、类型、版本号、描述、分支、提交ID等信息
     * @return 影响的行数
     */
    @Insert("insert into module_version " +
            "(oss_url,type,version,description,branch,commit_id) " +
            "values (#{ossUrl},#{type},#{version},#{description},#{branch},#{commitId})")
    int insert(ModuleVersionEntity moduleVersionEntity);

    /**
     * 插入新的代理服务模块版本配置记录
     *
     * @param serviceModuleVersionEntity 服务模块版本实体，包含各模块的启用状态和版本信息
     * @return 影响的行数
     */
    @Insert("insert into agent_service_module_version " +
            "(service_name,ares_enabled,ares_module_version,repeater_enabled,repeater_module_version,chaosblade_enabled,chaosblade_module_version,emock_enabled,emock_module_version) " +
            "values (#{serviceName},#{aresEnabled},#{aresModuleVersion},#{repeaterEnabled},#{repeaterModuleVersion},#{chaosbladeEnabled},#{chaosbladeModuleVersion},#{emockEnabled},#{emockModuleVersion})")
    int insertModuleVersion(ServiceModuleVersionEntity serviceModuleVersionEntity);

    /**
     * 更新模块版本状态
     *
     * @param id 模块版本记录ID
     * @param status 状态值
     * @return 影响的行数
     */
    @Update("update module_version set status=#{status} where id =#{id}")
    int updateStatusById(Long id,Integer status);

    /**
     * 根据服务名称查询默认模块版本配置
     *
     * @param serviceName 服务名称
     * @return 服务模块版本实体
     */
    @Select("select * from agent_service_module_version where service_name = #{serviceName}")
    ServiceModuleVersionEntity queryDefault(String serviceName);

    /**
     * 更新代理服务的模块版本配置
     *
     * @param entity 包含更新信息的服务模块版本实体
     * @return 影响的行数
     */
    @Update("<script> update agent_service_module_version\n" +
            "        <set>\n" +
            "            <if test=\"aresEnabled != null\">`ares_enabled` = #{aresEnabled}, </if>\n" +
            "            <if test=\"aresModuleVersion != null\">`ares_module_version` = #{aresModuleVersion}, </if>\n" +
            "            <if test=\"repeaterEnabled != null\">`repeater_enabled` = #{repeaterEnabled}, </if>\n" +
            "            <if test=\"repeaterModuleVersion != null \">`repeater_module_version` = #{repeaterModuleVersion}, </if>\n" +
            "            <if test=\"chaosbladeEnabled != null\">`chaosblade_enabled` = #{chaosbladeEnabled}, </if>\n" +
            "            <if test=\"chaosbladeModuleVersion != null\">`chaosblade_module_version` = #{chaosbladeModuleVersion}, </if>\n" +
            "            <if test=\"emockEnabled != null\">`emock_enabled` = #{emockEnabled}, </if>\n" +
            "            <if test=\"emockModuleVersion != null\">`emock_module_version` = #{emockModuleVersion}, </if>\n" +
            "        </set>\n" +
            "    where service_name = #{serviceName}</script>")
    int updateServiceVersion(ServiceModuleVersionEntity entity);

    /**
     * 查询指定类型的最新模块版本
     *
     * @param type 模块类型
     * @return 最新模块版本实体
     */
    @Select("select * from module_version where `type` = #{type} order by version desc limit 1 ")
    ModuleVersionEntity queryLatestVersion(Integer type);

    /**
     * 分页查询模块版本列表（排除已删除的记录）
     *
     * @param map 查询参数，包含模块类型、分页信息等
     * @return 模块版本实体列表，按版本号倒序排列
     */
    @Select("<script>" +
            "SELECT * from module_version a\n" +
            "        <where>\n" +
            "            1=1\n" +"and status !=-1"+
            "            <if test=\"type != null \">\n" +
            "                and `type` = #{type}\n" +
            "            </if>\n" +
            "        </where>\n" +
            "order by version desc \n" +
            "        <if test=\"limit != null and limit > 0\">\n" +
            "            limit #{offset}, #{limit}\n" +
            "        </if>" +
            "</script>")
    List<ModuleVersionEntity> queryList(Map<String, Object> map);

    /**
     * 查询模块版本总数（排除已删除的记录）
     *
     * @param map 查询参数，包含模块类型等过滤条件
     * @return 满足条件的模块版本总数
     */
    @Select("<script>" +
            "SELECT count(1) from module_version\n" +
            "        <where>\n" +
            "            1=1\n" +"and status !=-1"+
            "            <if test=\"type != null\">\n" +
            "                and `type` = #{type}\n" +
            "            </if>\n" +
            "        </where>" +
            "</script>")
    int queryTotal(Map<String, Object> map);
}
