package org.example.service;

import org.example.common.Result;

public interface MQMonitorService {
    Result getQueueOverview();
    Result getDlqRecords(String type);
    Result retryDlqMessage(String type, int index);
    Result clearDlqRecords(String type);
    Result getDlqSummary();
}
