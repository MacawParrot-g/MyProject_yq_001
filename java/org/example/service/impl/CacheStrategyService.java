package org.example.service.impl;

import org.example.entity.TestStatic;
import org.example.mapper.GeneranMapper;
import org.example.service.LocalCacheService;
import org.example.service.RedisCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CacheStrategyService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CacheStrategyService.class);

    @Autowired
    private RedisCacheService redisCacheService;

    @Autowired
    private LocalCacheService localCacheService;

    @Autowired
    private GeneranMapper generanMapper;

    private boolean redisAvailable = false;

    @Override
    public void run(String... args) {
        log.info("正在检测Redis连接...");
        try {
            redisAvailable = redisCacheService.isAvailable();
        } catch (Exception e) {
            redisAvailable = false;
        }

        if (redisAvailable) {
            log.info("✅ Redis连接成功，使用Redis作为去重缓存");
            List<TestStatic> records = generanMapper.selectList(null);
            redisCacheService.warmUpFromMySQL(records);
        } else {
            log.warn("⚠️ Redis连接失败，降级使用Spring本地缓存（Caffeine）");
        }
    }

    public boolean exists(String downloadUrl, String bundleId) {
        if (redisAvailable) {
            return redisCacheService.exists(downloadUrl, bundleId);
        }
        return localCacheService.exists(downloadUrl, bundleId);
    }

    public void save(String downloadUrl, String bundleId) {
        if (redisAvailable) {
            redisCacheService.save(downloadUrl, bundleId);
        } else {
            localCacheService.save(downloadUrl, bundleId);
        }
    }

    public boolean isRedisAvailable() {
        return redisAvailable;
    }
}
