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

    private volatile long lastCpuCheckTime = 0;
    private volatile double cachedCpuUsage = 0;
    private static final long CPU_CHECK_INTERVAL_MS = 5000;

    @Value("${logging.file.name:/data/logs/app.log}")
    private String logFilePath;

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    private final RedisTemplate<String, Object> redisTemplate;

    public SystemMonitorServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Result getSystemInfo() {
        oshi.SystemInfo si = new oshi.SystemInfo();
        oshi.hardware.HardwareAbstractionLayer hal = si.getHardware();
        oshi.software.os.OperatingSystem os = si.getOperatingSystem();

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
        LettuceConnectionFactory factory = null;
        try {
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
            config.setDatabase(db);
            factory = new LettuceConnectionFactory(config);
            factory.afterPropertiesSet();

            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(factory);
            template.setKeySerializer(new StringRedisSerializer());
            template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
            template.setHashKeySerializer(new StringRedisSerializer());
            template.setHashValueSerializer(new StringRedisSerializer());
            template.afterPropertiesSet();

            List<Map<String, Object>> keys = new ArrayList<>();
            Cursor<String> cursor = template.scan(ScanOptions.scanOptions().count(limit).build());
            int count = 0;
            while (cursor.hasNext() && count < limit) {
                String key = cursor.next();
                Map<String, Object> keyInfo = new LinkedHashMap<>();
                keyInfo.put("key", key);
                String type = Objects.requireNonNull(template.type(key)).code();
                keyInfo.put("type", type);
                Long ttl = template.getExpire(key);
                keyInfo.put("ttl", ttl != null ? ttl : -1);
                keys.add(keyInfo);
                count++;
            }
            cursor.close();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("db", db);
            result.put("keys", keys);
            result.put("total", count);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            return Result.fail("Redis查询失败: " + e.getMessage());
        } finally {
            if (factory != null) {
                try { factory.destroy(); } catch (Exception ignored) {}
            }
        }
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