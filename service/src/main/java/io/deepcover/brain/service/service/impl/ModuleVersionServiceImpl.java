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

package io.deepcover.brain.service.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.deepcover.brain.dal.entity.*;
import io.deepcover.brain.dal.mapper.AresConfigMapper;
import io.deepcover.brain.dal.mapper.AresReportMapper;
import io.deepcover.brain.dal.mapper.AresServiceMapper;
import io.deepcover.brain.dal.mapper.ModuleVersionMapper;
import io.deepcover.brain.model.ServiceModel;
import io.deepcover.brain.service.exception.RRException;
import io.deepcover.brain.service.service.AresAgentService;
import io.deepcover.brain.service.service.AresBrainRepeaterService;
import io.deepcover.brain.service.service.ModuleVersionService;
import io.deepcover.brain.service.util.client.HttpClientRequest;
import io.deepcover.brain.service.util.client.HttpClientResponse;
import io.deepcover.brain.service.util.client.HttpClientUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 模块版本管理服务实现类
 *
 * 提供模块版本管理的具体实现，包括版本信息的增删改查、
 * 服务版本管理、模块状态查询等功能
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
@Service
@Slf4j
public class ModuleVersionServiceImpl implements ModuleVersionService {

    @Autowired
    private ModuleVersionMapper moduleVersionMapper;

    @Autowired
    private AresReportMapper aresReportDao;

    /**
     * 查询服务模块版本列表
     *
     * 查询所有服务的模块版本信息，并实时获取各服务的活跃模块状态
     *
     * @param map 查询参数，支持分页和过滤条件
     * @return List<ServiceModuleVersionEntity> 返回服务版本列表，包含模块状态信息
     */
    @Override
    public List<ServiceModuleVersionEntity> queryServiceList(Map<String, Object> map) {
        List<ServiceModuleVersionEntity> serviceList = moduleVersionMapper.queryServiceList(map);


        serviceList.parallelStream().forEach(service->{
            List<ReportAgentEntity> reportList = aresReportDao.queryActiveList(service.getServiceName());
            Map<String,Integer> moduleMap = new HashMap<>();
            reportList.parallelStream().forEach(r->{
                HttpClientRequest request = new HttpClientRequest();
                request.setType(1);
                request.setUrl("http://" + r.getIp() + ":4769/sandbox/default/module/http/sandbox-module-mgr/list");
                HttpClientResponse response = HttpClientUtil.sendRequest(request);
                String body;
                if (response==null ||!"200".equals(response.getStateCode())||response.getResponseBody() == null) {
                    log.warn("应用=" + r.getServiceName() + ",ip=" + r.getIp() + "查询module列表失败");
                } else {
                    body = response.getResponseBody().toString();
                    String[] modules = body.split("\n");
                    for(String m:modules){
                        String[] fields = m.split("\t");
                        if(fields.length==7){
                            JSONObject module = new JSONObject();
                            List<String> ignoreModules= Arrays.asList("Login-Filter","sandbox-info","sandbox-module-mgr","sandbox-control");
                            if(ignoreModules.indexOf(fields[0].trim())>-1){
                                continue;
                            }
                            if("ACTIVE".equals(fields[1].trim())){
                                moduleMap.put(fields[0].trim(),moduleMap.getOrDefault(fields[0].trim(),0)+1);
                            }
                        }

                    }

                }
//                r.setModuleNum(moduleMap);
            });
            service.setModuleNum(moduleMap);
        });
        return serviceList;
    }

    /**
     * 查询服务总数
     *
     * @param map 查询参数
     * @return int 返回符合条件的记录总数
     */
    @Override
    public int queryServiceTotal(Map<String, Object> map) {
        return moduleVersionMapper.queryServiceTotal(map);
    }

    /**
     * 新增模块版本信息
     *
     * 根据类型自动设置模块名称，生成版本号和OSS URL地址
     * 支持的类型：1-ares, 2-repeater, 3-chaosblade, 4-emock
     *
     * @param moduleVersionEntity 模块版本实体，包含类型、分支、提交ID等信息
     */
    @Override
    public void add(ModuleVersionEntity moduleVersionEntity) {
        switch (moduleVersionEntity.getType()){
            case 1:
                moduleVersionEntity.setTypeName("ares");
                break;
            case 2:
                moduleVersionEntity.setTypeName("repeater");
                break;
            case 3:
                moduleVersionEntity.setTypeName("chaosblade");
                break;
            case 4:
                moduleVersionEntity.setTypeName("emock");
                break;
            default:
                throw new RRException("type不支持:"+moduleVersionEntity.getType());
        }
        ModuleVersionEntity lastestVersion = moduleVersionMapper.queryLatestVersion(moduleVersionEntity.getType());
        moduleVersionEntity.setVersion(lastestVersion==null?1:lastestVersion.getVersion()+1);
        moduleVersionEntity.setOssUrl(moduleVersionEntity.getOssUrl()+moduleVersionEntity.getTypeName()+"_"+moduleVersionEntity.getVersion()+".jar");
        moduleVersionMapper.insert(moduleVersionEntity);
    }

    /**
     * 根据ID删除模块版本
     *
     * 软删除：将状态设置为-1，而不是物理删除
     *
     * @param id 模块版本ID
     */
    @Override
    public void deleteById(Long id){
        moduleVersionMapper.updateStatusById(id,-1);
    }

    /**
     * 更新服务版本信息
     *
     * @param entity 服务版本实体，包含更新后的版本信息
     */
    @Override
    public void updateServiceVersion(ServiceModuleVersionEntity entity){
        moduleVersionMapper.updateServiceVersion(entity);
    }

    /**
     * 查询服务的默认版本信息
     *
     * @param serviceName 服务名称
     * @return ServiceModuleVersionEntity 返回默认版本信息
     */
    @Override
    public ServiceModuleVersionEntity queryDefault(String serviceName){
        return moduleVersionMapper.queryDefault(serviceName);
    }

    /**
     * 查询模块版本列表
     *
     * @param map 查询参数，支持按类型、服务等条件过滤
     * @return List<ModuleVersionEntity> 返回模块版本列表
     */
    @Override
    public List<ModuleVersionEntity> queryList(Map<String, Object> map) {
        return moduleVersionMapper.queryList(map);
    }

    /**
     * 查询模块版本总数
     *
     * @param map 查询参数
     * @return int 返回符合条件的记录总数
     */
    @Override
    public int queryTotal(Map<String, Object> map) {
        return moduleVersionMapper.queryTotal(map);
    }

}
