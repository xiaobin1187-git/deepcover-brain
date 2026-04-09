package io.deepcover.brain.service.util;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ObjectConverterUtilTest {

    // ---- helper beans for testing ----

    public static class SourceBean {
        private String name;
        private int age;

        public SourceBean() {}

        public SourceBean(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }

    public static class TargetBean {
        private String name;
        private int age;

        public TargetBean() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }

    public static class ExtendedBean extends SourceBean {
        private String email;

        public ExtendedBean() {}

        public ExtendedBean(String name, int age, String email) {
            super(name, age);
            this.email = email;
        }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    // ---- convert() tests ----

    @Test
    void convert_shouldCopyMatchingProperties() {
        SourceBean source = new SourceBean("test", 25);
        TargetBean target = new TargetBean();

        ObjectConverterUtil.convert(source, target);

        assertEquals("test", target.getName());
        assertEquals(25, target.getAge());
    }

    @Test
    void convert_shouldHandleNullSource() {
        TargetBean target = new TargetBean();
        target.setName("original");

        ObjectConverterUtil.convert(null, target);

        assertEquals("original", target.getName());
    }

    @Test
    void convert_shouldHandleNullTarget() {
        SourceBean source = new SourceBean("test", 25);

        assertDoesNotThrow(() -> ObjectConverterUtil.convert(source, null));
    }

    @Test
    void convert_shouldHandleBothNull() {
        assertDoesNotThrow(() -> ObjectConverterUtil.convert(null, null));
    }

    @Test
    void convert_shouldOverwriteTargetValues() {
        SourceBean source = new SourceBean("newName", 30);
        TargetBean target = new TargetBean();
        target.setName("oldName");
        target.setAge(20);

        ObjectConverterUtil.convert(source, target);

        assertEquals("newName", target.getName());
        assertEquals(30, target.getAge());
    }

    @Test
    void convert_shouldHandleSourceWithNullFields() {
        SourceBean source = new SourceBean(null, 0);
        TargetBean target = new TargetBean();
        target.setName("existing");
        target.setAge(99);

        ObjectConverterUtil.convert(source, target);

        assertNull(target.getName());
        assertEquals(0, target.getAge());
    }

    @Test
    void convert_shouldReuseCachedCopierOnSecondCall() {
        SourceBean source1 = new SourceBean("first", 10);
        TargetBean target1 = new TargetBean();
        ObjectConverterUtil.convert(source1, target1);

        SourceBean source2 = new SourceBean("second", 20);
        TargetBean target2 = new TargetBean();
        ObjectConverterUtil.convert(source2, target2);

        assertEquals("second", target2.getName());
        assertEquals(20, target2.getAge());
    }

    // ---- toJson() tests ----

    @Test
    void toJson_shouldSerializeObjectToJson() {
        SourceBean bean = new SourceBean("test", 25);

        String json = ObjectConverterUtil.toJson(bean);

        assertNotNull(json);
        assertTrue(json.contains("\"name\":\"test\""));
        assertTrue(json.contains("\"age\":25"));
    }

    @Test
    void toJson_shouldSerializeMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key", "value");
        map.put("num", 42);

        String json = ObjectConverterUtil.toJson(map);

        assertNotNull(json);
        assertTrue(json.contains("\"key\":\"value\""));
        assertTrue(json.contains("\"num\":42"));
    }

    @Test
    void toJson_shouldSerializeList() {
        List<String> list = Arrays.asList("a", "b", "c");

        String json = ObjectConverterUtil.toJson(list);

        assertNotNull(json);
        assertTrue(json.contains("a"));
        assertTrue(json.contains("b"));
        assertTrue(json.contains("c"));
    }

    @Test
    void toJson_shouldHandleNullObject() {
        String json = ObjectConverterUtil.toJson(null);

        assertEquals("null", json);
    }

    @Test
    void toJson_shouldSerializeEmptyBean() {
        SourceBean bean = new SourceBean();

        String json = ObjectConverterUtil.toJson(bean);

        assertNotNull(json);
        assertTrue(json.contains("\"name\":null"));
    }

    // ---- toMap() tests ----

    @Test
    void toMap_shouldConvertBeanToMap() {
        SourceBean bean = new SourceBean("test", 25);

        Map<String, Object> map = ObjectConverterUtil.toMap(bean);

        assertNotNull(map);
        assertEquals("test", map.get("name"));
        assertEquals(25, map.get("age"));
    }

    @Test
    void toMap_shouldReturnNullForNullInput() {
        Map<String, Object> result = ObjectConverterUtil.toMap(null);

        assertNull(result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void toMap_shouldReturnSameMapForMapInput() {
        Map<String, Object> original = new HashMap<>();
        original.put("key", "value");

        Map<String, Object> result = ObjectConverterUtil.toMap(original);

        assertSame(original, result);
    }

    @Test
    void toMap_shouldExcludeClassProperty() {
        SourceBean bean = new SourceBean("test", 25);

        Map<String, Object> map = ObjectConverterUtil.toMap(bean);

        assertFalse(map.containsKey("class"));
    }

    @Test
    void toMap_shouldHandleBeanWithNullFields() {
        SourceBean bean = new SourceBean(null, 0);

        Map<String, Object> map = ObjectConverterUtil.toMap(bean);

        assertNotNull(map);
        assertNull(map.get("name"));
        assertEquals(0, map.get("age"));
    }

    @Test
    void toMap_shouldIncludeInheritedProperties() {
        ExtendedBean bean = new ExtendedBean("test", 25, "test@example.com");

        Map<String, Object> map = ObjectConverterUtil.toMap(bean);

        assertEquals("test", map.get("name"));
        assertEquals(25, map.get("age"));
        assertEquals("test@example.com", map.get("email"));
    }

    // ---- listDistinct() tests ----

    @Test
    void listDistinct_shouldRemoveDuplicates() {
        List<String> list = Arrays.asList("a", "b", "a", "c", "b");

        List<String> result = ObjectConverterUtil.listDistinct(list);

        assertEquals(3, result.size());
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
        assertTrue(result.contains("c"));
    }

    @Test
    void listDistinct_shouldHandleNoDuplicates() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);

        List<Integer> result = ObjectConverterUtil.listDistinct(list);

        assertEquals(5, result.size());
    }

    @Test
    void listDistinct_shouldHandleEmptyList() {
        List<String> list = new ArrayList<>();

        List<String> result = ObjectConverterUtil.listDistinct(list);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void listDistinct_shouldHandleAllDuplicates() {
        List<String> list = Arrays.asList("x", "x", "x");

        List<String> result = ObjectConverterUtil.listDistinct(list);

        assertEquals(1, result.size());
        assertEquals("x", result.get(0));
    }

    // ---- jsonToList() tests ----

    @Test
    void jsonToList_shouldParseJsonArrayString() {
        String json = "[{\"name\":\"test\",\"age\":25}]";

        List<Object> result = ObjectConverterUtil.jsonToList(json);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0) instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) result.get(0);
        assertEquals("test", map.get("name"));
        assertEquals(25, map.get("age"));
    }

    @Test
    void jsonToList_shouldParseJsonArrayWithMultipleElements() {
        String json = "[{\"name\":\"a\"},{\"name\":\"b\"}]";

        List<Object> result = ObjectConverterUtil.jsonToList(json);

        assertEquals(2, result.size());
    }

    @Test
    void jsonToList_shouldHandleNonArrayInput() {
        SourceBean bean = new SourceBean("test", 25);

        List<Object> result = ObjectConverterUtil.jsonToList(bean);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0) instanceof Map);
    }

    @Test
    void jsonToList_shouldHandleEmptyJsonArray() {
        String json = "[]";

        List<Object> result = ObjectConverterUtil.jsonToList(json);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void jsonToList_shouldHandleArrayWithPrimitiveValues() {
        String json = "[1, 2, 3]";

        List<Object> result = ObjectConverterUtil.jsonToList(json);

        assertEquals(3, result.size());
    }
}
