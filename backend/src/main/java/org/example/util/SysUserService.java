package org.example.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.example.common.Result;
import org.example.entity.SysUser;
import org.example.mapper.SysUserMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SysUserService {

    private final SysUserMapper sysUserMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public SysUserService(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    public Result login(String uid, String rawPwd, HttpServletRequest request) {
        if (uid == null || uid.isBlank() || rawPwd == null || rawPwd.isBlank()) {
            return Result.fail("账号和密码不能为空");
        }
        SysUser user = sysUserMapper.findByUid(uid.trim());
        if (user == null) {
            return Result.fail("账号不存在");
        }
        if (!passwordEncoder.matches(rawPwd, user.getPwd())) {
            return Result.fail("密码错误");
        }
        HttpSession session = request.getSession(true);
        session.setAttribute("uid", user.getUid());
        session.setAttribute("name", user.getName());
        session.setAttribute("type", user.getType());
        session.setMaxInactiveInterval(30 * 24 * 60 * 60);
        Map<String, Object> data = new HashMap<>();
        data.put("uid", user.getUid());
        data.put("name", user.getName());
        data.put("type", user.getType());
        return Result.success("登录成功", data);
    }

    public Result logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return Result.success("已退出登录");
    }

    public Result getLoginStatus(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("uid") != null) {
            Map<String, Object> data = new HashMap<>();
            data.put("uid", session.getAttribute("uid"));
            data.put("name", session.getAttribute("name"));
            data.put("type", session.getAttribute("type"));
            return Result.success("已登录", Map.of("loggedIn", true, "data", data));
        }
        return Result.success("未登录", Map.of("loggedIn", false));
    }

    public Result createUser(String name, String rawPwd, String type, HttpServletRequest request) {
        String operatorType = getOperatorType(request);
        if (operatorType == null) {
            return Result.fail("未登录");
        }
        if (!"ADMIN".equals(operatorType)) {
            return Result.fail("仅管理员可创建用户");
        }
        if (name == null || name.isBlank() || rawPwd == null || rawPwd.isBlank()) {
            return Result.fail("姓名和密码不能为空");
        }
        String uid = generateUid();
        while (sysUserMapper.existsByUid(uid) > 0) {
            uid = generateUid();
        }
        SysUser user = new SysUser();
        user.setUid(uid);
        user.setName(name.trim());
        user.setPwd(passwordEncoder.encode(rawPwd));
        user.setType(type != null ? type : "USER");
        sysUserMapper.insertUser(user);
        Map<String, Object> data = new HashMap<>();
        data.put("uid", user.getUid());
        data.put("name", user.getName());
        data.put("type", user.getType());
        return Result.success("用户创建成功", data);
    }

    public Result deleteUser(String uid, HttpServletRequest request) {
        String operatorType = getOperatorType(request);
        if (operatorType == null) {
            return Result.fail("未登录");
        }
        if (!"ADMIN".equals(operatorType)) {
            return Result.fail("仅管理员可删除用户");
        }
        SysUser user = sysUserMapper.findByUid(uid);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        sysUserMapper.deleteByUid(uid);
        return Result.success("用户已删除");
    }

    public Result listUsers(HttpServletRequest request) {
        String operatorType = getOperatorType(request);
        if (operatorType == null) {
            return Result.fail("未登录");
        }
        if (!"ADMIN".equals(operatorType)) {
            return Result.fail("仅管理员可查看用户列表");
        }
        List<SysUser> users = sysUserMapper.findAll();
        return Result.success("查询成功", users);
    }

    private String getOperatorType(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("uid") == null) {
            return null;
        }
        return (String) session.getAttribute("type");
    }

    public Result initDefaultAdmin() {
        if (sysUserMapper.existsByUid("UADMIN00001") > 0) {
            return Result.fail("管理员账号已存在，无需重复初始化");
        }
        SysUser admin = new SysUser();
        admin.setUid("UADMIN00001");
        admin.setName("冯俊杰");
        admin.setPwd(passwordEncoder.encode("admin123"));
        admin.setType("ADMIN");
        sysUserMapper.insertUser(admin);
        return Result.success("管理员账号已创建，uid: UADMIN00001，密码: admin123");
    }

    private String generateUid() {
        StringBuilder sb = new StringBuilder("U");
        for (int i = 0; i < 10; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}