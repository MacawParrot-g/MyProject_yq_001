package org.example.controller;
import org.example.annotation.LogExecutionTime;
import org.example.common.Result;
import org.example.service.DedupSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dedup")
public class DedupController {

    @Autowired
    private DedupSessionService dedupSessionService;

    @PostMapping("/enable")
    @LogExecutionTime("开启自动去重")
    public Result enable(@RequestParam String username) {
        return dedupSessionService.enableDedup(username);
    }

    @PostMapping("/disable")
    @LogExecutionTime("关闭自动去重")
    public Result disable() {
        return dedupSessionService.disableDedup();
    }

    @GetMapping("/status")
    @LogExecutionTime("查询去重状态")
    public Result status() {
        return dedupSessionService.getStatus();
    }
}
