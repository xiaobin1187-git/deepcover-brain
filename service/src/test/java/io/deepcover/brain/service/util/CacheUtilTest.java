package io.deepcover.brain.service.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheUtilTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOps;

    @Mock
    private ListOperations<String, Object> listOps;

    @Mock
    private HashOperations<String, Object, Object> hashOps;

    @Mock
    private SetOperations<String, Object> setOps;

    private CacheUtil cacheUtil;

    @BeforeEach
    void setUp() {
        cacheUtil = new CacheUtil(redisTemplate);
    }

    @Test
    void set_shouldStoreValueWithExpiry() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        cacheUtil.set("key1", "value1");
        verify(valueOps).set("key1", "value1", 26 * 60 * 60, TimeUnit.SECONDS);
    }

    @Test
    void setAllService_shouldStoreWith2HourExpiry() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        cacheUtil.setAllService("key1", "value1");
        verify(valueOps).set("key1", "value1", 2 * 60 * 60, TimeUnit.SECONDS);
    }

    @Test
    void setHeat_shouldStoreIntValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        cacheUtil.setHeat("key1", 42);
        verify(valueOps).set("key1", 42, 26 * 60 * 60, TimeUnit.SECONDS);
    }

    @Test
    void setIfAbsent_shouldReturnTrueWhenSetSucceeds() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent("key1", "value1")).thenReturn(true);
        assertTrue(cacheUtil.setIfAbsent("key1", "value1"));
    }

    @Test
    void setIfAbsent_shouldReturnFalseWhenKeyExists() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent("key1", "value1")).thenReturn(false);
        assertFalse(cacheUtil.setIfAbsent("key1", "value1"));
    }

    @Test
    void get_shouldReturnValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("key1")).thenReturn("hello");
        assertEquals("hello", cacheUtil.get("key1", String.class));
    }

    @Test
    void get_shouldReturnNullWhenKeyNotFound() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("key1")).thenReturn(null);
        assertNull(cacheUtil.get("key1", String.class));
    }

    @Test
    void increment_shouldIncrementByValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment("key1", 5)).thenReturn(15L);
        assertEquals(15L, cacheUtil.increment("key1", 5));
    }

    @Test
    void decrement_shouldDecrementByValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.decrement("key1", 3)).thenReturn(7L);
        assertEquals(7L, cacheUtil.decrement("key1", 3));
    }

    @Test
    void listLeftPush_shouldPushToList() {
        when(redisTemplate.opsForList()).thenReturn(listOps);
        cacheUtil.listLeftPush("list1", "value1");
        verify(listOps).leftPush("list1", "value1");
    }

    @Test
    void listRightPush_shouldPushToList() {
        when(redisTemplate.opsForList()).thenReturn(listOps);
        cacheUtil.listRightPush("list1", "value1");
        verify(listOps).rightPush("list1", "value1");
    }

    @Test
    void listLeftPop_shouldReturnPoppedValue() {
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.leftPop("list1")).thenReturn("value1");
        assertEquals("value1", cacheUtil.listLeftPop("list1", String.class));
    }

    @Test
    void listLeftPop_shouldReturnNullWhenListEmpty() {
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.leftPop("list1")).thenReturn(null);
        assertNull(cacheUtil.listLeftPop("list1", String.class));
    }

    @Test
    void hashPut_shouldStoreInHash() {
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        cacheUtil.hashPut("hash1", "field1", "value1");
        verify(hashOps).put("hash1", "field1", "value1");
    }

    @Test
    void hashPutIfAbsent_shouldReturnTrueWhenFieldNotExists() {
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.putIfAbsent("hash1", "field1", "value1")).thenReturn(true);
        assertTrue(cacheUtil.hashPutIfAbsent("hash1", "field1", "value1"));
    }

    @Test
    void hashGet_shouldReturnFieldValue() {
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.get("hash1", "field1")).thenReturn("value1");
        assertEquals("value1", cacheUtil.hashGet("hash1", "field1", String.class));
    }

    @Test
    void hashDel_shouldDeleteFields() {
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.delete("hash1", "field1")).thenReturn(1L);
        assertEquals(1L, cacheUtil.hashDel("hash1", "field1"));
    }

    @Test
    void hashHasKey_shouldReturnTrueWhenFieldExists() {
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.hasKey("hash1", "field1")).thenReturn(true);
        assertTrue(cacheUtil.hashHasKey("hash1", "field1"));
    }

    @Test
    void setAdd_shouldAddMembers() {
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.add(eq("set1"), any())).thenReturn(1L);
        assertEquals(1L, cacheUtil.setAdd("set1", "member1"));
    }

    @Test
    void setRemove_shouldRemoveMembers() {
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(setOps.remove(eq("set1"), any())).thenReturn(1L);
        assertEquals(1L, cacheUtil.setRemove("set1", "member1"));
    }

    @Test
    void setMember_shouldReturnMembers() {
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        Set<Object> members = new HashSet<>(Arrays.asList("a", "b"));
        when(setOps.members("set1")).thenReturn(members);
        Set<String> result = cacheUtil.setMember("set1", String.class);
        assertEquals(2, result.size());
    }

    @Test
    void setExpire_shouldSetExpiration() {
        when(redisTemplate.expire("key1", 300L, TimeUnit.SECONDS)).thenReturn(true);
        assertTrue(cacheUtil.setExpire("key1", 300));
    }

    @Test
    void getExpire_shouldReturnTTL() {
        when(redisTemplate.getExpire("key1", TimeUnit.SECONDS)).thenReturn(300L);
        assertEquals(300L, cacheUtil.getExpire("key1"));
    }

    @Test
    void delete_shouldRemoveKeys() {
        when(redisTemplate.delete(Arrays.asList("key1", "key2"))).thenReturn(2L);
        assertEquals(2L, cacheUtil.delete("key1", "key2"));
    }

    @Test
    void hasKey_shouldReturnTrueWhenKeyExists() {
        when(redisTemplate.hasKey("key1")).thenReturn(true);
        assertTrue(cacheUtil.hasKey("key1"));
    }

    @Test
    void hasKey_shouldReturnFalseWhenKeyNotExists() {
        when(redisTemplate.hasKey("key1")).thenReturn(false);
        assertFalse(cacheUtil.hasKey("key1"));
    }
}
