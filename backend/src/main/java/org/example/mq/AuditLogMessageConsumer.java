// 文件路径: src/main/java/org/example/mq/AuditLogMessageConsumer.java
package org.example.mq;

import com.rabbitmq.client.Channel;
import org.example.entity.AuditLog;
import org.example.mapper.AuditLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AuditLogMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditLogMessageConsumer.class);

    @Autowired
    private AuditLogMapper auditLogMapper;

    @RabbitListener(queues = "audit.log.queue", ackMode = "MANUAL")
    public void onMessage(AuditLog auditLog, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            auditLogMapper.insertAuditLog(auditLog);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("❌ 审计日志入库失败: {}", e.getMessage());
            channel.basicNack(deliveryTag, false, true);
        }
    }
}