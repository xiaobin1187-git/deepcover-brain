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

package io.deepcover.brain.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

/**
 * @Author: huangtai
 * @Description: 场景热度模型
 * @Date: 2023-4-20 19:59
 */
@Setter
@Getter
public class SceneModel {

    Long id;

    //场景ID
    String sceneId;
    //场景名称
    String sceneName;
    //应用顺序
    String serviceOrder;
    //是否核心 0核心 1非核心
    int isCore;
    //是否有用-反馈  0有用 1无用
    int isDelete;
    //备注-反馈
    String description;
    //场景链路长度
    int length;
    //场景链路深度
    int depth;
    //0 是新增
    int isNew;
    //是集测覆盖，否集测未覆盖
    String isITCov;
    //场景是否重复的唯一标识
    String hashCode;

    //网关
    String source;
    //网关api
    String api;
    //类型
    String method;
    //网关请求体
    String requestBody;
    //网关请求头
    String requestHeader;
    //网关返回结果
    String responseBody;

    //应用名和接口
    String serviceId;
    String url;
    String appId;

    ArrayList nodeIds;
}
