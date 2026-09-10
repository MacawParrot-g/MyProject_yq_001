package org.example.service.impl;

import org.example.common.Result;
import org.example.service.DedupSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.time.Duration;
import java.util.Map;

@Service
public class DedupSessionServiceImpl implements DedupSessionService {

    private static final Logger log = LoggerFactory.getLogger(DedupSessionServiceImpl.class);
    private static final String DEDUP_SESSION_PREFIX = "dedup:session:";
    private static final String POD_HEARTBEAT_PREFIX = "dedup:pod:heartbeat:";
    private static final Duration SESSION_TTL = Duration.ofHours(8);
    private static final Duration HEARTBEAT_TTL = Duration.ofSeconds(30);

    private final String currentPodId;

    @Autowired
    @Qualifier("sessionRedisTemplate")
    private RedisTemplate<String, Object> sessionRedisTemplate;

    public DedupSessionServiceImpl() {
        String id;
        try {
            id = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            id = "pod-" + System.currentTimeMillis();
        }
        this.currentPodId = id;
    }

    @Scheduled(fixedRate = 10000)
    public void sendHeartbeat() {
        try {
            sessionRedisTemplate.opsForValue().set(
                    POD_HEARTBEAT_PREFIX + currentPodId, "alive", HEARTBEAT_TTL);
        } catch (Exception e) {
            log.warn("去重Pod心跳发送失败: {}", e.getMessage());
        }
    }

    @Override
    public Result enableDedup(String username) {
        try {
            String sessionKey = DEDUP_SESSION_PREFIX + username;
            sessionRedisTemplate.opsForValue().set(sessionKey, currentPodId, SESSION_TTL);
            log.info("🔓 用户 [{}] 开启了自动去重功能，分配至Pod: {}", username, currentPodId);
            return Result.success("去重功能已开启", Map.of(
                    "enabled", true,
                    "username", username,
                    "podId", currentPodId
            ));
        } catch (Exception e) {
            log.error("开启去重失败: {}", e.getMessage());
            return Result.fail("开启去重失败: " + e.getMessage());
        }
    }

    @Override
    public Result disableDedup() {
        try {
            String username = findAndDeleteSession();
            log.info("🔒 用户 [{}] 关闭了自动去重功能", username != null ? username : "unknown");
            return Result.success("去重功能已关闭", Map.of("enabled", false));
        } catch (Exception e) {
            log.error("关闭去重失败: {}", e.getMessage());
            return Result.fail("关闭去重失败: " + e.getMessage());
        }
    }

    @Override
    public Result getStatus() {
        try {
            String username = org.example.common.UserContext.getUsername();
            if (username == null || "无用户参数".equals(username)) {
                return Result.success("当前无去重会话", Map.of("enabled", false));
            }
            String sessionKey = DEDUP_SESSION_PREFIX + username;
            Object podId = sessionRedisTemplate.opsForValue().get(sessionKey);
            if (podId == null) {
                return Result.success("当前无去重会话", Map.of("enabled", false));
            }
            boolean podAlive = isPodAlive(String.valueOf(podId));
            if (!podAlive) {
                log.info("用户 [{}] 的原Pod [{}] 已宕机，重新生成Session至Pod: {}", username, podId, currentPodId);
                sessionRedisTemplate.opsForValue().set(sessionKey, currentPodId, SESSION_TTL);
                return Result.success("去重会话已重新分配", Map.of(
                        "enabled", true,
                        "username", username,
                        "podId", currentPodId
                ));
            }
            return Result.success("去重功能运行中", Map.of(
                    "enabled", true,
                    "username", username,
                    "podId", String.valueOf(podId)
            ));
        } catch (Exception e) {
            log.warn("获取去重状态失败: {}", e.getMessage());
            return Result.success("去重状态查询失败，默认关闭", Map.of("enabled", false));
        }
    }

    @Override
    public boolean isDedupEnabled() {
        try {
            String username = org.example.common.UserContext.getUsername();
            if (username == null || "无用户参数".equals(username)) return false;
            String sessionKey = DEDUP_SESSION_PREFIX + username;
            Object podId = sessionRedisTemplate.opsForValue().get(sessionKey);
            if (podId == null) return false;
            return isPodAlive(String.valueOf(podId));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void checkAndRegenerateSession(String username) {
        if (username == null || "无用户参数".equals(username)) return;
        try {
            String sessionKey = DEDUP_SESSION_PREFIX + username;
            Object podId = sessionRedisTemplate.opsForValue().get(sessionKey);
            if (podId == null) return;
            String storedPodId = String.valueOf(podId);
            if (!storedPodId.equals(currentPodId) && !isPodAlive(storedPodId)) {
                sessionRedisTemplate.opsForValue().set(sessionKey, currentPodId, SESSION_TTL);
                log.info("用户 [{}] 的原Pod [{}] 已宕机，Session已重新分配至Pod: {}", username, storedPodId, currentPodId);
            }
        } catch (Exception e) {
            log.warn("检查去重Session失败: {}", e.getMessage());
        }
    }

    private boolean isPodAlive(String podId) {
        try {
            if (podId.equals(currentPodId)) return true;
            Object heartbeat = sessionRedisTemplate.opsForValue().get(POD_HEARTBEAT_PREFIX + podId);
            return heartbeat != null;
        } catch (Exception e) {
            return false;
        }
    }

    private String findAndDeleteSession() {
        try {
            String username = org.example.common.UserContext.getUsername();
            if (username != null && !"无用户参数".equals(username)) {
                String sessionKey = DEDUP_SESSION_PREFIX + username;
                Boolean exists = sessionRedisTemplate.hasKey(sessionKey);
                if (Boolean.TRUE.equals(exists)) {
                    sessionRedisTemplate.delete(sessionKey);
                    return username;
                }
            }
        } catch (Exception e) {
            log.warn("查找并删除去重Session失败: {}", e.getMessage());
        }
        return null;
    }
}
