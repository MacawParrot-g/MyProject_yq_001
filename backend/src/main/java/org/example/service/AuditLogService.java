// 文件路径: src/main/java/org/example/service/AuditLogService.java
package org.example.service;

import org.example.common.Result;
import org.example.entity.AuditLog;

public interface AuditLogService {
    void recordAuditLogAsync(AuditLog auditLog);
    Result searchAuditLogs(String operator, String action, String dateFrom, String dateTo, int page, int size);
    Result cleanOldLogs(int retainDays);
}
