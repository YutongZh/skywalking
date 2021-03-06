/*
 * Copyright 2017, OpenSkywalking Organization All rights reserved.
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
 *
 * Project repository: https://github.com/OpenSkywalking/skywalking
 */

package org.skywalking.apm.collector.queue.disruptor.base;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import org.skywalking.apm.collector.core.CollectorException;
import org.skywalking.apm.collector.core.data.EndOfBatchQueueMessage;
import org.skywalking.apm.collector.queue.base.MessageHolder;
import org.skywalking.apm.collector.queue.base.QueueEventHandler;
import org.skywalking.apm.collector.queue.base.QueueExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于 Disruptor 的队列处理器实现类
 *
 * @author peng-yongsheng
 */
public class DisruptorEventHandler<MESSAGE extends EndOfBatchQueueMessage> implements EventHandler<MessageHolder<MESSAGE>>, QueueEventHandler<MESSAGE> {

    private final Logger logger = LoggerFactory.getLogger(DisruptorEventHandler.class);

    /**
     * RingBuffer 对象
     */
    private RingBuffer<MessageHolder<MESSAGE>> ringBuffer;
    /**
     * 执行器
     */
    private QueueExecutor<MESSAGE> executor;

    DisruptorEventHandler(RingBuffer<MessageHolder<MESSAGE>> ringBuffer, QueueExecutor<MESSAGE> executor) {
        this.ringBuffer = ringBuffer;
        this.executor = executor;
    }

    /**
     * Receive the message from disruptor, when message in disruptor is empty, then send the cached data
     * to the next workers.
     *
     * @param event published to the {@link RingBuffer}
     * @param sequence of the event being processed
     * @param endOfBatch flag to indicate if this is the last event in a batch from the {@link RingBuffer}
     */
    public void onEvent(MessageHolder<MESSAGE> event, long sequence, boolean endOfBatch) throws CollectorException {
        MESSAGE message = event.getMessage();

        // 清空消息
        event.reset();

        // 设置消息为该批量的结尾（最后一条）
        message.setEndOfBatch(endOfBatch);

        // 执行处理消息
        executor.execute(message);
    }

    /**
     * Push the message into disruptor ring buffer.
     *
     * @param message of the data to process.
     */
    public void tell(MESSAGE message) {
        long sequence = ringBuffer.next();
        try {
            ringBuffer.get(sequence).setMessage(message);
        } finally {
            ringBuffer.publish(sequence);
        }
    }
}
