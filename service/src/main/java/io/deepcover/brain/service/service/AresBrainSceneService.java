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

package io.deepcover.brain.service.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.deepcover.brain.dal.mapper.SceneServiceClassMapper;
import io.deepcover.brain.model.*;
import io.deepcover.brain.service.util.DingdingSendUtil;
import io.deepcover.brain.service.util.ObjectConverterUtil;
import io.deepcover.brain.service.util.client.HttpClientUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.*;

/**
 * Ares大脑场景管理服务
 *
 * 提供场景管理的核心业务功能，包括场景的增删改查、场景详情查询、
 * 服务管理、拓扑图生成、批量处理等功能
 *
 * @author aresbrain
 * @version 1.0
 * @since 2025-11-21
 */
@Slf4j
@Service
public class AresBrainSceneService {

    @Autowired
    SceneServiceClassMapper serviceClassMapper;

    /**
     * 添加场景反馈信息
     *
     * @param sceneModel 场景更新模型，包含场景ID和反馈信息
     * @return String 返回操作结果
     */
    @SneakyThrows
    public String addFeedBack(UpdateSceneBO sceneModel) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", sceneModel.getId());
        map.put("isDelete", sceneModel.getIsDelete());
        map.put("description", sceneModel.getDescription());
        JSONArray result = HttpClientUtil.httpClient("/sceneModel/set", 2, map);
        DingdingSendUtil.sendDingdingMessage(sceneModel.getAccount(), sceneModel.getDescription(), sceneModel.getId());
        return result.getString(0);
    }

    /**
     * 标记场景为核心场景
     *
     * @param sceneModel 场景更新模型，包含场景ID和核心标记
     * @return String 返回操作结果
     */
    @SneakyThrows
    public String markCore(UpdateSceneBO sceneModel) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", sceneModel.getId());
        map.put("isCore", sceneModel.getIsCore());
        JSONArray result = HttpClientUtil.httpClient("/sceneModel/set", 2, map);
        return result.getString(0);
    }

    /**
     * 设置场景名称
     *
     * @param sceneModel 场景更新模型，包含场景ID和场景名称
     * @return String 返回操作结果
     */
    @SneakyThrows
    public String setScene(UpdateSceneBO sceneModel) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", sceneModel.getId());
        map.put("sceneName", sceneModel.getSceneName());
        JSONArray result = HttpClientUtil.httpClient("/sceneModel/set", 2, map);
        return result.getString(0);
    }

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
    @SneakyThrows
    public List<ServiceModel> queryGatewayService() {

        JSONArray result = HttpClientUtil.httpClient("/sceneModel/queryGatewayService", 2, new HashMap<>());
        return result.toList(ServiceModel.class);
    }

    /**
     * 获取网关的接口
     *
     * @param searchModel
     * @return
     */
    @SneakyThrows
    public List<ApiModel> queryGatewayApi(SearchModel searchModel) {
        Map<String, Object> params = new HashMap<>();
        params.put("serviceId", searchModel.getServiceName());
        JSONArray result = HttpClientUtil.httpClient("sceneModel/queryGatewayApi", 2, params);
        return result.toList(ApiModel.class);
    }

    @SneakyThrows
    public List<ClassModel> queryClass(SearchModel searchModel) {

        List<String> classNames = serviceClassMapper.getClassName(searchModel.getServiceName());
        List<ClassModel> result = new ArrayList<>();
        for (String className : classNames) {
            ClassModel classModel = new ClassModel();
            classModel.setClassName(className);
            result.add(classModel);
        }
        return result;

//        Map<String, Object> findMapBody = new HashMap<>();
//        findMapBody.put("serviceName", searchModel.getServiceName());
//        JSONArray result = HttpClientUtil.httpClient("/linkAnalysisModel/queryClassName", 2, findMapBody);
//        return result.toList(ClassModel.class);
    }

    @SneakyThrows
    public List<MethodModel> queryMehtod(SearchModel searchModel) {
        Map<String, Object> findMapBody = new HashMap<>();
        findMapBody.put("serviceName", searchModel.getServiceName());
        findMapBody.put("className", searchModel.getClassName());
        JSONArray result = HttpClientUtil.httpClient("/linkAnalysisModel/queryMethodName", 2, findMapBody);
        return result.toList(MethodModel.class);
    }

    /**
     * 查询方法参数
     *
     * @param searchModel
     * @return
     */
    @SneakyThrows
    public List<MethodLineNumsModel> queryLineNums(SearchModel searchModel) {
        Map<String, Object> findMapBody = new HashMap<>();
        findMapBody.put("serviceName", searchModel.getServiceName());
        findMapBody.put("className", searchModel.getClassName());
        findMapBody.put("methodName", searchModel.getMethodName());
        String parameters = searchModel.getParameters();
        if (StringUtils.isNotBlank(parameters)) {
            parameters = StringUtils.replace(parameters, "\"", "\\\"");
            findMapBody.put("parameters", parameters);
        }
        JSONArray result = HttpClientUtil.httpClient("/linkAnalysisModel/queryLineNums", 2, findMapBody);
        return result.toList(MethodLineNumsModel.class);
    }

    /**
     * 查询方法参数
     *
     * @param searchModel
     * @return
     */
    @SneakyThrows
    public List<MethodParametersModel> queryMehtodParameters(SearchModel searchModel) {

        Map<String, Object> findMapBody = new HashMap<>();
        findMapBody.put("serviceName", searchModel.getServiceName());
        findMapBody.put("className", searchModel.getClassName());
        findMapBody.put("methodName", searchModel.getMethodName());
        JSONArray result = HttpClientUtil.httpClient("/linkAnalysisModel/queryParameters", 2, findMapBody);
        return result.toList(MethodParametersModel.class);
    }

    @SneakyThrows
    public FrontModel querySceneDetail(SearchModel searchModel) {
        Map<String, Object> findMapBody = new HashMap<>();
        findMapBody.put("id", searchModel.getSceneId());
        Long beginTime = System.currentTimeMillis();
        JSONArray sceneArray = HttpClientUtil.httpClient("sceneNodeModel/find", 2, findMapBody);
//        System.out.println("querySceneDetail:"+sceneArray);
        Long endtime = System.currentTimeMillis();
        Long opetime = endtime - beginTime;
        String msg = "querySceneDetail方法，sceneId:" + searchModel.getSceneId() + ",请求sceneNodeModel/find的耗时：";
        log.info(msg + opetime);
        FrontModel frontModel = new FrontModel();
        //获取应用List
        List<CategoriesModel> categoriesModels = getServices(searchModel, sceneArray);
        int serviceSize = categoriesModels.size();
        int scenesize = sceneArray.size();
        int width = (int) (50 + Math.floor(getNodeWidth(categoriesModels) / 2) * 20);

        List<NodesModel> nodesModels = new ArrayList<>();
        List<LinksModel> linksModels = new ArrayList<>();
        //给每个应用设置一个随机的颜色
        List<String> colors = new ArrayList<>();
        for (int i = 0; i < serviceSize + 1; i++) {
            String color = getColor();
            colors.add(color);
        }
//        CategoriesModel cate = new CategoriesModel();
//        cate.setName("cerberus-universal");
        categoriesModels.add(getCerberus(sceneArray));
        //节点信息、调用关系
        for (int i = 0; i < scenesize; i++) {
            int centerY = (int) Math.floor(serviceSize / 2);

            JSONObject jsonObject = sceneArray.getJSONObject(i);
            String linkId = jsonObject.getString("linkId");
            String parentLinkId = jsonObject.getString("parentLinkId");
            String serviceName = jsonObject.getString("serviceName");
            int type = jsonObject.getIntValue("type");
            String[] parentLinkIds = StringUtils.split(parentLinkId, ",");
            for (int index = 0; index < parentLinkIds.length; index++) {
                //节点调用关系
                LinksModel linksModel = new LinksModel();
                linksModel.setTarget(linkId);
                linksModel.setSource(parentLinkIds[index]);
                linksModels.add(linksModel);
            }
            NodesModel nodesModel = jsonObject.toJavaObject(NodesModel.class);
            nodesModel.setId(linkId);
            nodesModel.setCategory(getServicesIndex(categoriesModels, serviceName));
            if (type == 2) {
                nodesModel.setX(width);
                nodesModel.setY(60);
                nodesModel.setSymbolSize(30);
            } else {
                for (int m = 0; m < serviceSize; m++) {
                    CategoriesModel categoriesModel = categoriesModels.get(m);
                    int cateSize = categoriesModel.getSize();
                    List<Integer> yList = categoriesModel.getY();
                    if (serviceName.equals(categoriesModel.getName())) {
                        int index = categoriesModel.getIndex();
//                        double centerX = (double) Math.round(size / 2) + 1;
//                        double x = 50 + (size - centerX) * 10;
//                        int type = jsonObject.getIntValue("type");
                        int yindex = (int) Math.round(index / 10);
                        if (type == 1) {
                            nodesModel.setX(width + (5 - (index - 10 * yindex)) * 30);
                            nodesModel.setY(yList.get(0) - 30);
                            nodesModel.setSymbolSize(20);
                        } else {
                            nodesModel.setX(width + (5 - (index - 10 * yindex)) * 30);
                            nodesModel.setY(yList.get(yindex));
                            nodesModel.setSymbolSize(10);
                        }
                        index = index - 1;
                        categoriesModel.setIndex(index);
                    }
                }
            }
            int SymbolSize = getSymbolSize(sceneArray.size());
            if (parentLinkId.equals("0")) {
                nodesModel.setSymbolSize(SymbolSize + 5);
            } else {
                nodesModel.setSymbolSize(SymbolSize);
            }
            nodesModels.add(nodesModel);
        }


        frontModel.setCategories(categoriesModels);
        frontModel.setLinks(linksModels);
        frontModel.setNodesList(nodesModels);
        frontModel.setColors(colors);
        Long modeltime = System.currentTimeMillis();
        Long model = modeltime - endtime;
        String msgmodel = "querySceneDetail方法，sceneId:" + searchModel.getSceneId() + ",请求model数据耗时：";
        log.info(msgmodel + model);
        //设置节点的坐标
        return frontModel;
//        return setXY(frontModel, searchModel);
    }

    public List<CategoriesModel> getServices(JSONArray array) {
        List<CategoriesModel> categoriesModels = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            CategoriesModel categoriesModel = new CategoriesModel();
            String serviceName = array.getJSONObject(i).getString("serviceName");
            categoriesModel.setName(serviceName);
            categoriesModels.add(categoriesModel);
        }
        return categoriesModels;
    }

    //    public List<NodesModel> getNodeDetail(SearchModel searchModel,JSONArray sceneArray) {
//        List<NodesModel> nodesModels = new ArrayList<>();
//        int scenesize = sceneArray.size();
//        for (int i = 0; i < scenesize; i++) {
//            JSONObject jsonObject = sceneArray.getJSONObject(i);
//            String linkId = jsonObject.getString("linkId");
//            String parentLinkId = jsonObject.getString("parentLinkId");
//            String[] parentLinkIds = StringUtils.split(parentLinkId, ",");
//            for (int index = 0; index < parentLinkIds.length; index++) {
//                //节点调用关系
//                LinksModel linksModel = new LinksModel();
//                linksModel.setTarget(linkId);
//                linksModel.setSource(parentLinkIds[index]);
//                linksModels.add(linksModel);
//            }
//            NodesModel nodesModel = jsonObject.toJavaObject(NodesModel.class);
//            nodesModel.setId(linkId);
//            nodesModel.setCategory(getServicesIndex(categoriesModels, jsonObject.getString("serviceName")));
//            nodesModel.setX(50);
//            nodesModel.setY(200);
//            int SymbolSize = getSymbolSize(sceneArray.size());
//            if (parentLinkId.equals("0")) {
//                nodesModel.setSymbolSize(SymbolSize + 5);
//            } else {
//                nodesModel.setSymbolSize(SymbolSize);
//            }
//            nodesModels.add(nodesModel);
//        }
//        return nodesModels;
//    }
    public CategoriesModel getCerberus(JSONArray sceneArray) {
        CategoriesModel cate = new CategoriesModel();
        int scenesize = sceneArray.size();
        for (int m = 0; m < scenesize; m++) {
            JSONObject jsonObject = sceneArray.getJSONObject(m);
            int type = jsonObject.getIntValue("type");
            if (type == 2) {
                cate.setName(jsonObject.getString("serviceName"));
            }
        }
        return cate;
    }

    public List<CategoriesModel> getServices(SearchModel searchModel, JSONArray sceneArray) {
        List<CategoriesModel> categoriesModels = new ArrayList<>();
        String services = searchModel.getServiceName();
        String[] service = StringUtils.split(services, ",");
        int length = service.length;
        int scenesize = sceneArray.size();
        for (int i = 0; i < length; i++) {
            CategoriesModel categoriesModel = new CategoriesModel();
            categoriesModel.setName(service[i]);
            int size = 0;
            for (int m = 0; m < scenesize; m++) {
                JSONObject jsonObject = sceneArray.getJSONObject(m);
                String serviceName = jsonObject.getString("serviceName");
                if (service[i].equals(serviceName)) {
                    size = size + 1;
                }
            }
            categoriesModel.setSize(size);
            categoriesModel.setIndex(size);
            categoriesModels.add(categoriesModel);
        }
        double y = 60;
        for (int i = 0; i < length; i++) {
            CategoriesModel categoriesModel = categoriesModels.get(i);
            double size = categoriesModel.getSize();
            if (size < 10) {
                y = y + 60;
                List<Integer> list = new ArrayList<>();
                list.add((int) y);
                categoriesModel.setY(list);
            } else {
//                double c = Math.floor(size/10);
//                int d = (int) Math.ceil(c);

                DecimalFormat decimalFormat = new DecimalFormat("0.00");
                String divide = decimalFormat.format((float) size / (float) 10);
                int d = (int) Math.ceil(Double.parseDouble(divide));

                List<Integer> list = new ArrayList<>();
                for (int m = 0; m < d; m++) {
                    y = y + 60;
                    list.add((int) y);
                    categoriesModel.setY(list);
                }
            }
        }
//        CategoriesModel categoriesModel = new CategoriesModel();
//        categoriesModel.setName("cerberus-universal");
//        categoriesModel.setSize(1);
//        categoriesModels.add(categoriesModel);
        return categoriesModels;
    }

    public int getNodeWidth(List<CategoriesModel> categoriesModels) {
        int size = categoriesModels.size();
        int width = 0;
        for (int i = 0; i < size; i++) {
            int serviceSize = categoriesModels.get(i).getSize();
            if (serviceSize > width) {
                width = serviceSize;
            }
        }
        return width;
    }
//    public int getNodeFloorNum(List<CategoriesModel> categoriesModels) {
//        int size = categoriesModels.size();
//        int floorNum = size;
//        for (int i = 0; i < size; i++) {
//            int serviceSize=categoriesModels.get(i).getSize();
//            int a = (int) Math.floor(serviceSize/10);
//            floorNum = floorNum + a;
//
//        }
//        return floorNum;
//    }

    public int getServicesIndex(List<CategoriesModel> categoriesModels, String serviceName) {
        int index = -1;
        int serviceSize = categoriesModels.size();
        for (int i = 0; i < serviceSize; i++) {
            String service = categoriesModels.get(i).getName();
            if (service.equals(serviceName)) {
                index = i;
            }
        }
        return index;
    }

    public int getSymbolSize(int size) {
        int SymbolSize = -1;
        if (size < 100) {
            SymbolSize = 20;
        } else if (size < 150) {
            SymbolSize = 15;
        } else {
            SymbolSize = 10;
        }
        return SymbolSize;
    }

    public String getColor() {
        String red;
        String green;
        String blue;
        Random random = new Random();
        //生成红色颜色代码
        red = Integer.toHexString(random.nextInt(256)).toUpperCase();
        //生成绿色颜色代码
        green = Integer.toHexString(random.nextInt(256)).toUpperCase();
        //生成蓝色颜色代码
        blue = Integer.toHexString(random.nextInt(256)).toUpperCase();

        //判断红色代码的位数
        red = red.length() == 1 ? "0" + red : red;
        //判断绿色代码的位数
        green = green.length() == 1 ? "0" + green : green;
        //判断蓝色代码的位数
        blue = blue.length() == 1 ? "0" + blue : blue;
        //生成十六进制颜色值
        String color = "#" + red + green + blue;

        System.out.println(color);
        return color;

    }

    //设置节点的XY坐标
    @SneakyThrows
    public FrontModel setXY(FrontModel frontModel, SearchModel searchModel) {
        Long starttime = System.currentTimeMillis();
        Map<String, Object> findMapBody = new HashMap<>();
        findMapBody.put("sceneId", searchModel.getSceneId());
        Long starttime1 = System.currentTimeMillis();
//        JSONArray depthArray = HttpClientUtil.httpClient("sceneNodeModel/findDepth", 2, findMapBody);
        Long endtime1 = System.currentTimeMillis();
        Long xytime1 = endtime1 - starttime1;
        String msgSecond1 = "querySceneDetail方法，sceneId:" + searchModel.getSceneId() + ",请求sceneNodeModel/findDepth的耗时：";
        log.info(msgSecond1 + xytime1);
//        System.out.println("depthArray:" + depthArray);
//        int depth = depthArray.getJSONObject(0).getInt("depth");
        int depth = searchModel.getDepth();
        int centerY = (int) Math.floor(depth / 2);
//        System.out.println("centerY:" + centerY);
        List<String> parentLinkId = new ArrayList<>();
        parentLinkId.add("0");

        Long forfirst = System.currentTimeMillis();
        for (int i = 0; i < depth + 1; i++) {
            List<NodesModel> nodes = getNodesData(parentLinkId, frontModel);
            parentLinkId = getLinkData(parentLinkId, frontModel);
//            int size = nodes.size();
//            System.out.println("parentLinkId:"+parentLinkId);
            int centerX = (int) Math.floor(nodes.size() / 2) + 1;
            int nodeSize = nodes.size();
//            System.out.println("centerX:"+centerX);
            Long forSecond = System.currentTimeMillis();
            for (int m = 0; m < nodeSize; m++) {
                NodesModel node = nodes.get(m);
                if (node.getX() == 50) {
                    node.setX(node.getX() + (m - centerX) * 5);
//                    System.out.println("node的X:"+node.getX());
                }
                if (node.getY() == 200) {
                    node.setY(node.getY() + (i - centerY) * 4);
//                    System.out.println("node的Y:"+node.getY());
                }

            }
            Long forSecondend = System.currentTimeMillis();
            Long forSecondendtime = forSecondend - forSecond;
            String msgSecond = "querySceneDetail方法，sceneId:" + searchModel.getSceneId() + ",第二个for循环耗时：";
            log.info(msgSecond + forSecondendtime);

        }
        Long forfirstend = System.currentTimeMillis();
        Long forfirsttime = forfirstend - forfirst;
        String msgfirst = "querySceneDetail方法，sceneId:" + searchModel.getSceneId() + ",第一个for循环耗时：";
        log.info(msgfirst + forfirsttime);

        Long endtime = System.currentTimeMillis();
        Long xytime = endtime - starttime;
        String msgxy = "querySceneDetail方法，sceneId:" + searchModel.getSceneId() + ",处理xy坐标耗时：";
        log.info(msgxy + xytime);
//        System.out.println("frontModel:"+frontModel);
        return frontModel;
    }

    //根据parentLinkId获取下一层节点
    public List<NodesModel> getNodesData(List<String> parentLinkId, FrontModel frontModel) {
        List<NodesModel> nodesList = frontModel.getNodesList();
        List<LinksModel> links = frontModel.getLinks();

        List<NodesModel> targetNodesList = new ArrayList<>();
        List<String> targetIdList = new ArrayList<>();

        //根据parentLinkId查询下一个节点的id
        for (int i = 0; i < links.size(); i++) {
            String source = links.get(i).getSource();
            for (int m = 0; m < parentLinkId.size(); m++) {
                if (source.equals(parentLinkId.get(m))) {
                    String target = links.get(i).getTarget();
                    targetIdList.add(target);
                }
            }

        }
        for (int i = 0; i < nodesList.size(); i++) {
            String id = nodesList.get(i).getId();
            for (int m = 0; m < targetIdList.size(); m++) {
                if (id.equals(targetIdList.get(m))) {
                    targetNodesList.add(nodesList.get(i));
                }
            }
        }
//        System.out.println("getNodesData的result："+targetNodesList);
        return ObjectConverterUtil.listDistinct(targetNodesList);
    }

    //获取下一层节点的ID
    public List<String> getLinkData(List<String> parentLinkId, FrontModel frontModel) {
        List<LinksModel> links = frontModel.getLinks();
        List<String> result = new ArrayList<>();
        Map<String, List> map = new HashMap<>();
        List<String> parent = new ArrayList<>();
        List<String> link = new ArrayList<>();


        for (int i = 0; i < links.size(); i++) {
            String source = links.get(i).getSource();
            String target = links.get(i).getTarget();
            for (int m = 0; m < parentLinkId.size(); m++) {
                if (source.equals(parentLinkId.get(m))) {
                    result.add(target);
                    parent.add(target);
                } else {
                    link.add(target);
                }
            }
        }
        map.put("parent", parent);
        map.put("link", link);
//        System.out.println("getLinkData的map："+map);
//        System.out.println("getLinkData的result："+result);
        return ObjectConverterUtil.listDistinct(result);
    }

    /**
     * 查询场景信息
     *
     * @param querySceneBO
     * @return
     */
    @SneakyThrows
    public AresBrainResult queryScene(QuerySceneBO querySceneBO) {
        AresBrainResult aresBrainResult = new AresBrainResult();
        Map<String, String> request = new HashMap<>();
        if (StringUtils.isNotBlank(querySceneBO.getServiceName())) {
            request.put("serviceId", querySceneBO.getServiceName());
        }
        if (StringUtils.isNotBlank(querySceneBO.getUrl())) {
            request.put("api", querySceneBO.getUrl());
        }
        if (StringUtils.isNotBlank(querySceneBO.getMethod())) {
            request.put("method", querySceneBO.getMethod());
        }
        if (StringUtils.isNotBlank(querySceneBO.getBranch())) {
            request.put("branch", querySceneBO.getBranch());
        }
        if (StringUtils.isNotBlank(querySceneBO.getIsITCov())) {
            request.put("isITCov", querySceneBO.getIsITCov());
        }
        request.put("page", querySceneBO.getPage() + "");
        request.put("pageSize", querySceneBO.getPageSize() + "");
        JSONArray jsonArrayScene = HttpClientUtil.httpClient("sceneModel/querySceneByModel", 2, request);
        JSONObject jsonObject = jsonArrayScene.getJSONObject(0);
        QuerySceneVO querySceneVO = new QuerySceneVO();
        querySceneVO.setPage(querySceneBO.getPage());
        querySceneVO.setPageSize(querySceneBO.getPageSize());
        JSONObject count = jsonObject.getJSONObject("queryScenePage");
        querySceneVO.setTotalNum(count.getIntValue("totalNum"));
        int totalHeat = count.getIntValue("totalHeat");
        List<SceneVOModel> sceneVOModels = jsonObject.getList("sceneModelEntitys", SceneVOModel.class);
        for (SceneVOModel sceneVOModel : sceneVOModels) {
            if (StringUtils.isBlank(sceneVOModel.getRequestBody())) {
                sceneVOModel.setRequestBody(sceneVOModel.getUrl());
            }
            sceneVOModel.setStrId(sceneVOModel.getId() + "");
            sceneVOModel.setHeatDegree(totalHeat == 0 ? 0.0f : Float.valueOf(String.format("%.2f", 1.0 * sceneVOModel.getFlow() / totalHeat * 100)));
        }
        querySceneVO.setSceneModels(sceneVOModels);
        aresBrainResult.setData(querySceneVO);
        return aresBrainResult;
    }

    @SneakyThrows
    public AresBrainResult querySceneAuditLog(SearchModel searchModel) {
        AresBrainResult result = new AresBrainResult();
        Map<String, Object> map = new HashMap<>();
        map.put("id", searchModel.getSceneId());
        JSONArray jsonArrayScene = HttpClientUtil.httpClient("sceneModel/getSceneAuditLog", 2, map);
        JSONObject jsonObject = jsonArrayScene.getJSONObject(0);
        SceneVOModel sceneVOModel = jsonObject.toJavaObject(SceneVOModel.class);
        result.setData(sceneVOModel);
        return result;
    }

    @SneakyThrows
    public List<BranchModel> queryBranch() {

        JSONArray result = HttpClientUtil.httpClient("sceneBranch/querySceneBranch", 1, "");
        return result.toList(BranchModel.class);
    }


    @SneakyThrows
    public List<TraceIdListVo> querySceneBatch(QuerySceneBO querySceneBO) {
        Map<String, String> request = new HashMap<>();
        List<TraceIdListVo> traceIdListVos = new ArrayList<>();
        if (StringUtils.isNotBlank(querySceneBO.getServiceName())) {
            request.put("serviceId", querySceneBO.getServiceName());
        }
        if (StringUtils.isNotBlank(querySceneBO.getUrl())) {
            request.put("api", querySceneBO.getUrl());
        }
        if (StringUtils.isNotBlank(querySceneBO.getMethod())) {
            request.put("method", querySceneBO.getMethod());
        }
        if (StringUtils.isNotBlank(querySceneBO.getBranch())) {
            request.put("branch", querySceneBO.getBranch());
        }
        int page = querySceneBO.getPage();
        while (true) {
            request.put("page", page + "");
            request.put("pageSize", querySceneBO.getPageSize() + "");
            JSONArray jsonArrayScene = HttpClientUtil.httpClient("sceneModel/querySceneByModel", 2, request);
            JSONObject jsonObject = jsonArrayScene.getJSONObject(0);
            List<SceneVOModel> sceneVOModels = jsonObject.getList("sceneModelEntitys", SceneVOModel.class);
            for (SceneVOModel sceneVOModel : sceneVOModels) {
                TraceIdListVo traceIdListVo = new TraceIdListVo();
                traceIdListVo.setTraceId(sceneVOModel.getSceneId());
                traceIdListVo.setSceneId(sceneVOModel.getId());
                traceIdListVos.add(traceIdListVo);
            }
            if (sceneVOModels.size() < querySceneBO.getPageSize()) {
                break;
            }
            page++;
        }

        return traceIdListVos;
    }

}