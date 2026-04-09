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

package io.deepcover.brain.service.controller.agent;

import com.alibaba.fastjson.JSONObject;
import io.deepcover.brain.dal.entity.AresAgentBatchEntity;
import io.deepcover.brain.dal.entity.AresAgentEntity;
import io.deepcover.brain.dal.entity.ReportAgentEntity;
import io.deepcover.brain.service.exception.RRException;
import io.deepcover.brain.service.service.AresAgentService;
import io.deepcover.brain.service.util.PageUtils;
import io.deepcover.brain.service.util.Query;
import io.deepcover.brain.service.util.R;
import io.deepcover.brain.service.util.client.HttpClientRequest;
import io.deepcover.brain.service.util.client.HttpClientResponse;
import io.deepcover.brain.service.util.client.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Ares智能体管理控制器
 *
 * 提供Ares智能体管理的相关API接口，包括智能体的增删改查、
 * 服务信息上报、模块管理、配置一致性检查等功能
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
@Slf4j
@RestController
@RequestMapping("/aresbrain/ares")
public class AresAgentController {
    @Autowired
    private AresAgentService aresAgentService;

    /**
     * 新增Ares智能体配置
     *
     * 添加新的Ares智能体配置信息，包括服务名、包名等基本信息
     *
     * @param aresAgentEntity Ares智能体实体，包含服务名、包名等配置信息
     * @return R 返回操作结果
     * @throws RRException 当应用名或包名为空时抛出异常
     */
    @RequestMapping("/add")
    public R add(@RequestBody AresAgentEntity aresAgentEntity) {
        if (aresAgentEntity == null || StringUtils.isEmpty(aresAgentEntity.getServiceName()) || StringUtils.isEmpty(aresAgentEntity.getPackageName())) {
            throw new RRException("参数中应用名和包名都不能为空");
        }

        aresAgentService.add(aresAgentEntity);
        return R.ok();
    }

    /**
     * 修改Ares智能体配置
     *
     * 更新已存在的Ares智能体配置信息
     *
     * @param aresAgentEntity Ares智能体实体，包含要更新的配置信息
     * @return R 返回操作结果
     * @throws RRException 当应用名或包名为空，或本地化配置值不合法时抛出异常
     */
    @RequestMapping("/update")
    public R update(@RequestBody AresAgentEntity aresAgentEntity) {
        if (aresAgentEntity == null || StringUtils.isEmpty(aresAgentEntity.getServiceName()) || StringUtils.isEmpty(aresAgentEntity.getPackageName())) {
            throw new RRException("参数中应用名和包名都不能为空");
        }
        if (aresAgentEntity.getLocal() != 0 && aresAgentEntity.getLocal() != 1) {
            throw new RRException("本地化只能是0或者1");
        }
        aresAgentService.update(aresAgentEntity);
        return R.ok();
    }

    /**
     * 批量修改Ares智能体配置
     *
     * 批量更新多个Ares智能体的配置信息
     *
     * @param aresAgentBatchEntity 批量更新实体，包含要更新的智能体ID列表和配置信息
     * @return R 返回操作结果
     * @throws RRException 当智能体列表为空或本地化配置值不合法时抛出异常
     */
    @RequestMapping("/batch/update")
    public R batchUpdate(@RequestBody AresAgentBatchEntity aresAgentBatchEntity) {
        if (aresAgentBatchEntity == null || aresAgentBatchEntity.getIds() == null || aresAgentBatchEntity.getIds().size() == 0) {
            throw new RRException("参数中应用不能为空");
        }
        if (aresAgentBatchEntity.getLocal() != null && aresAgentBatchEntity.getLocal() != 0 && aresAgentBatchEntity.getLocal() != 1) {
            throw new RRException("本地化只能是0或者1");
        }

        aresAgentService.batchUpdate(aresAgentBatchEntity);
        return R.ok();
    }

    /**
     * 查询Ares智能体列表
     *
     * 根据查询条件获取Ares智能体列表，支持分页查询
     *
     * @param params 查询参数，包含分页信息page、limit等
     * @return R 返回分页查询结果，包含智能体列表和总数信息
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        //查询列表数据
        Query query = new Query(params);
        List<AresAgentEntity> aresList = aresAgentService.queryList(query);
        int total = aresAgentService.queryTotal(query);

        PageUtils pageUtil = new PageUtils(aresList, total, query.getLimit(), query.getPage());

        return R.ok().put("page", pageUtil);
    }

    /**
     * 查询配置不一致的智能体
     *
     * 检查并返回配置不一致的Ares智能体列表
     *
     * @param params 查询参数
     * @return R 返回配置不一致的智能体信息
     */
    @RequestMapping("/diff")
    public R diff(@RequestParam Map<String, Object> params) {
        JSONObject aresDiff = aresAgentService.queryDiffList();
        return R.ok().put("data", aresDiff);
    }




    /**
     * 查询所有服务名称列表
     *
     * 获取系统中所有已配置Ares智能体的服务名称
     *
     * @return R 返回服务名称列表
     */
    @RequestMapping("/list/serviceName")
    public R allServiceNameList() {
        //查询列表数据
        List<String> list = aresAgentService.getAllServiceName();

        return R.ok().put("info", list);
    }

    /**
     * 查询指定服务的Ares智能体信息
     *
     * 根据服务名称查询对应的Ares智能体配置详情
     *
     * @param serviceName 服务名称
     * @return R 返回Ares智能体的详细配置信息
     */
    @RequestMapping("/info/{serviceName}")
    public R info(@PathVariable("serviceName") String serviceName) {
        AresAgentEntity aresAgentEntity = aresAgentService.queryObject(serviceName);
        return R.ok().put("info", aresAgentEntity);
    }

    /**
     * 采集上报的服务信息
     *
     * 接收并处理服务节点上报的信息，包括IP、版本等运行时数据
     *
     * @param serverEntity 服务节点实体，包含IP、版本、服务名等信息
     * @return R 返回操作结果和对应的智能体配置信息
     */
    @PostMapping("/report/server/info")
    public R reportServerInfo(@RequestBody ReportAgentEntity serverEntity) {
        AresAgentEntity aresAgentEntity = aresAgentService.queryObject(serverEntity.getServiceName());
        aresAgentService.syncServerInfo(serverEntity);
//        log.info("serviceName:{},ip：{},version:{}",serverEntity.getServiceName(),serverEntity.getIp(),serverEntity.getVersion());
        return R.ok().put("info", aresAgentEntity);
    }

    /**
     * 查询指定服务的上报节点信息
     *
     * 获取指定服务下所有上报了信息的节点列表
     *
     * @param serviceName 服务名称
     * @return R 返回该服务下的所有节点信息列表
     */
    @RequestMapping("/list/report/server/info/{serviceName}")
    public R listReportServerInfo(@PathVariable("serviceName") String serviceName) {
        List<ReportAgentEntity> reportList = aresAgentService.listReportServerInfo(serviceName);
        return R.ok().put("info", reportList);
    }

    /**
     * 卸载代码覆盖率模块
     *
     * 通过调用sandbox接口卸载指定服务的代码覆盖率模块
     * 转发调用sandbox，示例：http://ip:4769/sandbox/default/module/http/code-coverage/unloadCodeModule?serviceName=file_system
     *
     * @param reportAgentEntity 服务节点实体，包含IP和服务名称
     * @return R 返回卸载操作的结果
     * @throws RRException 当卸载失败时抛出异常
     */
    @RequestMapping("/http/codeCoverage/unloadCodeModule")
    public R unloadCodeModule(@RequestBody ReportAgentEntity reportAgentEntity) {
        HttpClientRequest request = new HttpClientRequest();
        request.setType(1);
        request.setUrl("http://" + reportAgentEntity.getIp() + ":4769/sandbox/default/module/http/code-coverage/unloadCodeModule?serviceName=" + reportAgentEntity.getServiceName());
        HttpClientResponse response = HttpClientUtil.sendRequest(request);
        String body;
        if (response.getResponseBody() == null) {
            throw new RRException("应用=" + reportAgentEntity.getServiceName() + ",ip=" + reportAgentEntity.getIp() + "卸载失败");
        } else {
            body = response.getResponseBody().toString();
        }
//        List<ReportAgentEntity> reportList = aresAgentService.listReportServerInfo(serviceName);
        return R.ok().put("info", body);
    }
}