// 文件路径: src/main/java/org/example/entity/AuditLog.java
package org.example.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("audit_log")
public class AuditLog {
    private Long id;
    private String operator;
    private String operatorType;
    private String action;
    private String method;
    private String uri;
    private String params;
    private String ip;
    private Boolean success;
    private String errorMessage;
    private Long durationMs;
    private String createdAt;
}
