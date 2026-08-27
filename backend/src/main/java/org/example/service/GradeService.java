package org.example.service;

import org.example.common.Result;

public interface GradeService {
    Result getGradeByUrl(String url);
    Result saveGrade(String url, String grade, String recorder, String remark);
    void warmUpGradeCache();
    boolean isUrlGraded(String url);
    Result searchGrades(String grade, String recorder, String keyword, int page, int size);
    Result deleteGrade(String url);
    Result updateGrade(String url, String grade, String remark);
}
