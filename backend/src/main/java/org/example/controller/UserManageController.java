package org.example.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.annotation.LogExecutionTime;
import org.example.common.Result;
import org.example.service.UserManageService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class UserManageController {

    private final UserManageService userManageService;

    public UserManageController(UserManageService userManageService) {
        this.userManageService = userManageService;
    }

    @GetMapping("/users")
    @LogExecutionTime("查看用户在线状态")
    public Result listOnlineUsers(HttpServletRequest request) {
        return userManageService.listOnlineUsers(request);
    }

    @PostMapping("/user/kick")
    @LogExecutionTime("踢用户下线")
    public Result kickUser(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String uid = (String) body.get("uid");
        Integer banSeconds = body.get("banSeconds") != null ? ((Number) body.get("banSeconds")).intValue() : 0;
        return userManageService.kickUser(uid, banSeconds, request);
    }

    @PutMapping("/user/resetPwd")
    @LogExecutionTime("重置用户密码")
    public Result resetPassword(@RequestBody Map<String, String> body, HttpServletRequest request) {
        return userManageService.resetPassword(body.get("uid"), body.get("newPwd"), request);
    }
}
