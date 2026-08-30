package org.example.mq;

import com.rabbitmq.client.Channel;
import org.example.config.RabbitMQConfig;
import org.example.entity.RecordInsertMessage;
import org.example.mapper.GeneranMapper;
import org.example.service.HashCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RecordMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(RecordMessageConsumer.class);

    @Autowired
    private GeneranMapper generanMapper;

    @Autowired
    private HashCacheService hashCacheService;

    @RabbitListener(queues = RabbitMQConfig.RECORD_QUEUE, concurrency = "3-8")
    public void handleInsertMessage(
            @Payload RecordInsertMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            generanMapper.insertRecord(message.getRecord());
            if (message.getRecord().getHash() != null) {
                hashCacheService.save(message.getRecord().getHash());
            }
            channel.basicAck(deliveryTag, false);
            log.info("异步入库成功, hash: {}", message.getRecord().getHash());
        } catch (Exception e) {
            log.error("异步入库失败, hash: {}, 原因: {}", message.getRecord().getHash(), e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }
}