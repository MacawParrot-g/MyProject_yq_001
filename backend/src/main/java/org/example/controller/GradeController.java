package org.example.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.example.annotation.LogExecutionTime;
import org.example.common.Result;
import org.example.service.GradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/grade")
public class GradeController {

    @Autowired
    private GradeService gradeService;

    @GetMapping("/query")
    @LogExecutionTime("查询应用评级")
    public Result queryGrade(@RequestParam String bundleId) {
        Result result = gradeService.getGradeByBundleId(bundleId);
        boolean graded = gradeService.isBundleIdGraded(bundleId);
        if (result.isSuccess() && result.getData() != null) {
            Map<String, Object> wrapper = new LinkedHashMap<>();
            wrapper.put("grade", ((org.example.entity.Grade) result.getData()).getGrade());
            wrapper.put("recorder", ((org.example.entity.Grade) result.getData()).getRecorder());
            wrapper.put("remark", ((org.example.entity.Grade) result.getData()).getRemark());
            wrapper.put("bundleIdAlreadyGraded", graded);
            return Result.success(result.getMessage(), wrapper);
        }
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("bundleIdAlreadyGraded", graded);
        return Result.success(result.getMessage(), graded ? wrapper : null);
    }

    @PostMapping("/save")
    @LogExecutionTime("保存应用评级")
    public Result saveGrade(@RequestBody Map<String, String> body) {
        String bundleId = body.get("bundleId");
        String grade = body.get("grade");
        String recorder = body.get("recorder");
        String remark = body.get("remark");
        return gradeService.saveGrade(bundleId, grade, recorder, remark);
    }

    @PostMapping("/manage/search")
    @LogExecutionTime("评级管理-搜索")
    public Result search(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        if (!isDeveloper(request)) return Result.fail("仅开发者可访问评级管理");
        String bundleId = (String) body.get("bundleId");
        String grade = (String) body.get("grade");
        String recorder = (String) body.get("recorder");
        String keyword = (String) body.get("keyword");
        int page = body.get("page") != null ? ((Number) body.get("page")).intValue() : 1;
        int size = body.get("size") != null ? ((Number) body.get("size")).intValue() : 15;
        return gradeService.searchGrades(bundleId, grade, recorder, keyword, page, size);
    }

    @DeleteMapping("/manage/delete")
    @LogExecutionTime("评级管理-删除")
    public Result delete(HttpServletRequest request, @RequestParam String bundleId) {
        if (!isDeveloper(request)) return Result.fail("仅开发者可访问评级管理");
        return gradeService.deleteGrade(bundleId);
    }

    @PutMapping("/manage/update")
    @LogExecutionTime("评级管理-更新")
    public Result update(HttpServletRequest request, @RequestBody Map<String, String> body) {
        if (!isDeveloper(request)) return Result.fail("仅开发者可访问评级管理");
        String bundleId = body.get("bundleId");
        String grade = body.get("grade");
        String remark = body.get("remark");
        return gradeService.updateGrade(bundleId, grade, remark);
    }

    private boolean isDeveloper(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("type") == null) return false;
        return "DEVELOPER".equals(session.getAttribute("type"));
    }
}
