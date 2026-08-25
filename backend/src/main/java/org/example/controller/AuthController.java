package org.example.controller;
import jakarta.servlet.http.HttpServletRequest;
import org.example.annotation.LogExecutionTime;
import org.example.common.Result;
import org.example.util.SysUserService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SysUserService sysUserService;

    public AuthController(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    @PostMapping("/login")
    @LogExecutionTime("用户登录")
    public Result login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        return sysUserService.login(body.get("uid"), body.get("pwd"), request);
    }

    @PostMapping("/logout")
    @LogExecutionTime("用户退出")
    public Result logout(HttpServletRequest request) {
        return sysUserService.logout(request);
    }

    @GetMapping("/status")
    @LogExecutionTime("查询登陆状态")
    public Result status(HttpServletRequest request) {
        return sysUserService.getLoginStatus(request);
    }

    @PostMapping("/user/create")
    @LogExecutionTime("管理员创建用户")
    public Result createUser(@RequestBody Map<String, String> body, HttpServletRequest request) {
        return sysUserService.createUser(body.get("name"), body.get("pwd"), body.get("type"), request);
    }

    @DeleteMapping("/user/delete")
    @LogExecutionTime("管理员删除用户")
    public Result deleteUser(@RequestParam String uid, HttpServletRequest request) {
        return sysUserService.deleteUser(uid, request);
    }

    @GetMapping("/user/list")
    @LogExecutionTime("查询用户列表")
    public Result listUsers(HttpServletRequest request) {
        return sysUserService.listUsers(request);
    }
}
