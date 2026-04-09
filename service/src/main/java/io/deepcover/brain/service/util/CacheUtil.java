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

import org.springframework.data.redis.core.RedisTemplate;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis缓存工具类
 *
 * <p>基于Spring Data Redis的RedisTemplate封装，提供String、List、Hash、Set等常用数据结构的操作方法。</p>
 *
 * @author huangtai
 */
public class CacheUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    public CacheUtil(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * string类型 - 默认26小时过期
     **/
    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value, 26 * 60 * 60, TimeUnit.SECONDS);
    }

    /**
     * string类型 - 2小时过期
     **/
    public void setAllService(String key, String value) {
        redisTemplate.opsForValue().set(key, value, 2 * 60 * 60, TimeUnit.SECONDS);
    }

    /**
     * string类型 - 设置整数值，26小时过期
     **/
    public void setHeat(String key, int value) {
        redisTemplate.opsForValue().set(key, value, 26 * 60 * 60, TimeUnit.SECONDS);
    }

    public boolean setIfAbsent(String key, String value) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value);
        return result != null && result;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> values) {
        Object obj = redisTemplate.opsForValue().get(key);
        if (obj == null) {
            return null;
        }
        return (T) obj;
    }

    public Long increment(String key, int value) {
        return redisTemplate.opsForValue().increment(key, value);
    }

    public Long decrement(String key, int value) {
        return redisTemplate.opsForValue().decrement(key, value);
    }

    /**
     * List类型
     **/
    public void listLeftPush(String key, String value) {
        redisTemplate.opsForList().leftPush(key, value);
    }

    public void listRightPush(String key, String value) {
        redisTemplate.opsForList().rightPush(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T listLeftPop(String key, Class<T> var2) {
        Object obj = redisTemplate.opsForList().leftPop(key);
        if (obj == null) {
            return null;
        }
        return (T) obj;
    }

    @SuppressWarnings("unchecked")
    public <T> T listRightPop(String key, Class<T> var2) {
        Object obj = redisTemplate.opsForList().rightPop(key);
        if (obj == null) {
            return null;
        }
        return (T) obj;
    }

    /**
     * Hash类型
     **/
    public <T> void hashPut(String key, String hashKey, T value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    public <T> boolean hashPutIfAbsent(String key, String hashKey, T value) {
        Boolean result = redisTemplate.opsForHash().putIfAbsent(key, hashKey, value);
        return result != null && result;
    }

    @SuppressWarnings("unchecked")
    public <T> T hashGet(String key, String hashKey, Class<T> var3) {
        Object obj = redisTemplate.opsForHash().get(key, hashKey);
        if (obj == null) {
            return null;
        }
        return (T) obj;
    }

    public long hashDel(String key, String... hashKeys) {
        return redisTemplate.opsForHash().delete(key, (Object[]) hashKeys);
    }

    public boolean hashHasKey(String key, String hashKey) {
        Boolean result = redisTemplate.opsForHash().hasKey(key, hashKey);
        return result != null && result;
    }

    /**
     * set类型
     **/
    @SuppressWarnings("unchecked")
    public <T> long setAdd(String key, T... values) {
        Long result = redisTemplate.opsForSet().add(key, (Object[]) values);
        return result != null ? result : 0;
    }

    @SuppressWarnings("unchecked")
    public <T> long setRemove(String key, T... values) {
        Long result = redisTemplate.opsForSet().remove(key, (Object[]) values);
        return result != null ? result : 0;
    }

    @SuppressWarnings("unchecked")
    public <T> Set<T> setMember(String key, Class<T> values) {
        Set<Object> members = redisTemplate.opsForSet().members(key);
        if (members == null) {
            return java.util.Collections.emptySet();
        }
        return (Set<T>) members;
    }

    /**
     * 通用功能
     **/
    public boolean setExpire(String key, long timeout) {
        Boolean result = redisTemplate.expire(key, timeout, TimeUnit.SECONDS);
        return result != null && result;
    }

    public long getExpire(String key) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null ? ttl : 0;
    }

    public long delete(String... keys) {
        Long result = redisTemplate.delete(java.util.Arrays.asList(keys));
        return result != null ? result : 0;
    }

    public boolean hasKey(String key) {
        Boolean result = redisTemplate.hasKey(key);
        return result != null && result;
    }
}
