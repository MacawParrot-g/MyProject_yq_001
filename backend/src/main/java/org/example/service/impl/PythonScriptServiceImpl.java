//package org.example.service.impl;
//
//import org.example.common.Result;
//import org.example.entity.TestStatic;
//import org.example.mapper.GeneranMapper;
//import org.example.service.PythonScriptService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.io.BufferedReader;
//import java.io.InputStreamReader;
//
//@Service
//public class PythonScriptServiceImpl implements PythonScriptService {
//
//    @Autowired
//    private GeneranMapper generanMapper;
//
//    @Override
//    public int insertRecord(TestStatic record) {
//        return generanMapper.insert(record);
//    }
//
//    @Override
//    public Result executeExport(String pythonScriptPath) {
//        int notExportedCount = generanMapper.countNotExported();
//        if (notExportedCount == 0) {
//            return Result.fail("所有数据已被导出，无需重复执行");
//        }
//
//        try {
//            ProcessBuilder pb = new ProcessBuilder("python", pythonScriptPath);
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
//                return Result.success("导出成功", output.toString());
//            } else {
//                return Result.fail("Python脚本执行失败，退出码：" + exitCode + "，输出：" + output.toString());
//            }
//        } catch (Exception e) {
//            return Result.fail("执行异常：" + e.getMessage());
//        }
//    }
//}
