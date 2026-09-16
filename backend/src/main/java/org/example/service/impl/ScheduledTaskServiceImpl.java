// 文件路径: src/main/java/org/example/service/impl/ScheduledTaskServiceImpl.java
package org.example.service.impl;

import org.example.common.Result;
import org.example.entity.ScheduledTask;
import org.example.mapper.AuditLogMapper;
import org.example.mapper.ScheduledTaskMapper;
import org.example.service.AuditLogService;
import org.example.service.ScheduledTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

@Service
@Order(2)
public class ScheduledTaskServiceImpl implements ScheduledTaskService, CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskServiceImpl.class);

    @Autowired
    private ScheduledTaskMapper scheduledTaskMapper;

    @Autowired
    private org.example.mapper.GeneranMapper generanMapper;

    private final Map<String, ScheduledFuture<?>> runningTasks = new ConcurrentHashMap<>();
    private ScheduledExecutorService executor;

    @Override
    public void run(String... args) {
        initScheduledTasks();
    }

    @Override
    @Transactional(readOnly = true)
    public Result listTasks() {
        List<ScheduledTask> tasks = scheduledTaskMapper.selectAll();
        return Result.success("查询成功", tasks);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result createTask(String name, String taskType, String cronExpression, String params, String createdBy) {
        ScheduledTask task = new ScheduledTask();
        task.setId(UUID.randomUUID().toString());
        task.setName(name);
        task.setTaskType(taskType);
        task.setCronExpression(cronExpression);
        task.setParams(params);
        task.setEnabled(true);
        task.setTotalExecutions(0L);
        task.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        task.setCreatedBy(createdBy);
        scheduledTaskMapper.insertTask(task);
        scheduleTask(task);
        return Result.success("任务创建成功", task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result toggleTask(String id, boolean enabled) {
        scheduledTaskMapper.updateEnabled(id, enabled);
        if (enabled) {
            ScheduledTask task = scheduledTaskMapper.selectById(id);
            if (task != null) scheduleTask(task);
        } else {
            ScheduledFuture<?> future = runningTasks.remove(id);
            if (future != null) future.cancel(false);
        }
        return Result.success(enabled ? "任务已启用" : "任务已停用", Map.of("id", id, "enabled", enabled));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result deleteTask(String id) {
        ScheduledFuture<?> future = runningTasks.remove(id);
        if (future != null) future.cancel(false);
        scheduledTaskMapper.deleteById(id);
        return Result.success("任务已删除", id);
    }

    @Override
    public Result executeNow(String id) {
        ScheduledTask task = scheduledTaskMapper.selectById(id);
        if (task == null) return Result.fail("任务不存在");
        String result = executeTaskLogic(task);
        scheduledTaskMapper.updateExecution(id,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                result);
        return Result.success("执行完成: " + result, Map.of("id", id, "result", result));
    }

    @Override
    public void initScheduledTasks() {
        executor = Executors.newScheduledThreadPool(4, r -> {
            Thread t = new Thread(r, "task-scheduler");
            t.setDaemon(true);
            return t;
        });

        List<ScheduledTask> enabledTasks = scheduledTaskMapper.selectEnabled();
        for (ScheduledTask task : enabledTasks) {
            scheduleTask(task);
        }
        log.info("✅ 定时任务初始化完成，已加载 {} 个启用中的任务", enabledTasks.size());
    }

    private void scheduleTask(ScheduledTask task) {
        ScheduledFuture<?> existing = runningTasks.get(task.getId());
        if (existing != null) existing.cancel(false);

        long intervalMs = parseCronToIntervalMs(task.getCronExpression());
        if (intervalMs <= 0) intervalMs = 3600000;

        ScheduledFuture<?> future = executor.scheduleAtFixedRate(() -> {
            try {
                String result = executeTaskLogic(task);
                scheduledTaskMapper.updateExecution(task.getId(),
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        result);
                log.info("✅ 定时任务[{}]执行成功: {}", task.getName(), result);
            } catch (Exception e) {
                String error = "执行异常: " + e.getMessage();
                scheduledTaskMapper.updateExecution(task.getId(),
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        error);
                log.error("❌ 定时任务[{}]执行失败: {}", task.getName(), e.getMessage());
            }
        }, intervalMs, intervalMs, TimeUnit.MILLISECONDS);

        runningTasks.put(task.getId(), future);
        log.info("📅 任务[{}]已调度，间隔: {}ms", task.getName(), intervalMs);
    }

    private String executeTaskLogic(ScheduledTask task) {
        switch (task.getTaskType()) {
            case "AUDIT_LOG_CLEAN":
                int retainDays = 90;
                if (task.getParams() != null && !task.getParams().isEmpty()) {
                    try { retainDays = Integer.parseInt(task.getParams().trim()); } catch (Exception ignored) {}
                }
                String beforeDate = LocalDateTime.now().minusDays(retainDays).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                int cleaned = 0;
                try { cleaned = generanMapper.deleteOldAuditLogs(beforeDate + " 00:00:00"); } catch (Exception ignored) {}
                return "清理了 " + cleaned + " 条审计日志";

            case "DATA_ARCHIVE":
                return "数据归档任务已执行（需配合归档表使用）";

            case "EXPORT_MARK_RESET":
                int reset = 0;
                try { reset = generanMapper.resetExportMark(); } catch (Exception ignored) {}
                return "重置了 " + reset + " 条导出标记";

            default:
                return "未知任务类型: " + task.getTaskType();
        }
    }

    private long parseCronToIntervalMs(String cron) {
        if (cron == null || cron.isEmpty()) return 3600000;
        cron = cron.trim().toLowerCase();
        if (cron.endsWith("h")) {
            try { return Long.parseLong(cron.replace("h", "")) * 3600000; } catch (Exception e) { return 3600000; }
        }
        if (cron.endsWith("m")) {
            try { return Long.parseLong(cron.replace("m", "")) * 60000; } catch (Exception e) { return 3600000; }
        }
        if (cron.endsWith("d")) {
            try { return Long.parseLong(cron.replace("d", "")) * 86400000; } catch (Exception e) { return 3600000; }
        }
        try { return Long.parseLong(cron); } catch (Exception e) { return 3600000; }
    }
}
