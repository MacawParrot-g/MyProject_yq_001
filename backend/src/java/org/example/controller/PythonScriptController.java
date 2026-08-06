//package org.example.controller;
//import org.example.common.Result;
//import org.example.mapper.GeneranMapper;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.client.RestTemplate;
//
//import java.io.BufferedReader;
//import java.io.InputStreamReader;
//
//
//@RestController
//public class PythonScriptController {
//
//    @Autowired
//    private GeneranMapper generanMapper;
//
//    @Value("${export.python-exe-path}")
//    private String pythonExePath;
//
//    @Value("${export.python-script-path}")
//    private String pythonScriptPath;
//
//    @GetMapping("/check")
//    public Result checkExport() {
//        try {
//            ProcessBuilder pb = new ProcessBuilder(pythonExePath, pythonScriptPath, "check");
//            pb.redirectErrorStream(true);
//            Process process = pb.start();
//
//            StringBuilder output = new StringBuilder();
//            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    output.append(line).append("\n");
//                }
//            }
//
//            int exitCode = process.waitFor();
//            if (exitCode == 0) {
//                return Result.success("检查完成", output.toString().trim());
//            } else {
//                return Result.fail("检查失败：" + output.toString().trim());
//            }
//        } catch (Exception e) {
//            return Result.fail("执行异常：" + e.getMessage());
//        }
//    }
//
//    @PostMapping("/execute")
//    public Result executeExport() {
//        int notExportedCount = generanMapper.countNotExported();
//        if (notExportedCount == 0) {
//            return Result.fail("所有数据已被导出，无需重复执行");
//        }
//
//        try {
//            ProcessBuilder pb = new ProcessBuilder(pythonExePath, pythonScriptPath, "export");
//            pb.redirectErrorStream(true);
//            Process process = pb.start();
//
//            StringBuilder output = new StringBuilder();
//            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    output.append(line).append("\n");
//                }
//            }
//
//            int exitCode = process.waitFor();
//            if (exitCode == 0) {
//                generanMapper.markAllAsExported();
//                return Result.success("导出成功", output.toString().trim());
//            } else {
//                return Result.fail("Python脚本执行失败，退出码：" + exitCode + "\n" + output.toString().trim());
//            }
//        } catch (Exception e) {
//            return Result.fail("执行异常：" + e.getMessage());
//        }
//    }
//}
