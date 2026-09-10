package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.example.entity.ScheduledTask;

import java.util.List;

@Mapper
public interface ScheduledTaskMapper extends BaseMapper<ScheduledTask> {

    @Select("SELECT * FROM scheduled_task ORDER BY created_at DESC")
    List<ScheduledTask> selectAll();

    @Select("SELECT * FROM scheduled_task WHERE enabled = 1")
    List<ScheduledTask> selectEnabled();

    @Select("SELECT * FROM scheduled_task WHERE id = #{id}")
    ScheduledTask selectById(@Param("id") String id);

    @Insert("INSERT INTO scheduled_task (id, name, task_type, cron_expression, params, enabled, created_at, created_by) " +
            "VALUES (#{id}, #{name}, #{taskType}, #{cronExpression}, #{params}, #{enabled}, #{createdAt}, #{createdBy})")
    int insertTask(ScheduledTask task);

    @Update("UPDATE scheduled_task SET enabled = #{enabled} WHERE id = #{id}")
    int updateEnabled(@Param("id") String id, @Param("enabled") boolean enabled);

    @Update("UPDATE scheduled_task SET last_executed_at = #{lastExecutedAt}, last_result = #{lastResult}, " +
            "total_executions = total_executions + 1 WHERE id = #{id}")
    int updateExecution(@Param("id") String id,
                        @Param("lastExecutedAt") String lastExecutedAt,
                        @Param("lastResult") String lastResult);

    @Delete("DELETE FROM scheduled_task WHERE id = #{id}")
    int deleteById(@Param("id") String id);
}
