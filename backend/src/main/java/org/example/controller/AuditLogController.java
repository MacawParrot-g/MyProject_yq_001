// 文件路径: src/main/java/org/example/controller/AuditLogController.java
package org.example.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.annotation.LogExecutionTime;
import org.example.common.Result;
import org.example.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/audit")
public class AuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    @PostMapping("/search")
    @LogExecutionTime("审计日志搜索")
    public Result search(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        String operator = (String) body.get("operator");
        String action = (String) body.get("action");
        String dateFrom = (String) body.get("dateFrom");
        String dateTo = (String) body.get("dateTo");
        int page = body.get("page") != null ? ((Number) body.get("page")).intValue() : 1;
        int size = body.get("size") != null ? ((Number) body.get("size")).intValue() : 15;
        return auditLogService.searchAuditLogs(operator, action, dateFrom, dateTo, page, size);
    }

    @DeleteMapping("/clean")
    @LogExecutionTime("清理过期审计日志")
    public Result clean(@RequestParam(defaultValue = "90") int retainDays) {
        return auditLogService.cleanOldLogs(retainDays);
    }
}
