// 文件路径: src/main/java/org/example/controller/MQMonitorController.java
package org.example.controller;

import org.example.annotation.LogExecutionTime;
import org.example.common.Result;
import org.example.service.MQMonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mq")
public class MQMonitorController {

    @Autowired
    private MQMonitorService mqMonitorService;

    @GetMapping("/overview")
    @LogExecutionTime("MQ队列总览")
    public Result getQueueOverview() {
        return mqMonitorService.getQueueOverview();
    }

    @GetMapping("/dlq/records")
    @LogExecutionTime("DLQ记录查询")
    public Result getDlqRecords(@RequestParam String type) {
        return mqMonitorService.getDlqRecords(type);
    }

    @PostMapping("/dlq/retry")
    @LogExecutionTime("DLQ消息重试")
    public Result retryDlqMessage(@RequestParam String type, @RequestParam int index) {
        return mqMonitorService.retryDlqMessage(type, index);
    }

    @DeleteMapping("/dlq/clear")
    @LogExecutionTime("DLQ记录清除")
    public Result clearDlqRecords(@RequestParam String type) {
        return mqMonitorService.clearDlqRecords(type);
    }

    @GetMapping("/dlq/summary")
    @LogExecutionTime("DLQ摘要")
    public Result getDlqSummary() {
        return mqMonitorService.getDlqSummary();
    }
}
