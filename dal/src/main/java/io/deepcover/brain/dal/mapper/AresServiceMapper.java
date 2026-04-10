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

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.deepcover.brain.dal.entity.AresAgentBatchEntity;
import io.deepcover.brain.dal.entity.AresAgentEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Ares代理服务配置Mapper接口
 * <p>
 * 提供Ares代理服务配置信息的数据访问操作，负责管理服务的基本配置、采样率、异常阈值、队列配置等参数
 * </p>
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
@Repository
public interface AresServiceMapper extends BaseMapper<AresAgentEntity> {

    /**
     * 插入新的代理服务配置记录
     *
     * @param agentEntity 代理服务实体对象，包含服务名称、包名、忽略规则、采样率等配置信息
     * @return 影响的行数
     */
    @Insert("insert into agent_service " +
            "(service_name,package_name,ignore_urls,ignore_classes,ignore_methods,ignore_annos,sample_rate," +
            "limit_code_method_size,limit_code_method_line_size,report_period,exception_threshold,exception_calc_time,exception_pause_time," +
            "send_data_center_type,queue_num,queue_size,queue_msg_size,queue_recycle_time) " +
            "values (#{serviceName},#{packageName},#{ignoreUrls},#{ignoreClasses},#{ignoreMethods},#{ignoreAnnos}" +
            ",#{sampleRate},#{limitCodeMethodSize},#{limitCodeMethodLineSize},#{reportPeriod},#{exceptionThreshold},#{exceptionCalcTime},#{exceptionPauseTime}," +
            "#{sendDataCenterType},#{queueNum},#{queueSize},#{queueMsgSize},#{queueRecycleTime})")
    int insert(AresAgentEntity agentEntity);

    /**
     * 根据ID更新代理服务配置信息
     *
     * @param aresAgentEntity 代理服务实体对象，包含需要更新的配置信息
     * @return 影响的行数
     */
    @Update("<script> update agent_service\n" +
            "        <set>\n" +
            "            <if test=\"packageName != null and packageName.trim() != ''\">`package_name` = #{packageName}, </if>\n" +
            "            <if test=\"ignoreClasses != null and ignoreClasses.trim() != ''\">`ignore_classes` = #{ignoreClasses}, </if>\n" +
            "            <if test=\"ignoreMethods != null and ignoreMethods.trim() != ''\">`ignore_methods` = #{ignoreMethods}, </if>\n" +
            "            <if test=\"ignoreAnnos != null and ignoreAnnos.trim() != ''\">`ignore_annos` = #{ignoreAnnos}, </if>\n" +
            "            <if test=\"ignoreUrls != null\">`ignore_urls` = #{ignoreUrls}, </if>\n" +
            "            <if test=\"sampleRate != null\">`sample_rate` = #{sampleRate}, </if>\n" +
            "            <if test=\"limitCodeMethodSize != null\">`limit_code_method_size` = #{limitCodeMethodSize}, </if>\n" +
            "            <if test=\"limitCodeMethodLineSize != null\">`limit_code_method_line_size` = #{limitCodeMethodLineSize}, </if>\n" +
            "            <if test=\"reportPeriod != null\">`report_period` = #{reportPeriod}, </if>\n" +
            "            <if test=\"exceptionThreshold != null\">`exception_threshold` = #{exceptionThreshold}, </if>\n" +
            "            <if test=\"exceptionCalcTime != null\">`exception_calc_time` = #{exceptionCalcTime}, </if>\n" +
            "            <if test=\"exceptionPauseTime != null\">`exception_pause_time` = #{exceptionPauseTime}, </if>\n" +
            "            <if test=\"sendDataCenterType != null\">`send_data_center_type` = #{sendDataCenterType}, </if>\n" +
            "            <if test=\"queueNum != null\">`queue_num` = #{queueNum}, </if>\n" +
            "            <if test=\"queueSize != null\">`queue_size` = #{queueSize}, </if>\n" +
            "            <if test=\"queueMsgSize != null\">`queue_msg_size` = #{queueMsgSize}, </if>\n" +
            "            <if test=\"queueRecycleTime != null\">`queue_recycle_time` = #{queueRecycleTime}, </if>\n" +
            "            <if test=\"local != null\">`local` = #{local}, </if>\n" +
            "           version=version+1" +
            "        </set>\n" +
            "    where id = #{id}</script>")
    int updateById(AresAgentEntity aresAgentEntity);

    /**
     * 批量更新代理服务的关键配置信息
     *
     * @param aresAgentBatchEntity 批量更新实体，包含ID列表和需要更新的配置参数
     * @return 影响的行数
     */
    @Update(
            "<script> update agent_service\n"
                    + "        <set>\n"
                    + "            <if test=\"sampleRate != null\">`sample_rate` = #{sampleRate}, </if>\n"
                    + "            <if test=\"reportPeriod != null\">`report_period` = #{reportPeriod}, </if>\n"
                    + "            <if test=\"local != null\">`local` = #{local}, </if>\n"
                    + "            <if test=\"exceptionThreshold != null\">`exception_threshold` = #{exceptionThreshold}, </if>\n"
                    + "           version=version+1"
                    + "        </set>\n"
                    + "    where id in  <foreach item=\"id\" collection=\"ids\" open=\"(\" separator=\",\" close=\")\">\n"
                    + "\t\t\t#{id}\n"
                    + "\t\t</foreach>" +
                    "</script>")
    int batchUpdateByIds(AresAgentBatchEntity aresAgentBatchEntity);

    /**
     * 根据服务名称查询代理服务配置信息
     *
     * @param serviceName 服务名称
     * @return 代理服务实体对象，如果不存在则返回null
     */
    @Select("select * from agent_service where service_name = #{serviceName}")
    AresAgentEntity queryObject(String serviceName);

    /**
     * 分页查询代理服务配置列表，包含在线状态统计
     *
     * @param map 查询参数，包含服务名称、分页信息等
     * @return 代理服务实体列表，包含在线数量统计信息
     */
    @Select("<script>" +
            "SELECT a.* ,SUM(if(NOW()-b.modified_time &lt; 60,1,0)) as onLineNum from agent_service a\n" +
            "LEFT JOIN agent_service_report_info b ON a.service_name=b.service_name" +
            "        <where>\n" +
            "            1=1\n" +
            "            <if test=\"serviceName != null and serviceName.trim() != ''\">\n" +
            "                and a.service_Name like concat('%',#{serviceName},'%')\n" +
            "            </if>\n" +
            "        </where>\n" +
            "group BY a.service_name order by onLineNum desc , a.service_name asc \n" +
            "        <if test=\"limit != null and limit > 0\">\n" +
            "            limit #{offset}, #{limit}\n" +
            "        </if>" +
            "</script>")
    List<AresAgentEntity> queryList(Map<String, Object> map);

    /**
     * 查询采样率分布统计信息
     *
     * @return 采样率统计列表，包含每个采样率对应的服务数量和服务名称
     */
  @Select(
      "<script>"
          + "SELECT a.sample_rate ,COUNT(1) AS diffCount ,GROUP_CONCAT(a.service_name ORDER BY a.service_name SEPARATOR ', ') AS serviceName\n"
          + "FROM agent_service a  GROUP BY a.sample_rate\n"
          + "ORDER BY diffCount desc "
          + "</script>")
  List<JSONObject> querySampleRateDiffList();

    /**
     * 查询数据中心类型分布统计信息
     *
     * @return 数据中心类型统计列表，包含每种类型对应的服务数量和服务名称
     */
    @Select(
            "<script>"
                    + "SELECT a.send_data_center_type ,COUNT(1) AS diffCount ,GROUP_CONCAT(a.service_name ORDER BY a.service_name SEPARATOR ', ') AS serviceName\n"
                    + "FROM agent_service a  GROUP BY a.send_data_center_type\n"
                    + "ORDER BY diffCount desc "
                    + "</script>")
    List<JSONObject> querySendDataCenterTypeDiffList();

    /**
     * 查询异常阈值分布统计信息
     *
     * @return 异常阈值统计列表，包含每种阈值配置对应的服务数量和服务名称
     */
    @Select(
            "<script>"
                    + "SELECT a.exception_threshold ,a.exception_calc_time,COUNT(1) AS diffCount ,GROUP_CONCAT(a.service_name ORDER BY a.service_name SEPARATOR ', ') AS serviceName\n"
                    + "FROM agent_service a  GROUP BY a.exception_threshold ,a.exception_calc_time\n"
                    + "ORDER BY diffCount desc "
                    + "</script>")
    List<JSONObject> queryExceptionThresholdDiffList();

    /**
     * 查询代理服务总数（支持按服务名称模糊查询）
     *
     * @param map 查询参数，包含服务名称等过滤条件
     * @return 满足条件的代理服务总数
     */
    @Select("<script>" +
            "SELECT count(1) from agent_service\n" +
            "        <where>\n" +
            "            1=1\n" +
            "            <if test=\"serviceName != null and serviceName.trim() != ''\">\n" +
            "                and service_Name like concat('%',#{serviceName},'%')\n" +
            "            </if>\n" +
            "        </where>" +
            "</script>")
    int queryTotal(Map<String, Object> map);

    /**
     * 获取所有代理服务的名称列表
     *
     * @return 服务名称列表
     */
    @Select("SELECT service_name FROM agent_service")
    List<String> getAllServiceName();
}
