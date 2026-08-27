package org.example.service.impl;

import org.example.common.Result;
import org.example.entity.Grade;
import org.example.mapper.GradeMapper;
import org.example.service.GradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GradeServiceImpl implements GradeService {

    private static final Logger log = LoggerFactory.getLogger(GradeServiceImpl.class);
    private static final String GRADE_URLS_KEY = "grade:urls";
    private static final String GRADE_RECORD_KEY = "grade:record";

    @Autowired
    private GradeMapper gradeMapper;

    @Autowired
    @Qualifier("gradeRedisTemplate")
    private RedisTemplate<String, Object> gradeRedisTemplate;

    @Override
    public Result getGradeByUrl(String url) {
        if (url == null || url.isEmpty()) {
            return Result.fail("URL不能为空");
        }
        try {
            Object cached = gradeRedisTemplate.opsForHash().get(GRADE_RECORD_KEY, url);
            if (cached != null) {
                String val = String.valueOf(cached);
                String[] parts = val.split("\\|", -1);
                Grade g = new Grade();
                g.setUrl(url);
                g.setGrade(parts.length > 0 ? parts[0] : "");
                g.setRecorder(parts.length > 1 ? parts[1] : "");
                g.setRemark(parts.length > 2 ? parts[2] : "");
                return Result.success("查询评级成功（缓存）", g);
            }
        } catch (Exception e) {
            log.warn("Redis评级缓存读取失败，回退MySQL: {}", e.getMessage());
        }

        Grade grade = gradeMapper.selectByUrl(url);
        if (grade != null) {
            cacheToRedis(grade);
            return Result.success("查询评级成功（数据库）", grade);
        }
        return Result.success("该应用暂无评级", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result saveGrade(String url, String grade, String recorder, String remark) {
        if (url == null || url.isEmpty()) return Result.fail("URL不能为空");
        if (grade == null || grade.isEmpty()) return Result.fail("请选择评级等级");

        Grade record = new Grade();
        record.setUrl(url);
        record.setGrade(grade);
        record.setRecorder(recorder);
        record.setRemark(remark);

        Grade existing = gradeMapper.selectByUrl(url);
        if (existing != null) {
            gradeMapper.updateGrade(record);
        } else {
            gradeMapper.insertGrade(record);
        }

        cacheToRedis(record);
        log.info("✅ 应用评级保存成功: URL={}, grade={}, recorder={}", url, grade, recorder);
        return Result.success("评级保存成功", record);
    }

    @Override
    public void warmUpGradeCache() {
        try {
            List<Grade> grades = gradeMapper.selectAll();
            if (grades == null || grades.isEmpty()) {
                log.info("ℹ️ 评级表为空，跳过Redis DB3预热");
                return;
            }
            int count = 0;
            for (Grade g : grades) {
                cacheToRedis(g);
                count++;
            }
            log.info("✅ Redis DB3 评级缓存预热完成，共加载 {} 条", count);
        } catch (Exception e) {
            log.error("❌ 评级缓存预热失败: {}", e.getMessage());
        }
    }

    @Override
    public boolean isUrlGraded(String url) {
        if (url == null || url.isEmpty()) return false;
        try {
            return Boolean.TRUE.equals(gradeRedisTemplate.opsForSet().isMember(GRADE_URLS_KEY, url));
        } catch (Exception e) {
            log.warn("Redis评级去重检查失败: {}", e.getMessage());
            return false;
        }
    }

    private void cacheToRedis(Grade g) {
        try {
            gradeRedisTemplate.opsForSet().add(GRADE_URLS_KEY, g.getUrl());
            String val = (g.getGrade() != null ? g.getGrade() : "") + "|" +
                    (g.getRecorder() != null ? g.getRecorder() : "") + "|" +
                    (g.getRemark() != null ? g.getRemark() : "");
            gradeRedisTemplate.opsForHash().put(GRADE_RECORD_KEY, g.getUrl(), val);
        } catch (Exception e) {
            log.warn("Redis评级缓存写入失败: {}", e.getMessage());
        }
    }
}
