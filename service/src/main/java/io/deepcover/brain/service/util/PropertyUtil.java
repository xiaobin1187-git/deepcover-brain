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


import org.apache.commons.lang3.StringUtils;

public class PropertyUtil {

    /**
     * 获取系统属性带默认值
     *
     * @param key          属性key
     * @param defaultValue 默认值
     * @return 属性值 or 默认值
     */
    public static String getSystemPropertyOrDefault(String key, String defaultValue) {
        String property = System.getProperty(key);
        return StringUtils.isEmpty(property) ? defaultValue : property;
    }

    /**
     * 获取环境变量
     * @param key
     * @param defaultValue
     * @return
     */
    public static String getEnvPropertyOrDefault(String key, String defaultValue) {
        String property = System.getenv(key);
        return StringUtils.isEmpty(property) ? defaultValue : property;
    }
}
