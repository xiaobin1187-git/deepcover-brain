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
import io.deepcover.brain.dal.entity.DiffRecordEntity;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 代码差异记录Mapper接口
 * <p>
 * 提供代码差异记录的数据访问操作，负责管理代码比对任务的记录、状态跟踪、结果查询等功能
 * </p>
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
@Repository
public interface DiffRecordMapper extends BaseMapper<DiffRecordEntity> {
    /**
     * 插入新的代码差异记录
     *
     * @param recordEntity 代码差异记录实体，包含服务名、环境、版本信息等
     * @return 影响的行数
     */
    @Insert("insert into code_diff_record " +
            "(service_name,env_code,base_git_url,base_branch,base_version,now_git_url,now_branch,now_version,status,test_result,add_by,publish_time,last_start_diff_time) " +
            "values (#{serviceName},#{envCode},#{baseGitUrl},#{baseBranch},#{baseVersion},#{nowGitUrl},#{nowBranch},#{nowVersion},#{status},#{testResult},#{addBy},#{publishTime},#{lastStartDiffTime})")
    @SelectKey(statement="select LAST_INSERT_ID()",keyProperty = "id",before = false,resultType = Long.class)
    int insert(DiffRecordEntity recordEntity);

    /**
     * 根据ID查询代码差异记录
     *
     * @param id 记录ID
     * @return 代码差异记录实体
     */
    @Select("select * from code_diff_record where id = #{id} ")
    DiffRecordEntity queryObjectById(Long id);

    /**
     * 根据ID查询EPAAS相关的代码差异记录
     *
     * @param id 记录ID
     * @param isEpaas 是否为EPAAS记录
     * @return 代码差异记录实体
     */
    @Select("select * from code_diff_record where id = #{id}")
    DiffRecordEntity queryEpaasObjectById(Long id,int isEpaas);

    /**
     * 根据服务名、环境、基础版本和当前版本查询唯一的代码差异记录
     *
     * @param recordEntity 包含查询条件的代码差异记录实体
     * @return 代码差异记录实体
     */
    @Select("select * from code_diff_record where service_name = #{serviceName} and env_code=#{envCode} and base_version=#{baseVersion} and now_version=#{nowVersion}")
    DiffRecordEntity queryObjectByUnique(DiffRecordEntity recordEntity);

    /**
     * 更新代码差异记录的测试结果和状态信息
     *
     * @param diffRecordEntity 包含更新结果的代码差异记录实体
     * @return 影响的行数
     */
    @Update("<script>" +
            "update code_diff_record set " +
            "`test_result` = #{testResult}, " +
            "`status` = #{status}, " +
            "`status_descrip` = #{statusDescrip}, " +
            "`diff_detail_id` = #{diffDetailId}" +
            "<if test='isEpaas != null'> `is_epaas` = #{isEpaas}</if> " +
            " where id = #{id}" +
            "</script>")
    int updateRestResult(DiffRecordEntity diffRecordEntity);

    /**
     * 更新代码差异记录的EPAAS标识
     *
     * @param diffRecordEntity 包含EPAAS标识的代码差异记录实体
     * @return 影响的行数
     */
    @Update("<script>" +
            "update code_diff_record set " +
            "<if test='isEpaas != null'>`is_epaas` = #{isEpaas}</if> " +
            " where id = #{id}" +
            "</script>")
    int updateRestResultEpaas(DiffRecordEntity diffRecordEntity);

    /**
     * 更新代码差异记录的状态信息
     *
     * @param diffRecordEntity 包含状态更新信息的代码差异记录实体
     * @return 影响的行数
     */
    @Update("<script>" +
            "update code_diff_record set `status` = #{status} " +
            "<if test=\"lastStartDiffTime != null \">\n" +
            "    , last_start_diff_time =#{lastStartDiffTime}\n" +
            "</if>\n" +
            "where id = #{id}"+
            "</script>" )
    int updateStatus(DiffRecordEntity diffRecordEntity);

  /**
     * 查询最新的代码差异记录列表（每个服务环境基础版本组合只返回最新的一条）
     *
     * @param map 查询参数，包含服务名称等过滤条件
     * @return 最新代码差异记录列表
     */
  @Select(
      "<script>"
          + "SELECT b.*,a.id,a.base_branch,a.base_git_url,a.now_version,a.now_branch,a.status,a.status_descrip,a.diff_detail_id,a.created_time  from \n"
          + "(" +
              "SELECT max(a.id) AS maxId,count(a.id) AS recordTotal,a.service_name,a.env_code,a.base_version\n"
          + "from code_diff_record a \n"
              +"        <where>\n"
              + "            1=1\n"
              + "            <if test=\"serviceName != null and serviceName.trim() != ''\">\n"
              + "                and a.service_Name = #{serviceName}\n"
              + "            </if>\n"
              + "           and a.created_time >= DATE_SUB(CURDATE(), INTERVAL 2 MONTH)\n"
              + "        </where>\n"
          + "            GROUP BY a.service_name ,a.env_code,a.base_version\n"
          + "            order by maxId DESC\n" +
              ") b left join code_diff_record a on b.maxId=a.id\n"
          + "</script>")
  List<DiffRecordEntity> queryLatestList(Map<String, Object> map);

    /**
     * 查询代码差异记录列表（支持多种条件过滤）
     *
     * @param map 查询参数，包含服务名称、EPAAS标识、差异类型、状态等过滤条件
     * @return 代码差异记录列表，按创建时间倒序排列
     */
    @Select("<script>" +
            "SELECT a.id,a.service_name,a.env_code,a.base_version,a.base_branch,a.base_git_url,a.now_version,a.now_branch,a.status,a.status_descrip,a.diff_detail_id,a.created_time  from code_diff_record a \n" +
            "        <where>\n" +
            "            1=1\n" +
            "            <if test=\"serviceName != null and serviceName.trim() != ''\">\n" +
            "                and a.service_Name = #{serviceName}\n" +
            "            </if>\n" +
            "            <if test=\"isEpaas != null and isEpaas != ''\">\n" +
            "                and a.is_epaas = #{isEpaas}\n" +
            "            </if>\n" +
            "<if test=\"diffType != null and diffType.trim() != ''\">\n" +
            "                and a.diff_type =#{diffType}\n" +
            "            </if>\n" +
            "<if test=\"status != null and status.trim() != ''\">\n" +
            "                and a.status =#{status}\n" +
            "            </if>\n" +
            "and a.created_time >= DATE_SUB(CURDATE(), INTERVAL 2 MONTH)"+
            "        </where>\n" +
            "order by a.created_time desc \n" +
            "</script>")
    List<DiffRecordEntity> queryList(Map<String, Object> map);

    /**
     * 查询差异统计信息（统计每个服务的差异记录数量）
     *
     * @param map 查询参数，包含服务名称过滤条件和分页信息
     * @return 统计信息列表，包含服务ID、服务名称和差异记录数量
     */
    @Select("<script>"+
            "SELECT a.id,a.service_name,sum(if(b.created_time >= DATE_SUB(CURDATE(), INTERVAL 2 MONTH),1,0)) AS diffCount FROM agent_service a LEFT JOIN code_diff_record b \n"
            + "ON a.service_name=b.service_name "
            +"        <where>\n"
            +"            1=1\n"
            +"            <if test=\"serviceName != null and serviceName.trim() != ''\">\n"
            +"                and a.service_name = #{serviceName}\n"
            +"            </if>\n"
            +"        </where>"
            +"GROUP BY a.service_name ORDER BY diffCount desc  , a.service_name asc\n"
          +"        <if test=\"limit != null and limit > 0\">\n"
          +"            limit #{offset}, #{limit}\n"
          +"        </if>"
            +"</script>")
    List<Map<String, Object>> queryDiffStats(Map<String, Object> map);

    /**
     * 查询差异统计总数
     *
     * @param map 查询参数，包含服务名称等过滤条件
     * @return 满足条件的服务总数
     */
    @Select("<script>" +
            "SELECT count(1) from agent_service\n" +
            "        <where>\n" +
            "            1=1\n" +
            "            <if test=\"serviceName != null and serviceName.trim() != ''\">\n" +
            "                and service_name = #{serviceName}\n" +
            "            </if>\n" +
            "        </where>" +
            "</script>")
    int queryDiffStatsTotal(Map<String, Object> map);

    /**
     * 查询指定服务和环境的最新差异记录
     *
     * @param serviceName 服务名称
     * @param envCode 环境编码
     * @param num 返回记录数量
     * @return 最新差异记录列表，按创建时间倒序排列
     */
    @Select("SELECT * FROM code_diff_record WHERE service_name = #{serviceName} AND env_code = #{envCode}  AND DATEDIFF(CURDATE(), publish_time) < 30 ORDER BY created_time DESC LIMIT  #{num}")
    List<DiffRecordEntity> selectLatestByServiceNameAndEnv(@Param("serviceName") String serviceName, @Param("envCode") String envCode, @Param("num")int num);
}
