package org.example.service;

import org.example.common.Result;

public interface SystemMonitorService {

    Result getSystemInfo();

    Result getRedisInfo();

    Result tailLog(int lines, String level);

    Result getRedisKeys(int db, int limit);
}