package org.example.service;

import java.util.List;
import java.util.Map;

public interface DevHistoryService {
    void addRecord(String recorder, Map<String, Object> record);
    List<Map<String, Object>> getHistory(String recorder);
    boolean isRedisAvailable();
    void clearHistory(String recorder);
    void deleteRecord(String recorder, String timestamp);
}
