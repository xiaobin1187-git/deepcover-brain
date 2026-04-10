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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.beans.BeanCopier;
import net.sf.cglib.core.Converter;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模型转换器
 *
 * @author houlandong
 */
@Slf4j
public class ObjectConverterUtil {
    private static final Map<String, BeanCopier> beanCopierMap = new HashMap<String, BeanCopier>();
    private static final Converter converter = new CglibConverter();

    /**
     * 把source对象属性值赋值给target对象
     *
     * @param source
     * @param target
     */
    public static <T, E> void convert(T source, E target) {

        if (source == null || target == null) {
            return;
        }
        log.info("进行模型转换");
        String key = source.getClass() + " " + target.getClass();
        BeanCopier beanCopier;
        if (beanCopierMap.get(key) == null) {
            beanCopier = BeanCopier.create(source.getClass(),
                    target.getClass(), true);
            beanCopierMap.put(key, beanCopier);
        } else {
            beanCopier = beanCopierMap.get(key);
        }
        beanCopier.copy(source, target, converter);
    }

    /**
     * @param obj
     * @return
     */
    public static String toJson(Object obj) {
        String jsonStr = null;
        try {
            jsonStr = new ObjectMapper().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Convert bean to json failed.", e);
        }
        return jsonStr;
    }

    /**
     * 对象转Map
     *
     * @param obj
     * @return
     */
    public static Map<String, Object> toMap(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Map) return (Map<String, Object>) obj;

        Map<String, Object> map = new HashMap<String, Object>();
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor property : propertyDescriptors) {
                String key = property.getName();
                if (!key.equals("class")) {
                    Method getter = property.getReadMethod();
                    Object value = getter.invoke(obj);
                    map.put(key, value);
                }
            }
        } catch (Exception e) {
            log.error("Convert bean to map failed. Bean is {}.", toJson(obj), e);
        }

        return map;
    }


    public static <T> List<T> listDistinct(List<T> list) {
        list = list.stream().distinct().collect(Collectors.toList());
        return list;
    }

    public static List<Object> jsonToList(Object jsonStr) {
        List<Object> mapList = new ArrayList<Object>();
        JSONArray jsonArray = null;
        try {
            jsonArray = JSONArray.fromObject(jsonStr);
        } catch (Exception e) {
            mapList.add(toMap(jsonStr));
            return mapList;
        }

        Iterator<JSONObject> it = jsonArray.iterator();
        while (it.hasNext()) {
            Object obj = it.next();
            if (obj instanceof JSONObject) {
                JSONObject jsonbject = (JSONObject) obj;
                mapList.add(toMap(jsonbject));
            } else {
                mapList.add(obj);
            }
        }

        return mapList;
    }
}
