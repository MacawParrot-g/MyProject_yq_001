package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.example.entity.Grade;

import java.util.List;

@Mapper
public interface GradeMapper extends BaseMapper<Grade> {

    @Select("SELECT * FROM grade_bundle WHERE bundleId = #{bundleId}")
    Grade selectByBundleId(@Param("bundleId") String bundleId);

    @Insert("INSERT INTO grade_bundle (bundleId, grade, recorder, remark) VALUES (#{bundleId}, #{grade}, #{recorder}, #{remark})")
    int insertGrade(Grade grade);

    @Update("UPDATE grade_bundle SET grade = #{grade}, recorder = #{recorder}, remark = #{remark} WHERE bundleId = #{bundleId}")
    int updateGrade(Grade grade);

    @Select("SELECT * FROM grade_bundle")
    List<Grade> selectAll();

    @Delete("DELETE FROM grade_bundle WHERE bundleId = #{bundleId}")
    int deleteByBundleId(@Param("bundleId") String bundleId);

    @Select("<script>" +
            "SELECT * FROM grade_bundle WHERE 1=1" +
            "<if test='grade != null and grade != \"\"'> AND grade = #{grade}</if>" +
            "<if test='recorder != null and recorder != \"\"'> AND recorder LIKE CONCAT('%', #{recorder}, '%')</if>" +
            "<if test='keyword != null and keyword != \"\"'> AND remark LIKE CONCAT('%', #{keyword}, '%')</if>" +
            " ORDER BY grade ASC, bundleId DESC" +
            " LIMIT #{size} OFFSET #{offset}" +
            "</script>")
    List<Grade> searchGrades(@Param("grade") String grade,
                             @Param("recorder") String recorder,
                             @Param("keyword") String keyword,
                             @Param("size") int size,
                             @Param("offset") int offset);

    @Select("<script>" +
            "SELECT COUNT(*) FROM grade_bundle WHERE 1=1" +
            "<if test='grade != null and grade != \"\"'> AND grade = #{grade}</if>" +
            "<if test='recorder != null and recorder != \"\"'> AND recorder LIKE CONCAT('%', #{recorder}, '%')</if>" +
            "<if test='keyword != null and keyword != \"\"'> AND remark LIKE CONCAT('%', #{keyword}, '%')</if>" +
            "</script>")
    long countSearch(@Param("grade") String grade,
                     @Param("recorder") String recorder,
                     @Param("keyword") String keyword);
}
