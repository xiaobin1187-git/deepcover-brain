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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.deepcover.brain.service.util.client.HttpClientUtil;
import io.deepcover.brain.service.util.client.HttpHeader;
import net.sf.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 钉钉消息工具类
 */
public class DingdingSendUtil {

    private final static String token = "YOUR_DINGTALK_ACCESS_TOKEN";

    /**
     * 发送钉钉工作通知
     *
     * @param account
     * @param desc
     * @param sceneId
     */
    public static void sendDingdingMessage(String account, String desc, long sceneId) {
        List<String> dingIds = Arrays.asList("YOUR_DINGTALK_USER_ID_1", "YOUR_DINGTALK_USER_ID_2");
        StringBuffer theMessage = new StringBuffer();
        String enter = "\n\r";
        theMessage.append("场景反馈告警,请及时处理,信息如下:").append(enter);
        theMessage.append("反馈人:" + account).append(enter);
        theMessage.append("场景Id:" + sceneId).append(enter);
        theMessage.append("反馈信息:" + desc);
        theMessage.append("@" + dingIds.get(0) + "@" + dingIds.get(1)).append(enter);

        String url = "https://oapi.dingtalk.com/robot/send?access_token=" + token;
        JSONObject json = new JSONObject();
        json.put("title", "场景反馈提醒");
        json.put("text", theMessage.toString());

        Map<String, Object> at = new HashMap<>();
        at.put("isAtAll", false);//false @指定的手机号码，true @所有人
        at.put("atMobiles", "");
        if (dingIds != null) {
            at.put("atUserIds", dingIds);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("msgtype", "markdown");
        params.put("markdown", json);
        params.put("at", at);
        JsonObject postJson = new JsonParser().parse(new Gson().toJson(params)).getAsJsonObject();
        try {

            HttpHeader httpHeader = new HttpHeader();
            httpHeader.addHeader("Content-Type", "application/json");
            HttpClientUtil.postJson(url, postJson, httpHeader);
        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}
