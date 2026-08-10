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
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExportServiceImpl implements ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportServiceImpl.class);

    @Autowired
    private GeneranMapper generanMapper;

    @Autowired
    private ExportMessageProducer exportMessageProducer;

    @Value("${export.file-dir}")
    private String exportDir;

    private final ConcurrentHashMap<String, String> exportFileMap = new ConcurrentHashMap<>();

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
    public Result executeExport(String recorder) {
        if (recorder == null || recorder.isBlank()) {
            return Result.fail("用户名不能为空");
        }
        int count = generanMapper.countUnexportedByRecorder(recorder);
        if (count == 0) {
            return Result.fail("当前用户所有数据已被导出，无需重复执行");
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "export_" + recorder + "_" + timestamp + ".txt";

        ExportMessage message = new ExportMessage(recorder, fileName, System.currentTimeMillis());
        boolean sent = exportMessageProducer.sendExportMessage(message);
        if (!sent) {
            return Result.fail("导出消息投递失败，请稍后重试");
        }

        exportFileMap.put(recorder, fileName);
        return Result.success("导出任务已提交，共 " + count + " 条数据正在处理", Map.of(
                "fileName", fileName,
                "count", count
        ));
    }

    @Override
    public Result getExportStatus(String recorder) {
        if (recorder == null || recorder.isBlank()) {
            return Result.fail("用户名不能为空");
        }
        String fileName = exportFileMap.get(recorder);
        if (fileName == null) {
            return Result.success("暂无导出任务", Map.of("ready", false));
        }
        Path filePath = Paths.get(exportDir, fileName);
        if (Files.exists(filePath)) {
            return Result.success("文件已就绪", Map.of("ready", true, "fileName", fileName));
        }
        return Result.success("文件生成中，请稍候", Map.of("ready", false));
    }

    @Override
    public void downloadAndDelete(String recorder, HttpServletResponse response) {
        String fileName = exportFileMap.get(recorder);
        if (fileName == null) {
            throw new RuntimeException("无可下载的文件，请先执行导出");
        }
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
            exportFileMap.remove(recorder);
            log.info("📤 文件已下载并删除: {}, 用户: {}", fileName, recorder);
        } catch (Exception e) {
            log.error("❌ 文件下载失败: {}, 原因: {}", fileName, e.getMessage());
            throw new RuntimeException("文件下载失败：" + e.getMessage());
        }
    }
}
