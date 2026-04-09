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
 * @Description: hbase原数据模型
 * @Date: 2023-4-20 11:13
 */
@Setter
@Getter
public class HbaseDataModel {

    Integer invokeId;

    String processId;

    Long sceneId;

    String serviceName;

    String url;

    String method;

    String className;

    String methodName;

    List<String> parameters;

    List<Integer> lineNums;

    String linkId;

    String parentLinkId;

    String beginTime;

    String branch;

    //0 表示方法节点，1，服务接口节点 2.网关节点
    int type;

    public void setLineNums(List<Integer> lineNumsTemp) {
        if (lineNumsTemp != null) {
            lineNums = lineNumsTemp.stream().distinct().collect(Collectors.toList());
            Collections.sort(lineNums);
        }
    }
}
