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
import com.baomidou.mybatisplus.autoconfigure.SpringBootVFS;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisXMLLanguageDriver;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

/**
 * MyBatis默认数据源配置类
 *
 * <p>配置应用的主数据源（默认数据源），作为应用主要的数据库连接来源。</p>
 *
 * @author deepcover
 * @version 1.0
 */
@Configuration
@MapperScan(
        basePackages = {"io.deepcover.brain.dal.mapper"},
        sqlSessionFactoryRef = "defaultSqlSessionFactory",
        sqlSessionTemplateRef = "defaultSqlSessionTemplate")
public class MybatisDefaultDataSourceConfig {

    @Autowired
    private PaginationInterceptor paginationInterceptor;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    static SqlSessionFactory getSqlSessionFactory(
            DataSource dataSource, String driverClassName, Interceptor[] plugins) throws Exception {
        MybatisSqlSessionFactoryBean sqlSessionFactory = new MybatisSqlSessionFactoryBean();
        sqlSessionFactory.setDataSource(dataSource);
        sqlSessionFactory.setVfs(SpringBootVFS.class);

        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setDbConfig(new GlobalConfig.DbConfig().setColumnFormat("`%s`"));
        sqlSessionFactory.setGlobalConfig(globalConfig);
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setDefaultScriptingLanguage(MybatisXMLLanguageDriver.class);
        configuration.setJdbcTypeForNull(JdbcType.NULL);
        configuration.setMapUnderscoreToCamelCase(true);
        sqlSessionFactory.setConfiguration(configuration);
        sqlSessionFactory.setPlugins(plugins);
        return sqlSessionFactory.getObject();
    }

    @Primary
    @Bean(name = "defaultDataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource defaultDataSource() {
        return new DruidDataSource();
    }

    @Primary
    @Bean(name = "defaultSqlSessionFactory")
    public SqlSessionFactory defaultSqlSessionFactory(
            @Qualifier("defaultDataSource") DataSource dataSource) throws Exception {
        Interceptor[] plugins = {paginationInterceptor};
        return getSqlSessionFactory(dataSource, driverClassName, plugins);
    }

    @Primary
    @Bean(name = "defaultTransactionManager")
    public DataSourceTransactionManager defaultTransactionManager(
            @Qualifier("defaultDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Primary
    @Bean(name = "defaultSqlSessionTemplate")
    public SqlSessionTemplate defaultSqlSessionTemplate(
            @Qualifier("defaultSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
