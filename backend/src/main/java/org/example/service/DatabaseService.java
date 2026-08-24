package org.example.service;
import org.example.common.Result;
import org.example.entity.TestStatic;

import java.util.List;

public interface DatabaseService {
    int insertRecord(TestStatic record);

    void submitRecordAsync(TestStatic record);

    void submitUpdateAsync(TestStatic record);

    void submitDeleteAsync(String url);

    List<TestStatic> queryByViewType(String ascribe, boolean frozenOnly);

    List<TestStatic> getUnexportedRecords();

    int updateRecord(TestStatic record);

    int deleteRecord(String url);

    Result getNumberByName(TestStatic record);

    Result queryByPage(String ascribe, boolean frozenOnly, String recorder, int page, int size);

    Result getRandomRecordForRetest(List<String> dates);

    Result getDailyReport(String recordData);

    Result adminSearch(String dateFrom, String dateTo, String bundleId, String keyword,
                       String exceptionType, String ascribe, boolean frozenOnly,
                       String recorder, Integer isOutput, int page, int size);

    Result adminStats();

    Result adminBatchDelete(List<String> urls);

    Result adminSummary(String dateFrom, String dateTo, String bundleId, String keyword,
                        String exceptionType, String ascribe, boolean frozenOnly,
                        String recorder, Integer isOutput);

}
