package org.example.controller;

import org.example.annotation.LogExecutionTime;
import org.example.common.Result;
import org.example.service.SystemMonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system")
public class SystemMonitorController {

    @Autowired
    private SystemMonitorService systemMonitorService;

    @GetMapping("/info")
    @LogExecutionTime("系统监控信息")
    public Result getSystemInfo() {
        return systemMonitorService.getSystemInfo();
    }

    @GetMapping("/redis/info")
    @LogExecutionTime("Redis数据库信息")
    public Result getRedisInfo() {
        return systemMonitorService.getRedisInfo();
    }

    @GetMapping("/redis/keys")
    @LogExecutionTime("Redis键查询")
    public Result getRedisKeys(@RequestParam(defaultValue = "0") int db,
                               @RequestParam(defaultValue = "100") int limit) {
        return systemMonitorService.getRedisKeys(db, limit);
    }

    @GetMapping("/log/tail")
    @LogExecutionTime("日志查看")
    public Result tailLog(@RequestParam(defaultValue = "200") int lines,
                          @RequestParam(defaultValue = "ALL") String level) {
        return systemMonitorService.tailLog(lines, level);
    }
}
