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

package io.deepcover.brain.service.mq;

import com.alibaba.fastjson2.JSON;
import io.deepcover.brain.service.service.CallbackRecordService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Slf4j
@Component
@RocketMQMessageListener(
        topic = "${datacenter.callback.brain.topic:deepcover_hbase_code_info_topic}",
        consumerGroup = "${datacenter.callback.brain.consumer:deepcover_hbase_code_info_consumer}"
)
public class MQConsumer implements RocketMQListener<MessageExt> {

    @Autowired
    private CallbackRecordService callbackRecordService;

    @Autowired
    @Qualifier("newAsyncServiceExecutor")
    private Executor taskExecutor;

    // 用于在内存中跟踪正在处理的traceId，防止并发处理相同的traceId
    private static final Set<String> processingTraceIds = ConcurrentHashMap.newKeySet();

    @Override
    public void onMessage(MessageExt messageExt) {
        byte[] body = messageExt.getBody();
        if (body == null || body.length == 0) {
            log.error("MQ消息字节数组为空");
            return;
        }

        try {
            MQRecord record = JSON.parseObject(new String(body), MQRecord.class);
            if (record == null) {
                log.error("MQ消息体为空");
                return;
            }

            String traceId = record.getTraceId();

            // 检查是否已经在处理相同的traceId
            if (!processingTraceIds.add(traceId)) {
                log.info("traceId {} 正在被处理中，跳过重复消息", traceId);
                return;
            }

            // 异步处理耗时任务
            CompletableFuture.runAsync(() -> {
                try {
                    int result = callbackRecordService.analysisHbaseData(traceId);
                    if (-1 == result) {
                        log.info("删除已有数据失败导致消费失败,traceId=" + traceId + "，标记为已处理，无需重试");
                    } else {
                        log.info("traceId消费成功:" + traceId);
                    }
                } catch (Exception e) {
                    log.error("异步处理traceId {} 时发生异常", traceId, e);
                } finally {
                    processingTraceIds.remove(traceId);
                }
            }, taskExecutor);

        } catch (Exception e) {
            log.error("consume fail.", e);
        }
    }
}
