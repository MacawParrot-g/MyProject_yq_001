package org.example.service.impl;

import org.example.common.Result;
import org.example.service.SystemMonitorService;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SystemMonitorServiceImpl implements SystemMonitorService {

    private volatile long lastCpuCheckTime = 0;
    private volatile double cachedCpuUsage = 0;
    private static final long CPU_CHECK_INTERVAL_MS = 5000;

    @Override
    public Result getSystemInfo() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        OperatingSystem os = si.getOperatingSystem();

        CentralProcessor processor = hal.getProcessor();
        GlobalMemory memory = hal.getMemory();

        long totalMemory = memory.getTotal();
        long availableMemory = memory.getAvailable();
        long usedMemory = totalMemory - availableMemory;
        double memoryUsagePercent = (double) usedMemory / totalMemory * 100;

        double[] cpuLoad = processor.getSystemLoadAverage(3);

        double cpuUsagePercent;
        long now = System.currentTimeMillis();
        if (now - lastCpuCheckTime > CPU_CHECK_INTERVAL_MS) {
            long[] prevTicks = processor.getSystemCpuLoadTicks();
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            cachedCpuUsage = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
            lastCpuCheckTime = now;
        }
        cpuUsagePercent = cachedCpuUsage;

        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
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
}
