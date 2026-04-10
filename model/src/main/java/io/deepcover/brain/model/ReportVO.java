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

import java.util.List;

/**
 * 报告VO类
 */
@Data
public class ReportVO {
    /**
     * 对应的traceId列表
     */
    private List<String> traceIds;
    
    /**
     * traceId数量
     */
    private Integer traceIdCount;
    
    /**
     * 已查看的traceId数量
     */
    private Integer viewedCount;
    
    /**
     * 服务名
     */
    private String serviceName;
    
    /**
     * 环境代码
     */
    private String envCode;
    
    /**
     * 基础版本
     */
    private String baseVersion;
    
    /**
     * 当前版本
     */
    private String nowVersion;
    
    /**
     * 分支名称
     */
    private String branchName;
    
    /**
     * 记录数
     */
    private Integer recordCount;
}