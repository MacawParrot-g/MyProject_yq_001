package org.example.service;

import org.example.common.Result;

public interface NotificationService {
    void sendNotification(String receiver, String type, String title, String content);
    Result getNotifications(String receiver, int page, int size);
    Result getUnreadCount(String receiver);
    Result markAsRead(String receiver, Long id);
    Result markAllAsRead(String receiver);
}
