package org.example.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    // 限流配置：1分钟内最多 200 次请求
    private static final int MAX_REQUESTS = 200;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    // 封禁配置：触发限流后封禁 3 分钟
    private static final Duration BAN_DURATION = Duration.ofMinutes(2);

    // 记录每个 IP 的封禁到期时间戳
    private final Map<String, Long> bannedIps = new ConcurrentHashMap<>();

    // 记录每个 IP 的限流状态（请求数 + 窗口开始时间）
    private final Map<String, RateLimitState> rateLimitStates = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = getClientIp(request);
        long currentTime = System.currentTimeMillis();

        // 1. 检查是否处于封禁状态
        Long banExpiry = bannedIps.get(ip);
        if (banExpiry != null) {
            if (currentTime < banExpiry) {
                writeRateLimitResponse(response, ip, banExpiry);
                return false;
            }
            // 封禁已过期，清除状态
            bannedIps.remove(ip);
            rateLimitStates.remove(ip);
        }

        // 2. 检查限流状态（滑动/固定窗口）
        RateLimitState state = rateLimitStates.computeIfAbsent(ip, k -> new RateLimitState(currentTime));

        // 如果当前时间已经超出了时间窗口，重置计数器
        if (currentTime - state.windowStart > WINDOW.toMillis()) {
            state.windowStart = currentTime;
            state.count.set(0);
        }

        // 尝试消耗一个请求额度
        if (state.count.incrementAndGet() <= MAX_REQUESTS) {
            return true; // 放行
        }

        // 3. 触发限流，执行封禁
        long newBanExpiry = currentTime + BAN_DURATION.toMillis();
        bannedIps.put(ip, newBanExpiry);
        rateLimitStates.remove(ip); // 清除限流计数，等待下次解封后重新计算

        log.warn("IP {} 因请求过于频繁被封禁3分钟", ip);
        writeRateLimitResponse(response, ip, newBanExpiry);
        return false;
    }

    /**
     * 获取客户端真实 IP
     */
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

    /**
     * 返回 429 限流响应
     */
    private void writeRateLimitResponse(HttpServletResponse response, String ip, long banExpiry) throws Exception {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        long remainSec = (banExpiry - System.currentTimeMillis()) / 1000;
        String json = "{\"success\":false,\"resultMsg\":\"请求过于频繁，IP已被临时封禁，请" + remainSec + "秒后再试\"}";
        response.getWriter().write(json);
    }

    /**
     * 内部类：用于记录某个 IP 的限流状态
     */
    private static class RateLimitState {
        long windowStart; // 窗口开始时间
        AtomicInteger count; // 窗口内的请求数

        RateLimitState(long windowStart) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(0);
        }
    }
}