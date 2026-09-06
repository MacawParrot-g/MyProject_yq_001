// 文件路径: src/main/java/org/example/service/impl/LocalCacheServiceImpl.java
package org.example.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.example.service.LocalCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class LocalCacheServiceImpl implements LocalCacheService {

    private static final Logger log = LoggerFactory.getLogger(LocalCacheServiceImpl.class);

    private final Cache<String, String> dedupCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfter(new Expiry<String, String>() {
                @Override
                public long expireAfterCreate(String key, String value, long currentTime) {
                    return Duration.between(LocalDateTime.now(), LocalDate.now().plusDays(1).atStartOfDay()).toNanos();
                }

                @Override
                public long expireAfterUpdate(String key, String value, long currentTime, long currentDuration) {
                    return Duration.between(LocalDateTime.now(), LocalDate.now().plusDays(1).atStartOfDay()).toNanos();
                }

                @Override
                public long expireAfterRead(String key, String value, long currentTime, long currentDuration) {
                    return currentDuration;
                }
            })
            .build();

    @Override
    public boolean exists(String downloadUrl, String bundleId) {
        String key = buildKey(downloadUrl, bundleId);
        boolean exists = dedupCache.getIfPresent(key) != null;
        if (exists) {
            log.info("缓存命中，重复记录: {}", key);
        }
        return exists;
    }

    @Override
    public void save(String downloadUrl, String bundleId) {
        String key = buildKey(downloadUrl, bundleId);
        dedupCache.put(key, "1");
        log.info("缓存写入: {}", key);
    }

    private String buildKey(String downloadUrl, String bundleId) {
        return "dedup:" + downloadUrl + "::" + bundleId;
    }
}
