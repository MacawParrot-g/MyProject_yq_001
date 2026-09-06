// 文件路径: src/main/java/org/example/mapper/NotificationMapper.java
package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.example.entity.Notification;

import java.util.List;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    @Insert("INSERT INTO notification (receiver, type, title, content, is_read, created_at) " +
            "VALUES (#{receiver}, #{type}, #{title}, #{content}, #{isRead}, #{createdAt})")
    int insertNotification(Notification notification);

    @Select("SELECT * FROM notification WHERE receiver = #{receiver} ORDER BY created_at DESC LIMIT #{size} OFFSET #{offset}")
    List<Notification> selectByReceiver(@Param("receiver") String receiver,
                                        @Param("size") int size,
                                        @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM notification WHERE receiver = #{receiver}")
    long countByReceiver(@Param("receiver") String receiver);

    @Select("SELECT COUNT(*) FROM notification WHERE receiver = #{receiver} AND is_read = 0")
    int countUnread(@Param("receiver") String receiver);

    @Update("UPDATE notification SET is_read = 1 WHERE receiver = #{receiver} AND id = #{id}")
    int markAsRead(@Param("receiver") String receiver, @Param("id") Long id);

    @Update("UPDATE notification SET is_read = 1 WHERE receiver = #{receiver}")
    int markAllAsRead(@Param("receiver") String receiver);

    @Delete("DELETE FROM notification WHERE created_at < #{beforeDate}")
    int deleteOldNotifications(@Param("beforeDate") String beforeDate);
}
