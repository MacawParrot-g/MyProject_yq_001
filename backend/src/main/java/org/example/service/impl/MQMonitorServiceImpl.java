// 文件路径: src/main/java/org/example/service/impl/MQMonitorServiceImpl.java
package org.example.service.impl;

import org.example.common.Result;
import org.example.config.RabbitMQConfig;
import org.example.service.MQMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class MQMonitorServiceImpl implements MQMonitorService {

    private static final Logger log = LoggerFactory.getLogger(MQMonitorServiceImpl.class);
    private static final String DLQ_RECORDS_KEY_PREFIX = "mq:dlq:records:";

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Result getQueueOverview() {
        try {
            List<Map<String, Object>> queues = new ArrayList<>();
            queues.add(buildQueueInfo("Insert 队列", RabbitMQConfig.RECORD_QUEUE, RabbitMQConfig.RECORD_EXCHANGE));
            queues.add(buildQueueInfo("Update 队列", RabbitMQConfig.UPDATE_QUEUE, RabbitMQConfig.UPDATE_EXCHANGE));
            queues.add(buildQueueInfo("Delete 队列", RabbitMQConfig.DELETE_QUEUE, RabbitMQConfig.DELETE_EXCHANGE));
            queues.add(buildQueueInfo("Export 队列", RabbitMQConfig.EXPORT_QUEUE, RabbitMQConfig.EXPORT_EXCHANGE));
            queues.add(buildQueueInfo("Audit 队列", RabbitMQConfig.AUDIT_QUEUE, RabbitMQConfig.AUDIT_EXCHANGE));
            queues.add(buildQueueInfo("Insert DLQ", RabbitMQConfig.DLQ_QUEUE, RabbitMQConfig.DLX_EXCHANGE));
            queues.add(buildQueueInfo("Update DLQ", RabbitMQConfig.UPDATE_DLQ_QUEUE, RabbitMQConfig.UPDATE_DLX_EXCHANGE));
            queues.add(buildQueueInfo("Delete DLQ", RabbitMQConfig.DELETE_DLQ_QUEUE, RabbitMQConfig.DELETE_DLX_EXCHANGE));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("queues", queues);
            result.put("timestamp", System.currentTimeMillis());
            return Result.success("队列信息获取成功", result);
        } catch (Exception e) {
            log.error("获取队列信息失败", e);
            return Result.fail("获取队列信息失败: " + e.getMessage());
        }
    }

    private Map<String, Object> buildQueueInfo(String label, String queueName, String exchange) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("label", label);
        info.put("queueName", queueName);
        info.put("exchange", exchange);
        try {
            Integer count = rabbitTemplate.execute(channel -> {
                return channel.queueDeclarePassive(queueName).getMessageCount();
            });
            info.put("messageCount", count != null ? count : 0);
            info.put("status", "healthy");
        } catch (Exception e) {
            info.put("messageCount", -1);
            info.put("status", "error");
            info.put("error", e.getMessage());
        }

        String type = resolveDlqType(queueName);
        if (type != null) {
            Long dlqSize = redisTemplate.opsForList().size(DLQ_RECORDS_KEY_PREFIX + type);
            info.put("dlqRecordCount", dlqSize != null ? dlqSize : 0);
        }
        return info;
    }

    private String resolveDlqType(String queueName) {
        if (RabbitMQConfig.DLQ_QUEUE.equals(queueName)) return "insert";
        if (RabbitMQConfig.UPDATE_DLQ_QUEUE.equals(queueName)) return "update";
        if (RabbitMQConfig.DELETE_DLQ_QUEUE.equals(queueName)) return "delete";
        return null;
    }

    @Override
    public Result getDlqRecords(String type) {
        try {
            String redisKey = DLQ_RECORDS_KEY_PREFIX + type;
            List<Object> records = redisTemplate.opsForList().range(redisKey, 0, -1);
            return Result.success("DLQ记录获取成功", records != null ? records : List.of());
        } catch (Exception e) {
            return Result.fail("DLQ记录获取失败: " + e.getMessage());
        }
    }

    @Override
    public Result retryDlqMessage(String type, int index) {
        try {
            String redisKey = DLQ_RECORDS_KEY_PREFIX + type;
            Object record = redisTemplate.opsForList().index(redisKey, index);
            if (record == null) {
                return Result.fail("未找到对应的死信记录");
            }

            Map<String, Object> recordMap = (Map<String, Object>) record;
            String body = (String) recordMap.get("body");
            String exchange = (String) recordMap.get("exchange");
            String routingKey = (String) recordMap.get("routingKey");

            if (exchange == null || routingKey == null) {
                return Result.fail("死信记录缺少exchange或routingKey信息，无法重试");
            }

            rabbitTemplate.convertAndSend(exchange, routingKey, body);

            redisTemplate.opsForList().set(redisKey, index, null);
            List<Object> current = redisTemplate.opsForList().range(redisKey, 0, -1);
            List<Object> filtered = new ArrayList<>();
            for (Object obj : current) {
                if (obj != null) filtered.add(obj);
            }
            redisTemplate.delete(redisKey);
            if (!filtered.isEmpty()) {
                redisTemplate.opsForList().rightPushAll(redisKey, filtered);
            }

            return Result.success("死信消息重试成功");
        } catch (Exception e) {
            return Result.fail("死信消息重试失败: " + e.getMessage());
        }
    }

    @Override
    public Result clearDlqRecords(String type) {
        try {
            String redisKey = DLQ_RECORDS_KEY_PREFIX + type;
            redisTemplate.delete(redisKey);
            return Result.success("DLQ记录已清除");
        } catch (Exception e) {
            return Result.fail("DLQ记录清除失败: " + e.getMessage());
        }
    }

    @Override
    public Result getDlqSummary() {
        try {
            Map<String, Object> summary = new LinkedHashMap<>();
            long insertCount = countDlqRecords("insert");
            long updateCount = countDlqRecords("update");
            long deleteCount = countDlqRecords("delete");
            summary.put("insertDlqCount", insertCount);
            summary.put("updateDlqCount", updateCount);
            summary.put("deleteDlqCount", deleteCount);
            summary.put("totalDlqCount", insertCount + updateCount + deleteCount);
            return Result.success("DLQ摘要获取成功", summary);
        } catch (Exception e) {
            return Result.fail("DLQ摘要获取失败: " + e.getMessage());
        }
    }

    private long countDlqRecords(String type) {
        Long size = redisTemplate.opsForList().size(DLQ_RECORDS_KEY_PREFIX + type);
        return size != null ? size : 0;
    }
}
