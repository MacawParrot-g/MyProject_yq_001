package org.example.controller;

import org.example.annotation.LogExecutionTime;
import org.example.common.Result;
import org.example.service.DatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/record")
public class AdminRecordController {

    @Autowired
    private DatabaseService databaseService;

    @PostMapping("/search")
    @LogExecutionTime("管理员高级搜索")
    public Result search(@RequestBody Map<String, Object> params) {
        try {
            String dateFrom = (String) params.get("dateFrom");
            String dateTo = (String) params.get("dateTo");
            String bundleId = (String) params.get("bundleId");
            String keyword = (String) params.get("keyword");
            String exceptionType = (String) params.get("exceptionType");
            String ascribe = (String) params.get("ascribe");
            boolean frozenOnly = Boolean.TRUE.equals(params.get("frozenOnly"));
            String recorder = (String) params.get("recorder");
            Integer isOutput = params.get("isOutput") != null ? ((Number) params.get("isOutput")).intValue() : null;
            int page = params.get("page") != null ? ((Number) params.get("page")).intValue() : 1;
            int size = params.get("size") != null ? ((Number) params.get("size")).intValue() : 20;
            return databaseService.adminSearch(dateFrom, dateTo, bundleId, keyword, exceptionType, ascribe, frozenOnly, recorder, isOutput, page, size);
        } catch (Exception e) {
            return Result.fail("查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/stats")
    @LogExecutionTime("管理员统计数据")
    public Result stats() {
        try {
            return databaseService.adminStats();
        } catch (Exception e) {
            return Result.fail("查询统计失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/batch-delete")
    @LogExecutionTime("管理员批量删除")
    public Result batchDelete(@RequestBody Map<String, List<String>> body) {
        try {
            List<String> hashes = body.get("hashes");
            return databaseService.adminBatchDelete(hashes);
        } catch (Exception e) {
            return Result.fail("批量删除失败：" + e.getMessage());
        }
    }

    @PostMapping("/summary")
    @LogExecutionTime("管理员质量统计")
    public Result summary(@RequestBody Map<String, Object> params) {
        try {
            String dateFrom = (String) params.get("dateFrom");
            String dateTo = (String) params.get("dateTo");
            String bundleId = (String) params.get("bundleId");
            String keyword = (String) params.get("keyword");
            String exceptionType = (String) params.get("exceptionType");
            String ascribe = (String) params.get("ascribe");
            boolean frozenOnly = Boolean.TRUE.equals(params.get("frozenOnly"));
            String recorder = (String) params.get("recorder");
            Integer isOutput = params.get("isOutput") != null ? ((Number) params.get("isOutput")).intValue() : null;
            return databaseService.adminSummary(dateFrom, dateTo, bundleId, keyword, exceptionType, ascribe, frozenOnly, recorder, isOutput);
        } catch (Exception e) {
            return Result.fail("查询统计失败：" + e.getMessage());
        }
    }

    @PostMapping("/batch-import")
    @LogExecutionTime("管理员批量导入")
    public Result batchImport(@RequestBody Map<String, String> body) {
        try {
            String rawText = body.get("rawText");
            return databaseService.batchImportRecords(rawText);
        } catch (Exception e) {
            return Result.fail("批量导入失败：" + e.getMessage());
        }
    }
}
