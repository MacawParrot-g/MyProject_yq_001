// 文件路径: src/main/java/org/example/service/impl/NotificationServiceImpl.java
package org.example.service.impl;

import org.example.common.Result;
import org.example.entity.Notification;
import org.example.mapper.NotificationMapper;
import org.example.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @Autowired
    private NotificationMapper notificationMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendNotification(String receiver, String type, String title, String content) {
        Notification notification = new Notification();
        notification.setReceiver(receiver);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        try {
            notificationMapper.insertNotification(notification);
        } catch (Exception e) {
            log.error("❌ 发送通知失败: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Result getNotifications(String receiver, int page, int size) {
        int offset = (page - 1) * size;
        List<Notification> list = notificationMapper.selectByReceiver(receiver, size, offset);
        long total = notificationMapper.countByReceiver(receiver);
        int unread = notificationMapper.countUnread(receiver);
        Map<String, Object> data = Map.of("list", list, "unread", unread);
        return Result.success("查询成功", data, null, total, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public Result getUnreadCount(String receiver) {
        int count = notificationMapper.countUnread(receiver);
        return Result.success("查询成功", count);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result markAsRead(String receiver, Long id) {
        notificationMapper.markAsRead(receiver, id);
        return Result.success("已标记为已读");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result markAllAsRead(String receiver) {
        notificationMapper.markAllAsRead(receiver);
        return Result.success("全部标记为已读");
    }
}
