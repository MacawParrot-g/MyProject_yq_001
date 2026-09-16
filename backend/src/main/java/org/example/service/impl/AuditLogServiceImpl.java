// 文件路径: src/main/java/org/example/service/impl/AuditLogServiceImpl.java
package org.example.service.impl;

import org.example.common.Result;
import org.example.entity.AuditLog;
import org.example.mapper.AuditLogMapper;
import org.example.mq.AuditLogMessageProducer;
import org.example.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogServiceImpl.class);

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Autowired
    private AuditLogMessageProducer auditLogMessageProducer;

    @Override
    public void recordAuditLogAsync(AuditLog auditLog) {
        boolean sent = auditLogMessageProducer.sendAuditLog(auditLog);
        if (!sent) {
            try {
                auditLogMapper.insertAuditLog(auditLog);
                log.info("📥 审计日志MySQL直连入库成功（降级模式）");
            } catch (Exception e) {
                log.error("❌ 审计日志直连入库也失败: {}", e.getMessage());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Result searchAuditLogs(String operator, String action, String dateFrom, String dateTo, int page, int size) {
        int offset = (page - 1) * size;
        List<AuditLog> list = auditLogMapper.searchAuditLogs(operator, action, dateFrom, dateTo, size, offset);
        long total = auditLogMapper.countAuditLogs(operator, action, dateFrom, dateTo);
        return Result.success("查询成功，共 " + total + " 条", list, null, total, page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result cleanOldLogs(int retainDays) {
        String beforeDate = LocalDate.now().minusDays(retainDays).format(DateTimeFormatter.ISO_LOCAL_DATE);
        int count = auditLogMapper.deleteOldLogs(beforeDate + " 00:00:00");
        return Result.success("成功清理 " + count + " 条审计日志（保留 " + retainDays + " 天内）", count);
    }
}
