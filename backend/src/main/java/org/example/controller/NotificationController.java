// 文件路径: src/main/java/org/example/controller/NotificationController.java
package org.example.controller;

import org.example.annotation.LogExecutionTime;
import org.example.common.Result;
import org.example.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/list")
    @LogExecutionTime("查询通知列表")
    public Result list(@RequestParam String receiver,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "20") int size) {
        return notificationService.getNotifications(receiver, page, size);
    }

    @GetMapping("/unread-count")
    @LogExecutionTime("查询未读通知数")
    public Result unreadCount(@RequestParam String receiver) {
        return notificationService.getUnreadCount(receiver);
    }

    @PutMapping("/read")
    @LogExecutionTime("标记通知已读")
    public Result markRead(@RequestParam String receiver, @RequestParam Long id) {
        return notificationService.markAsRead(receiver, id);
    }

    @PutMapping("/read-all")
    @LogExecutionTime("全部标记已读")
    public Result markAllRead(@RequestParam String receiver) {
        return notificationService.markAllAsRead(receiver);
    }

    @PostMapping("/send")
    @LogExecutionTime("发送通知")
    public Result send(@RequestBody Map<String, String> body) {
        return notificationService.sendNotificationApi(
                body.get("receiver"),
                body.get("type"),
                body.get("title"),
                body.get("content")
        );
    }

    @GetMapping("/all")
    @LogExecutionTime("查询全部通知")
    public Result all(@RequestParam(defaultValue = "1") int page,
                      @RequestParam(defaultValue = "20") int size) {
        return notificationService.getAllNotifications(page, size);
    }

    @DeleteMapping("/delete")
    @LogExecutionTime("删除通知")
    public Result delete(@RequestParam Long id) {
        return notificationService.deleteNotification(id);
    }
}
