package org.example.service.impl;

import org.example.entity.TestStatic;
import org.example.service.RedisCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class RedisCacheServiceImpl implements RedisCacheService {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheServiceImpl.class);
    private static final String KEY_PREFIX = "dedup:task:";
    private static final long TTL_DAYS = 7;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean exists(String url, String bundleId) {
        try {
            String key = KEY_PREFIX + url + "::" + bundleId;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("Redis去重检查失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void save(String url, String bundleId) {
        try {
            String key = KEY_PREFIX + url + "::" + bundleId;
            redisTemplate.opsForValue().set(key, "1", TTL_DAYS, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("Redis保存去重记录失败: {}", e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            return Boolean.TRUE.equals(redisTemplate.getConnectionFactory().getConnection().ping() != null);
        } catch (Exception e) {
            log.warn("Redis连接不可用: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void warmUpFromMySQL(List<TestStatic> records) {
        if (records == null || records.isEmpty()) return;
        int count = 0;
        for (TestStatic record : records) {
            if (record.getUrl() != null && record.getBundleId() != null) {
                save(record.getUrl(), record.getBundleId());
                count++;
            }
        }
        log.info("Redis预热完成，从MySQL导入{}条去重记录", count);
    }
}
