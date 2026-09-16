// 文件路径: src/main/java/org/example/controller/TableManageController.java
package org.example.controller;

import org.example.annotation.LogExecutionTime;
import org.example.common.Result;
import org.example.service.TableManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dev/table")
public class TableManageController {

    @Autowired
    private TableManageService tableManageService;

    @GetMapping("/list")
    @LogExecutionTime("查询所有表")
    public Result listTables() {
        return tableManageService.listTables();
    }

    @GetMapping("/describe")
    @LogExecutionTime("查询表结构")
    public Result describeTable(@RequestParam String tableName) {
        return tableManageService.describeTable(tableName);
    }

    @PostMapping("/create")
    @LogExecutionTime("建表")
    public Result createTable(@RequestBody Map<String, String> body) {
        return tableManageService.createTable(body.get("tableName"), body.get("columnDefinitions"));
    }

    @DeleteMapping("/drop")
    @LogExecutionTime("删表")
    public Result dropTable(@RequestParam String tableName) {
        return tableManageService.dropTable(tableName);
    }

    @PostMapping("/column/add")
    @LogExecutionTime("新增字段")
    public Result addColumn(@RequestBody Map<String, String> body) {
        return tableManageService.addColumn(body.get("tableName"), body.get("columnDefinition"));
    }

    @PutMapping("/column/modify")
    @LogExecutionTime("修改字段")
    public Result modifyColumn(@RequestBody Map<String, String> body) {
        return tableManageService.modifyColumn(body.get("tableName"), body.get("columnDefinition"));
    }

    @DeleteMapping("/column/drop")
    @LogExecutionTime("删除字段")
    public Result dropColumn(@RequestBody Map<String, String> body) {
        return tableManageService.dropColumn(body.get("tableName"), body.get("columnName"));
    }

    @PostMapping("/execute-sql")
    @LogExecutionTime("执行SQL")
    public Result executeSQL(@RequestBody Map<String, String> body) {
        return tableManageService.executeSQL(body.get("sql"), body.get("tableName"));
    }

    @PostMapping("/batch-import")
    @LogExecutionTime("批量导入")
    public Result batchImport(@RequestBody Map<String, Object> body) {
        return tableManageService.batchImport(body);
    }
}
