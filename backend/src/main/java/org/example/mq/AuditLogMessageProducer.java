// 文件路径: src/main/java/org/example/mq/AuditLogMessageProducer.java
package org.example.mq;

import org.example.config.RabbitMQConfig;
import org.example.entity.AuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuditLogMessageProducer {

    private static final Logger log = LoggerFactory.getLogger(AuditLogMessageProducer.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public boolean sendAuditLog(AuditLog auditLog) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.AUDIT_EXCHANGE,
                    RabbitMQConfig.AUDIT_ROUTING_KEY,
                    auditLog
            );
            return true;
        } catch (Exception e) {
            log.warn("⚠️ 审计日志MQ投递失败: {}", e.getMessage());
            return false;
        }
    }
}
