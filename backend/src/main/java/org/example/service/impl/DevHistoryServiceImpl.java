package org.example.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.service.DevHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

@Service
public class DevHistoryServiceImpl implements DevHistoryService {

    private static final Logger log = LoggerFactory.getLogger(DevHistoryServiceImpl.class);
    private static final String KEY_PREFIX = "dev:history:";
    private static final long TTL_DAYS = 7;
    private static final int MAX_LOCAL_SIZE = 100;

    @Autowired
    @Qualifier("devHistoryRedisTemplate")
    private RedisTemplate<String, Object> devRedisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentLinkedDeque<String> localHistory = new ConcurrentLinkedDeque<>();
    private volatile boolean redisDown = false;

    @Override
    public void addRecord(String recorder, Map<String, Object> record) {
        record.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        try {
            if (isRedisAvailable()) {
                String key = KEY_PREFIX + recorder;
                String json = objectMapper.writeValueAsString(record);
                devRedisTemplate.opsForList().leftPush(key, json);
                devRedisTemplate.opsForList().trim(key, 0, 499);
                devRedisTemplate.expire(key, TTL_DAYS, TimeUnit.DAYS);
                redisDown = false;
            } else {
                redisDown = true;
                addToLocal(record);
            }
        } catch (Exception e) {
            log.warn("保存开发历史记录到Redis失败，降级到本地缓存: {}", e.getMessage());
            redisDown = true;
            addToLocal(record);
        }
    }

    @Override
    public List<Map<String, Object>> getHistory(String recorder) {
        try {
            if (isRedisAvailable()) {
                String key = KEY_PREFIX + recorder;
                List<Object> rawList = devRedisTemplate.opsForList().range(key, 0, -1);
                if (rawList != null && !rawList.isEmpty()) {
                    List<Map<String, Object>> result = new ArrayList<>();
                    for (Object raw : rawList) {
                        try {
                            String json = raw instanceof String ? (String) raw : raw.toString();
                            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});
                            result.add(map);
                        } catch (Exception e) {
                            log.warn("解析历史记录失败: {}", e.getMessage());
                        }
                    }
                    redisDown = false;
                    return result;
                }
                redisDown = false;
                return new ArrayList<>();
            } else {
                redisDown = true;
                return getFromLocal();
            }
        } catch (Exception e) {
            log.warn("获取开发历史记录失败: {}", e.getMessage());
            redisDown = true;
            return getFromLocal();
        }
    }

    @Override
    public boolean isRedisAvailable() {
        try {
            return devRedisTemplate.getConnectionFactory() != null
                    && devRedisTemplate.getConnectionFactory().getConnection().ping() != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void clearHistory(String recorder) {
        try {
            if (isRedisAvailable()) {
                devRedisTemplate.delete(KEY_PREFIX + recorder);
            }
        } catch (Exception e) {
            log.warn("清除历史失败: {}", e.getMessage());
        }
        localHistory.clear();
    }

    @Override
    public void deleteRecord(String recorder, String timestamp) {
        try {
            if (isRedisAvailable()) {
                String key = KEY_PREFIX + recorder;
                List<Object> rawList = devRedisTemplate.opsForList().range(key, 0, -1);
                if (rawList != null) {
                    for (Object raw : rawList) {
                        String json = raw instanceof String ? (String) raw : raw.toString();
                        Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});
                        if (timestamp.equals(map.get("timestamp"))) {
                            devRedisTemplate.opsForList().remove(key, 1, raw);
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("删除历史记录失败: {}", e.getMessage());
        }
    }

    private void addToLocal(Map<String, Object> record) {
        try {
            localHistory.addFirst(objectMapper.writeValueAsString(record));
            while (localHistory.size() > MAX_LOCAL_SIZE) {
                localHistory.removeLast();
            }
        } catch (Exception e) {
            log.warn("本地缓存写入失败: {}", e.getMessage());
        }
    }

    private List<Map<String, Object>> getFromLocal() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (String json : localHistory) {
            try {
                result.add(objectMapper.readValue(json, new TypeReference<>() {}));
            } catch (Exception e) {
                log.warn("本地历史解析失败: {}", e.getMessage());
            }
        }
        return result;
    }
}
