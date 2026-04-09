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
import io.deepcover.brain.dal.entity.AresAgentBatchEntity;
import io.deepcover.brain.dal.entity.AresAgentEntity;
import io.deepcover.brain.dal.entity.ReportAgentEntity;
import io.deepcover.brain.dal.entity.ServiceModuleVersionEntity;
import io.deepcover.brain.dal.mapper.*;
import io.deepcover.brain.model.ServiceModel;
import io.deepcover.brain.service.exception.RRException;
import io.deepcover.brain.service.service.AresAgentService;
import io.deepcover.brain.service.util.client.HttpClientRequest;
import io.deepcover.brain.service.util.client.HttpClientResponse;
import io.deepcover.brain.service.util.client.HttpClientUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Ares代理服务实现类
 *
 * 提供Ares代理管理的具体业务实现，包括代理配置管理、服务器信息同步、
 * 配置差异分析、模块信息查询等核心功能
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
@Service
@Slf4j
public class AresAgentServiceImpl implements AresAgentService {

    @Autowired
    private AresServiceMapper aresAgentDao;

    @Autowired
    private AresReportMapper aresReportDao;

    @Autowired
    private AresConfigMapper aresConfigDao;

    @Autowired
    private ModuleVersionMapper moduleVersionMapper;

    @Autowired
    SceneServiceClassMapper serviceClassMapper;

    /**
     * 添加Ares代理配置
     *
     * 创建新的Ares代理配置，自动加载默认配置参数，包括采样率、忽略URL、
     * 异常熔断配置、队列配置等，同时初始化模块版本信息
     *
     * @param aresAgentEntity Ares代理实体，包含应用名、包名等基本信息
     * @throws RRException 当应用名已存在时抛出异常
     */
    @Override
    @Transactional
    public void add(AresAgentEntity aresAgentEntity) {

        if (aresAgentDao.queryObject(aresAgentEntity.getServiceName()) == null) {
            aresAgentEntity.setIgnoreUrls(aresConfigDao.queryObject("ignore_urls").getValue());
            aresAgentEntity.setSampleRate(Long.parseLong(aresConfigDao.queryObject("sampleRate").getValue()));
            aresAgentEntity.setLimitCodeMethodSize(Long.parseLong(aresConfigDao.queryObject("limitCodeMethodSize").getValue()));
            aresAgentEntity.setLimitCodeMethodLineSize(Long.parseLong(aresConfigDao.queryObject("limitCodeMethodLineSize").getValue()));
            aresAgentEntity.setIgnoreClasses(aresConfigDao.queryObject("ignore_classes").getValue());
            aresAgentEntity.setIgnoreMethods(aresConfigDao.queryObject("ignore_methods").getValue());
            aresAgentEntity.setIgnoreAnnos(aresConfigDao.queryObject("ignore_annos").getValue());
            aresAgentEntity.setReportPeriod(Integer.parseInt(aresConfigDao.queryObject("report_period").getValue()));
            //异常熔断配置
            aresAgentEntity.setExceptionThreshold(Long.parseLong(aresConfigDao.queryObject("exception_threshold").getValue()));
            aresAgentEntity.setExceptionCalcTime(Long.parseLong(aresConfigDao.queryObject("exception_calc_time").getValue()));
            aresAgentEntity.setExceptionPauseTime(Long.parseLong(aresConfigDao.queryObject("exception_pause_time").getValue()));

            aresAgentEntity.setSendDataCenterType(Integer.parseInt(aresConfigDao.queryObject("send_data_center_type").getValue()));
            //队列配置
            aresAgentEntity.setQueueNum(Integer.parseInt(aresConfigDao.queryObject("queue_num").getValue()));
            aresAgentEntity.setQueueSize(Integer.parseInt(aresConfigDao.queryObject("queue_size").getValue()));
            aresAgentEntity.setQueueMsgSize(Integer.parseInt(aresConfigDao.queryObject("queue_msg_size").getValue()));
            aresAgentEntity.setQueueRecycleTime(Integer.parseInt(aresConfigDao.queryObject("queue_recycle_time").getValue()));

            aresAgentEntity.setPackageName(aresAgentEntity.getPackageName() + ".*");
            aresAgentDao.insert(aresAgentEntity);

            ServiceModuleVersionEntity serviceModuleVersionEntity = new ServiceModuleVersionEntity();
            serviceModuleVersionEntity.setServiceName(aresAgentEntity.getServiceName());
            serviceModuleVersionEntity.setAresEnabled(Integer.parseInt(aresConfigDao.queryObject("ares_enabled").getValue()));
            serviceModuleVersionEntity.setAresModuleVersion(Integer.parseInt(aresConfigDao.queryObject("ares_module_version").getValue()));
            serviceModuleVersionEntity.setRepeaterEnabled(Integer.parseInt(aresConfigDao.queryObject("repeater_enabled").getValue()));
            serviceModuleVersionEntity.setRepeaterModuleVersion(Integer.parseInt(aresConfigDao.queryObject("repeater_module_version").getValue()));
            serviceModuleVersionEntity.setChaosbladeEnabled(Integer.parseInt(aresConfigDao.queryObject("chaosblade_enabled").getValue()));
            serviceModuleVersionEntity.setChaosbladeModuleVersion(Integer.parseInt(aresConfigDao.queryObject("chaosblade_module_version").getValue()));
            serviceModuleVersionEntity.setEmockEnabled(Integer.parseInt(aresConfigDao.queryObject("emock_enabled").getValue()));
            serviceModuleVersionEntity.setEmockModuleVersion(Integer.parseInt(aresConfigDao.queryObject("emock_module_version").getValue()));
            moduleVersionMapper.insertModuleVersion(serviceModuleVersionEntity);

        } else {
            throw new RRException("应用名已存在，请勿重复操作");
        }
    }

    /**
     * 更新Ares代理配置
     *
     * 根据传入的实体信息更新指定的Ares代理配置
     *
     * @param aresAgentEntity Ares代理实体，包含需要更新的配置信息
     */
    @Override
    public void update(AresAgentEntity aresAgentEntity) {
        aresAgentDao.updateById(aresAgentEntity);
    }

    /**
     * 批量更新Ares代理配置
     *
     * 根据批量实体中的配置信息，同时更新多个Ares代理配置
     *
     * @param aresAgentBatchEntity 批量更新实体，包含多个代理的ID和配置信息
     */
    @Override
    public void batchUpdate(AresAgentBatchEntity aresAgentBatchEntity) {
        aresAgentDao.batchUpdateByIds(aresAgentBatchEntity);
    }


    /**
     * 查询Ares代理列表
     *
     * 根据查询参数条件查询符合条件的Ares代理配置列表
     *
     * @param map 查询参数，支持按应用名、本地化标志等条件过滤
     * @return List<AresAgentEntity> 返回符合条件的代理配置列表
     */
    @Override
    public List<AresAgentEntity> queryList(Map<String, Object> map) {
        return aresAgentDao.queryList(map);
    }

    /**
     * 查询配置差异列表
     *
     * 统计各代理服务间的配置差异，包括采样率、数据中心类型、异常阈值等关键参数
     *
     * @return JSONObject 返回包含各种配置差异信息的JSON对象
     */
    @Override
    public JSONObject queryDiffList() {
        JSONObject result = new JSONObject();
        List<JSONObject> sampleList =aresAgentDao.querySampleRateDiffList();
        result.put("sampleRate",sampleList);

        List<JSONObject> sendDataCenterTypeList =aresAgentDao.querySendDataCenterTypeDiffList();
        result.put("sendDataCenterType",sendDataCenterTypeList);

        List<JSONObject> exceptionThreshold =aresAgentDao.queryExceptionThresholdDiffList();
        result.put("exceptionThreshold",exceptionThreshold);
        return result;
    }

    /**
     * 获取所有服务名称列表
     *
     * @return List<String> 返回所有已配置的Ares代理服务名称列表
     */
    @Override
    public List<String> getAllServiceName() {
        return aresAgentDao.getAllServiceName();
    }

    /**
     * 查询Ares代理总数
     *
     * 根据查询条件统计符合条件的Ares代理配置总数
     *
     * @param map 查询参数
     * @return int 返回符合条件的代理总数
     */
    @Override
    public int queryTotal(Map<String, Object> map) {
        return aresAgentDao.queryTotal(map);
    }

//    @Override
//    public List<AresAgentModel> queryList(Map<String, Object> map) {
//        return null;
//    }

//    @Override
//    public int queryTotal(Map<String, Object> map) {
//        return 0;
//    }

    /**
     * 根据服务名称查询Ares代理配置
     *
     * @param serviceName 服务名称
     * @return AresAgentEntity 返回对应的代理配置信息，如不存在则返回null
     */
    @Override
    public AresAgentEntity queryObject(String serviceName) {
        return aresAgentDao.queryObject(serviceName);
    }

    /**
     * 异步同步服务器信息
     *
     * 使用数据库专用线程池异步处理上报的服务器信息，执行插入或更新操作。
     * 该方法避免了HTTP调用阻塞数据库操作，提高系统响应性能
     *
     * @param serverEntity 服务器上报实体，包含IP、版本、状态等信息
     */
    @Override
    @Async("dbExecutor")
    public void syncServerInfo(ReportAgentEntity serverEntity) {
        // 使用数据库专用线程池执行插入或更新操作
        aresReportDao.insertOrUpdate(serverEntity);
        /*if (report != null) {
            aresReportDao.update(report.getId());
        } else {
            aresReportDao.insert(serverEntity);
        }*/
    }

    /**
     * 查询上报的服务器信息列表
     *
     * 获取指定服务上报的服务器信息，并实时查询每个服务器的模块状态信息，
     * 包括模块名称、激活状态、加载状态、版本号等详细信息
     *
     * @param serviceName 服务名称
     * @return List<ReportAgentEntity> 返回该服务上报的服务器信息列表，包含模块信息
     */
    @Override
    public List<ReportAgentEntity> listReportServerInfo(String serviceName) {
        List<ReportAgentEntity> result = aresReportDao.queryList(serviceName, 10);
        for (ReportAgentEntity r : result) {
            JSONArray moduleInfo = new JSONArray();
            if (r.getStatus() != 1) {
                continue;
            }
            HttpClientRequest request = new HttpClientRequest();
            request.setType(1);
            request.setUrl("http://" + r.getIp() + ":4769/sandbox/default/module/http/sandbox-module-mgr/list");
            HttpClientResponse response = HttpClientUtil.sendRequest(request);
            String body;
            if (response == null || !"200".equals(response.getStateCode()) || response.getResponseBody() == null) {
//                    throw new RRException("应用=" + r.getServiceName() + ",ip=" + r.getIp() + "查询module列表失败");
                log.warn("应用=" + r.getServiceName() + ",ip=" + r.getIp() + "查询module列表失败");
            } else {
                body = response.getResponseBody().toString();
                String[] modules = body.split("\n");

                for (String m : modules) {
                    String[] fields = m.split("\t");
                    if (fields.length == 7) {
                        JSONObject module = new JSONObject();
                        List<String> ignoreModules = Arrays.asList("Login-Filter", "sandbox-info", "sandbox-module-mgr", "sandbox-control");
                        if (ignoreModules.indexOf(fields[0].trim()) > -1) {
                            continue;
                        }
                        module.put("name", fields[0].trim());
                        module.put("activeStatus", fields[1].trim());
                        module.put("loadStatus", fields[2].trim());
                        module.put("cCnt", fields[3].trim());
                        module.put("mCnt", fields[4].trim());
                        module.put("version", fields[5].trim());
                        moduleInfo.add(module);
                    }

                }

            }
            r.setModuleInfo(moduleInfo);

        }
        return result;
    }

//    @Override
//    @Async("asyncServiceExecutor")
//    public void syncServerInfo(ReportAgentModel reportEntity){
//        //        先不写入库，数据量太大
//        aresAgentDao.insertOrUpdate(reportEntity);
//
//    }

//    public void syncServerInfo1(ReportAgentModel reportEntity){
//        AresAgentModel aresEntity = this.queryObject(reportEntity.getServiceName());
//        if(aresEntity==null){
//            logger.error("[serviceName={}]配置不存在",reportEntity.getServiceName());
//            throw new RRException("serviceName="+reportEntity.getServiceName()+"配置不存在");
//        }
//        reportEntity.setServiceId(aresEntity.getId());
////        先不写入库，数据量太大
////        serviceReportInfoDao.save(reportEntity);
//
//        if(aresEntity.getVersion()<=reportEntity.getVersion()){
//            return;
//        }
//        String key="ares:"+reportEntity.getServiceName()+":"+reportEntity.getIp()+":"+reportEntity.getVersion();
//
////        if(redisUtils.get(key)!=null){
////            return;
////        }else{
////            redisUtils.set(key,"updating",30);
////        }
//
//        //重新推送配置，并动态生效String unload_url = "http://"+serverEntity.getIp()+":4769/sandbox/default/module/http/sandbox-module-mgr/unload?action=unload&ids=code-coverage";
//        HttpClientUtil http = new HttpClientUtil();
//
//        String syncConfig_url = "http://"+reportEntity.getIp()+":4769/sandbox/default/module/http/code-coverage/syncConfig"
//                +"?version="+aresEntity.getVersion()
//                +"&sampleRate="+aresEntity.getSampleRate()
//                +"&reportPeriod="+aresEntity.getReportPeriod()
//                +"&ignoreUrls="+aresEntity.getIgnoreUrls()
//                +"&ignoreClasses="+aresEntity.getIgnoreClasses();
////        String syncConfig_resp = http.doGet(syncConfig_url);
//        String syncConfig_resp = "xxxx";
//        if(syncConfig_resp.indexOf("success")!=-1){
////            redisUtils.delete(key);
//        }else{
//            logger.error("更新配置失败,serviceName={},ip={},version={},{}",aresEntity.getServiceName(),reportEntity.getIp(),aresEntity.getVersion());
//        }
//
////        String detail_url = "http://"+reportEntity.getIp()+":4769/sandbox/default/module/http/sandbox-module-mgr/detail?id=code-coverage";
////        String detail_resp = http.doGet(detail_url);
////        if(detail_resp.indexOf("STATE : ACTIVE")!=-1){
////            //重新推送配置，并动态生效
////            String unload_url = "http://"+reportEntity.getIp()+":4769/sandbox/default/module/http/sandbox-module-mgr/unload?action=unload&ids=code-coverage";
////            String unload_resp = http.doGet(unload_url);
////            try {
////                Thread.sleep(5000L);
////            } catch (InterruptedException e) {
////                e.printStackTrace();
////            }
////        }
//
//        //重新刷新配置，并动态生效
////        String flush_url = "http://"+reportEntity.getIp()+":4769/sandbox/default/module/http/sandbox-module-mgr/flush?force=true";
////        String flush_resp = http.doGet(flush_url);
////
////        String active_url = "http://"+reportEntity.getIp()+":4769/sandbox/default/module/http/sandbox-module-mgr/active?ids=code-coverage";
////        String active_resp = http.doGet(active_url);
//
////        activingIps.remove(reportEntity.getIp());
//
////        redisUtils.delete(key);
//    }

    /**
     * 查询服务列表
     *
     * 获取所有已注册的服务名称列表，并转换为服务模型对象返回。
     * 该方法用于前端展示服务列表等场景
     *
     * @return List<ServiceModel> 返回所有服务的基本信息模型列表
     */
    @SneakyThrows
    public List<ServiceModel> queryService() {
        List<String> serviceNames = serviceClassMapper.getServiceName();//getAllServiceName();
        List<ServiceModel> result = new ArrayList<>();
        for (String serviceName : serviceNames) {
            ServiceModel serviceModel = new ServiceModel();
            serviceModel.setServiceName(serviceName);
            result.add(serviceModel);
        }
        return result;
    }
}
