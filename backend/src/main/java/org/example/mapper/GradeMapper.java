package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.example.entity.Grade;

import java.util.List;

@Mapper
public interface GradeMapper extends BaseMapper<Grade> {

    @Select("SELECT * FROM grade WHERE URL = #{url}")
    Grade selectByUrl(@Param("url") String url);

    @Insert("INSERT INTO grade (URL, grade, recorder, remark) VALUES (#{url}, #{grade}, #{recorder}, #{remark})")
    int insertGrade(Grade grade);

    @Update("UPDATE grade SET grade = #{grade}, recorder = #{recorder}, remark = #{remark} WHERE URL = #{url}")
    int updateGrade(Grade grade);

    @Select("SELECT * FROM grade")
    List<Grade> selectAll();

    @Delete("DELETE FROM grade WHERE URL = #{url}")
    int deleteByUrl(@Param("url") String url);
}
