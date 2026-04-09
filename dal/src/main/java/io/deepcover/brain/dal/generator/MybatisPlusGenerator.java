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

import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;
import com.baomidou.mybatisplus.generator.AutoGenerator;
import com.baomidou.mybatisplus.generator.InjectionConfig;
import com.baomidou.mybatisplus.generator.config.*;
import com.baomidou.mybatisplus.generator.config.builder.ConfigBuilder;
import com.baomidou.mybatisplus.generator.config.rules.FileType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Scanner;


/**
 * 自动生成mybatisplus的相关代码
 */
public class MybatisPlusGenerator {

    private static final boolean OVERWRITE_BOOT = false;

    private static String DB_URL =
            "jdbc:mysql://192.168.2.137:3306/ares?useUnicode=true&characterEncoding=utf8&characterResultSets=utf8&autoReconnect=true&serverTimezone=GMT%2b8";
    private static String DB_DRIVER_NAME = "com.mysql.cj.jdbc.Driver";
    private static String DB_USERNAME = "root";
    private static String DB_PASSWORD = "root123";

    private static String DB_URL2 = "jdbc:mysql://proxy.esign.cn:60321/redline?useSSL=false";
    private static String DB_DRIVER_NAME2 = "com.mysql.cj.jdbc.Driver";
    private static String DB_USERNAME2 = "suyao";
    private static String DB_PASSWORD2 = "Ziying2023";

    private static boolean HAS_BEGIN_MODEL;
    private static String MYBATIS_PLUS_GENERATE_MODEL = "";
    private static String MYBATIS_PLUS_GENERATE_PARENT = "io.deepcover.brain.dal";
    private static String MYBATIS_PLUS_GENERATE_ENTITY = "entity";
    private static String MYBATIS_PLUS_GENERATE_MAPPER = "mapper";
    private static String MYBATIS_PLUS_GENERATE_FACADE = "facade";
    private static String MYBATIS_PLUS_GENERATE_SERVICE = "service";

    public static String scanner(String tip) {
        Scanner scanner = new Scanner(System.in);
        StringBuilder help = new StringBuilder();
        help.append("请输入" + tip + "：");
        System.out.println(help.toString());
        if (scanner.hasNext()) {
            String ipt = scanner.next();
            if (StringUtils.isNotEmpty(ipt)) {
                return ipt;
            }
        }
        throw new MybatisPlusException("请输入正确的" + tip + "！");
    }

    public static boolean askScanner(String messageTemplate, String... values) {
        Scanner scanner = new Scanner(System.in);
        String message = String.format(messageTemplate, values);
        System.out.println(message);
        String param = scanner.next();
        switch (param) {
            case "n":
            case "N":
                return false;
            case "y":
            case "Y":
                return true;
        }
        return false;
    }

    public static void main(String[] args) {
        String[] tables = scanner("表名，多个英文逗号分割").split(",");
        generate(tables, true, true, true, false);
        //        generate(tables,false,false,true,false);
    }

    private static void generate(
            String[] tables, boolean entity, boolean mapper, boolean service, boolean controller) {
        // 代码生成器
        AutoGenerator mpg = new RemoveSCAutoGenerator(entity, mapper, service, controller);

        // 全局配置
        GlobalConfig gc = new GlobalConfig();
        String projectPath = System.getProperty("user.dir");
        gc.setOutputDir(projectPath + "/dal/src/main/java");
        gc.setAuthor("mybatis-plus-generator");
        gc.setOpen(false);
        // 实体属性 Swagger2 注解
        gc.setSwagger2(false);
        mpg.setGlobalConfig(gc);

        // 数据源配置
        DataSourceConfig dsc = new DataSourceConfig();
        dsc.setUrl(DB_URL);
        dsc.setDriverName(DB_DRIVER_NAME);
        dsc.setUsername(DB_USERNAME);
        dsc.setPassword(DB_PASSWORD);
        mpg.setDataSource(dsc);

        // 包配置
        PackageConfig pc = new PackageConfig();
        if (HAS_BEGIN_MODEL) {
            pc.setModuleName(MYBATIS_PLUS_GENERATE_MODEL);
        }
        pc.setParent(MYBATIS_PLUS_GENERATE_PARENT);
        pc.setEntity(MYBATIS_PLUS_GENERATE_ENTITY);
        pc.setMapper(MYBATIS_PLUS_GENERATE_MAPPER);
        pc.setService(MYBATIS_PLUS_GENERATE_FACADE);
        pc.setServiceImpl(MYBATIS_PLUS_GENERATE_SERVICE);
        // 去掉controller
        pc.setController("");
        mpg.setPackageInfo(pc);

        // 自定义配置
        InjectionConfig cfg =
                new InjectionConfig() {
                    @Override
                    public void initMap() {
                        // to do nothing
                    }
                };

        cfg.setFileCreate(
                new IFileCreate() {
                    @Override
                    public boolean isCreate(
                            ConfigBuilder configBuilder, FileType fileType, String filePath) {
                        // 判断自定义文件夹是否需要创建
                        // 如果是Entity则直接返回true表示写文件
                        if (fileType.name().equals(FileType.ENTITY.name())) {
                            return true;
                        }
                        // 否则先判断文件是否存在
                        File file = new File(filePath);
                        boolean exist = file.exists();
                        if (!exist) {
                            file.getParentFile().mkdirs();
                        }
                        // 文件不存在或者全局配置的fileOverride为true才写文件
                        return !exist || configBuilder.getGlobalConfig().isFileOverride();
                        //                checkDir("调用默认方法创建的目录");
                        //                return false;
                    }
                });

        mpg.setCfg(cfg);

        // 配置模板
        TemplateConfig templateConfig = new TemplateConfig();

        templateConfig.setXml(null);
        mpg.setTemplate(templateConfig);

        // 策略配置
        StrategyConfig strategy = new StrategyConfig();
        strategy.setNaming(NamingStrategy.underline_to_camel);
        strategy.setColumnNaming(NamingStrategy.underline_to_camel);
        strategy.setSuperEntityClass("com.baomidou.mybatisplus.extension.activerecord.Model");
        strategy.setEntityLombokModel(true);
        strategy.setRestControllerStyle(true);

        strategy.setEntityLombokModel(true);
        strategy.setInclude(tables);
        strategy.setControllerMappingHyphenStyle(true);
        strategy.setTablePrefix(pc.getModuleName() + "_");
        mpg.setStrategy(strategy);
        mpg.setTemplateEngine(new FreemarkerTemplateEngine());
        mpg.execute();
    }

//    private static void overwriteBootParams() {
//        if (OVERWRITE_BOOT) {
//            DB_URL = PropertiesUtils.getProperty("spring.datasource.seal.url");
//            DB_DRIVER_NAME =
//                    PropertiesUtils.getProperty("spring.datasource.seal.driver-class-name");
//            DB_USERNAME = PropertiesUtils.getProperty("spring.datasource.seal.username");
//            DB_PASSWORD = PropertiesUtils.getProperty("spring.datasource.seal.password");
//
//            MYBATIS_PLUS_GENERATE_MODEL =
//                    PropertiesUtils.getProperty("mybatis-plus.generator.model");
//            MYBATIS_PLUS_GENERATE_PARENT =
//                    PropertiesUtils.getProperty("mybatis-plus.generator.parent-path");
//            MYBATIS_PLUS_GENERATE_ENTITY =
//                    PropertiesUtils.getProperty("mybatis-plus.generator.entity-package-name");
//            MYBATIS_PLUS_GENERATE_MAPPER =
//                    PropertiesUtils.getProperty("mybatis-plus.generator.mapper-package-name");
//            MYBATIS_PLUS_GENERATE_FACADE =
//                    PropertiesUtils.getProperty("mybatis-plus.generator.facade-package-name");
//            MYBATIS_PLUS_GENERATE_SERVICE =
//                    PropertiesUtils.getProperty("mybatis-plus.generator.service-package-name");
//        }
//    }service-package-name

    static {
        //overwriteBootParams();
        if (StringUtils.isEmpty(MYBATIS_PLUS_GENERATE_MODEL)) {
            HAS_BEGIN_MODEL = false;
        } else {
            HAS_BEGIN_MODEL = true;
        }
    }
}
