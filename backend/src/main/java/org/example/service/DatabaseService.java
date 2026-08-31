package org.example.service;
import org.example.common.Result;
import org.example.entity.TestStatic;

import java.util.List;

public interface DatabaseService {
    int insertRecord(TestStatic record);

    void submitRecordAsync(TestStatic record);

    void submitUpdateAsync(TestStatic record);

    void submitDeleteAsync(String hash);

    List<TestStatic> queryByViewType(String ascribe, boolean frozenOnly);

    List<TestStatic> getUnexportedRecords();

    int updateRecord(TestStatic record);

    int deleteRecord(String hash);

    Result getNumberByName(TestStatic record);


    // ... existing code ...

    Result queryByPage(String ascribe, boolean frozenOnly, String recorder, String dateFrom, String dateTo, int page, int size);

    // ... existing code ...


    Result getRandomRecordForRetest(List<String> dates);

    Result getDailyReport(String recordData);

    Result adminSearch(String dateFrom, String dateTo, String bundleId, String keyword,
                       String exceptionType, String ascribe, boolean frozenOnly,
                       String recorder, Integer isOutput, int page, int size);

    Result adminStats();

    Result adminBatchDelete(List<String> hashes);

    Result adminSummary(String dateFrom, String dateTo, String bundleId, String keyword,
                        String exceptionType, String ascribe, boolean frozenOnly,
                        String recorder, Integer isOutput);

    Result batchImportRecords(String rawText);

}