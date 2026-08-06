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
        double cpuUsagePercent = cpuLoad[0] >= 0 ? cpuLoad[0] / processor.getLogicalProcessorCount() * 100 : 0;

        long[] prevTicks = processor.getSystemCpuLoadTicks();
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        double cpuLoadBetweenTicks = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;

        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        long uptimeMs = runtimeBean.getUptime();
        long uptimeSec = uptimeMs / 1000;
        String uptime = String.format("%d天 %d小时 %d分钟", uptimeSec / 86400, (uptimeSec % 86400) / 3600, (uptimeSec % 3600) / 60);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("osName", os.toString());
        data.put("osArch", System.getProperty("os.arch"));
        data.put("cpuName", processor.getProcessorIdentifier().getName().trim());
        data.put("cpuCores", processor.getLogicalProcessorCount());
        data.put("cpuUsagePercent", Math.round(cpuLoadBetweenTicks * 100.0) / 100.0);
        data.put("cpuLoadAvg1min", cpuLoad[0] >= 0 ? Math.round(cpuLoad[0] * 100.0) / 100.0 : "N/A");
        data.put("totalMemoryGB", Math.round(totalMemory / 1024.0 / 1024.0 / 1024.0 * 100.0) / 100.0);
        data.put("usedMemoryGB", Math.round(usedMemory / 1024.0 / 1024.0 / 1024.0 * 100.0) / 100.0);
        data.put("availableMemoryGB", Math.round(availableMemory / 1024.0 / 1024.0 / 1024.0 * 100.0) / 100.0);
        data.put("memoryUsagePercent", Math.round(memoryUsagePercent * 100.0) / 100.0);
        data.put("javaUptime", uptime);

        return Result.success("系统信息获取成功", data);
    }
}
