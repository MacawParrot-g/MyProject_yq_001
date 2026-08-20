package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.example.entity.TestStatic;

import java.util.List;

@Mapper
public interface GeneranMapper extends BaseMapper<TestStatic> {
    @Select("SELECT COUNT(*) FROM test_static WHERE isOutput = 0")
    int countNotExported();

    @Update("UPDATE test_static SET isOutput = 1 WHERE isOutput = 0")
    int markAllAsExported();

    @Select("SELECT * FROM test_static WHERE ascribe = #{ascribe}")
    List<TestStatic> obtainEventByAscribe(String ascribe);

    @Select("SELECT * FROM test_static WHERE remark LIKE CONCAT('%', #{remark}, '%')")
    List<TestStatic> obtainEventByStatus(String remark);

    @Select("SELECT * FROM test_static WHERE event_number IS NOT NULL AND ascribe IS NOT NULL AND (remark IS NULL OR remark NOT LIKE '%已冻结%')")
    List<TestStatic> selectForDedupWarmup();

    @Select("SELECT * FROM test_static WHERE isOutput = 0")
    List<TestStatic> selectUnexported();

    @Select("<script>" +
            "SELECT * FROM test_static WHERE 1=1" +
            "<if test='ascribe != null and ascribe != \"\"'> AND ascribe LIKE CONCAT('%', #{ascribe}, '%')</if>" +
            "<if test='frozenOnly'> AND remark LIKE '%已冻结%'</if>" +
            "</script>")
    List<TestStatic> selectByCondition(@Param("ascribe") String ascribe, @Param("frozenOnly") boolean frozenOnly);

    @Insert("INSERT INTO test_static (URL, bundleId, ascribe, event_number, exception_type, record_data, recorder, remark, isOutput) " +
            "VALUES (#{url}, #{bundleId}, #{ascribe}, #{eventNumber}, #{exceptionType}, #{recordData}, #{recorder}, #{remark}, #{isOutput})")
    int insertRecord(TestStatic record);

    @Update("<script>" +
            "UPDATE test_static" +
            "<set>" +
            "<if test='bundleId != null'>bundleId = #{bundleId},</if>" +
            "<if test='ascribe != null'>ascribe = #{ascribe},</if>" +
            "<if test='eventNumber != null'>event_number = #{eventNumber},</if>" +
            "<if test='exceptionType != null'>exception_type = #{exceptionType},</if>" +
            "<if test='recordData != null'>record_data = #{recordData},</if>" +
            "<if test='recorder != null'>recorder = #{recorder},</if>" +
            "<if test='remark != null'>remark = #{remark},</if>" +
            "<if test='isOutput != null'>isOutput = #{isOutput},</if>" +
            "</set>" +
            " WHERE URL = #{url}" +
            "</script>")
    int updateRecord(TestStatic record);

    @Delete("DELETE FROM test_static WHERE URL = #{url}")
    int deleteByURL(String url);

    @Select("SELECT COUNT(*) FROM test_static WHERE recorder = #{recorder} AND record_data = #{record_data}")
    int count(String recorder,String record_data);

    @Select("SELECT * FROM test_static WHERE isOutput = 0 AND recorder = #{recorder}")
    List<TestStatic> selectUnexportedByRecorder(@Param("recorder") String recorder);

    @Select("SELECT COUNT(*) FROM test_static WHERE isOutput = 0 AND recorder = #{recorder}")
    int countUnexportedByRecorder(@Param("recorder") String recorder);

    @Update("UPDATE test_static SET isOutput = 1 WHERE isOutput = 0 AND recorder = #{recorder}")
    int markAsExportedByRecorder(@Param("recorder") String recorder);

    @Select("<script>" +
            "SELECT URL, bundleId, ascribe, event_number, exception_type, record_data, recorder, remark, isOutput " +
            "FROM test_static WHERE 1=1" +
            "<if test='ascribe != null and ascribe != \"\"'> AND ascribe = #{ascribe}</if>" +
            "<if test='frozenOnly'> AND remark LIKE '%已冻结%'</if>" +
            "<if test='recorder != null and recorder != \"\"'> AND recorder LIKE CONCAT('%', #{recorder}, '%')</if>" +
            " ORDER BY URL" +
            " LIMIT #{size} OFFSET #{offset}" +
            "</script>")
    List<TestStatic> selectByConditionPaged(@Param("ascribe") String ascribe,
                                            @Param("frozenOnly") boolean frozenOnly,
                                            @Param("recorder") String recorder,
                                            @Param("size") int size,
                                            @Param("offset") int offset);

    @Select("<script>" +
            "SELECT COUNT(*) FROM test_static WHERE 1=1" +
            "<if test='ascribe != null and ascribe != \"\"'> AND ascribe = #{ascribe}</if>" +
            "<if test='frozenOnly'> AND remark LIKE '%已冻结%'</if>" +
            "<if test='recorder != null and recorder != \"\"'> AND recorder LIKE CONCAT('%', #{recorder}, '%')</if>" +
            "</script>")
    long countByCondition(@Param("ascribe") String ascribe,
                          @Param("frozenOnly") boolean frozenOnly,
                          @Param("recorder") String recorder);

    @Select("<script>" +
            "SELECT URL, bundleId FROM test_static WHERE record_data IN" +
            "<foreach collection='dates' item='d' open='(' separator=',' close=')'>" +
            "#{d}" +
            "</foreach>" +
            " ORDER BY RAND() LIMIT 1" +
            "</script>")
    TestStatic selectRandomByDates(@Param("dates") List<String> dates);


}
