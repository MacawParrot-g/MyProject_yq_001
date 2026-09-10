// 文件路径: src/main/java/org/example/controller/ScheduledTaskController.java
package org.example.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.annotation.LogExecutionTime;
import org.example.common.Result;
import org.example.service.ScheduledTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/task")
public class ScheduledTaskController {

    @Autowired
    private ScheduledTaskService scheduledTaskService;

    @GetMapping("/list")
    @LogExecutionTime("查看定时任务列表")
    public Result list() {
        return scheduledTaskService.listTasks();
    }

    @PostMapping("/create")
    @LogExecutionTime("创建定时任务")
    public Result create(HttpServletRequest request, @RequestBody Map<String, String> body) {
        String createdBy = request.getHeader("X-User-Name");
        return scheduledTaskService.createTask(
                body.get("name"),
                body.get("taskType"),
                body.get("cronExpression"),
                body.get("params"),
                createdBy
        );
    }

    @PutMapping("/toggle")
    @LogExecutionTime("切换定时任务状态")
    public Result toggle(@RequestBody Map<String, Object> body) {
        String id = (String) body.get("id");
        boolean enabled = (Boolean) body.get("enabled");
        return scheduledTaskService.toggleTask(id, enabled);
    }

    @DeleteMapping("/delete")
    @LogExecutionTime("删除定时任务")
    public Result delete(@RequestParam String id) {
        return scheduledTaskService.deleteTask(id);
    }

    @PostMapping("/execute")
    @LogExecutionTime("立即执行定时任务")
    public Result executeNow(@RequestParam String id) {
        return scheduledTaskService.executeNow(id);
    }
}
