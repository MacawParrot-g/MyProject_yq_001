package org.example.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.example.common.Result;
import org.example.entity.SysUser;
import org.example.mapper.SysUserMapper;
import org.example.service.UserManageService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class UserManageServiceImpl implements UserManageService {

    private final SysUserMapper sysUserMapper;
    private final RedisTemplate<String, Object> kickRedisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final ConcurrentHashMap<String, HttpSession> activeSessions = new ConcurrentHashMap<>();

    public UserManageServiceImpl(SysUserMapper sysUserMapper,
                                 @Qualifier("kickRedisTemplate") RedisTemplate<String, Object> kickRedisTemplate) {
        this.sysUserMapper = sysUserMapper;
        this.kickRedisTemplate = kickRedisTemplate;
    }

    @Override
    public void registerSession(String uid, HttpSession session) {
        activeSessions.put(uid, session);
    }

    @Override
    public void unregisterSession(String uid) {
        activeSessions.remove(uid);
    }

    @Override
    public boolean isBanned(String uid) {
        return Boolean.TRUE.equals(kickRedisTemplate.hasKey("ban:uid:" + uid));
    }

    @Override
    public long getBanRemainingSeconds(String uid) {
        Long ttl = kickRedisTemplate.getExpire("ban:uid:" + uid, TimeUnit.SECONDS);
        return ttl != null ? ttl : 0;
    }

    @Override
    public Result listOnlineUsers(HttpServletRequest request) {
        String operatorType = getOperatorType(request);
        if (operatorType == null) {
            return Result.fail("未登录");
        }
        if (!"ADMIN".equals(operatorType)) {
            return Result.fail("仅管理员可查看");
        }
        List<SysUser> users = sysUserMapper.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysUser u : users) {
            Map<String, Object> map = new HashMap<>();
            map.put("uid", u.getUid());
            map.put("name", u.getName());
            map.put("type", u.getType());
            boolean online = isSessionAlive(u.getUid());
            map.put("online", online);
            result.add(map);
        }
        return Result.success("查询成功", result);
    }

    @Override
    public boolean isOnline(String uid) {
        HttpSession session = activeSessions.get(uid);
        if (session == null) return false;
        try {
            session.getAttribute("uid");
            return true;
        } catch (IllegalStateException e) {
            activeSessions.remove(uid);
            return false;
        }
    }

    @Override
    public Result kickUser(String uid, Integer banSeconds, HttpServletRequest request) {
        String operatorType = getOperatorType(request);
        if (operatorType == null) {
            return Result.fail("未登录");
        }
        if (!"ADMIN".equals(operatorType)) {
            return Result.fail("仅管理员可踢人");
        }
        SysUser user = sysUserMapper.findByUid(uid);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        invalidateSession(uid);
        if (banSeconds != null && banSeconds > 0) {
            kickRedisTemplate.opsForValue().set("ban:uid:" + uid, "1", banSeconds, TimeUnit.SECONDS);
        }
        return Result.success("已踢下线" + (banSeconds != null && banSeconds > 0 ? "，封禁 " + banSeconds + " 秒" : ""));
    }

    @Override
    public Result resetPassword(String uid, String newPwd, HttpServletRequest request) {
        String operatorType = getOperatorType(request);
        if (operatorType == null) {
            return Result.fail("未登录");
        }
        if (!"ADMIN".equals(operatorType)) {
            return Result.fail("仅管理员可重置密码");
        }
        if (newPwd == null || newPwd.isBlank()) {
            return Result.fail("新密码不能为空");
        }
        SysUser user = sysUserMapper.findByUid(uid);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        sysUserMapper.updatePassword(uid, passwordEncoder.encode(newPwd));
        invalidateSession(uid);
        return Result.success("密码已重置，用户已被踢下线");
    }

    private boolean isSessionAlive(String uid) {
        HttpSession session = activeSessions.get(uid);
        if (session == null) return false;
        try {
            session.getAttribute("uid");
            return true;
        } catch (IllegalStateException e) {
            activeSessions.remove(uid);
            return false;
        }
    }

    private void invalidateSession(String uid) {
        HttpSession session = activeSessions.remove(uid);
        if (session != null) {
            try { session.invalidate(); } catch (Exception ignored) {}
        }
        kickRedisTemplate.delete("session:uid:" + uid);
    }

    private String getOperatorType(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("uid") == null) {
            return null;
        }
        return (String) session.getAttribute("type");
    }
}