package org.example.service;

import org.example.common.Result;

public interface GradeService {
    Result getGradeByBundleId(String bundleId);
    Result saveGrade(String bundleId, String grade, String recorder, String remark);
    void warmUpGradeCache();
    boolean isBundleIdGraded(String bundleId);
    Result searchGrades(String grade, String recorder, String keyword, int page, int size);
    Result deleteGrade(String bundleId);
    Result updateGrade(String bundleId, String grade, String remark);
}
