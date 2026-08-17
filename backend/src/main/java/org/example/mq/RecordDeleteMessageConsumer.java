package org.example.mq;
import com.rabbitmq.client.Channel;
import org.example.config.RabbitMQConfig;
import org.example.entity.RecordDeleteMessage;
import org.example.mapper.GeneranMapper;
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

    @RabbitListener(queues = RabbitMQConfig.DELETE_QUEUE, concurrency = "2-4")
    public void handleDeleteMessage(
            @Payload RecordDeleteMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            generanMapper.deleteByURL(message.getUrl());
            channel.basicAck(deliveryTag, false);
            log.info("异步删除成功, URL: {}", message.getUrl());
        } catch (Exception e) {
            log.error("异步删除失败, URL: {}, 原因: {}", message.getUrl(), e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.DELETE_DLQ_QUEUE)
    public void handleDeleteDlqMessage(
            @Payload RecordDeleteMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.error("🚨 删除消息进入死信队列, URL: {}, 请人工排查", message.getUrl());
        channel.basicAck(deliveryTag, false);
    }
}
