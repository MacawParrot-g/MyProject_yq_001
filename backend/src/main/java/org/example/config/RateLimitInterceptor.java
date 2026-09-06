package org.example.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private static final int MAX_REQUESTS = 200;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final Duration BAN_DURATION = Duration.ofMinutes(2);
    private static final long CLEANUP_INTERVAL_MS = 300_000;

    private final Map<String, Long> bannedIps = new ConcurrentHashMap<>();
    private final Map<String, RateLimitState> rateLimitStates = new ConcurrentHashMap<>();
    private volatile long lastCleanupTime = System.currentTimeMillis();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod handlerMethod) {
            if (handlerMethod.hasMethodAnnotation(org.example.annotation.SkipRateLimit.class)) {
                return true;
            }
        }
        cleanupExpiredEntries();

        String ip = getClientIp(request);
        long currentTime = System.currentTimeMillis();

        Long banExpiry = bannedIps.get(ip);
        if (banExpiry != null) {
            if (currentTime < banExpiry) {
                writeRateLimitResponse(response, ip, banExpiry);
                return false;
            }
            bannedIps.remove(ip);
            rateLimitStates.remove(ip);
        }

        RateLimitState state = rateLimitStates.computeIfAbsent(ip, k -> new RateLimitState(currentTime));

        if (currentTime - state.windowStart > WINDOW.toMillis()) {
            state.windowStart = currentTime;
            state.count.set(0);
        }

        if (state.count.incrementAndGet() <= MAX_REQUESTS) {
            return true;
        }

        long newBanExpiry = currentTime + BAN_DURATION.toMillis();
        bannedIps.put(ip, newBanExpiry);
        rateLimitStates.remove(ip);

        log.warn("IP {} 因请求过于频繁被封禁3分钟", ip);
        writeRateLimitResponse(response, ip, newBanExpiry);
        return false;
    }

    private void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime < CLEANUP_INTERVAL_MS) {
            return;
        }
        lastCleanupTime = now;

        int removedBans = 0;
        Iterator<Map.Entry<String, Long>> banIt = bannedIps.entrySet().iterator();
        while (banIt.hasNext()) {
            if (banIt.next().getValue() < now) {
                banIt.remove();
                removedBans++;
            }
        }

        int removedStates = 0;
        long windowThreshold = now - WINDOW.toMillis();
        Iterator<Map.Entry<String, RateLimitState>> stateIt = rateLimitStates.entrySet().iterator();
        while (stateIt.hasNext()) {
            Map.Entry<String, RateLimitState> entry = stateIt.next();
            if (entry.getValue().windowStart < windowThreshold) {
                stateIt.remove();
                removedStates++;
            }
        }

        if (removedBans > 0 || removedStates > 0) {
            log.debug("限流状态清理: 移除过期封禁{}条, 过期状态{}条", removedBans, removedStates);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }

    private void writeRateLimitResponse(HttpServletResponse response, String ip, long banExpiry) throws Exception {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        long remainSec = (banExpiry - System.currentTimeMillis()) / 1000;
        String json = "{\"success\":false,\"resultMsg\":\"请求过于频繁，IP已被临时封禁，请" + remainSec + "秒后再试\"}";
        response.getWriter().write(json);
    }

    private static class RateLimitState {
        long windowStart;
        AtomicInteger count;

        RateLimitState(long windowStart) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(0);
        }
    }
}