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

package io.deepcover.brain.dal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 代码差异分析记录实体类
 * 用于记录代码差异分析任务的详细信息，包括基线版本、目标版本、分析状态等
 * 支持对Git仓库进行版本间的代码差异分析，为代码质量评估和变更影响分析提供数据支撑
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DiffRecordEntity extends Model {

    private static final long serialVersionUID = 1L;

    /**
     * 主键自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 聚合比对条数
     */
    private Integer recordTotal;

    /**
     * 环境类型
     */
    private String envCode;

    /**
     * 原始/基线git远程仓库地址
     */
    private String baseGitUrl;

    /**
     * 原始/基线git远程仓库地址
     */
    private String baseBranch;

    /**
     * git基线原始分支或commitId或tag
     */
    private String baseVersion;

    /**
     * 现在/当前git远程仓库地址
     */
    private String nowGitUrl;
    /**
     * git现分支或tag
     */
    private String nowBranch;

    /**
     * git现分支或tag
     */
    private String nowVersion;

    /**
     * 状态：-1:已删除，0-初始化，1-分析中，2-分析成功，3-分析失败
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String statusDescrip;

    /**
     * 差异详情ID，关联差异详情表
     */
    private Long diffDetailId;

    /**
     * 测试结果
     */
    private String testResult;

    /**
     * 创建时间
     */
    private Date createdTime;

    /**
     * 更新时间
     */
    private Date modifiedTime;

    /**
     * 发布时间
     */
    private Date publishTime;

    /**
     * 最后开始差异分析时间
     */
    private Date lastStartDiffTime;

    /**
     * 最后完成差异分析时间
     */
    private Date lastEndDiffTime;

    /**
     * 创建用户
     */
    private String addBy;

    /**
     * 更新用户
     */
    private String updateBy;
    /**
     * 是否属于Epaas系统：0-否，1-是
     */
    private Integer isEpaas;
}
