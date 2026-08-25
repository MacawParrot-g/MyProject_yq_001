package org.example.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String RECORD_EXCHANGE = "record.exchange";
    public static final String RECORD_QUEUE = "record.insert.queue";
    public static final String RECORD_ROUTING_KEY = "record.insert";
    public static final String DLX_EXCHANGE = "record.dlx.exchange";
    public static final String DLQ_QUEUE = "record.insert.dlq";
    public static final String DLQ_ROUTING_KEY = "record.insert.dlq";

    public static final String EXPORT_EXCHANGE = "export.exchange";
    public static final String EXPORT_QUEUE = "export.file.queue";
    public static final String EXPORT_ROUTING_KEY = "export.file";

    public static final String UPDATE_EXCHANGE = "record.update.exchange";
    public static final String UPDATE_QUEUE = "record.update.queue";
    public static final String UPDATE_ROUTING_KEY = "record.update";
    public static final String UPDATE_DLX_EXCHANGE = "record.update.dlx.exchange";
    public static final String UPDATE_DLQ_QUEUE = "record.update.dlq";
    public static final String UPDATE_DLQ_ROUTING_KEY = "record.update.dlq";

    public static final String DELETE_EXCHANGE = "record.delete.exchange";
    public static final String DELETE_QUEUE = "record.delete.queue";
    public static final String DELETE_ROUTING_KEY = "record.delete";
    public static final String DELETE_DLX_EXCHANGE = "record.delete.dlx.exchange";
    public static final String DELETE_DLQ_QUEUE = "record.delete.dlq";
    public static final String DELETE_DLQ_ROUTING_KEY = "record.delete.dlq";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setMandatory(true);
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                org.slf4j.LoggerFactory.getLogger(RabbitMQConfig.class)
                        .error("❌ 消息投递到Exchange失败, cause: {}", cause);
            }
        });
        template.setReturnsCallback(returned -> {
            org.slf4j.LoggerFactory.getLogger(RabbitMQConfig.class)
                    .error("❌ 消息从Exchange路由到Queue失败, exchange: {}, routingKey: {}, replyCode: {}, replyText: {}",
                            returned.getExchange(), returned.getRoutingKey(),
                            returned.getReplyCode(), returned.getReplyText());
        });
        return template;
    }

    // ==================== Insert 队列 ====================

    @Bean
    public DirectExchange recordExchange() {
        return ExchangeBuilder.directExchange(RECORD_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue recordInsertQueue() {
        return QueueBuilder.durable(RECORD_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding recordBinding() {
        return BindingBuilder.bind(recordInsertQueue()).to(recordExchange()).with(RECORD_ROUTING_KEY);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return ExchangeBuilder.directExchange(DLX_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue dlqQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(dlqQueue()).to(dlxExchange()).with(DLQ_ROUTING_KEY);
    }

    // ==================== Export 队列 ====================

    @Bean
    public DirectExchange exportExchange() {
        return ExchangeBuilder.directExchange(EXPORT_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue exportFileQueue() {
        return QueueBuilder.durable(EXPORT_QUEUE).build();
    }

    @Bean
    public Binding exportBinding() {
        return BindingBuilder.bind(exportFileQueue()).to(exportExchange()).with(EXPORT_ROUTING_KEY);
    }

    // ==================== Update 队列 ====================

    @Bean
    public DirectExchange updateExchange() {
        return ExchangeBuilder.directExchange(UPDATE_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue updateQueue() {
        return QueueBuilder.durable(UPDATE_QUEUE)
                .withArgument("x-dead-letter-exchange", UPDATE_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", UPDATE_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding updateBinding() {
        return BindingBuilder.bind(updateQueue()).to(updateExchange()).with(UPDATE_ROUTING_KEY);
    }

    @Bean
    public DirectExchange updateDlxExchange() {
        return ExchangeBuilder.directExchange(UPDATE_DLX_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue updateDlqQueue() {
        return QueueBuilder.durable(UPDATE_DLQ_QUEUE).build();
    }

    @Bean
    public Binding updateDlqBinding() {
        return BindingBuilder.bind(updateDlqQueue()).to(updateDlxExchange()).with(UPDATE_DLQ_ROUTING_KEY);
    }

    // ==================== Delete 队列 ====================

    @Bean
    public DirectExchange deleteExchange() {
        return ExchangeBuilder.directExchange(DELETE_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue deleteQueue() {
        return QueueBuilder.durable(DELETE_QUEUE)
                .withArgument("x-dead-letter-exchange", DELETE_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DELETE_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding deleteBinding() {
        return BindingBuilder.bind(deleteQueue()).to(deleteExchange()).with(DELETE_ROUTING_KEY);
    }

    @Bean
    public DirectExchange deleteDlxExchange() {
        return ExchangeBuilder.directExchange(DELETE_DLX_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue deleteDlqQueue() {
        return QueueBuilder.durable(DELETE_DLQ_QUEUE).build();
    }

    @Bean
    public Binding deleteDlqBinding() {
        return BindingBuilder.bind(deleteDlqQueue()).to(deleteDlxExchange()).with(DELETE_DLQ_ROUTING_KEY);
    }
}
