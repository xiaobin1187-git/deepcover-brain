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

package io.deepcover.brain.service.controller;

import io.deepcover.brain.model.*;
import io.deepcover.brain.service.aop.OperateLogger;
import io.deepcover.brain.service.service.AresBrainSceneService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ares大脑场景管理控制器
 *
 * 提供场景管理相关的API接口，包括场景的增删改查、场景标记、
 * 项目服务查询、类方法查询等功能
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
@RestController
@RequestMapping(value = "aresbrain", produces = "application/json")
public class AresBrainSceneController {

    @Autowired
    private AresBrainSceneService aresBrainSceneService;

    /**
     * 添加场景反馈信息
     *
     * @param sceneModel 场景更新模型，包含场景ID和反馈信息
     * @return AresBrainResult<String> 返回操作结果，包含反馈信息
     */
    @OperateLogger
    @ApiOperation(value = "addFeedBack")
    @RequestMapping(value = "/addFeedBack", method = RequestMethod.POST)
    public AresBrainResult<String> addFeedBack(@RequestBody UpdateSceneBO sceneModel) {
        AresBrainResult<String> result = new AresBrainResult();
        result.setData(aresBrainSceneService.addFeedBack(sceneModel));
        return result;
    }

    /**
     * 标记场景为核心场景
     *
     * @param sceneModel 场景更新模型，包含场景ID和标记信息
     * @return AresBrainResult<String> 返回操作结果，包含标记信息
     */
    @OperateLogger
    @ApiOperation(value = "markCore")
    @RequestMapping(value = "/markCore", method = RequestMethod.POST)
    public AresBrainResult<String> markCore(@RequestBody UpdateSceneBO sceneModel) {
        AresBrainResult<String> result = new AresBrainResult();
        result.setData(aresBrainSceneService.markCore(sceneModel));
        return result;
    }

    /**
     * 设置场景信息
     *
     * @param sceneModel 场景更新模型，包含场景的详细信息
     * @return AresBrainResult<String> 返回操作结果，包含设置信息
     */
    @OperateLogger
    @ApiOperation(value = "setScene")
    @RequestMapping(value = "/setScene", method = RequestMethod.POST)
    public AresBrainResult<String> setScene(@RequestBody UpdateSceneBO sceneModel) {
        AresBrainResult<String> result = new AresBrainResult();
        result.setData(aresBrainSceneService.setScene(sceneModel));
        return result;
    }

    /**
     * 查询项目/服务列表
     *
     * @return AresBrainResult 返回查询结果，包含所有服务信息
     */
    @ApiOperation(value = "queryProject")
    @RequestMapping(value = "/queryProject", method = RequestMethod.POST)
    public AresBrainResult queryService() {
        AresBrainResult result = new AresBrainResult();
        result.setData(aresBrainSceneService.queryService());
        return result;
    }

    /**
     * 查询网关服务列表
     *
     * @return AresBrainResult 返回查询结果，包含所有网关服务信息
     */
    @ApiOperation(value = "queryGatewayService")
    @RequestMapping(value = "/queryGatewayService", method = RequestMethod.POST)
    public AresBrainResult queryGatewayService() {
        AresBrainResult result = new AresBrainResult();
        result.setData(aresBrainSceneService.queryGatewayService());
        return result;
    }

    /**
     * 查询网关API列表
     *
     * @param searchModel 搜索模型，包含查询条件
     * @return AresBrainResult 返回查询结果，包含匹配的网关API信息
     */
    @ApiOperation(value = "queryGatewayApi")
    @RequestMapping(value = "/queryGatewayApi", method = RequestMethod.POST)
    public AresBrainResult queryGatewayApi(@RequestBody SearchModel searchModel) {
        AresBrainResult result = new AresBrainResult();
        result.setData(aresBrainSceneService.queryGatewayApi(searchModel));
        return result;
    }

    /**
     * 查询类列表
     *
     * @param searchModel 搜索模型，包含查询条件
     * @return AresBrainResult 返回查询结果，包含匹配的类信息
     */
    @ApiOperation(value = "queryClass")
    @RequestMapping(value = "/queryClass", method = RequestMethod.POST)
    public AresBrainResult queryClass(@RequestBody SearchModel searchModel) {
        AresBrainResult result = new AresBrainResult();
        result.setData(aresBrainSceneService.queryClass(searchModel));
        return result;
    }

    /**
     * 查询方法列表
     *
     * @param searchModel 搜索模型，包含查询条件
     * @return AresBrainResult 返回查询结果，包含匹配的方法信息
     */
    @ApiOperation(value = "queryMehtod")
    @RequestMapping(value = "/queryMehtod", method = RequestMethod.POST)
    public AresBrainResult queryMehtod(@RequestBody SearchModel searchModel) {
        AresBrainResult result = new AresBrainResult();
        result.setData(aresBrainSceneService.queryMehtod(searchModel));
        return result;
    }

    /**
     * 查询方法参数列表
     *
     * @param searchModel 搜索模型，包含查询条件
     * @return AresBrainResult 返回查询结果，包含匹配的方法参数信息
     */
    @ApiOperation(value = "queryMehtodParameters")
    @RequestMapping(value = "/queryMehtodParameters", method = RequestMethod.POST)
    public AresBrainResult queryMehtodParameters(@RequestBody SearchModel searchModel) {
        AresBrainResult result = new AresBrainResult();
        result.setData(aresBrainSceneService.queryMehtodParameters(searchModel));
        return result;
    }

    /**
     * 查询行号范围
     *
     * @param searchModel 搜索模型，包含查询条件
     * @return AresBrainResult 返回查询结果，包含行号范围信息
     */
    @ApiOperation(value = "queryLineNums")
    @RequestMapping(value = "/queryLineNums", method = RequestMethod.POST)
    public AresBrainResult queryLineNums(@RequestBody SearchModel searchModel) {
        AresBrainResult result = new AresBrainResult();
        result.setData(aresBrainSceneService.queryLineNums(searchModel));
        return result;
    }

    /**
     * 查询场景详细信息
     *
     * @param searchModel 搜索模型，包含场景查询条件
     * @return AresBrainResult<FrontModel> 返回查询结果，包含场景的详细信息
     */
    @OperateLogger
    @ApiOperation(value = "querySceneDetail")
    @RequestMapping(value = "/querySceneDetail", method = RequestMethod.POST)
    public AresBrainResult<FrontModel> querySceneDetail(@RequestBody SearchModel searchModel) {
        AresBrainResult<FrontModel> result = new AresBrainResult();
        result.setData(aresBrainSceneService.querySceneDetail(searchModel));
        return result;
    }

    /**
     * 查询场景列表
     *
     * @param querySceneBO 场景查询模型，包含分页和筛选条件
     * @return AresBrainResult 返回查询结果，包含场景列表信息
     */
    @OperateLogger
    @ApiOperation(value = "queryScene")
    @RequestMapping(value = "/queryScene", method = RequestMethod.POST)
    public AresBrainResult queryScene(@RequestBody QuerySceneBO querySceneBO) {
        return aresBrainSceneService.queryScene(querySceneBO);
    }

    /**
     * 查询场景审计日志
     *
     * @param searchModel 搜索模型，包含日志查询条件
     * @return AresBrainResult 返回查询结果，包含审计日志信息
     */
    @ApiOperation(value = "querySceneAuditLog")
    @RequestMapping(value = "/querySceneAuditLog", method = RequestMethod.POST)
    public AresBrainResult queryScene(@RequestBody SearchModel searchModel) {
        return aresBrainSceneService.querySceneAuditLog(searchModel);
    }

    /**
     * 查询分支列表
     *
     * @return AresBrainResult 返回查询结果，包含所有分支信息
     */
    @ApiOperation(value = "queryBranch")
    @RequestMapping(value = "/queryBranch", method = RequestMethod.GET)
    public AresBrainResult queryScene() {
        AresBrainResult result = new AresBrainResult();
        result.setData(aresBrainSceneService.queryBranch());
        return result;
    }

    /**
     * 批量查询场景信息（异步处理）
     *
     * @param querySceneBO 场景查询模型，包含批量查询条件
     */
    @OperateLogger
    @ApiOperation(value = "querySceneBatch")
    @RequestMapping(value = "/querySceneBatch", method = RequestMethod.POST)
    public void querySceneBatch(@RequestBody QuerySceneBO querySceneBO) {
        aresBrainSceneService.querySceneBatch(querySceneBO);
    }
}