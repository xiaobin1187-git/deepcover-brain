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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: huangtai
 * @Description: 链路分析模型
 * @Date: 2023-4-20 19:59
 */
@Getter
@Setter
public class LinkAnalysisModel {

    Long id;

    //场景ID
    Long sceneId;

    //场景是否重复的唯一标识
    String hashCode;

    //应用顺序
    String serviceOrder;

    String serviceName;

    String className;

    String methodName;

    List<String> parameters;

    List<Integer> lineNums;

    public void setLineNums(List<Integer> lineNumsTemp) {
        if (lineNumsTemp != null) {
            lineNums = lineNumsTemp.stream().distinct().collect(Collectors.toList());
            Collections.sort(lineNums);
        }
    }
}
