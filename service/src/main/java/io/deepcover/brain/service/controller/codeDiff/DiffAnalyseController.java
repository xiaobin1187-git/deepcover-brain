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

package io.deepcover.brain.service.controller.codeDiff;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.deepcover.brain.dal.entity.DiffRecordEntity;
import io.deepcover.brain.dal.entity.DiffResultDetailEntity;
import io.deepcover.brain.dal.entity.SceneTraceid;
import io.deepcover.brain.dal.mapper.SceneTraceIdMapper;
import io.deepcover.brain.model.ReportVO;
import io.deepcover.brain.service.aop.OperateLogger;
import io.deepcover.brain.service.exception.RRException;
import io.deepcover.brain.service.service.DiffAnalyseService;
import io.deepcover.brain.service.util.PageUtils;
import io.deepcover.brain.service.util.Query;
import io.deepcover.brain.service.util.R;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 代码差异分析控制器
 *
 * 提供代码差异分析的相关API接口，包括变更分析、记录管理、
 * 服务列表查询、场景刷新、报告管理等功能
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
@Slf4j
@RestController
@RequestMapping("/aresbrain/diff/analyse")
public class DiffAnalyseController {
    @Autowired
    private DiffAnalyseService diffAnalyseService;
    @Autowired
    private SceneTraceIdMapper sceneTraceIdMapper;

    /**
     * 新增代码变更分析记录
     *
     * 添加新的代码变更分析记录，分析指定服务在不同版本之间的差异
     *
     * @param recordEntity 差异分析记录实体，包含服务名、环境、Git地址、版本信息等
     * @return R 返回新增操作的结果
     * @throws RRException 当Git地址为空、非SSH格式、环境编码为空或版本相同时抛出异常
     */
    @RequestMapping("/add")
    public R add(@RequestBody DiffRecordEntity recordEntity) {
        if (recordEntity == null) {
            throw new RRException("参数recordEntity不能为空");
        }
        log.info(
                "开始变更分析，serviceName:{},envCode:{},gitUrl:{},baseCommitId:{},nowCommitId:{}",
                recordEntity.getServiceName(),
                recordEntity.getEnvCode(),
                recordEntity.getBaseGitUrl(),
                recordEntity.getBaseVersion(),
                recordEntity.getNowVersion());
        if (StringUtils.isEmpty(recordEntity.getBaseGitUrl())) {
            throw new RRException("基准git地址不能为空");
        }

        if (!recordEntity.getBaseGitUrl().startsWith("git@")) {
            throw new RRException("代码地址必须是ssh的git@开头");
        }

        if (StringUtils.isEmpty(recordEntity.getEnvCode())) {
            throw new RRException("envCode不能为空");
        }

        if (recordEntity.getBaseVersion() == null
                || recordEntity.getBaseVersion().equals(recordEntity.getNowVersion())) {
            throw new RRException("commitId相同，不进行比对");
        }

        recordEntity.getBaseVersion();

        Boolean dingMsgExist = diffAnalyseService.preAdd(recordEntity);
        recordEntity.setCreatedTime(new Date());
        diffAnalyseService.addDiffAnalyseData(recordEntity, dingMsgExist);
        return R.ok();
    }

    /**
     * 查询服务列表及版本信息
     *
     * 获取所有服务的差异分析列表，支持分页查询
     *
     * @param params 查询参数，包含分页信息page、limit等
     * @return R 返回分页查询结果，包含服务列表和总数信息
     */
    @RequestMapping("/service/list")
    public R serviceList(@RequestParam Map<String, Object> params) {
        // 查询列表数据
        Query query = new Query(params);
        PageUtils pageUtil = diffAnalyseService.queryServiceList(query);

        return R.ok().put("page", pageUtil);
    }

    /**
     * 查询变更记录最近一次比对列表
     *
     * 获取每个服务最近一次的变更分析记录列表，支持分页查询
     *
     * @param params 查询参数，包含分页信息page、limit等
     * @return R 返回分页查询结果，包含最近变更记录列表和总数信息
     */
    @RequestMapping("/latestList")
    public R latestList(@RequestParam Map<String, Object> params) {
        // 查询列表数据
        Query query = new Query(params);
        PageUtils pageUtil = diffAnalyseService.queryLatestList(query);

        return R.ok().put("page", pageUtil);
    }

    /**
     * 查询变更记录列表
     *
     * 获取所有变更分析记录列表，支持分页查询和条件过滤
     *
     * @param params 查询参数，包含分页信息page、limit和过滤条件
     * @return R 返回分页查询结果，包含变更记录列表和总数信息
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        // 查询列表数据
        Query query = new Query(params);
        PageUtils pageUtil = diffAnalyseService.queryList(query);

        return R.ok().put("page", pageUtil);
    }

    /**
     * 查询差异分析详细信息
     *
     * 根据ID查询指定差异分析记录的详细信息
     *
     * @param params 查询参数，必须包含id参数
     * @return R 返回差异分析详细信息
     * @throws RRException 当id参数为空时抛出异常
     */
    @OperateLogger
    @RequestMapping("/file")
    public R queryDiffAnalyseDetail(@RequestParam Map<String, Object> params) {
        if (params == null || params.get("id") == null) {
            throw new RRException("id不能为空");
        }
        Long id = Long.parseLong(params.get("id").toString());
        DiffResultDetailEntity detailEntity = diffAnalyseService.queryDetailById(id);
        return R.ok().put("info", detailEntity);
    }

    /**
     * 刷新影响的场景
     */
    @RequestMapping("/refresh/scene")
    public R refreshScene(@RequestParam Map<String, Object> params) {

        diffAnalyseService.refreshScene(
                Long.valueOf(params.get("resultId").toString()), params.get("serviceName").toString());
        return R.ok();
    }

    /**
     * 查询具体信息
     */
    @RequestMapping("/getCompareFile")
    public R getCompareFile(@RequestParam Map<String, Object> params) throws FileNotFoundException {
        if (params.get("id") == null || params.get("classFile") == null) {
            throw new RRException("参数中id和文件名不能为空");
        }
        Map<String, String> diff = diffAnalyseService.getCompareFile(params);
        return R.ok().put("info", diff);
    }

    /**
     * 查询具体信息
     */
    @RequestMapping("/reDownloadCodeDiff/{id}")
    public R reDownloadCodeDiff(@PathVariable("id") Long id) {
        if (id == null) {
            throw new RRException("参数中id不能为空");
        }

        diffAnalyseService.reDownloadCodeDiff(id);
        return R.ok();
    }

    /**
     * 查询测试具体信息
     */
    @RequestMapping("/query/test")
    public R queryTest(@RequestBody DiffRecordEntity recordEntity) {
        if (recordEntity == null) {
            throw new RRException("参数recordEntity不能为空");
        }
        if (recordEntity.getId() == null) {
            throw new RRException("参数中id不能为空");
        }
        JSONObject test = diffAnalyseService.queryTest(recordEntity);
        return R.ok().put("info", test);
    }


    /**
     * 标记traceId为已查看
     */
    @RequestMapping("/markAsViewed")
    public R markAsViewed(@RequestBody Map<String, Object> params) {
        String traceId = (String) params.get("traceId");
        Boolean viewed = (Boolean) params.get("viewed");
        String operator = (String) params.get("operator"); // 获取操作用户

        // 添加调试日志
        log.info("接收到标记请求，traceId: {}, viewed: {}, operator: {}", traceId, viewed, operator);
        
        if (traceId == null || viewed == null) {
            throw new RRException("参数traceId和viewed不能为空");
        }

        // 调用service层方法更新状态
        boolean result = diffAnalyseService.updateTraceIdStatus(traceId, viewed ? 1 : 0, operator);

        
        if (result) {
            return R.ok();
        } else {
            return R.error("标记失败");
        }
    }

    /**
     * 获取traceId的查看状态
     */
    @RequestMapping("/getViewedStatus")
    public R getViewedStatus(@RequestParam String traceId) {
        if (traceId == null) {
            throw new RRException("参数traceId不能为空");
        }
        boolean viewed = diffAnalyseService.getViewedStatus(traceId);
        return R.ok().put("viewed", viewed);
    }

    /**
     * 查询去重的报告地址列表（支持分页和过滤）
     *
     * 获取去重后的分析报告列表，按照服务参数组合进行去重，支持分页和条件过滤
     *
     * @param page 页码，默认为1
     * @param limit 每页数量，默认为10
     * @param serviceName 服务名称，可选过滤条件
     * @param envCode 环境编码，可选过滤条件
     * @param branchName 分支名称，可选过滤条件
     * @return R 返回去重后的报告列表，包含分页信息和统计数据
     */
    @GetMapping("/reportList")
    public R getReportList(@RequestParam(defaultValue = "1") Integer page,
                           @RequestParam(defaultValue = "10") Integer limit,
                           @RequestParam(required = false) String serviceName,
                           @RequestParam(required = false) String envCode,
                           @RequestParam(required = false) String branchName) {
        try {
            // 查询所有符合条件的记录
            List<SceneTraceid> records = sceneTraceIdMapper.getTraceIdsByServiceParams(serviceName, envCode, branchName);
            
            // 用于存储去重的报告参数组合和对应的traceId列表
            Map<String, ReportVO> reportMap = new LinkedHashMap<>();
            
            // 遍历记录，根据服务参数组合进行去重
            for (SceneTraceid record : records) {
                // 创建服务参数组合的唯一标识
                String reportKey = createReportKey(record);
                
                if (reportMap.containsKey(reportKey)) {
                    // 如果已存在相同的报告参数组合，添加traceId到列表中
                    ReportVO reportVO = reportMap.get(reportKey);
                    reportVO.getTraceIds().add(record.getTraceid());
                    // 更新记录数
                    reportVO.setRecordCount(reportVO.getRecordCount() + 1);
                    // 更新traceIdCount
                    reportVO.setTraceIdCount(reportVO.getTraceIdCount() + 1);
                    
                    // 如果状态为已查看（假设1表示已查看），则增加已查看数量
                    Integer status = record.getStatus();
                    if (status != null && status == 1) {
                        reportVO.setViewedCount(reportVO.getViewedCount() + 1);
                    }
                } else {
                    // 如果不存在相同的报告参数组合，创建新的ReportVO
                    ReportVO reportVO = new ReportVO();
                    reportVO.setServiceName(record.getServiceName());
                    reportVO.setEnvCode(record.getEnvCode());
                    reportVO.setBranchName(record.getBranchName());
                    reportVO.setBaseVersion(record.getBaseVersion());
                    reportVO.setNowVersion(record.getNowVersion());
                    
                    // 创建traceId列表并添加当前traceId
                    List<String> traceIds = new ArrayList<>();
                    traceIds.add(record.getTraceid());
                    reportVO.setTraceIds(traceIds);
                    
                    // 设置记录数
                    reportVO.setRecordCount(1);
                    // 设置traceIdCount
                    reportVO.setTraceIdCount(1);
                    
                    // 如果状态为已查看（假设1表示已查看），则设置已查看数量为1，否则为0
                    Integer status = record.getStatus();
                    if (status != null && status == 1) {
                        reportVO.setViewedCount(1);
                    } else {
                        reportVO.setViewedCount(0);
                    }
                    
                    // 将新的ReportVO添加到map中
                    reportMap.put(reportKey, reportVO);
                }
            }
            
            // 将map中的值转换为列表
            List<ReportVO> allReports = new ArrayList<>(reportMap.values());
            
            // 按指定优先级排序
            allReports.sort((r1, r2) -> {
                // 按应用名排序
                int serviceCompare = r1.getServiceName().compareTo(r2.getServiceName());
                if (serviceCompare != 0) {
                    return serviceCompare;
                }
                
                // 按环境排序
                int envCompare = r1.getEnvCode().compareTo(r2.getEnvCode());
                if (envCompare != 0) {
                    return envCompare;
                }
                
                // 按基础版本排序
                int baseVersionCompare = r1.getBaseVersion().compareTo(r2.getBaseVersion());
                if (baseVersionCompare != 0) {
                    return baseVersionCompare;
                }
                
                // 按当前版本排序
                int nowVersionCompare = r1.getNowVersion().compareTo(r2.getNowVersion());
                if (nowVersionCompare != 0) {
                    return nowVersionCompare;
                }
                
                // 按分支名称排序
                return r1.getBranchName().compareTo(r2.getBranchName());
            });
            
            // 分页处理
            int total = allReports.size();
            int fromIndex = (page - 1) * limit;
            int toIndex = Math.min(fromIndex + limit, total);
            
            // 确保索引有效
            List<ReportVO> pagedReports = new ArrayList<>();
            if (fromIndex < total) {
                pagedReports = allReports.subList(fromIndex, toIndex);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("reports", pagedReports);
            result.put("total", total);
            result.put("page", page);
            result.put("limit", limit);
            result.put("pages", (int) Math.ceil((double) total / limit)); // 添加总页数
            
            return R.ok().put("data", result);
        } catch (Exception e) {
            log.error("查询报告列表失败", e);
            return R.error("查询报告列表失败");
        }
    }
    
    /**
     * 创建报告参数组合的唯一标识
     *
     * 根据服务参数组合生成唯一的key，用于报告去重
     *
     * @param record 场景追踪记录实体
     * @return String 返回参数组合的唯一标识字符串
     */
    private String createReportKey(SceneTraceid record) {
        return record.getServiceName() + "|" + 
               record.getEnvCode() + "|" + 
               record.getBranchName() + "|" + 
               record.getBaseVersion() + "|" + 
               record.getNowVersion();
    }
}