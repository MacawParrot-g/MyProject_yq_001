package org.example.service;

import org.example.common.Result;

public interface ScheduledTaskService {
    Result listTasks();
    Result createTask(String name, String taskType, String cronExpression, String params, String createdBy);
    Result toggleTask(String id, boolean enabled);
    Result deleteTask(String id);
    Result executeNow(String id);
    void initScheduledTasks();
}
