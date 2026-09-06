
package org.example.service.impl;
import org.example.config.RedisDistributedLock;
import org.example.entity.TestStatic;
import org.example.mapper.GeneranMapper;
import org.example.service.CacheStrategyService;
import org.example.service.DedupSessionService;
import org.example.service.LocalCacheService;
import org.example.service.RedisCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class CacheStrategyServiceImpl implements CacheStrategyService, CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CacheStrategyServiceImpl.class);
    private static final String WARMUP_LOCK_KEY = "lock:redis:warmup";
    private static final Duration WARMUP_LOCK_EXPIRE = Duration.ofMinutes(5);


    @Autowired
    private RedisCacheService redisCacheService;

    @Autowired
    private LocalCacheService localCacheService;

    @Autowired
    private GeneranMapper generanMapper;

    @Autowired
    private RedisDistributedLock redisDistributedLock;

    @Autowired
    private DedupSessionService dedupSessionService;

    private volatile boolean redisAvailable = false;

    @Override
    public void run(String... args) {
        checkRedisAvailability();
        if (redisAvailable) {
            log.info("✅ Redis连接成功，使用Redis作为去重缓存");
            String lockValue = UUID.randomUUID().toString();
            boolean locked = redisDistributedLock.tryLock(WARMUP_LOCK_KEY, lockValue, WARMUP_LOCK_EXPIRE);
            if (locked) {
                try {
                    log.info("成功获取分布式锁，开始执行Redis预热...");
                    List<TestStatic> records = generanMapper.selectForDedupWarmup();
                    redisCacheService.warmUpFromMySQL(records);
                    log.info("Redis预热完成");
                } catch (Exception e) {
                    log.error("Redis预热失败: {}", e.getMessage(), e);
                } finally {
                    redisDistributedLock.unlock(WARMUP_LOCK_KEY, lockValue);
                }
            } else {
                log.info("其他Pod正在执行Redis预热，跳过本次预热操作");
            }
        } else {
            log.warn("⚠️ Redis连接失败，降级使用本地缓存（Caffeine）");
        }
    }

    @Scheduled(fixedDelay = 30000, initialDelay = 30000)
    public void scheduledRedisCheck() {
        checkRedisAvailability();
    }

    private void checkRedisAvailability() {
        try {
            boolean nowAvailable = redisCacheService.isAvailable();
            if (nowAvailable && !redisAvailable) {
                log.info("✅ Redis恢复可用，切换至Redis缓存模式");
            } else if (!nowAvailable && redisAvailable) {
                log.warn("⚠️ Redis变为不可用，降级至本地缓存（Caffeine）");
            }
            redisAvailable = nowAvailable;
        } catch (Exception e) {
            if (redisAvailable) {
                log.warn("⚠️ Redis检测异常，降级至本地缓存: {}", e.getMessage());
            }
            redisAvailable = false;
        }
    }

    @Override
    public boolean exists(String downloadUrl, String bundleId) {
        if (!isDedupEnabled()) return false;
        if (redisAvailable) {
            return redisCacheService.exists(downloadUrl, bundleId);
        }
        return localCacheService.exists(downloadUrl, bundleId);
    }

    @Override
    public void save(String downloadUrl, String bundleId) {
        if (!isDedupEnabled()) return;
        if (redisAvailable) {
            redisCacheService.save(downloadUrl, bundleId);
        } else {
            localCacheService.save(downloadUrl, bundleId);
        }
    }

    @Override
    public boolean isRedisAvailable() {
        return redisAvailable;
    }

    @Override
    public boolean isDedupEnabled() {
        return dedupSessionService.isDedupEnabled();
    }
}