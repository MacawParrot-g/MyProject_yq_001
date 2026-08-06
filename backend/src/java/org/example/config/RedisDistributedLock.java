package org.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisDistributedLock {

    private static final Logger log = LoggerFactory.getLogger(RedisDistributedLock.class);

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
            Object currentValue = redisTemplate.opsForValue().get(lockKey);
            if (lockValue.equals(String.valueOf(currentValue))) {
                redisTemplate.delete(lockKey);
            }
        } catch (Exception e) {
            log.error("释放分布式锁失败, key={}: {}", lockKey, e.getMessage());
        }
    }
}
