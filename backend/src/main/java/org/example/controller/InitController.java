//package org.example.controller;
//
//import org.example.common.Result;
//import org.example.util.SysUserService;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/init")
//public class InitController {
//
//    private final SysUserService sysUserService;
//
//    public InitController(SysUserService sysUserService) {
//        this.sysUserService = sysUserService;
//    }
//
//    @GetMapping("/admin")
//    public Result initAdmin() {
//        return sysUserService.initDefaultAdmin();
//    }
//}