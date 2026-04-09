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

package io.deepcover.brain.dal.generator;

import com.baomidou.mybatisplus.generator.AutoGenerator;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveSCAutoGenerator extends AutoGenerator {

    private static final Logger logger = LoggerFactory.getLogger(AutoGenerator.class);

    public boolean entity;
    public boolean mapper;
    public boolean service;
    public boolean controller;

    public RemoveSCAutoGenerator(boolean entity, boolean mapper, boolean service, boolean controller) {
        this.entity = entity;
        this.mapper = mapper;
        this.service = service;
        this.controller = controller;
    }

    /**
     * 生成代码
     */
    @Override
    public void execute() {
        logger.debug("==========================准备生成文件...==========================");
        // 初始化配置
        if (null == config) {
            config = new RemoveSCConfigBuilder(this.getPackageInfo(), this.getDataSource(), this.getStrategy(), this.getTemplate(), this.getGlobalConfig(),entity,mapper,service,controller);
            if (null != injectionConfig) {
                injectionConfig.setConfig(config);
            }
        }
        if (null == this.getTemplateEngine()) {
            // 为了兼容之前逻辑，采用 Velocity 引擎 【 默认 】
            this.setTemplateEngine(new VelocityTemplateEngine());
        }
        // 模板引擎初始化执行文件输出
        this.getTemplateEngine().init(this.pretreatmentConfigBuilder(config)).mkdirs().batchOutput().open();
        logger.debug("==========================文件生成完成！！！==========================");
    }

}
