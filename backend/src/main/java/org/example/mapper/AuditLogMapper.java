package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.example.entity.AuditLog;

import java.util.List;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {

    @Insert("INSERT INTO audit_log (operator, operator_type, action, method, uri, params, ip, success, error_message, duration_ms, created_at) " +
            "VALUES (#{operator}, #{operatorType}, #{action}, #{method}, #{uri}, #{params}, #{ip}, #{success}, #{errorMessage}, #{durationMs}, #{createdAt})")
    int insertAuditLog(AuditLog log);

    @Select("<script>" +
            "SELECT * FROM audit_log WHERE 1=1" +
            "<if test='operator != null and operator != \"\"'> AND operator LIKE CONCAT('%', #{operator}, '%')</if>" +
            "<if test='action != null and action != \"\"'> AND action LIKE CONCAT('%', #{action}, '%')</if>" +
            "<if test='dateFrom != null and dateFrom != \"\"'> AND created_at &gt;= #{dateFrom}</if>" +
            "<if test='dateTo != null and dateTo != \"\"'> AND created_at &lt;= #{dateTo}</if>" +
            " ORDER BY created_at DESC" +
            " LIMIT #{size} OFFSET #{offset}" +
            "</script>")
    List<AuditLog> searchAuditLogs(@Param("operator") String operator,
                                   @Param("action") String action,
                                   @Param("dateFrom") String dateFrom,
                                   @Param("dateTo") String dateTo,
                                   @Param("size") int size,
                                   @Param("offset") int offset);

    @Select("<script>" +
            "SELECT COUNT(*) FROM audit_log WHERE 1=1" +
            "<if test='operator != null and operator != \"\"'> AND operator LIKE CONCAT('%', #{operator}, '%')</if>" +
            "<if test='action != null and action != \"\"'> AND action LIKE CONCAT('%', #{action}, '%')</if>" +
            "<if test='dateFrom != null and dateFrom != \"\"'> AND created_at &gt;= #{dateFrom}</if>" +
            "<if test='dateTo != null and dateTo != \"\"'> AND created_at &lt;= #{dateTo}</if>" +
            "</script>")
    long countAuditLogs(@Param("operator") String operator,
                        @Param("action") String action,
                        @Param("dateFrom") String dateFrom,
                        @Param("dateTo") String dateTo);

    @Delete("DELETE FROM audit_log WHERE created_at < #{beforeDate}")
    int deleteOldLogs(@Param("beforeDate") String beforeDate);
}
