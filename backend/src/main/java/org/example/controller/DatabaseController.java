package org.example.controller;

import org.example.annotation.LogExecutionTime;
import org.example.common.DataViewType;
import org.example.common.Result;
import org.example.entity.TestStatic;
import org.example.service.DatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class DatabaseController {

    @Autowired
    private DatabaseService databaseService;

    @GetMapping("/api/record/querybyname")
    @LogExecutionTime("按用户查询记录数")
    public Result getNumberByName(TestStatic record){
        return  databaseService.getNumberByName(record);
    }

    @PostMapping("/api/record/insert")
    @LogExecutionTime("数据入库")
    public Result insertRecord(@RequestBody TestStatic record) {
        try {
            if (record.getIsOutput() == null) {
                record.setIsOutput(0);
            }
            databaseService.submitRecordAsync(record);
            return Result.success("入库请求已接收，正在异步处理", null);
        } catch (Exception e) {
            return Result.fail("入库失败：" + e.getMessage());
        }
    }

    @GetMapping("/api/record/unexported")
    @LogExecutionTime("查询未导出记录")
    public Map<String, Object> getUnexportedRecords() {
        try {
            List<TestStatic> list = databaseService.getUnexportedRecords();
            return Map.of("success", true, "data", list, "total", list.size());
        } catch (Exception e) {
            return Map.of("success", false, "resultMsg", "查询失败：" + e.getMessage());
        }
    }

    @PutMapping("/api/record/update")
    @LogExecutionTime("数据更新")
    public Result updateRecord(@RequestBody TestStatic record) {
        try {
            databaseService.submitUpdateAsync(record);
            return Result.success("更新请求已接收，正在异步处理", null);
        } catch (Exception e) {
            return Result.fail("更新失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/api/record/delete")
    @LogExecutionTime("数据删除")
    public Result deleteRecord(@RequestParam String hash) {
        try {
            databaseService.submitDeleteAsync(hash);
            return Result.success("删除请求已接收，正在异步处理", null);
        } catch (Exception e) {
            return Result.fail("删除失败：" + e.getMessage());
        }
    }

    @PostMapping("/api/record/random-for-retest")
    @LogExecutionTime("随机获取复测数据")
    public Result getRandomRecordForRetest(@RequestBody Map<String, List<String>> body) {
        try {
            List<String> dates = body.get("dates");
            return databaseService.getRandomRecordForRetest(dates);
        } catch (Exception e) {
            return Result.fail("获取复测数据失败：" + e.getMessage());
        }
    }
    @GetMapping("/api/record/daily-report")
    @LogExecutionTime("查询日报统计")
    public Result getDailyReport(@RequestParam String recordData) {
        try {
            return databaseService.getDailyReport(recordData);
        } catch (Exception e) {
            return Result.fail("查询日报失败：" + e.getMessage());
        }
    }

    @GetMapping("/api/record/list")
    @LogExecutionTime("分页查询记录")
    public Result getRecordList(
            @RequestParam(required = false) String ascribe,
            @RequestParam(required = false, defaultValue = "false") boolean frozen,
            @RequestParam(required = false) String recorder,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "15") int size) {
        try {
            return databaseService.queryByPage(ascribe, frozen, recorder, dateFrom, dateTo, page, size);
        } catch (Exception e) {
            return Result.fail("查询失败：" + e.getMessage());
        }
    }
}

