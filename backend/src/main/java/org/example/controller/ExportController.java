package org.example.controller;

import org.example.annotation.LogExecutionTime;
import org.example.common.Result;
import org.example.service.ExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    @Autowired
    private ExportService exportService;

    @GetMapping("/unexported")
    @LogExecutionTime("查询未导出数据")
    public Result getUnexported(@RequestParam String recorder) {
        return exportService.getUnexportedByUser(recorder);
    }

    @PostMapping("/execute-all")
    @LogExecutionTime("导出所有未导出数据")
    public Result executeExportAll(@RequestParam String recorder) {
        return exportService.executeExportAll(recorder);
    }

    @GetMapping("/status")
    @LogExecutionTime("查询导出状态")
    public Result getStatus(@RequestParam String recorder) {
        return exportService.getExportStatus(recorder);
    }

    @GetMapping("/download")
    @LogExecutionTime("下载导出文件")
    public void download(@RequestParam String recorder, HttpServletResponse response) {
        exportService.downloadAndDelete(recorder, response);
    }

    @PostMapping("/execute-by-date")
    @LogExecutionTime("按日期导出数据")
    public Result executeExportByDate(@RequestParam String recorder, @RequestParam String date) {
        return exportService.executeExportByDate(recorder, date);
    }

    @PostMapping("/execute-by-hashes")
    @LogExecutionTime("按选中行导出数据")
    public Result executeExportByHashes(@RequestBody Map<String, Object> body) {
        String recorder = (String) body.get("recorder");
        @SuppressWarnings("unchecked")
        List<String> hashes = (List<String>) body.get("hashes");
        return exportService.executeExportByHashes(recorder, hashes);
    }


}
