package org.example.controller;

import org.example.annotation.LogExecutionTime;
import org.example.common.Result;
import org.example.service.DevHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dev")
public class DevHistoryController {

    @Autowired
    private DevHistoryService devHistoryService;

    @PostMapping("/history/save")
    @LogExecutionTime("保存开发历史记录")
    public Result saveHistory(@RequestBody Map<String, Object> record,
                              @RequestHeader(value = "X-User-Name", required = false) String recorder) {
        try {
            devHistoryService.addRecord(recorder, record);
            return Result.success("保存成功");
        } catch (Exception e) {
            return Result.fail("保存失败：" + e.getMessage());
        }
    }

    @GetMapping("/history")
    @LogExecutionTime("查询开发历史记录")
    public Result getHistory(@RequestHeader(value = "X-User-Name", required = false) String recorder) {
        try {
            List<Map<String, Object>> list = devHistoryService.getHistory(recorder);
            boolean redisOk = devHistoryService.isRedisAvailable();
            return Result.success(redisOk ? "查询成功" : "⚠️ Redis不可用，当前使用本地缓存（最多100条）", list);
        } catch (Exception e) {
            return Result.fail("查询失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/history/clear")
    @LogExecutionTime("清空开发历史记录")
    public Result clearHistory(@RequestHeader(value = "X-User-Name", required = false) String recorder) {
        try {
            devHistoryService.clearHistory(recorder);
            return Result.success("清空成功");
        } catch (Exception e) {
            return Result.fail("清空失败：" + e.getMessage());
        }
    }

    @GetMapping("/history/redis-status")
    public Result redisStatus() {
        boolean available = devHistoryService.isRedisAvailable();
        return Result.success(available ? "Redis可用" : "Redis不可用", Map.of("available", available));
    }

    @DeleteMapping("/history/delete")
    @LogExecutionTime("删除单条开发历史记录")
    public Result deleteRecord(@RequestHeader(value = "X-User-Name", required = false) String recorder,
                               @RequestParam String timestamp) {
        try {
            devHistoryService.deleteRecord(recorder, timestamp);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.fail("删除失败：" + e.getMessage());
        }
    }
}
