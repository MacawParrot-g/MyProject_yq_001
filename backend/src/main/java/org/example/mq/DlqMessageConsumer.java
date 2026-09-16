// 文件路径: src/main/java/org/example/mq/DlqMessageConsumer.java
package org.example.mq;

import com.rabbitmq.client.Channel;
import org.example.config.RabbitMQConfig;
import org.example.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class DlqMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(DlqMessageConsumer.class);
    private static final String DLQ_RECORDS_KEY_PREFIX = "mq:dlq:records:";
    private static final int DLQ_RECORDS_MAX_SIZE = 200;
    private static final long DLQ_RECORD_TTL_HOURS = 72;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.DLQ_QUEUE)
    public void handleInsertDlq(Message message, Channel channel,
                                @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        processDlqMessage("insert", RabbitMQConfig.DLQ_QUEUE, message, channel, deliveryTag);
    }

    @RabbitListener(queues = RabbitMQConfig.UPDATE_DLQ_QUEUE)
    public void handleUpdateDlq(Message message, Channel channel,
                                @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        processDlqMessage("update", RabbitMQConfig.UPDATE_DLQ_QUEUE, message, channel, deliveryTag);
    }

    @RabbitListener(queues = RabbitMQConfig.DELETE_DLQ_QUEUE)
    public void handleDeleteDlq(Message message, Channel channel,
                                @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        processDlqMessage("delete", RabbitMQConfig.DELETE_DLQ_QUEUE, message, channel, deliveryTag);
    }

    private void processDlqMessage(String type, String queueName, Message message,
                                   Channel channel, long deliveryTag) throws IOException {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        log.error("🚨 死信消息 [{}] 队列: {}, 时间: {}, 内容: {}", type, queueName, now, body);

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("type", type);
        record.put("queueName", queueName);
        record.put("body", body);
        record.put("timestamp", now);
        record.put("exchange", message.getMessageProperties().getReceivedExchange());
        record.put("routingKey", message.getMessageProperties().getReceivedRoutingKey());
        record.put("originalQueue", message.getMessageProperties().getReceivedRoutingKey());
        record.put("failureCount", getMessageHeader(message, "x-death"));

        String redisKey = DLQ_RECORDS_KEY_PREFIX + type;
        redisTemplate.opsForList().leftPush(redisKey, record);
        redisTemplate.opsForList().trim(redisKey, 0, DLQ_RECORDS_MAX_SIZE - 1);
        redisTemplate.expire(redisKey, DLQ_RECORD_TTL_HOURS, TimeUnit.HOURS);

        try {
            notificationService.sendNotification(
                    "admin",
                    "MQ_ALERT",
                    "🚨 消息队列死信告警 [" + type.toUpperCase() + "]",
                    "队列: " + queueName + "\n时间: " + now + "\n内容: " + body.substring(0, Math.min(body.length(), 200))
            );
        } catch (Exception e) {
            log.warn("死信告警通知发送失败: {}", e.getMessage());
        }

        channel.basicAck(deliveryTag, false);
    }

    private Object getMessageHeader(Message message, String headerName) {
        Object header = message.getMessageProperties().getHeader(headerName);
        return header != null ? header.toString() : "N/A";
    }
}
