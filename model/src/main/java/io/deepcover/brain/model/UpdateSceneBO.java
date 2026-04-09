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

import lombok.Data;

/**
 * @Author: huangtai
 * @Description: 修改场景信息
 * @Date: 2023-4-20 19:59
 */
@Data
public class UpdateSceneBO {

    private long id;
    //场景名称
    String sceneName;
    //是否核心 0核心 1非核心
    int isCore = -1;
    //是否有用-反馈  0有用 1无用
    int isDelete = -1;
    //备注-反馈
    String description;
    String account;
}
