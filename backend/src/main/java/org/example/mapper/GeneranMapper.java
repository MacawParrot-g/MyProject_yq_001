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

    @Insert("INSERT INTO test_static (hash, URL, bundleId, ascribe, event_number, exception_type, record_data, recorder, remark, isOutput) " +
            "VALUES (#{hash}, #{url}, #{bundleId}, #{ascribe}, #{eventNumber}, #{exceptionType}, #{recordData}, #{recorder}, #{remark}, #{isOutput})")
    int insertRecord(TestStatic record);

    @Update("<script>" +
            "UPDATE test_static" +
            "<set>" +
            "<if test='url != null'>URL = #{url},</if>" +
            "<if test='bundleId != null'>bundleId = #{bundleId},</if>" +
            "<if test='ascribe != null'>ascribe = #{ascribe},</if>" +
            "<if test='eventNumber != null'>event_number = #{eventNumber},</if>" +
            "<if test='exceptionType != null'>exception_type = #{exceptionType},</if>" +
            "<if test='recordData != null'>record_data = #{recordData},</if>" +
            "<if test='recorder != null'>recorder = #{recorder},</if>" +
            "<if test='remark != null'>remark = #{remark},</if>" +
            "<if test='isOutput != null'>isOutput = #{isOutput},</if>" +
            "</set>" +
            " WHERE hash = #{hash}" +
            "</script>")
    int updateRecord(TestStatic record);

    @Delete("DELETE FROM test_static WHERE hash = #{hash}")
    int deleteByHash(@Param("hash") String hash);

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
            "SELECT hash, URL, bundleId, ascribe, event_number, exception_type, record_data, recorder, remark, isOutput " +
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

    @Select("SELECT COUNT(*) FROM test_static WHERE record_data = #{recordData}")
    int countByRecordData(@Param("recordData") String recordData);

    @Select("SELECT COUNT(*) FROM test_static WHERE record_data = #{recordData} AND ascribe IS NOT NULL AND ascribe != ''")
    int countQualifiedByRecordData(@Param("recordData") String recordData);

    @Select("SELECT COUNT(*) FROM test_static WHERE record_data = #{recordData} AND ascribe IS NOT NULL AND FIND_IN_SET('appflyer', REPLACE(ascribe, ';', ','))")
    int countAppflyerByDate(@Param("recordData") String recordData);

    @Select("SELECT COUNT(*) FROM test_static WHERE record_data = #{recordData} AND ascribe IS NOT NULL AND FIND_IN_SET('adjust', REPLACE(ascribe, ';', ','))")
    int countAdjustByDate(@Param("recordData") String recordData);

    @Select("SELECT COUNT(*) FROM test_static WHERE record_data = #{recordData} AND ascribe IS NOT NULL AND FIND_IN_SET('singular', REPLACE(ascribe, ';', ','))")
    int countSingularByDate(@Param("recordData") String recordData);

    @Select("SELECT COUNT(*) FROM test_static WHERE record_data = #{recordData} AND ascribe IS NOT NULL AND FIND_IN_SET('tenjin', REPLACE(ascribe, ';', ','))")
    int countTenjinByDate(@Param("recordData") String recordData);

    @Select("<script>" +
            "SELECT hash, URL, bundleId, ascribe, event_number, exception_type, record_data, recorder, remark, isOutput " +
            "FROM test_static WHERE 1=1" +
            "<if test='dateFrom != null and dateFrom != \"\"'> AND STR_TO_DATE(REPLACE(record_data, '/', '-'), '%Y-%m-%d') &gt;= STR_TO_DATE(REPLACE(#{dateFrom}, '/', '-'), '%Y-%m-%d')</if>" +
            "<if test='dateTo != null and dateTo != \"\"'> AND STR_TO_DATE(REPLACE(record_data, '/', '-'), '%Y-%m-%d') &lt;= STR_TO_DATE(REPLACE(#{dateTo}, '/', '-'), '%Y-%m-%d')</if>" +
            "<if test='bundleId != null and bundleId != \"\"'> AND bundleId = #{bundleId}</if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (URL LIKE CONCAT('%', #{keyword}, '%') OR bundleId LIKE CONCAT('%', #{keyword}, '%') OR remark LIKE CONCAT('%', #{keyword}, '%'))</if>" +
            "<if test='exceptionType != null and exceptionType != \"\"'> AND exception_type = #{exceptionType}</if>" +
            "<if test='ascribe != null and ascribe != \"\"'> AND ascribe LIKE CONCAT('%', #{ascribe}, '%')</if>" +
            "<if test='frozenOnly'> AND remark LIKE '%已冻结%'</if>" +
            "<if test='recorder != null and recorder != \"\"'> AND recorder LIKE CONCAT('%', #{recorder}, '%')</if>" +
            "<if test='isOutput != null'> AND isOutput = #{isOutput}</if>" +
            " ORDER BY URL DESC" +
            " LIMIT #{size} OFFSET #{offset}" +
            "</script>")
    List<TestStatic> adminSearch(@Param("dateFrom") String dateFrom,
                                 @Param("dateTo") String dateTo,
                                 @Param("bundleId") String bundleId,
                                 @Param("keyword") String keyword,
                                 @Param("exceptionType") String exceptionType,
                                 @Param("ascribe") String ascribe,
                                 @Param("frozenOnly") boolean frozenOnly,
                                 @Param("recorder") String recorder,
                                 @Param("isOutput") Integer isOutput,
                                 @Param("size") int size,
                                 @Param("offset") int offset);

    @Select("<script>" +
            "SELECT COUNT(*) FROM test_static WHERE 1=1" +
            "<if test='dateFrom != null and dateFrom != \"\"'> AND STR_TO_DATE(REPLACE(record_data, '/', '-'), '%Y-%m-%d') &gt;= STR_TO_DATE(REPLACE(#{dateFrom}, '/', '-'), '%Y-%m-%d')</if>" +
            "<if test='dateTo != null and dateTo != \"\"'> AND STR_TO_DATE(REPLACE(record_data, '/', '-'), '%Y-%m-%d') &lt;= STR_TO_DATE(REPLACE(#{dateTo}, '/', '-'), '%Y-%m-%d')</if>" +
            "<if test='bundleId != null and bundleId != \"\"'> AND bundleId = #{bundleId}</if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (URL LIKE CONCAT('%', #{keyword}, '%') OR bundleId LIKE CONCAT('%', #{keyword}, '%') OR remark LIKE CONCAT('%', #{keyword}, '%'))</if>" +
            "<if test='exceptionType != null and exceptionType != \"\"'> AND exception_type = #{exceptionType}</if>" +
            "<if test='ascribe != null and ascribe != \"\"'> AND ascribe LIKE CONCAT('%', #{ascribe}, '%')</if>" +
            "<if test='frozenOnly'> AND remark LIKE '%已冻结%'</if>" +
            "<if test='recorder != null and recorder != \"\"'> AND recorder LIKE CONCAT('%', #{recorder}, '%')</if>" +
            "<if test='isOutput != null'> AND isOutput = #{isOutput}</if>" +
            "</script>")
    long adminSearchCount(@Param("dateFrom") String dateFrom,
                          @Param("dateTo") String dateTo,
                          @Param("bundleId") String bundleId,
                          @Param("keyword") String keyword,
                          @Param("exceptionType") String exceptionType,
                          @Param("ascribe") String ascribe,
                          @Param("frozenOnly") boolean frozenOnly,
                          @Param("recorder") String recorder,
                          @Param("isOutput") Integer isOutput);

    @Select("SELECT COUNT(*) FROM test_static")
    long countAll();

    @Select("SELECT COUNT(*) FROM test_static WHERE isOutput = 1")
    long countExported();

    @Select("SELECT COUNT(*) FROM test_static WHERE remark LIKE '%已冻结%'")
    long countFrozen();

    @Select("SELECT DISTINCT exception_type FROM test_static WHERE exception_type IS NOT NULL AND exception_type != '' ORDER BY exception_type")
    List<String> selectDistinctExceptionTypes();

    @Select("SELECT DISTINCT recorder FROM test_static WHERE recorder IS NOT NULL AND recorder != '' ORDER BY recorder")
    List<String> selectDistinctRecorders();

    @Select("SELECT COUNT(*) FROM test_static WHERE record_data = #{recordData} AND exception_type = #{exceptionType}")
    int countByDateAndException(@Param("recordData") String recordData, @Param("exceptionType") String exceptionType);

    @Select("<script>" +
            "SELECT COUNT(*) FROM test_static WHERE 1=1" +
            " AND ascribe IS NOT NULL AND ascribe != ''" +
            "<if test='dateFrom != null and dateFrom != \"\"'> AND STR_TO_DATE(REPLACE(record_data, '/', '-'), '%Y-%m-%d') &gt;= STR_TO_DATE(REPLACE(#{dateFrom}, '/', '-'), '%Y-%m-%d')</if>" +
            "<if test='dateTo != null and dateTo != \"\"'> AND STR_TO_DATE(REPLACE(record_data, '/', '-'), '%Y-%m-%d') &lt;= STR_TO_DATE(REPLACE(#{dateTo}, '/', '-'), '%Y-%m-%d')</if>" +
            "<if test='bundleId != null and bundleId != \"\"'> AND bundleId = #{bundleId}</if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (URL LIKE CONCAT('%', #{keyword}, '%') OR bundleId LIKE CONCAT('%', #{keyword}, '%') OR remark LIKE CONCAT('%', #{keyword}, '%'))</if>" +
            "<if test='exceptionType != null and exceptionType != \"\"'> AND exception_type = #{exceptionType}</if>" +
            "<if test='ascribe != null and ascribe != \"\"'> AND ascribe LIKE CONCAT('%', #{ascribe}, '%')</if>" +
            "<if test='frozenOnly'> AND remark LIKE '%已冻结%'</if>" +
            "<if test='recorder != null and recorder != \"\"'> AND recorder LIKE CONCAT('%', #{recorder}, '%')</if>" +
            "<if test='isOutput != null'> AND isOutput = #{isOutput}</if>" +
            "</script>")
    long adminSearchQualifiedCount(@Param("dateFrom") String dateFrom,
                                   @Param("dateTo") String dateTo,
                                   @Param("bundleId") String bundleId,
                                   @Param("keyword") String keyword,
                                   @Param("exceptionType") String exceptionType,
                                   @Param("ascribe") String ascribe,
                                   @Param("frozenOnly") boolean frozenOnly,
                                   @Param("recorder") String recorder,
                                   @Param("isOutput") Integer isOutput);

    @Select("<script>" +
            "SELECT COUNT(*) FROM test_static WHERE 1=1" +
            " AND ascribe IS NOT NULL AND ascribe != ''" +
            " AND ascribe LIKE CONCAT('%', #{attrType}, '%')" +
            "<if test='dateFrom != null and dateFrom != \"\"'> AND STR_TO_DATE(REPLACE(record_data, '/', '-'), '%Y-%m-%d') &gt;= STR_TO_DATE(REPLACE(#{dateFrom}, '/', '-'), '%Y-%m-%d')</if>" +
            "<if test='dateTo != null and dateTo != \"\"'> AND STR_TO_DATE(REPLACE(record_data, '/', '-'), '%Y-%m-%d') &lt;= STR_TO_DATE(REPLACE(#{dateTo}, '/', '-'), '%Y-%m-%d')</if>" +
            "<if test='bundleId != null and bundleId != \"\"'> AND bundleId = #{bundleId}</if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (URL LIKE CONCAT('%', #{keyword}, '%') OR bundleId LIKE CONCAT('%', #{keyword}, '%') OR remark LIKE CONCAT('%', #{keyword}, '%'))</if>" +
            "<if test='exceptionType != null and exceptionType != \"\"'> AND exception_type = #{exceptionType}</if>" +
            "<if test='ascribe != null and ascribe != \"\"'> AND ascribe LIKE CONCAT('%', #{ascribe}, '%')</if>" +
            "<if test='frozenOnly'> AND remark LIKE '%已冻结%'</if>" +
            "<if test='recorder != null and recorder != \"\"'> AND recorder LIKE CONCAT('%', #{recorder}, '%')</if>" +
            "<if test='isOutput != null'> AND isOutput = #{isOutput}</if>" +
            "</script>")
    long adminSearchAttrCount(@Param("attrType") String attrType,
                              @Param("dateFrom") String dateFrom,
                              @Param("dateTo") String dateTo,
                              @Param("bundleId") String bundleId,
                              @Param("keyword") String keyword,
                              @Param("exceptionType") String exceptionType,
                              @Param("ascribe") String ascribe,
                              @Param("frozenOnly") boolean frozenOnly,
                              @Param("recorder") String recorder,
                              @Param("isOutput") Integer isOutput);

    @Select("SELECT hash FROM test_static WHERE hash IS NOT NULL AND hash != ''")
    List<String> selectAllHashes();

    @Insert("<script>" +
            "INSERT INTO test_static (hash, URL, bundleId, ascribe, event_number, exception_type, record_data, recorder, remark, isOutput) VALUES " +
            "<foreach collection='records' item='r' separator=','>" +
            "(#{r.hash}, #{r.url}, #{r.bundleId}, #{r.ascribe}, #{r.eventNumber}, #{r.exceptionType}, #{r.recordData}, #{r.recorder}, #{r.remark}, #{r.isOutput})" +
            "</foreach>" +
            "</script>")
    int batchInsertRecords(@Param("records") List<TestStatic> records);

    @Select("<script>" +
            "SELECT hash, URL, bundleId, ascribe, event_number, exception_type, record_data, recorder, remark, isOutput " +
            "FROM test_static WHERE 1=1" +
            "<if test='ascribe != null and ascribe != \"\"'> AND ascribe LIKE CONCAT('%', #{ascribe}, '%')</if>" +
            "<if test='frozenOnly'> AND remark LIKE '%已冻结%'</if>" +
            "<if test='recorder != null and recorder != \"\"'> AND recorder LIKE CONCAT('%', #{recorder}, '%')</if>" +
            "<if test='recordData != null and recordData != \"\"'> AND record_data = #{recordData}</if>" +
            " ORDER BY URL" +
            " LIMIT #{size} OFFSET #{offset}" +
            "</script>")
    List<TestStatic> selectByConditionPaged(@Param("ascribe") String ascribe,
                                            @Param("frozenOnly") boolean frozenOnly,
                                            @Param("recorder") String recorder,
                                            @Param("recordData") String recordData,
                                            @Param("size") int size,
                                            @Param("offset") int offset);

    @Select("<script>" +
            "SELECT COUNT(*) FROM test_static WHERE 1=1" +
            "<if test='ascribe != null and ascribe != \"\"'> AND ascribe LIKE CONCAT('%', #{ascribe}, '%')</if>" +
            "<if test='frozenOnly'> AND remark LIKE '%已冻结%'</if>" +
            "<if test='recorder != null and recorder != \"\"'> AND recorder LIKE CONCAT('%', #{recorder}, '%')</if>" +
            "<if test='recordData != null and recordData != \"\"'> AND record_data = #{recordData}</if>" +
            "</script>")
    long countByCondition(@Param("ascribe") String ascribe,
                          @Param("frozenOnly") boolean frozenOnly,
                          @Param("recorder") String recorder,
                          @Param("recordData") String recordData);

    @Select("SELECT * FROM test_static WHERE isOutput = 0 AND recorder = #{recorder} AND record_data = #{recordData}")
    List<TestStatic> selectUnexportedByDateAndRecorder(@Param("recorder") String recorder, @Param("recordData") String recordData);

    @Select("SELECT COUNT(*) FROM test_static WHERE isOutput = 0 AND recorder = #{recorder} AND record_data = #{recordData}")
    int countUnexportedByDateAndRecorder(@Param("recorder") String recorder, @Param("recordData") String recordData);

    @Select("<script>" +
            "SELECT * FROM test_static WHERE hash IN" +
            "<foreach collection='hashes' item='h' open='(' separator=',' close=')'>" +
            "#{h}" +
            "</foreach>" +
            "</script>")
    List<TestStatic> selectByHashes(@Param("hashes") List<String> hashes);

    @Update("<script>" +
            "UPDATE test_static SET isOutput = 1 WHERE hash IN" +
            "<foreach collection='hashes' item='h' open='(' separator=',' close=')'>" +
            "#{h}" +
            "</foreach>" +
            "</script>")
    int markAsExportedByHashes(@Param("hashes") List<String> hashes);




}