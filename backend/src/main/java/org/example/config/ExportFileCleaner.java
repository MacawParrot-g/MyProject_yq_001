package org.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Component
public class ExportFileCleaner {

    private static final Logger log = LoggerFactory.getLogger(ExportFileCleaner.class);

    @Value("${export.file-dir}")
    private String exportDir;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanExpiredExportFiles() {
        try {
            Path dirPath = Paths.get(exportDir);
            if (!Files.exists(dirPath)) return;

            int deleted = 0;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, "export_*.*")) {
                for (Path file : stream) {
                    long lastModified = Files.getLastModifiedTime(file).toMillis();
                    long ageHours = (System.currentTimeMillis() - lastModified) / (1000 * 60 * 60);
                    if (ageHours > 24) {
                        Files.deleteIfExists(file);
                        deleted++;
                        log.info("清理过期导出文件: {}, 已存在{}小时", file.getFileName(), ageHours);
                    }
                }
            }
            if (deleted > 0) {
                log.info("导出文件清理完成，共清理{}个文件", deleted);
            }
        } catch (Exception e) {
            log.error("清理导出文件失败: {}", e.getMessage());
        }
    }
}
