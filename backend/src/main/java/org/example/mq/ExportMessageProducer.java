package org.example.mq;

import org.example.config.RabbitMQConfig;
import org.example.entity.ExportMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ExportMessageProducer {

    private static final Logger log = LoggerFactory.getLogger(ExportMessageProducer.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public boolean sendExportMessage(ExportMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXPORT_EXCHANGE,
                    RabbitMQConfig.EXPORT_ROUTING_KEY,
                    message
            );
            log.info("导出消息已投递至RabbitMQ, 用户: {}, 文件: {}", message.getRecorder(), message.getFileName());
            return true;
        } catch (Exception e) {
            log.error("❌ 导出消息投递失败, 用户: {}, 原因: {}", message.getRecorder(), e.getMessage());
            return false;
        }
    }
}