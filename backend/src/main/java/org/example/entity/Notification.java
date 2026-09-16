// 文件路径: src/main/java/org/example/entity/Notification.java
package org.example.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("notification")
public class Notification {
    private Long id;
    private String receiver;
    private String type;
    private String title;
    private String content;
    private Boolean isRead;
    private String createdAt;
}
