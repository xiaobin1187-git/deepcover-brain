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

import com.baomidou.mybatisplus.generator.config.*;
import com.baomidou.mybatisplus.generator.config.builder.ConfigBuilder;

public class RemoveSCConfigBuilder extends ConfigBuilder {

    /**
     * 在构造器中处理配置
     *
     * @param packageConfig    包配置
     * @param dataSourceConfig 数据源配置
     * @param strategyConfig   表配置
     * @param template         模板配置
     * @param globalConfig     全局配置
     */
    public RemoveSCConfigBuilder(PackageConfig packageConfig, DataSourceConfig dataSourceConfig, StrategyConfig strategyConfig, TemplateConfig template, GlobalConfig globalConfig, boolean entity, boolean mapper, boolean service, boolean controller) {
        super(packageConfig, dataSourceConfig, strategyConfig, template, globalConfig);
        removeSC(entity, mapper, service, controller);
        resetServicePP(service);
    }

    private void removeSC(boolean entity, boolean mapper, boolean service, boolean controller) {
        if (!entity) {
            //去掉entity
            this.getPackageInfo().remove(ConstVal.ENTITY);
            this.getPathInfo().remove(ConstVal.ENTITY_PATH);
        }
        if (!mapper) {
            //去掉mapper
            this.getPackageInfo().remove(ConstVal.MAPPER);
            this.getPathInfo().remove(ConstVal.MAPPER_PATH);
        }
        if (!service) {
            //去掉facade
            this.getPackageInfo().remove(ConstVal.SERVICE);
            this.getPathInfo().remove(ConstVal.SERVICE_PATH);
        }
        if (!service) {
            //去掉service
            this.getPackageInfo().remove(ConstVal.SERVICE_IMPL);
            this.getPathInfo().remove(ConstVal.SERVICE_IMPL_PATH);
        }
        if (!controller) {
            //去掉controller
            this.getPackageInfo().remove(ConstVal.CONTROLLER);
            this.getPathInfo().remove(ConstVal.CONTROLLER_PATH);
        }
    }

    private void resetServicePP(boolean service) {
        if (service) {
            this.getPackageInfo().put(ConstVal.SERVICE, "io.deepcover.brain.service");
            this.getPackageInfo().put(ConstVal.SERVICE_IMPL, "io.deepcover.brain.service.impl");
            int index = System.getProperty("user.dir").lastIndexOf("/") == -1 ? System.getProperty("user.dir").lastIndexOf("\\") : System.getProperty("user.dir").lastIndexOf("/");
            String projectPath = System.getProperty("user.dir").substring(0, index) + "/service";
            String working = projectPath + "/src/main/java/";
            this.getPathInfo().put(ConstVal.SERVICE_PATH, working + this.getPackageInfo().get(ConstVal.SERVICE).replaceAll("\\.", "/"));
            this.getPathInfo().put(ConstVal.SERVICE_IMPL_PATH, working + this.getPackageInfo().get(ConstVal.SERVICE_IMPL).replaceAll("\\.", "/"));
        }
    }

}
