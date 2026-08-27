package org.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;

@Component
public class RedisDistributedLock {

    private static final Logger log = LoggerFactory.getLogger(RedisDistributedLock.class);

    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "  return redis.call('del', KEYS[1]) " +
                    "else " +
                    "  return 0 " +
                    "end";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public boolean tryLock(String lockKey, String lockValue, Duration expireTime) {
        try {
            Boolean result = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, expireTime);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("尝试获取分布式锁失败, key={}: {}", lockKey, e.getMessage());
            return false;
        }
    }

    public void unlock(String lockKey, String lockValue) {
        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
            redisTemplate.execute(script, Collections.singletonList(lockKey), lockValue);
        } catch (Exception e) {
            log.error("释放分布式锁失败, key={}: {}", lockKey, e.getMessage());
        }
    }
}
