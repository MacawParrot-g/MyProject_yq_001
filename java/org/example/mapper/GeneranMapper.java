package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.entity.TestStatic;

import java.util.List;

@Mapper
public interface GeneranMapper extends BaseMapper<TestStatic> {
    @Select("SELECT COUNT(*) FROM test_static WHERE isOutput = 0")
    int countNotExported();

    @Update("UPDATE test_static SET isOutput = 1 WHERE isOutput = 0")
    int markAllAsExported();

    @Select("SELECT * FROM test_static WHERE ascribe = #{ascribe}")
    List<TestStatic> obtainEventByAscribe(String scribe);

    @Select("SELECT * FROM test_static WHERE remark LIKE CONCAT('%', #{remark}, '%')")
    List<TestStatic> obtainEventByStatus(String remark);


}
