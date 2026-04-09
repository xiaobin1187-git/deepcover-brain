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

import io.deepcover.brain.service.service.HbaseDataAnalysisService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ares大脑核心功能控制器
 *
 * 提供Ares大脑的核心功能API接口，主要包括HBase数据分析等功能
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
@RestController
@RequestMapping(value = "aresbrain", produces = "application/json")
public class AresBrainController {

    @Autowired
    HbaseDataAnalysisService analysisService;

    /**
     * 分析HBase数据
     *
     * 根据指定的traceId分析HBase中存储的数据，用于性能分析和问题排查
     *
     * @param traceId 链路追踪ID，用于标识一次完整的请求链路
     */
    @ApiOperation(value = "analysisHbaseData")
    @RequestMapping(value = "/analysisHbaseData", method = RequestMethod.GET)
    public void analysisHbaseData(@RequestParam String traceId) {
        analysisService.analysisHbaseData(traceId);
    }
}
