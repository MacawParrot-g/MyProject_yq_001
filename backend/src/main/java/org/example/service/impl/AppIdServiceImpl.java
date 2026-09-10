package org.example.service.impl;

import org.example.entity.AppIdRecord;
import org.example.mapper.AppIdMapper;
import org.example.service.AppIdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class AppIdServiceImpl implements AppIdService {

    private static final Logger log = LoggerFactory.getLogger(AppIdServiceImpl.class);
    private static final String APPID_CACHE_KEY = "appid:cache";
    private static final long CACHE_TTL_DAYS = 1;

    @Autowired
    private AppIdMapper appIdMapper;

    @Autowired
    @Qualifier("appIdRedisTemplate")
    private RedisTemplate<String, Object> appIdRedisTemplate;

    @Override
    public boolean isAppIdExists(Long appId) {
        if (appId == null) return false;
        String key = String.valueOf(appId);
        try {
            Boolean exists = appIdRedisTemplate.opsForSet().isMember(APPID_CACHE_KEY, key);
            if (Boolean.TRUE.equals(exists)) {
                return true;
            }
        } catch (Exception e) {
            log.warn("Redis appid缓存读取失败，回退数据库: {}", e.getMessage());
        }
        AppIdRecord record = appIdMapper.selectByAppId(appId);
        return record != null;
    }

    @Override
    public void saveAppId(String bundleId, Long appId) {
        if (bundleId == null || appId == null) return;
        try {
            AppIdRecord existing = appIdMapper.selectByBundleId(bundleId);
            if (existing != null) {
                existing.setAppId(appId);
                appIdMapper.insertAppId(existing);
            } else {
                AppIdRecord record = new AppIdRecord();
                record.setBundleId(bundleId);
                record.setAppId(appId);
                appIdMapper.insertAppId(record);
            }
            appIdRedisTemplate.opsForSet().add(APPID_CACHE_KEY, String.valueOf(appId));
            log.info("✅ appid记录已写入: bundleId={}, appId={}", bundleId, appId);
        } catch (Exception e) {
            log.error("❌ appid写入失败: bundleId={}, appId={}, 原因: {}", bundleId, appId, e.getMessage());
        }
    }

    @Override
    public void warmUpAppIdCache() {
        refreshAppIdCache();
    }

    @Scheduled(fixedRate = 120000)
    public void refreshAppIdCache() {
        try {
            appIdRedisTemplate.delete(APPID_CACHE_KEY);
            List<AppIdRecord> records = appIdMapper.selectAll();
            if (records == null || records.isEmpty()) {
                log.info("🔄 appid缓存已更新（MySQL为空，Redis DB11已清空）");
                return;
            }
            for (AppIdRecord r : records) {
                if (r.getAppId() != null) {
                    appIdRedisTemplate.opsForSet().add(APPID_CACHE_KEY, String.valueOf(r.getAppId()));
                }
            }
            appIdRedisTemplate.expire(APPID_CACHE_KEY, CACHE_TTL_DAYS, TimeUnit.DAYS);
            log.info("🔄 appid缓存已更新，共从MySQL加载 {} 条记录到Redis DB11", records.size());
        } catch (Exception e) {
            log.error("❌ appid缓存定时刷新失败: {}", e.getMessage());
        }
    }
}
