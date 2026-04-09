package io.deepcover.brain.service.mq;

import io.deepcover.brain.service.service.CallbackRecordService;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.concurrent.Executor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MQConsumerTest {

    @Mock
    private CallbackRecordService callbackRecordService;

    @Mock
    private Executor taskExecutor;

    private MQConsumer mqConsumer;

    @BeforeEach
    void setUp() throws Exception {
        mqConsumer = new MQConsumer();
        injectField(mqConsumer, "callbackRecordService", callbackRecordService);
        injectField(mqConsumer, "taskExecutor", taskExecutor);
    }

    private static void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void onMessage_withNullBody_shouldNotProcess() {
        MessageExt msg = mock(MessageExt.class);
        when(msg.getBody()).thenReturn(null);
        mqConsumer.onMessage(msg);
        verifyNoInteractions(callbackRecordService);
    }

    @Test
    void onMessage_withEmptyBody_shouldNotProcess() {
        MessageExt msg = mock(MessageExt.class);
        when(msg.getBody()).thenReturn(new byte[0]);
        mqConsumer.onMessage(msg);
        verifyNoInteractions(callbackRecordService);
    }

    @Test
    void onMessage_withValidBody_shouldExecuteAsync() {
        MessageExt msg = mock(MessageExt.class);
        String json = "{\"traceId\":\"test-trace-123\"}";
        when(msg.getBody()).thenReturn(json.getBytes());

        // Make the executor run synchronously for testing
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(taskExecutor).execute(any(Runnable.class));

        when(callbackRecordService.analysisHbaseData("test-trace-123")).thenReturn(0);

        mqConsumer.onMessage(msg);
        verify(callbackRecordService).analysisHbaseData("test-trace-123");
    }

    @Test
    void onMessage_withInvalidJson_shouldNotThrow() {
        MessageExt msg = mock(MessageExt.class);
        when(msg.getBody()).thenReturn("not-json".getBytes());

        // Should not throw exception
        assertDoesNotThrow(() -> mqConsumer.onMessage(msg));
    }

    private void assertDoesNotThrow(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            throw new AssertionError("Expected no exception but got: " + e.getMessage(), e);
        }
    }
}
