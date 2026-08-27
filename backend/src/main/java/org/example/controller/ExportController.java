package org.example.controller;

import org.example.annotation.LogExecutionTime;
import org.example.common.Result;
import org.example.service.ExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/execute")
    @LogExecutionTime("执行数据导出")
    public Result executeExport(@RequestParam String recorder) {
        return exportService.executeExport(recorder);
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
}
