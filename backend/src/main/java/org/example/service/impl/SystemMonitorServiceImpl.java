package org.example.service.impl;

import org.example.common.Result;
import org.example.service.SystemMonitorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class SystemMonitorServiceImpl implements SystemMonitorService {

    @Value("${logging.file.name:/data/logs/app.log}")
    private String logFilePath;

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    private final RedisTemplate<String, Object> redisTemplate;

    private volatile long lastCpuCheckTime = 0;
    private volatile double cachedCpuUsage = 0;
    private static final long CPU_CHECK_INTERVAL_MS = 5000;

    private final oshi.SystemInfo systemInfo = new oshi.SystemInfo();

    public SystemMonitorServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Result getSystemInfo() {
        oshi.SystemInfo si = new oshi.SystemInfo();
        oshi.hardware.HardwareAbstractionLayer hal = systemInfo.getHardware();
        oshi.software.os.OperatingSystem os = systemInfo.getOperatingSystem();

        oshi.hardware.CentralProcessor processor = hal.getProcessor();
        oshi.hardware.GlobalMemory memory = hal.getMemory();

        long totalMemory = memory.getTotal();
        long availableMemory = memory.getAvailable();
        long usedMemory = totalMemory - availableMemory;
        double memoryUsagePercent = (double) usedMemory / totalMemory * 100;

        double[] cpuLoad = processor.getSystemLoadAverage(3);

        double cpuUsagePercent;
        long now = System.currentTimeMillis();
        if (now - lastCpuCheckTime > CPU_CHECK_INTERVAL_MS) {
            long[] prevTicks = processor.getSystemCpuLoadTicks();
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}
            double raw = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
            cachedCpuUsage = Double.isNaN(raw) || raw < 0 ? 0 : raw;
            lastCpuCheckTime = now;
        }
        cpuUsagePercent = cachedCpuUsage;

        java.lang.management.RuntimeMXBean runtimeBean = java.lang.management.ManagementFactory.getRuntimeMXBean();
        long uptimeMs = runtimeBean.getUptime();
        long uptimeSec = uptimeMs / 1000;
        String uptime = String.format("%d天 %d小时 %d分钟", uptimeSec / 86400, (uptimeSec % 86400) / 3600, (uptimeSec % 3600) / 60);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("osName", os.toString());
        data.put("osArch", System.getProperty("os.arch"));
        data.put("cpuName", processor.getProcessorIdentifier().getName().trim());
        data.put("cpuCores", processor.getLogicalProcessorCount());
        data.put("cpuUsagePercent", Math.round(cpuUsagePercent * 100.0) / 100.0);
        data.put("cpuLoadAvg1min", cpuLoad[0] >= 0 ? Math.round(cpuLoad[0] * 100.0) / 100.0 : "N/A");
        data.put("totalMemoryGB", Math.round(totalMemory / 1024.0 / 1024.0 / 1024.0 * 100.0) / 100.0);
        data.put("usedMemoryGB", Math.round(usedMemory / 1024.0 / 1024.0 / 1024.0 * 100.0) / 100.0);
        data.put("availableMemoryGB", Math.round(availableMemory / 1024.0 / 1024.0 / 1024.0 * 100.0) / 100.0);
        data.put("memoryUsagePercent", Math.round(memoryUsagePercent * 100.0) / 100.0);
        data.put("javaUptime", uptime);

        return Result.success("系统信息获取成功", data);
    }

    @Override
    public Result getRedisInfo() {
        List<Map<String, Object>> databases = new ArrayList<>();
        LettuceConnectionFactory factory = null;
        try {
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
            factory = new LettuceConnectionFactory(config);
            factory.afterPropertiesSet();

            Properties serverInfo = null;
            try {
                var conn = factory.getConnection();
                try {
                    serverInfo = conn.info("server");
                } finally {
                    conn.close();
                }
            } catch (Exception e) {
                // serverInfo 获取失败，继续尝试获取 keyspace
            }

            for (int db = 0; db < 16; db++) {
                Map<String, Object> dbInfo = new LinkedHashMap<>();
                try {
                    var conn = factory.getConnection();
                    try {
                        conn.select(db);
                        if (serverInfo == null) {
                            serverInfo = conn.info("server");
                        }
                        Properties info = conn.info("keyspace");
                        long keyCount = 0;
                        if (info != null) {
                            String dbLine = info.getProperty("db" + db);
                            if (dbLine != null && dbLine.contains("keys=")) {
                                String part = dbLine.substring(dbLine.indexOf("keys=") + 5);
                                if (part.contains(",")) part = part.substring(0, part.indexOf(","));
                                keyCount = Long.parseLong(part.trim());
                            }
                        }
                        dbInfo.put("db", db);
                        dbInfo.put("keys", keyCount);
                        dbInfo.put("status", "connected");
                    } finally {
                        conn.close();
                    }
                } catch (Exception e) {
                    dbInfo.put("db", db);
                    dbInfo.put("keys", 0);
                    dbInfo.put("status", "error: " + e.getMessage());
                }
                databases.add(dbInfo);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("host", redisHost + ":" + redisPort);
            result.put("redisVersion", serverInfo != null ? serverInfo.getProperty("redis_version") : "N/A");
            result.put("usedMemory", serverInfo != null ? serverInfo.getProperty("used_memory_human") : "N/A");
            result.put("usedMemoryPeak", serverInfo != null ? serverInfo.getProperty("used_memory_peak_human") : "N/A");
            result.put("connectedClients", serverInfo != null ? serverInfo.getProperty("connected_clients") : "N/A");
            result.put("uptimeSeconds", serverInfo != null ? serverInfo.getProperty("uptime_in_seconds") : "N/A");
            result.put("databases", databases);
            return Result.success("Redis信息获取成功", result);
        } catch (Exception e) {
            return Result.fail("Redis信息获取失败: " + e.getMessage());
        } finally {
            if (factory != null) {
                try { factory.destroy(); } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public Result tailLog(int lines, String level) {
        try {
            java.io.File logFile = new java.io.File(logFilePath);
            if (!logFile.exists()) {
                return Result.success("日志文件不存在", List.of());
            }
            List<String> allLines = readLastLines(logFile, lines * 3);
            List<String> filtered = new ArrayList<>();
            for (String line : allLines) {
                if (level == null || level.isEmpty() || "ALL".equals(level) || line.contains(level)) {
                    filtered.add(line);
                }
            }
            if (filtered.size() > lines) {
                filtered = filtered.subList(filtered.size() - lines, filtered.size());
            }
            return Result.success("日志获取成功", filtered);
        } catch (IOException e) {
            return Result.fail("日志读取失败: " + e.getMessage());
        }
    }

    @Override
    public Result getRedisKeys(int db, int limit) {
        try {
            List<Map<String, Object>> keys = redisTemplate.execute((org.springframework.data.redis.connection.RedisConnection connection) -> {
                connection.select(db);

                ScanOptions scanOptions = ScanOptions.scanOptions()
                        .count(Math.max(limit, 100))
                        .build();

                List<Map<String, Object>> result = new ArrayList<>();
                try (Cursor<byte[]> cursor = connection.scan(scanOptions)) {
                    int count = 0;
                    while (cursor.hasNext() && count < limit) {
                        byte[] keyBytes = cursor.next();
                        String key = new String(keyBytes, StandardCharsets.UTF_8);

                        String type = connection.keyCommands().type(keyBytes).code();
                        Long ttl = connection.keyCommands().pTtl(keyBytes);

                        Map<String, Object> keyInfo = new LinkedHashMap<>();
                        keyInfo.put("key", key);
                        keyInfo.put("type", type);
                        keyInfo.put("ttl", ttl != null ? ttl : -1);
                        result.add(keyInfo);
                        count++;
                    }
                }
                return result;
            });

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("db", db);
            response.put("keys", keys != null ? keys : List.of());
            response.put("total", keys != null ? keys.size() : 0);
            return Result.success("查询成功", response);
        } catch (Exception e) {
            return Result.fail("Redis查询失败: " + e.getMessage());
        }
    }

    @Override
    public Result executeCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return Result.fail("命令不能为空");
        }
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            if (os.contains("win")) {
                pb = new ProcessBuilder("cmd.exe", "/c", command);
            } else {
                java.io.File bash = new java.io.File("/bin/bash");
                if (bash.exists()) {
                    pb = new ProcessBuilder("/bin/bash", "-c", command);
                } else {
                    pb = new ProcessBuilder("/bin/sh", "-c", command);
                }
            }
            pb.redirectErrorStream(false);

            long startTime = System.currentTimeMillis();
            Process process = pb.start();

            String output;
            String error;
            try (var is = process.getInputStream();
                 var es = process.getErrorStream()) {
                output = readAllBytes(is);
                error = readAllBytes(es);
            }

            boolean finished = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
            long elapsed = System.currentTimeMillis() - startTime;

            if (!finished) {
                process.destroyForcibly();
                return Result.fail("命令执行超时（30秒）");
            }

            int exitCode = process.exitValue();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("output", output);
            data.put("error", error);
            data.put("exitCode", exitCode);
            data.put("elapsed", elapsed);
            data.put("command", command);

            return Result.success("命令执行完成", data);
        } catch (Exception e) {
            return Result.fail("命令执行失败: " + e.getMessage());
        }
    }

    private String readAllBytes(java.io.InputStream is) throws java.io.IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        while ((len = is.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        return baos.toString(StandardCharsets.UTF_8);
    }

    private List<String> readLastLines(java.io.File file, int count) throws IOException {
        List<String> lines = new ArrayList<>();
        int chunkSize = 8192;
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long length = file.length();
            if (length == 0) return lines;

            long pos = length;
            ByteBuffer buffer = ByteBuffer.allocate(chunkSize);
            StringBuilder remainder = new StringBuilder();

            while (pos > 0 && lines.size() < count) {
                int readSize = (int) Math.min(chunkSize, pos);
                pos -= readSize;
                raf.seek(pos);
                byte[] bytes = new byte[readSize];
                raf.readFully(bytes);

                System.arraycopy(bytes, 0, buffer.array(), 0, readSize);
                buffer.position(0);
                buffer.limit(readSize);
                String chunk = remainder.toString() + StandardCharsets.UTF_8.decode(buffer).toString();
                remainder.setLength(0);

                String[] parts = chunk.split("\n");
                remainder.append(parts[0]);

                for (int i = parts.length - 1; i >= 1; i--) {
                    String line = parts[i];
                    if (!line.isEmpty() || lines.size() < count) {
                        lines.add(line);
                    }
                    if (lines.size() >= count) break;
                }
            }

            if (remainder.length() > 0 && lines.size() < count) {
                lines.add(remainder.toString());
            }
        }
        Collections.reverse(lines);
        return lines;
    }
}