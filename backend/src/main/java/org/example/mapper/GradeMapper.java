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

    @Select("<script>" +
            "SELECT * FROM grade WHERE 1=1" +
            "<if test='grade != null and grade != \"\"'> AND grade = #{grade}</if>" +
            "<if test='recorder != null and recorder != \"\"'> AND recorder LIKE CONCAT('%', #{recorder}, '%')</if>" +
            "<if test='keyword != null and keyword != \"\"'> AND remark LIKE CONCAT('%', #{keyword}, '%')</if>" +
            " ORDER BY grade ASC, URL DESC" +
            " LIMIT #{size} OFFSET #{offset}" +
            "</script>")
    List<Grade> searchGrades(@Param("grade") String grade,
                             @Param("recorder") String recorder,
                             @Param("keyword") String keyword,
                             @Param("size") int size,
                             @Param("offset") int offset);

    @Select("<script>" +
            "SELECT COUNT(*) FROM grade WHERE 1=1" +
            "<if test='grade != null and grade != \"\"'> AND grade = #{grade}</if>" +
            "<if test='recorder != null and recorder != \"\"'> AND recorder LIKE CONCAT('%', #{recorder}, '%')</if>" +
            "<if test='keyword != null and keyword != \"\"'> AND remark LIKE CONCAT('%', #{keyword}, '%')</if>" +
            "</script>")
    long countSearch(@Param("grade") String grade,
                     @Param("recorder") String recorder,
                     @Param("keyword") String keyword);
}
