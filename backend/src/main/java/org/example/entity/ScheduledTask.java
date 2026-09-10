package org.example.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("scheduled_task")
public class ScheduledTask {
    private String id;
    private String name;
    private String taskType;
    private String cronExpression;
    private String params;
    private Boolean enabled;
    private String lastExecutedAt;
    private String lastResult;
    private Long totalExecutions;
    private String createdAt;
    private String createdBy;
}
