package io.deepcover.brain.service.mq;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MQRecordTest {

    @Test
    void testGetterSetter() {
        MQRecord record = new MQRecord();
        record.setTraceId("trace-123");
        assertEquals("trace-123", record.getTraceId());
    }

    @Test
    void testToString() {
        MQRecord record = new MQRecord();
        record.setTraceId("trace-456");
        String str = record.toString();
        assertTrue(str.contains("trace-456"));
    }

    @Test
    void testDefaultValues() {
        MQRecord record = new MQRecord();
        assertNull(record.getTraceId());
    }
}
