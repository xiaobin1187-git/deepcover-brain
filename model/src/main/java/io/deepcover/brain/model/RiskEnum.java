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

package io.deepcover.brain.model;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONType;

/**
 * 状态枚举
 *
 * @author houlandong
 */
@JSONType(serializeEnumAsJavaBean = true)
public enum RiskEnum {

    HIGHRISK("高风险", 1, "primary"),
    MEDIUMRISK("中风险", 2, "warning"),
    LOWRISK("低风险", 3, "info"),
    NORISK("无风险", 4, "success"),
    UNKNOWRISK("风险未知", 5, "primary"),
    NULL("", 6, "");
    private final String riskName;
    private final int code;
    private final String color;

    /**
     * @param riskName
     * @param code
     */
    RiskEnum(String riskName, int code, String color) {
        this.riskName = riskName;
        this.code = code;
        this.color = color;
    }

    public String getRiskName() {
        return riskName;
    }

    public int getCode() {
        return code;
    }

    public String getColor() {
        return color;
    }

    @Override
    public String toString() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", code);
        jsonObject.put("color", color);
        jsonObject.put("riskName", riskName);
        return jsonObject.toString();
    }
}