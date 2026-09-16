package org.example.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.example.common.Result;

public interface UserManageService {
    Result listOnlineUsers(HttpServletRequest request);
    Result kickUser(String uid, Integer banSeconds, HttpServletRequest request);
    Result resetPassword(String uid, String newPwd, HttpServletRequest request);
    boolean isBanned(String uid);
    long getBanRemainingSeconds(String uid);
    void registerSession(String uid, HttpSession session);
    void unregisterSession(String uid);
    boolean isOnline(String uid);
}
