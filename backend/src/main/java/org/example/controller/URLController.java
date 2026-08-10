package org.example.controller;

import org.example.annotation.LogExecutionTime;
import org.example.service.URLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class URLController {

    @Autowired
    private URLService urlService;

    @GetMapping("/api/proxy/task")
    @LogExecutionTime("代理获取任务")
    public Map<String, Object> proxyTask() {
        return urlService.proxyTask();
    }

    @GetMapping("/api/proxy/obtain")
    @LogExecutionTime("代理获取详情")
    public Map<String, Object> proxyObtain(@RequestParam Long appleid) {
        return urlService.proxyObtain(appleid);
    }

    @GetMapping("/api/proxy/event")
    @LogExecutionTime("代理获取事件")
    public Map<String, Object> proxyEvent(@RequestParam String bundleId) {
        return urlService.proxyEvent(bundleId);
    }

    @GetMapping("/api/proxy/attribution")
    @LogExecutionTime("代理获取归因")
    public Map<String, Object> proxyAttribution(@RequestParam String bundleId, @RequestParam String type) {
        return urlService.proxyAttribution(bundleId, type);
    }

    @GetMapping("/api/proxy/frozen")
    @LogExecutionTime("代理冻结任务")
    public Map<String, Object> proxyFrozen(@RequestParam Long id) {
        return urlService.proxyFrozen(id);
    }

}
