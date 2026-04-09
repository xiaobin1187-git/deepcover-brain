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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户操作日志实体类
 * 用于记录用户在系统中的操作行为，包括访问的账号、日期和页面信息
 * 支持对用户行为进行追踪和统计分析，为系统监控和用户体验优化提供数据支持
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
@Setter
@Getter
@EqualsAndHashCode(callSuper = true)
public class UserLog extends Model {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 用户账号
     */
    private String account;

    /**
     * 操作日期
     */
    private String date;

    /**
     * 访问页面
     */
    private String page;
}
