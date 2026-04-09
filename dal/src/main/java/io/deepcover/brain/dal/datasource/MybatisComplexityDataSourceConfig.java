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

package io.deepcover.brain.dal.datasource;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

import static io.deepcover.brain.dal.datasource.MybatisDefaultDataSourceConfig.getSqlSessionFactory;

/**
 * MyBatis复杂数据源配置类
 *
 * <p>配置第二个数据源（复杂数据源），支持多数据源环境。</p>
 *
 * @author deepcover
 * @version 1.0
 */
@ConditionalOnProperty(value = "spring.datasource.complexity.url")
@Configuration
@MapperScan(
        basePackages = "io.deepcover.brain.dal.complexitymapper",
        sqlSessionFactoryRef = "complexitySqlSessionFactory",
        sqlSessionTemplateRef = "complexitySqlSessionTemplate")
public class MybatisComplexityDataSourceConfig {

    @Autowired
    private PaginationInterceptor paginationInterceptor;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Bean(name = "complexityDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.complexity")
    public DataSource complexityDataSource() {
        return new DruidDataSource();
    }

    @Bean(name = "complexitySqlSessionFactory")
    public SqlSessionFactory complexitySqlSessionFactory(@Qualifier("complexityDataSource") DataSource dataSource)
            throws Exception {
        Interceptor[] plugins = {paginationInterceptor};
        return getSqlSessionFactory(dataSource, driverClassName, plugins);
    }

    @Bean(name = "complexityTransactionManager")
    public DataSourceTransactionManager complexityTransactionManager(
            @Qualifier("complexityDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean(name = "complexitySqlSessionTemplate")
    public SqlSessionTemplate complexitySqlSessionTemplate(
            @Qualifier("complexitySqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
