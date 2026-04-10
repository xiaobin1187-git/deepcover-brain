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

package io.deepcover.brain.service.controller.repeater;

import io.deepcover.brain.model.ApiRequestAresVo;
import io.deepcover.brain.model.AresBrainResult;
import io.deepcover.brain.model.RepeaterModel;
import io.deepcover.brain.service.aop.OperateLogger;
import io.deepcover.brain.service.service.AresBrainRepeaterService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Ares回放管理控制器
 *
 * 提供场景回放管理的相关API接口，包括单次回放、批量回放、
 * 回放结果查询、测试用例生成等功能
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
@RestController
@RequestMapping(value = "aresbrain", produces = "application/json")
public class AresRepeaterController {

    @Autowired
    AresBrainRepeaterService aresBrainRepeaterService;

    /**
     * 开始场景回放
     *
     * 执行单次场景回放操作，根据回放模型配置重现指定场景
     *
     * @param repeaterModel 回放模型，包含场景ID、回放参数等配置信息
     * @return AresBrainResult 返回回放操作的结果
     */
    @OperateLogger
    @ApiOperation(value = "startReplay")
    @RequestMapping(value = "/startReplay", method = RequestMethod.POST)
    public AresBrainResult startReplay(@RequestBody RepeaterModel repeaterModel) {
        return aresBrainRepeaterService.startReplay(repeaterModel);
    }

    /**
     * 批量场景回放
     *
     * 执行批量场景回放操作，支持一次回放多个场景
     *
     * @param repeaterModel 回放模型，包含批量回放的配置信息
     */
    @OperateLogger
    @ApiOperation(value = "startReplayBatch")
    @RequestMapping(value = "/startReplayBatch", method = RequestMethod.POST)
    public void startReplayBatch(@RequestBody RepeaterModel repeaterModel) {
        aresBrainRepeaterService.startReplayBatch(repeaterModel);
    }

    /**
     * 查询回放结果
     *
     * 根据场景ID查询指定场景的回放结果
     *
     * @param sceneId 场景ID
     * @return AresBrainResult 返回回放结果信息
     */
    @ApiOperation(value = "ReplayResult")
    @RequestMapping(value = "/ReplayResult", method = RequestMethod.GET)
    public AresBrainResult ReplayResult(@RequestParam long sceneId) {
        AresBrainResult result = new AresBrainResult();
        result.setData(aresBrainRepeaterService.ReplayResult(sceneId));
        return result;
    }

    /**
     * 创建测试用例
     *
     * 基于API请求信息自动创建测试用例
     *
     * @param apiRequestVo API请求数据对象，包含请求参数、接口信息等
     * @return AresBrainResult 返回测试用例创建结果
     */
    @OperateLogger
    @ApiOperation(value = "aresCreateTestCase")
    @RequestMapping(value = "/aresCreateTestCase", method = RequestMethod.POST)
    public AresBrainResult aresCreateTestCase(@RequestBody ApiRequestAresVo apiRequestVo) {
        return aresBrainRepeaterService.aresCreateTestCase(apiRequestVo);
    }
}
