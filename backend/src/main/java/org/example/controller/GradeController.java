package org.example.controller;

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
    public Result queryGrade(@RequestParam String url) {
        Result result = gradeService.getGradeByUrl(url);
        boolean graded = gradeService.isUrlGraded(url);
        if (result.isSuccess() && result.getData() != null) {
            Map<String, Object> wrapper = new LinkedHashMap<>();
            wrapper.put("grade", ((org.example.entity.Grade) result.getData()).getGrade());
            wrapper.put("recorder", ((org.example.entity.Grade) result.getData()).getRecorder());
            wrapper.put("remark", ((org.example.entity.Grade) result.getData()).getRemark());
            wrapper.put("urlAlreadyGraded", graded);
            return Result.success(result.getMessage(), wrapper);
        }
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("urlAlreadyGraded", graded);
        return Result.success(result.getMessage(), graded ? wrapper : null);
    }

    @PostMapping("/save")
    @LogExecutionTime("保存应用评级")
    public Result saveGrade(@RequestBody Map<String, String> body) {
        String url = body.get("url");
        String grade = body.get("grade");
        String recorder = body.get("recorder");
        String remark = body.get("remark");
        return gradeService.saveGrade(url, grade, recorder, remark);
    }
}
