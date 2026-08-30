package org.example.service.impl;

import org.example.common.Result;
import org.example.entity.ExportMessage;
import org.example.entity.TestStatic;
import org.example.mapper.GeneranMapper;
import org.example.mq.ExportMessageProducer;
import org.example.service.ExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class ExportServiceImpl implements ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportServiceImpl.class);
    private static final String EXPORT_TASK_KEY_PREFIX = "export:task:";
    private static final long EXPORT_TASK_TTL_HOURS = 2;

    @Autowired
    private GeneranMapper generanMapper;

    @Autowired
    private ExportMessageProducer exportMessageProducer;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${export.file-dir}")
    private String exportDir;

    @Override
    public Result getUnexportedByUser(String recorder) {
        if (recorder == null || recorder.isBlank()) {
            return Result.fail("用户名不能为空");
        }
        List<TestStatic> list = generanMapper.selectUnexportedByRecorder(recorder);
        return Result.success("查询成功，共 " + list.size() + " 条未导出数据", Map.of(
                "list", list,
                "total", list.size()
        ));
    }

    @Override
    public Result getExportStatus(String recorder) {
        if (recorder == null || recorder.isBlank()) {
            return Result.fail("用户名不能为空");
        }
        String redisKey = EXPORT_TASK_KEY_PREFIX + recorder;
        Object fileName = redisTemplate.opsForValue().get(redisKey);
        if (fileName == null) {
            return Result.success("暂无导出任务", Map.of("ready", false));
        }
        Path filePath = Paths.get(exportDir, String.valueOf(fileName));
        if (Files.exists(filePath)) {
            return Result.success("文件已就绪", Map.of("ready", true, "fileName", String.valueOf(fileName)));
        }
        return Result.success("文件生成中，请稍候", Map.of("ready", false));
    }

    @Override
    public void downloadAndDelete(String recorder, HttpServletResponse response) {
        String redisKey = EXPORT_TASK_KEY_PREFIX + recorder;
        Object value = redisTemplate.opsForValue().get(redisKey);
        if (value == null) {
            throw new RuntimeException("无可下载的文件，请先执行导出");
        }
        String fileName = String.valueOf(value);
        Path filePath = Paths.get(exportDir, fileName);
        if (!Files.exists(filePath)) {
            throw new RuntimeException("文件尚未生成完毕，请稍后重试");
        }

        try (InputStream in = Files.newInputStream(filePath);
             OutputStream out = response.getOutputStream()) {
            String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);
            response.setContentLengthLong(Files.size(filePath));

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
            out.close();

            Files.deleteIfExists(filePath);
            redisTemplate.delete(redisKey);
            log.info("📤 文件已下载并删除: {}, 用户: {}", fileName, recorder);
        } catch (Exception e) {
            log.error("❌ 文件下载失败: {}, 原因: {}", fileName, e.getMessage());
            throw new RuntimeException("文件下载失败：" + e.getMessage());
        }
    }

    @Override
    public Result executeExportByDate(String recorder, String date) {
        if (recorder == null || recorder.isBlank()) {
            return Result.fail("用户名不能为空");
        }
        if (date == null || date.isBlank()) {
            return Result.fail("日期不能为空");
        }
        int count = generanMapper.countUnexportedByDateAndRecorder(recorder, date);
        if (count == 0) {
            return Result.fail("该日期下无未导出数据");
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "export_" + recorder + "_" + timestamp + ".txt";

        ExportMessage message = new ExportMessage(recorder, fileName, System.currentTimeMillis(), "date", date, null);
        boolean sent = exportMessageProducer.sendExportMessage(message);
        if (!sent) {
            return Result.fail("导出消息投递失败，请稍后重试");
        }

        String redisKey = EXPORT_TASK_KEY_PREFIX + recorder;
        redisTemplate.opsForValue().set(redisKey, fileName, EXPORT_TASK_TTL_HOURS, TimeUnit.HOURS);

        return Result.success("导出任务已提交，共 " + count + " 条数据正在处理", Map.of(
                "fileName", fileName,
                "count", count
        ));
    }

    @Override
    public Result executeExportByHashes(String recorder, List<String> hashes) {
        if (recorder == null || recorder.isBlank()) {
            return Result.fail("用户名不能为空");
        }
        if (hashes == null || hashes.isEmpty()) {
            return Result.fail("请选择要导出的数据");
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "export_" + recorder + "_" + timestamp + ".txt";

        ExportMessage message = new ExportMessage(recorder, fileName, System.currentTimeMillis(), "hash", null, hashes);
        boolean sent = exportMessageProducer.sendExportMessage(message);
        if (!sent) {
            return Result.fail("导出消息投递失败，请稍后重试");
        }

        String redisKey = EXPORT_TASK_KEY_PREFIX + recorder;
        redisTemplate.opsForValue().set(redisKey, fileName, EXPORT_TASK_TTL_HOURS, TimeUnit.HOURS);

        return Result.success("导出任务已提交，共 " + hashes.size() + " 条数据正在处理", Map.of(
                "fileName", fileName,
                "count", hashes.size()
        ));
    }

    @Override
    public Result executeExportAll(String recorder) {
        if (recorder == null || recorder.isBlank()) {
            return Result.fail("用户名不能为空");
        }
        int count = generanMapper.countUnexportedByRecorder(recorder);
        if (count == 0) {
            return Result.fail("您当前无未导出数据");
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "export_all_" + recorder + "_" + timestamp + ".txt";

        ExportMessage message = new ExportMessage(recorder, fileName, System.currentTimeMillis(), "all", null, null);
        boolean sent = exportMessageProducer.sendExportMessage(message);
        if (!sent) {
            return Result.fail("导出消息投递失败，请稍后重试");
        }

        String redisKey = EXPORT_TASK_KEY_PREFIX + recorder;
        redisTemplate.opsForValue().set(redisKey, fileName, EXPORT_TASK_TTL_HOURS, TimeUnit.HOURS);

        return Result.success("导出任务已提交，共 " + count + " 条未导出数据正在处理", Map.of(
                "fileName", fileName,
                "count", count
        ));
    }
}


