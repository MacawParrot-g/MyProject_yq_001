//package org.example.mq;
//
//import com.rabbitmq.client.Channel;
//import org.example.config.RabbitMQConfig;
//import org.example.entity.ExportMessage;
//import org.example.entity.TestStatic;
//import org.example.mapper.GeneranMapper;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.amqp.support.AmqpHeaders;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.messaging.handler.annotation.Header;
//import org.springframework.messaging.handler.annotation.Payload;
//import org.springframework.stereotype.Component;
//
//import java.io.BufferedWriter;
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.List;
//
//@Component
//public class ExportMessageConsumer {
//
//    private static final Logger log = LoggerFactory.getLogger(ExportMessageConsumer.class);
//
//    @Autowired
//    private GeneranMapper generanMapper;
//
//    @Value("${export.file-dir}")
//    private String exportDir;
//
//    @RabbitListener(queues = RabbitMQConfig.EXPORT_QUEUE)
//    public void handleExportMessage(
//            @Payload ExportMessage message,
//            Channel channel,
//            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
//        try {
//            String recorder = message.getRecorder();
//            String fileName = message.getFileName();
//
//            List<TestStatic> records = generanMapper.selectUnexportedByRecorder(recorder);
//
//            if (records.isEmpty()) {
//                log.warn("用户 {} 无未导出数据，跳过文件生成", recorder);
//                channel.basicAck(deliveryTag, false);
//                return;
//            }
//
//            Path dirPath = Paths.get(exportDir);
//            if (!Files.exists(dirPath)) {
//                Files.createDirectories(dirPath);
//            }
//
//            Path filePath = dirPath.resolve(fileName);
//            try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
//                writer.write("====================================================");
//                writer.newLine();
//                writer.write("  数据导出报告");
//                writer.newLine();
//                writer.write("  导出用户: " + recorder);
//                writer.newLine();
//                writer.write("  记录总数: " + records.size());
//                writer.newLine();
//                writer.write("====================================================");
//                writer.newLine();
//                writer.newLine();
//                for (int i = 0; i < records.size(); i++) {
//                    TestStatic r = records.get(i);
//
//                    String url = r.getUrl() != null ? r.getUrl() : "";
//                    String bundleId = r.getBundleId() != null ? r.getBundleId() : "";
//                    String ascribe = r.getAscribe() != null ? r.getAscribe() : "";
//                    String eventNumber = r.getEventNumber() != null ? String.valueOf(r.getEventNumber()) : "";
//                    String exceptionType = r.getExceptionType() != null ? r.getExceptionType() : "";
//                    String recordData = r.getRecordData() != null ? r.getRecordData() : "";
//                    String recorderName = r.getRecorder() != null ? r.getRecorder() : "";
//                    String remark = r.getRemark() != null ? r.getRemark() : "";
//
//                    StringBuilder line = new StringBuilder();
//                    line.append(url).append("\t")
//                            .append(bundleId).append("\t")
//                            .append(ascribe).append("\t")
//                            .append(eventNumber).append("\t")
//                            .append(exceptionType).append("\t")
//                            .append(recordData).append("\t")
//                            .append(recorderName).append("\t")
//                            .append(remark);
//
//                    writer.write(line.toString());
//                    writer.newLine();
//                }
//            }
//
//
//            generanMapper.markAsExportedByRecorder(recorder);
//            log.info("✅ 导出文件生成成功: {}, 用户: {}, 共{}条", fileName, recorder, records.size());
//            channel.basicAck(deliveryTag, false);
//        } catch (Exception e) {
//            log.error("❌ 导出文件生成失败, 用户: {}, 原因: {}", message.getRecorder(), e.getMessage());
//            channel.basicNack(deliveryTag, false, false);
//        }
//    }
//}

package org.example.mq;

import com.rabbitmq.client.Channel;
import org.example.config.RabbitMQConfig;
import org.example.entity.ExportMessage;
import org.example.entity.TestStatic;
import org.example.mapper.GeneranMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class ExportMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(ExportMessageConsumer.class);

    @Autowired
    private GeneranMapper generanMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${export.file-dir}")
    private String exportDir;

    @RabbitListener(queues = RabbitMQConfig.EXPORT_QUEUE)
    public void handleExportMessage(
            @Payload ExportMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            String recorder = message.getRecorder();
            String fileName = message.getFileName();

            List<TestStatic> records = generanMapper.selectUnexportedByRecorder(recorder);

            if (records.isEmpty()) {
                log.warn("用户 {} 无未导出数据，跳过文件生成", recorder);
                channel.basicAck(deliveryTag, false);
                return;
            }

            Path dirPath = Paths.get(exportDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            Path filePath = dirPath.resolve(fileName);
            try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
                writer.write("====================================================");
                writer.newLine();
                writer.write("  数据导出报告");
                writer.newLine();
                writer.write("  导出用户: " + recorder);
                writer.newLine();
                writer.write("  记录总数: " + records.size());
                writer.newLine();
                writer.write("====================================================");
                writer.newLine();
                writer.newLine();
                for (int i = 0; i < records.size(); i++) {
                    TestStatic r = records.get(i);

                    String url = r.getUrl() != null ? r.getUrl() : "";
                    String bundleId = r.getBundleId() != null ? r.getBundleId() : "";
                    String ascribe = r.getAscribe() != null ? r.getAscribe() : "";
                    String eventNumber = r.getEventNumber() != null ? String.valueOf(r.getEventNumber()) : "";
                    String exceptionType = r.getExceptionType() != null ? r.getExceptionType() : "";
                    String recordData = r.getRecordData() != null ? r.getRecordData() : "";
                    String recorderName = r.getRecorder() != null ? r.getRecorder() : "";
                    String remark = r.getRemark() != null ? r.getRemark() : "";

                    StringBuilder line = new StringBuilder();
                    line.append(url).append("\t")
                            .append(bundleId).append("\t")
                            .append(ascribe).append("\t")
                            .append(eventNumber).append("\t")
                            .append(exceptionType).append("\t")
                            .append(recordData).append("\t")
                            .append(recorderName).append("\t")
                            .append(remark);

                    writer.write(line.toString());
                    writer.newLine();
                }
            }


            generanMapper.markAsExportedByRecorder(recorder);

            String redisKey = "export:task:" + recorder;
            redisTemplate.opsForValue().set(redisKey, fileName, 2, TimeUnit.HOURS);

            log.info("✅ 导出文件生成成功: {}, 用户: {}, 共{}条", fileName, recorder, records.size());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("❌ 导出文件生成失败, 用户: {}, 原因: {}", message.getRecorder(), e.getMessage());
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
