package org.example.service.impl;

import org.example.service.HashCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HashCacheServiceImpl implements HashCacheService {

    private static final Logger log = LoggerFactory.getLogger(HashCacheServiceImpl.class);
    private static final String HASH_PREFIX = "hash:unique:";

    @Autowired
    @Qualifier("hashRedisTemplate")
    private RedisTemplate<String, Object> hashRedisTemplate;

    @Override
    public boolean exists(String hash) {
        try {
            return Boolean.TRUE.equals(hashRedisTemplate.hasKey(HASH_PREFIX + hash));
        } catch (Exception e) {
            log.warn("Redis hash存在性检查失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void save(String hash) {
        try {
            hashRedisTemplate.opsForValue().set(HASH_PREFIX + hash, "1");
        } catch (Exception e) {
            log.warn("Redis hash缓存写入失败: {}", e.getMessage());
        }
    }

    @Override
    public void remove(String hash) {
        try {
            hashRedisTemplate.delete(HASH_PREFIX + hash);
        } catch (Exception e) {
            log.warn("Redis hash缓存删除失败: {}", e.getMessage());
        }
    }

    @Override
    public void warmUp(List<String> hashes) {
        if (hashes == null || hashes.isEmpty()) return;
        try {
            int count = 0;
            for (String hash : hashes) {
                if (hash != null && !hash.isEmpty()) {
                    hashRedisTemplate.opsForValue().set(HASH_PREFIX + hash, "1");
                    count++;
                }
            }
            log.info("✅ Redis db10 hash缓存预热完成，共写入 {} 条", count);
        } catch (Exception e) {
            log.error("❌ Redis hash预热失败: {}", e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            var connection = hashRedisTemplate.getConnectionFactory().getConnection();
            try {
                boolean ok = connection.ping() != null;
                return ok;
            } finally {
                connection.close();
            }
        } catch (Exception e) {
            log.warn("Redis db10 连接不可用: {}", e.getMessage());
            return false;
        }
    }
}
