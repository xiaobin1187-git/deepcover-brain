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

package io.deepcover.brain.service.util;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.deepcover.brain.dal.entity.DiffRecordEntity;
import io.deepcover.brain.service.util.client.HttpClientRequest;
import io.deepcover.brain.service.util.client.HttpClientResponse;
import io.deepcover.brain.service.util.client.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author: wuchen
 * @since: 2020-03-19 19:26
 **/
@Slf4j
@Component
public class DingNotifyUtils {

    private Logger logger = LoggerFactory.getLogger(getClass());
    private static String dingIds;
    private static String mobiles;

    @Value("${epaas.dingId:e8bda016b61fedd99559a4f15c81b0f446c12471a68522092e47986ac5ce2bb3}")
    public void setDingIds(String dingIds) {
        DingNotifyUtils.dingIds = dingIds;
    }
    @Value("${epaas.mobiles:13738006160,13516718242,15158135403,17605883323}")
    public void setmobiles(String mobiles) {
        DingNotifyUtils.mobiles = mobiles;
    }
    public static void sendEpassAnalyseWorkMessage(DiffRecordEntity recordEntity, String traceId) {
        // 固定的手机号列表用于@功能
        List<String> mobileList = Arrays.asList(mobiles.split(","));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        String serviceName = recordEntity != null ? recordEntity.getServiceName() : "未知服务";
        String envCode = recordEntity != null ? recordEntity.getEnvCode() : "未知环境";
        String baseVersion = recordEntity != null ? recordEntity.getBaseVersion() : "未知";
        String nowVersion = recordEntity != null ? recordEntity.getNowVersion() : "未知";
        String nowBranch = recordEntity != null ? recordEntity.getNowBranch() : "未知分支";
        Date publishTime = recordEntity != null ? recordEntity.getPublishTime() : null;
        Date createdTime = recordEntity != null ? recordEntity.getCreatedTime() : new Date();
        
        String reportUrl = HttpClientUtil.aresFaceUrl + "/diffDetailEpaas" +
                "?serviceName=" + serviceName +
                "&envCode=" + envCode +
                "&baseVersion=" + baseVersion +
                "&nowVersion=" + nowVersion;
        
        // 构建消息文本
        StringBuilder messageText = new StringBuilder();
        messageText.append("#### epaas精准测试结果通知\\n\\n");
        messageText.append("- 应用：").append(serviceName).append("\\n");
        messageText.append("- 环境：").append(envCode).append("\\n");
        messageText.append("- traceId：").append(traceId).append("\\n");
        messageText.append("- 发布分支：").append(nowBranch).append("\\n");
        messageText.append("- 版本对比：").append(baseVersion).append(" -> ").append(nowVersion).append("\\n");
        if (publishTime != null) {
            messageText.append("- 发布时间：").append(sdf.format(publishTime)).append("\\n");
        } else {
            messageText.append("- 触发时间：").append(sdf.format(createdTime)).append("\\n");
        }
        messageText.append("- 变更分析详情：[点击查看](").append(reportUrl).append(")\\n");
        
        // 在消息文本中添加@信息，这是钉钉机器人@功能的关键
        StringBuilder atInfo = new StringBuilder();
        atInfo.append("\\n");
        for (String mobile : mobileList) {
            atInfo.append("@").append(mobile).append(" ");
        }
        messageText.append(atInfo.toString()).append("\\n");
        
        // 构建@人员列表（手机号）
        StringBuilder atMobiles = new StringBuilder();
        for (int i = 0; i < mobileList.size(); i++) {
            if (i > 0) {
                atMobiles.append(", ");
            }
            atMobiles.append("\"").append(mobileList.get(i)).append("\"");
        }
        
        // 构建完整的JSON消息
        String jsonStr = "{\n" +
                "    \"msgtype\": \"markdown\",\n" +
                "    \"markdown\": {\n" +
                "        \"title\": \"epaas精准测试结果通知\",\n" +
                "        \"text\": \"" + messageText.toString() + "\"\n" +
                "    },\n" +
                "    \"at\": {\n" +
                "        \"atMobiles\": [" + atMobiles.toString() + "],\n" +
                "        \"isAtAll\": false\n" +
                "    }\n" +
                "}";
        
        log.info("发送的钉钉消息内容: {}", jsonStr);
        
        // 发送钉钉消息
        List<String> robotIdList = Arrays.asList(dingIds.split(","));
        JSONObject jsonObject = JSONObject.parseObject(jsonStr);
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json; charset=UTF-8");
        
        for (String robotId : robotIdList) {
            HttpClientRequest sendRequest = new HttpClientRequest();
            sendRequest.setType(2);
            sendRequest.setUrl("https://oapi.dingtalk.com/robot/send?access_token=" + robotId.trim());
            sendRequest.setRequestBody(jsonObject);
            sendRequest.setHeaders(headers);
            
            HttpClientResponse result = HttpClientUtil.sendRequest(sendRequest);
            log.info("发送钉钉消息结果: {}", result.getResponseBody());
        }
    }

    public void sendMsg(String access_token, String username, String action, String param) {
        String par = param.isEmpty() ? "all" : param;
        String url = "https://oapi.dingtalk.com/robot/send?access_token=" + access_token;
        String encod = "UTF-8";
        String jsonStr = "{\n" +
                "    \"msgtype\": \"markdown\",\n" +
                "    \"markdown\": {\n" +
                "        \"title\": \"GTR\",\n" +
                "        \"text\": \"#### **<font color=#BB5500 >【GTR平台操作监控】</font>**\\n> ###### 压测操作：" + action + "\\n> ###### 操作环境:VPC模拟环境 \\n> ######  压测任务名 ：" + par + "\\n> ######  压测执行人 ：" + username + "\\n> ###### 详见 [GTR平台系统日志](http://sml-gtr.esign.cn/gtr/index.html#modules/sys/log.html)\\n\"\n" +
                "    },\n" +
                "    \"at\": {\n" +
                "        \"atMobiles\": [\n" +
                "            \"18658191187\"\n" +
                "        ],\n" +
                "        \"isAtAll\": false\n" +
                "    }\n" +
                "}";

        HttpClientRequest request = new HttpClientRequest();
        request.setType(2);
        request.setUrl(url);
        request.setRequestBody(jsonStr);
        HttpClientUtil.sendRequest(request);
    }


    public static void feedMsg(Map<String, Object> params) {
        String token = params.get("dingToken").toString();
        String title = "压测结果通知";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String enter = "\n";
        StringBuilder sb = new StringBuilder();
        sb.append("#### **<font color=#008B8B >【").append("压测结果通知").append("】</font>**").append(enter)
                .append("> - 流程ID：").append(params.get("flowId")).append(enter);
        if ((Integer) params.get("stressStatus") == 2) {
            sb.append("> - 压测状态：").append("成功").append(enter);
            sb.append("> - 压测报告地址：").append("[点击查看](").append(params.get("stressUrl")).append(")").append(enter);
        } else {
            sb.append("> - 压测状态：").append("失败").append(enter);
        }
        sb.append("> - 时间：").append(sdf.format(new Date())).append(enter)
                .append("> ###### 数据来源：[GTR性能压测平台]").append("(http://sml-gtr.esign.cn/gtr/index.html)").append(enter);

        String[] tokens = token.split(",");
        Arrays.stream(tokens).forEach((o) ->
                sendDingding(title, sb.toString(), o)
        );
    }

    private static void sendDingding(String title, String text, String token) {
        String url = "https://oapi.dingtalk.com/robot/send?access_token=" + token;
        JSONObject json = new JSONObject();
        json.put("title", title);
        json.put("text", text);

        Map<String, Object> at = new HashMap<>();
        at.put("isAtAll", false);
        at.put("atMobiles", "");

        JSONObject params = new JSONObject();
        params.put("msgtype", "markdown");
        params.put("markdown", json.toString());
        params.put("at", at.toString());

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        HttpClientRequest request = new HttpClientRequest();
        request.setType(2);
        request.setUrl(url);
        request.setRequestBody(params);
        request.setHeaders(headers);
        HttpClientUtil.sendRequest(request);
    }

    public static void sendDiffAnalyseWorkMessage(String action, DiffRecordEntity recordEntity) {
        if (recordEntity.getAddBy() == null) {
            return;
        }
        HttpClientRequest request = new HttpClientRequest();
        request.setType(1);
        request.setUrl(HttpClientUtil.innerUserUrl + "?alias=" + recordEntity.getAddBy());
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json; charset=UTF-8");
        request.setHeaders(headers);
        HttpClientResponse response = HttpClientUtil.sendRequest(request);
        if (response == null || !"200".equals(response.getStateCode()) || response.getResponseBody() == null) {
            log.error("查询用户信息失败,花名：{}", recordEntity.getAddBy());
        } else {
            JSONArray json = JSONObject.parseObject(response.getResponseBody().toString()).getJSONArray("data");
            if (json != null && json.size() == 1) {
                String usr = json.getJSONObject(0).getString("account");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                sdf.format(new Date());
                String reportUrl = HttpClientUtil.aresFaceUrl + "/diffDetail" +
                        "?serviceName=" + recordEntity.getServiceName() +
                        "&envCode=" + recordEntity.getEnvCode() +
                        "&baseVersion=" + recordEntity.getBaseVersion() +
                        "&nowVersion=" + recordEntity.getNowVersion();

                String jsonStr = "{\n" +
                        "    \"msgtype\": \"1\",\n" +
                        "    \"message\": \"【精准测试结果通知】 "
                        + "\\n操作：" + action
                        + "\\n应用：" + recordEntity.getServiceName()
                        + "\\n环境：" + recordEntity.getEnvCode()
                        + (recordEntity.getNowBranch() == null ? "" : "\\n发布分支：" + recordEntity.getNowBranch())
                        + "\\n触发人：" + recordEntity.getAddBy()
                        + (recordEntity.getPublishTime() == null ? "\\n触发时间：" + sdf.format(recordEntity.getCreatedTime()) : "\\n发布时间：" + recordEntity.getPublishTime())
                        + "\\n变更分析详情：" + reportUrl + "\\n\",\n" +
//                + "\\n 分析完成时间: " + startTime + "\\n 环境类型: " + stopTime + "\\n 变更分支：" + status +"\\n 操作环境：模拟环境 \\n 压测脚本名：" + taskName + "\\n 触发人：" + usr + "\\n 压测报告Id: " + reportId + "\\n 详情查看：" + reportUrl + "\\n\",\n" +
                        "    \"task_id\": \"012\",\n" +
                        "    \"userList\": [\"" + usr + "\"]\n" +
                        "}";

                request.setType(2);
                request.setUrl(HttpClientUtil.dingtalkUrl);

                request.setRequestBody(jsonStr);
                HttpClientResponse result = HttpClientUtil.sendRequest(request);
                log.info(result.toString());
            } else {
                log.error("查询用户信息失败,花名：{}", recordEntity.getAddBy());
            }

        }
    }

    public static void main(String[] args) {
//        sendWorkMessage("停止","2023-01-11 20:45:03","2023-01-11 20:45:19","test.jmx","压测完成","yingzhu","http://dingtalk.testk8s.tsign.cn/dingtalk/sendMessage/","001");
//        String report = "2020030311533843/case20200304015338136/case202003040153381362530.csv";
//        String tile = report.substring(0,report.indexOf("."));
//        System.out.println(tile);
        https://oapi.dingtalk.com/robot/send?access_token=e8bda016b61fedd99559a4f15c81b0f446c12471a68522092e47986ac5ce2bb3
                     sendDingding("aa","11","e8bda016b61fedd99559a4f15c81b0f446c12471a68522092e47986ac5ce2bb3");
    }
}
