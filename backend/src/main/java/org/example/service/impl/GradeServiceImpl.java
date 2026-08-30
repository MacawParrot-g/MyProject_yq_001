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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GradeServiceImpl implements GradeService {

    private static final Logger log = LoggerFactory.getLogger(GradeServiceImpl.class);
    private static final String GRADE_BUNDLE_IDS_KEY = "grade:bundleIds";
    private static final String GRADE_RECORD_KEY = "grade:record";

    @Autowired
    private GradeMapper gradeMapper;

    @Autowired
    @Qualifier("gradeRedisTemplate")
    private RedisTemplate<String, Object> gradeRedisTemplate;

    @Override
    public Result getGradeByBundleId(String bundleId) {
        if (bundleId == null || bundleId.isEmpty()) {
            return Result.fail("BundleId不能为空");
        }
        try {
            Object cached = gradeRedisTemplate.opsForHash().get(GRADE_RECORD_KEY, bundleId);
            if (cached != null) {
                String val = String.valueOf(cached);
                String[] parts = val.split("\\|", -1);
                Grade g = new Grade();
                g.setBundleId(bundleId);
                g.setGrade(parts.length > 0 ? parts[0] : "");
                g.setRecorder(parts.length > 1 ? parts[1] : "");
                g.setRemark(parts.length > 2 ? parts[2] : "");
                return Result.success("查询评级成功（缓存）", g);
            }
        } catch (Exception e) {
            log.warn("Redis评级缓存读取失败，回退MySQL: {}", e.getMessage());
        }

        Grade grade = gradeMapper.selectByBundleId(bundleId);
        if (grade != null) {
            cacheToRedis(grade);
            return Result.success("查询评级成功（数据库）", grade);
        }
        return Result.success("该应用暂无评级", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result saveGrade(String bundleId, String grade, String recorder, String remark) {
        if (bundleId == null || bundleId.isEmpty()) return Result.fail("BundleId不能为空");
        if (grade == null || grade.isEmpty()) return Result.fail("请选择评级等级");

        Grade record = new Grade();
        record.setBundleId(bundleId);
        record.setGrade(grade);
        record.setRecorder(recorder);
        record.setRemark(remark);

        Grade existing = gradeMapper.selectByBundleId(bundleId);
        if (existing != null) {
            gradeMapper.updateGrade(record);
        } else {
            gradeMapper.insertGrade(record);
        }

        cacheToRedis(record);
        log.info("✅ bundleId为{}的评级信息已经被修改(数据看板修改的), grade={}, recorder={}", bundleId, grade, recorder);
        return Result.success("评级保存成功", record);
    }

    @Override
    public void warmUpGradeCache() {
        refreshGradeCache();
    }

    @Scheduled(fixedRate = 120000)
    public void refreshGradeCache() {
        try {
            gradeRedisTemplate.delete(GRADE_BUNDLE_IDS_KEY);
            gradeRedisTemplate.delete(GRADE_RECORD_KEY);

            List<Grade> grades = gradeMapper.selectAll();
            if (grades == null || grades.isEmpty()) {
                log.info("🔄 评级缓存数据已更新（MySQL为空，Redis DB3已清空）");
                return;
            }
            for (Grade g : grades) {
                cacheToRedis(g);
            }
            log.info("🔄 缓存数据已更新，共从MySQL重新加载 {} 条评级记录到Redis DB3", grades.size());
        } catch (Exception e) {
            log.error("❌ 评级缓存定时刷新失败: {}", e.getMessage());
        }
    }

    @Override
    public boolean isBundleIdGraded(String bundleId) {
        if (bundleId == null || bundleId.isEmpty()) return false;
        try {
            return Boolean.TRUE.equals(gradeRedisTemplate.opsForSet().isMember(GRADE_BUNDLE_IDS_KEY, bundleId));
        } catch (Exception e) {
            log.warn("Redis评级去重检查失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Result searchGrades(String grade, String recorder, String keyword, int page, int size) {
        int offset = (page - 1) * size;
        List<Grade> list = gradeMapper.searchGrades(grade, recorder, keyword, size, offset);
        long total = gradeMapper.countSearch(grade, recorder, keyword);
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return Result.success("查询成功，共 " + total + " 条", result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result deleteGrade(String bundleId) {
        if (bundleId == null || bundleId.isEmpty()) return Result.fail("BundleId不能为空");
        try {
            int count = gradeMapper.deleteByBundleId(bundleId);
            if (count == 0) return Result.fail("该评级记录不存在");
            gradeRedisTemplate.opsForSet().remove(GRADE_BUNDLE_IDS_KEY, bundleId);
            gradeRedisTemplate.opsForHash().delete(GRADE_RECORD_KEY, bundleId);
            log.info("🗑️ bundleId为{}的评级信息已经被修改(数据看板修改的), 操作类型=删除", bundleId);
            return Result.success("删除成功", null);
        } catch (Exception e) {
            log.error("❌ 删除评级记录失败: {}", e.getMessage());
            return Result.fail("删除失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateGrade(String bundleId, String grade, String remark) {
        if (bundleId == null || bundleId.isEmpty()) return Result.fail("BundleId不能为空");
        if (grade == null || grade.isEmpty()) return Result.fail("请选择评级等级");
        try {
            Grade existing = gradeMapper.selectByBundleId(bundleId);
            if (existing == null) return Result.fail("该评级记录不存在");
            existing.setGrade(grade);
            existing.setRemark(remark);
            gradeMapper.updateGrade(existing);
            cacheToRedis(existing);
            log.info("✅ bundleId为{}的评级信息已经被修改(数据看板修改的), grade={}", bundleId, grade);
            return Result.success("更新成功", existing);
        } catch (Exception e) {
            log.error("❌ 更新评级记录失败: {}", e.getMessage());
            return Result.fail("更新失败：" + e.getMessage());
        }
    }

    private void cacheToRedis(Grade g) {
        try {
            gradeRedisTemplate.opsForSet().add(GRADE_BUNDLE_IDS_KEY, g.getBundleId());
            String val = (g.getGrade() != null ? g.getGrade() : "") + "|" +
                    (g.getRecorder() != null ? g.getRecorder() : "") + "|" +
                    (g.getRemark() != null ? g.getRemark() : "");
            gradeRedisTemplate.opsForHash().put(GRADE_RECORD_KEY, g.getBundleId(), val);
        } catch (Exception e) {
            log.warn("Redis评级缓存写入失败: {}", e.getMessage());
        }
    }
}