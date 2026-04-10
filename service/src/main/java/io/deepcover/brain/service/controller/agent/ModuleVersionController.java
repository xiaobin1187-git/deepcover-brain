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

import io.deepcover.brain.dal.entity.*;
import io.deepcover.brain.service.exception.RRException;
import io.deepcover.brain.service.service.ModuleVersionService;
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
 * 模块版本管理控制器
 *
 * 提供模块版本管理的相关API接口，包括版本的增删改查、
 * 服务版本管理、模块激活/冻结等功能
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
@Slf4j
@RestController
@RequestMapping("/aresbrain/module/version")
public class ModuleVersionController {

    @Autowired
    private ModuleVersionService moduleVersionService;

    /**
     * 查询应用列表及版本信息
     *
     * 获取所有应用的模块版本信息，支持分页查询
     *
     * @param params 查询参数，包含分页信息page、limit等
     * @return R 返回分页结果，包含服务列表和总数信息
     */
    @RequestMapping("/service/list")
    public R serviceList(@RequestParam Map<String, Object> params) {
        //查询列表数据
        Query query = new Query(params);
        List<ServiceModuleVersionEntity> serviceList = moduleVersionService.queryServiceList(query);
        int total = moduleVersionService.queryServiceTotal(query);

        PageUtils pageUtil = new PageUtils(serviceList, total, query.getLimit(), query.getPage());

        return R.ok().put("page", pageUtil);
    }

    /**
     * 新增模块版本信息
     *
     * 添加新的模块版本记录，包含分支信息和提交ID
     *
     * @param moduleVersionEntity 模块版本实体，包含类型、分支、提交ID等信息
     * @return R 返回操作结果，包含新增的版本信息
     */
    @RequestMapping("/add")
    public R add(@RequestBody ModuleVersionEntity moduleVersionEntity) {
        if (moduleVersionEntity == null||moduleVersionEntity.getType() == null || StringUtils.isEmpty(moduleVersionEntity.getBranch()) || StringUtils.isEmpty(moduleVersionEntity.getCommitId())) {
            throw new RRException("参数中分支和commitId都不能为空");
        }

        moduleVersionService.add(moduleVersionEntity);
        return R.ok().put("data",moduleVersionEntity);
    }

    /**
     * 删除模块版本信息
     *
     * 根据ID删除指定的模块版本记录
     *
     * @param id 模块版本ID
     * @return R 返回删除操作结果
     */
    @RequestMapping("/delete/{id}")
    public R delete(@PathVariable Long id) {
        if (id == null) {
            throw new RRException("参数中id都不能为空");
        }

        moduleVersionService.deleteById(id);
        return R.ok();
    }

    /**
     * 查询版本列表
     *
     * 获取模块版本列表，支持查询参数过滤
     *
     * @param params 查询参数，用于过滤版本信息
     * @return R 返回查询结果，包含版本列表和总数
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        //查询列表数据
        if(params.get("limit")!=null){
            params = new Query(params);
        }
        List<ModuleVersionEntity> aresList = moduleVersionService.queryList(params);
        int total = moduleVersionService.queryTotal(params);
        return R.ok().put("data", aresList).put("total",total);
    }

    /**
     * 修改服务版本信息
     *
     * 更新指定服务的版本配置信息
     *
     * @param params 服务版本实体，包含服务名称和版本信息
     * @return R 返回更新操作结果
     */
    @RequestMapping("/update/service")
    public R updateServiceVersion(@RequestBody ServiceModuleVersionEntity params) {
        if (params == null || StringUtils.isEmpty(params.getServiceName()) ) {
            throw new RRException("参数中应用名称都不能为空");
        }
        moduleVersionService.updateServiceVersion(params);
        return R.ok();
    }

    /**
     * 查询默认版本信息
     *
     * 根据服务名称查询该服务的默认模块版本信息
     *
     * @param serviceName 服务名称
     * @return R 返回查询结果，包含默认版本信息
     */
    @RequestMapping("/query/default")
    public R queryDefault(@RequestParam String serviceName) {
        if (serviceName == null ) {
            throw new RRException("参数中应用名称都不能为空");
        }
        ServiceModuleVersionEntity module = moduleVersionService.queryDefault(serviceName);
        return R.ok().put("data",module);
    }

    /**
     * 冻结指定模块
     *
     * 通过调用sandbox接口冻结指定服务的模块
     * 转发调用sandbox，示例：http://ip:4769/sandbox/default/module/http/sandbox-module-mgr/frozen?ids=code-coverage
     *
     * @param reportAgentEntity 服务节点实体，包含IP和服务名称
     * @param name 要冻结的模块名称
     * @return R 返回冻结操作的结果
     * @throws RRException 当冻结失败时抛出异常
     */
    @RequestMapping("/frozen/{name}")
    public R frozenCodeModule(@RequestBody ReportAgentEntity reportAgentEntity,@PathVariable("name") String name) {
        String url = "http://" + reportAgentEntity.getIp() + ":4769/sandbox/default/module/http/sandbox-module-mgr/frozen?ids=" + name;
        log.info("执行：{}",url);
        HttpClientRequest request = new HttpClientRequest();
        request.setType(1);
        request.setUrl(url);
        HttpClientResponse response = HttpClientUtil.sendRequest(request);
        String body;
        if (response.getResponseBody() == null) {
            throw new RRException("应用=" + reportAgentEntity.getServiceName() + ",ip=" + reportAgentEntity.getIp() + "冻结失败");
        } else {
            body = response.getResponseBody().toString();
        }
//        List<ReportAgentEntity> reportList = aresAgentService.listReportServerInfo(serviceName);
        return R.ok().put("info", body);
    }

    /**
     * 激活指定模块
     *
     * 通过调用sandbox接口激活指定服务的模块
     * 转发调用sandbox，示例：http://ip:4769/sandbox/default/module/http/sandbox-module-mgr/active?ids=code-coverage
     *
     * @param reportAgentEntity 服务节点实体，包含IP和服务名称
     * @param name 要激活的模块名称
     * @return R 返回激活操作的结果
     * @throws RRException 当激活失败时抛出异常
     */
    @RequestMapping("/active/{name}")
    public R activeCodeModule(@RequestBody ReportAgentEntity reportAgentEntity,@PathVariable("name") String name) {
        String url = "http://" + reportAgentEntity.getIp() + ":4769/sandbox/default/module/http/sandbox-module-mgr/active?ids=" + name;
        log.info("执行：{}",url);
        HttpClientRequest request = new HttpClientRequest();
        request.setType(1);
        request.setUrl(url);
        HttpClientResponse response = HttpClientUtil.sendRequest(request);
        String body;
        if (response.getResponseBody() == null) {
            throw new RRException("应用=" + reportAgentEntity.getServiceName() + ",ip=" + reportAgentEntity.getIp() + "激活失败");
        } else {
            body = response.getResponseBody().toString();
        }
//        List<ReportAgentEntity> reportList = aresAgentService.listReportServerInfo(serviceName);
        return R.ok().put("info", body);
    }
}