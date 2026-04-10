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

import io.deepcover.brain.model.AresBrainResult;
import io.deepcover.brain.model.QuerySceneBO;
import io.deepcover.brain.service.aop.OperateLogger;
import io.deepcover.brain.service.service.AresBrainLinkService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ares大脑链接管理控制器
 *
 * 提供链接管理相关的API接口，主要用于查询场景链接的详细信息
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
@RestController
@RequestMapping(value = "aresbrain", produces = "application/json")
public class AresBrainLinkController {

    @Autowired
    private AresBrainLinkService aresBrainLinkService;

    /**
     * 查询链接详细信息
     *
     * 根据查询条件获取场景链接的详细信息，包括关联的服务、API等信息
     *
     * @param querySceneBO 场景查询模型，包含分页和筛选条件
     * @return AresBrainResult 返回查询结果，包含链接详细信息
     */
    @OperateLogger
    @ApiOperation(value = "queryLinkDetail")
    @RequestMapping(value = "/queryLinkDetail", method = RequestMethod.POST)
    public AresBrainResult queryLinkDetail(@RequestBody QuerySceneBO querySceneBO) {
        return aresBrainLinkService.queryLinkDetail(querySceneBO);
    }
}