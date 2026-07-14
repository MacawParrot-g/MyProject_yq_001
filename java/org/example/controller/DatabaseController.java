package org.example.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.common.DataViewType;
import org.example.common.Result;
import org.example.entity.TestStatic;
import org.example.mapper.GeneranMapper;
import org.example.service.DatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class DatabaseController {

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private GeneranMapper generanMapper;

    @PostMapping("/api/record/insert")
    public Map<String, Object> insertRecord(@RequestBody TestStatic record) {
        try {
            if (record.getIsOutput() == null) {
                record.setIsOutput(0);
            }
            int rows = databaseService.insertRecord(record);
            return Map.of("success", rows > 0, "resultMsg", rows > 0 ? "入库成功" : "入库失败");
        } catch (Exception e) {
            return Map.of("success", false, "resultMsg", "入库失败：" + e.getMessage());
        }
    }

    @GetMapping("/api/record/unexported")
    public Map<String, Object> getUnexportedRecords() {
        try {
            List<TestStatic> list = generanMapper.selectList(
                    new LambdaQueryWrapper<TestStatic>().eq(TestStatic::getIsOutput, 0)
            );
            return Map.of("success", true, "data", list, "total", list.size());
        } catch (Exception e) {
            return Map.of("success", false, "resultMsg", "查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/api/record/list")
    public Result getRecordList(
            @RequestParam(required = false) String ascribe,
            @RequestParam(required = false, defaultValue = "false") boolean frozen) {
        try {
            List<TestStatic> list = databaseService.queryByViewType(ascribe, frozen);

            DataViewType viewType = DataViewType.ALL;
            boolean hasAscribe = ascribe != null && !ascribe.isEmpty();

            if (hasAscribe && frozen) {
                try {
                    viewType = DataViewType.valueOf(ascribe.toUpperCase() + "_FROZEN");
                } catch (IllegalArgumentException e) {
                    viewType = DataViewType.FROZEN;
                }
            } else if (frozen) {
                viewType = DataViewType.FROZEN;
            } else if (hasAscribe) {
                try {
                    viewType = DataViewType.valueOf(ascribe.toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            }

            return Result.success("查询成功，共 " + list.size() + " 条", list, viewType);
        } catch (Exception e) {
            return Result.fail("查询失败：" + e.getMessage());
        }
    }

    @PutMapping("/api/record/update")
    public Result updateRecord(@RequestBody TestStatic record) {
        try {
            int rows = databaseService.updateRecord(record);
            return rows > 0 ? Result.success("更新成功", null) : Result.fail("更新失败，未找到对应记录");
        } catch (Exception e) {
            return Result.fail("更新失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/api/record/delete")
    public Result deleteRecord(@RequestParam String url) {
        try {
            int rows = databaseService.deleteRecord(url);
            return rows > 0 ? Result.success("删除成功", null) : Result.fail("删除失败，未找到对应记录");
        } catch (Exception e) {
            return Result.fail("删除失败：" + e.getMessage());
        }
    }
}
