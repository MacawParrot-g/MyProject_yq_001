package org.example.mq;
import com.rabbitmq.client.Channel;
import org.example.config.RabbitMQConfig;
import org.example.entity.RecordDeleteMessage;
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
public class RecordDeleteMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(RecordDeleteMessageConsumer.class);

    @Autowired
    private GeneranMapper generanMapper;

    @Autowired
    private HashCacheService hashCacheService;

    @RabbitListener(queues = RabbitMQConfig.DELETE_QUEUE, concurrency = "2-4")
    public void handleDeleteMessage(
            @Payload RecordDeleteMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            generanMapper.deleteByHash(message.getHash());
            hashCacheService.remove(message.getHash());
            channel.basicAck(deliveryTag, false);
            log.info("异步删除成功, hash: {}", message.getHash());
        } catch (Exception e) {
            log.error("异步删除失败, hash: {}, 原因: {}", message.getHash(), e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.DELETE_DLQ_QUEUE)
    public void handleDeleteDlqMessage(
            @Payload RecordDeleteMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.error("🚨 删除消息进入死信队列, hash: {}, 请人工排查", message.getHash());
        channel.basicAck(deliveryTag, false);
    }
}
