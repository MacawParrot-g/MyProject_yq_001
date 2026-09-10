package org.example.mq;
import com.rabbitmq.client.Channel;
import org.example.config.RabbitMQConfig;
import org.example.entity.RecordUpdateMessage;
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
public class RecordUpdateMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(RecordUpdateMessageConsumer.class);

    @Autowired
    private GeneranMapper generanMapper;

    @RabbitListener(queues = RabbitMQConfig.UPDATE_QUEUE, concurrency = "2-4")
    public void handleUpdateMessage(
            @Payload RecordUpdateMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            generanMapper.updateRecord(message.getRecord());
            channel.basicAck(deliveryTag, false);
            log.info("异步更新成功, URL: {}", message.getRecord().getUrl());
        } catch (Exception e) {
            log.error("异步更新失败, URL: {}, 原因: {}", message.getRecord().getUrl(), e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.UPDATE_DLQ_QUEUE)
    public void handleUpdateDlqMessage(
            @Payload RecordUpdateMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.error("🚨 更新消息进入死信队列, URL: {}, 请人工排查", message.getRecord().getUrl());
        channel.basicAck(deliveryTag, false);
    }
}
