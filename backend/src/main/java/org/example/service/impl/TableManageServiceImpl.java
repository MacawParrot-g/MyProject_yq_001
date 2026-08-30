// 文件路径: src/main/java/org/example/service/impl/TableManageServiceImpl.java
package org.example.service.impl;

import org.example.common.Result;
import org.example.service.TableManageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TableManageServiceImpl implements TableManageService {

    private static final Logger log = LoggerFactory.getLogger(TableManageServiceImpl.class);

    private static final Set<String> DANGEROUS_KEYWORDS = Set.of(
            "DROP DATABASE", "TRUNCATE DATABASE", "SHUTDOWN"
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Result listTables() {
        try {
            List<Map<String, Object>> tables = jdbcTemplate.queryForList("SHOW TABLES");
            List<String> tableNames = new ArrayList<>();
            for (Map<String, Object> row : tables) {
                tableNames.add(row.values().iterator().next().toString());
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("count", tableNames.size());
            data.put("tables", tableNames);
            return Result.success("查询成功，共 " + tableNames.size() + " 张表", data);
        } catch (Exception e) {
            log.error("查询表列表失败: {}", e.getMessage());
            return Result.fail("查询表列表失败：" + e.getMessage());
        }
    }

    @Override
    public Result describeTable(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return Result.fail("表名不能为空");
        }
        if (!isValidTableName(tableName)) {
            return Result.fail("表名包含非法字符");
        }
        try {
            List<Map<String, Object>> columns = jdbcTemplate.queryForList("SHOW FULL COLUMNS FROM `" + tableName + "`");
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, Object> col : columns) {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("field", col.get("Field"));
                info.put("type", col.get("Type"));
                info.put("null", col.get("Null"));
                info.put("key", col.get("Key"));
                info.put("default", col.get("Default"));
                info.put("extra", col.get("Extra"));
                info.put("collation", col.get("Collation"));
                info.put("comment", col.get("Comment"));
                result.add(info);
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("tableName", tableName);
            data.put("columnCount", result.size());
            data.put("columns", result);
            return Result.success("查询成功，共 " + result.size() + " 个字段", data);
        } catch (Exception e) {
            log.error("查询表结构失败: {}", e.getMessage());
            return Result.fail("查询表结构失败：" + e.getMessage());
        }
    }

    @Override
    public Result createTable(String tableName, String columnDefinitions) {
        if (tableName == null || tableName.isBlank()) {
            return Result.fail("表名不能为空");
        }
        if (columnDefinitions == null || columnDefinitions.isBlank()) {
            return Result.fail("字段定义不能为空");
        }
        if (!isValidTableName(tableName)) {
            return Result.fail("表名包含非法字符");
        }
        try {
            String sql = "CREATE TABLE `" + tableName + "` (" + columnDefinitions + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
            jdbcTemplate.execute(sql);
            log.info("建表成功: {}", tableName);
            return Result.success("建表成功：" + tableName);
        } catch (Exception e) {
            log.error("建表失败: {}", e.getMessage());
            return Result.fail("建表失败：" + e.getMessage());
        }
    }

    @Override
    public Result dropTable(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return Result.fail("表名不能为空");
        }
        if (!isValidTableName(tableName)) {
            return Result.fail("表名包含非法字符");
        }
        try {
            jdbcTemplate.execute("DROP TABLE `" + tableName + "`");
            log.info("删表成功: {}", tableName);
            return Result.success("删表成功：" + tableName);
        } catch (Exception e) {
            log.error("删表失败: {}", e.getMessage());
            return Result.fail("删表失败：" + e.getMessage());
        }
    }

    @Override
    public Result addColumn(String tableName, String columnDefinition) {
        if (tableName == null || tableName.isBlank()) {
            return Result.fail("表名不能为空");
        }
        if (columnDefinition == null || columnDefinition.isBlank()) {
            return Result.fail("字段定义不能为空");
        }
        if (!isValidTableName(tableName)) {
            return Result.fail("表名包含非法字符");
        }
        try {
            String sql = "ALTER TABLE `" + tableName + "` ADD COLUMN " + columnDefinition;
            jdbcTemplate.execute(sql);
            log.info("新增字段成功: {}.{}", tableName, columnDefinition);
            return Result.success("新增字段成功");
        } catch (Exception e) {
            log.error("新增字段失败: {}", e.getMessage());
            return Result.fail("新增字段失败：" + e.getMessage());
        }
    }

    @Override
    public Result modifyColumn(String tableName, String columnDefinition) {
        if (tableName == null || tableName.isBlank()) {
            return Result.fail("表名不能为空");
        }
        if (columnDefinition == null || columnDefinition.isBlank()) {
            return Result.fail("字段定义不能为空");
        }
        if (!isValidTableName(tableName)) {
            return Result.fail("表名包含非法字符");
        }
        try {
            String sql = "ALTER TABLE `" + tableName + "` MODIFY COLUMN " + columnDefinition;
            jdbcTemplate.execute(sql);
            log.info("修改字段成功: {}", tableName);
            return Result.success("修改字段成功");
        } catch (Exception e) {
            log.error("修改字段失败: {}", e.getMessage());
            return Result.fail("修改字段失败：" + e.getMessage());
        }
    }

    @Override
    public Result dropColumn(String tableName, String columnName) {
        if (tableName == null || tableName.isBlank()) {
            return Result.fail("表名不能为空");
        }
        if (columnName == null || columnName.isBlank()) {
            return Result.fail("字段名不能为空");
        }
        if (!isValidTableName(tableName) || !isValidTableName(columnName)) {
            return Result.fail("名称包含非法字符");
        }
        try {
            String sql = "ALTER TABLE `" + tableName + "` DROP COLUMN `" + columnName + "`";
            jdbcTemplate.execute(sql);
            log.info("删除字段成功: {}.{}", tableName, columnName);
            return Result.success("删除字段成功");
        } catch (Exception e) {
            log.error("删除字段失败: {}", e.getMessage());
            return Result.fail("删除字段失败：" + e.getMessage());
        }
    }

    @Override
    public Result executeSQL(String sql, String tableName) {
        if (sql == null || sql.isBlank()) {
            return Result.fail("SQL语句不能为空");
        }
        String upperSql = sql.trim().toUpperCase();
        for (String keyword : DANGEROUS_KEYWORDS) {
            if (upperSql.contains(keyword)) {
                return Result.fail("禁止执行危险SQL操作：" + keyword);
            }
        }
        try {
            if (upperSql.startsWith("SELECT") || upperSql.startsWith("SHOW") || upperSql.startsWith("DESCRIBE") || upperSql.startsWith("EXPLAIN")) {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("rowCount", rows.size());
                data.put("rows", rows);
                data.put("sql", sql);
                return Result.success("查询成功，返回 " + rows.size() + " 行", data);
            } else {
                int affected = jdbcTemplate.update(sql);
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("affectedRows", affected);
                data.put("sql", sql);
                return Result.success("执行成功，影响 " + affected + " 行", data);
            }
        } catch (Exception e) {
            log.error("执行SQL失败: {}", e.getMessage());
            return Result.fail("执行SQL失败：" + e.getMessage());
        }
    }

    @Override
    public Result batchImport(Map<String, Object> params) {
        String sourceUrl = (String) params.get("sourceUrl");
        String sourceUsername = (String) params.get("sourceUsername");
        String sourcePassword = (String) params.get("sourcePassword");
        List<String> tables = (List<String>) params.get("tables");
        Boolean truncateBefore = (Boolean) params.get("truncateBefore");

        if (sourceUrl == null || sourceUrl.isBlank()) {
            return Result.fail("源数据库URL不能为空");
        }
        if (sourceUsername == null || sourceUsername.isBlank()) {
            return Result.fail("源数据库用户名不能为空");
        }
        if (tables == null || tables.isEmpty()) {
            return Result.fail("请至少选择一张要导入的表");
        }

        JdbcTemplate sourceJdbc = null;
        try {
            DriverManagerDataSource sourceDs = new DriverManagerDataSource();
            sourceDs.setDriverClassName("com.mysql.cj.jdbc.Driver");
            sourceDs.setUrl(sourceUrl);
            sourceDs.setUsername(sourceUsername);
            sourceDs.setPassword(sourcePassword != null ? sourcePassword : "");
            sourceJdbc = new JdbcTemplate(sourceDs);
            sourceJdbc.queryForObject("SELECT 1", Integer.class);

            Map<String, Object> importResult = new LinkedHashMap<>();
            List<Map<String, Object>> details = new ArrayList<>();
            int totalImported = 0;

            for (String table : tables) {
                if (!isValidTableName(table)) {
                    Map<String, Object> detail = new LinkedHashMap<>();
                    detail.put("table", table);
                    detail.put("status", "skipped");
                    detail.put("message", "表名包含非法字符");
                    details.add(detail);
                    continue;
                }
                try {
                    List<Map<String, Object>> rows = sourceJdbc.queryForList("SELECT * FROM `" + table + "`");
                    if (rows.isEmpty()) {
                        Map<String, Object> detail = new LinkedHashMap<>();
                        detail.put("table", table);
                        detail.put("status", "empty");
                        detail.put("message", "源表无数据");
                        detail.put("count", 0);
                        details.add(detail);
                        continue;
                    }

                    if (Boolean.TRUE.equals(truncateBefore)) {
                        try {
                            jdbcTemplate.execute("TRUNCATE TABLE `" + table + "`");
                        } catch (Exception ignored) {
                        }
                    }

                    int imported = 0;
                    for (Map<String, Object> row : rows) {
                        try {
                            StringBuilder insertSql = new StringBuilder("INSERT INTO `" + table + "` (");
                            StringBuilder values = new StringBuilder(" VALUES (");
                            List<Object> paramList = new ArrayList<>();
                            int idx = 0;
                            for (Map.Entry<String, Object> entry : row.entrySet()) {
                                if (idx > 0) {
                                    insertSql.append(", ");
                                    values.append(", ");
                                }
                                insertSql.append("`").append(entry.getKey()).append("`");
                                values.append("?");
                                paramList.add(entry.getValue());
                                idx++;
                            }
                            insertSql.append(")").append(values).append(")");
                            jdbcTemplate.update(insertSql.toString(), paramList.toArray());
                            imported++;
                        } catch (Exception rowEx) {
                            log.warn("导入行失败 [{}]: {}", table, rowEx.getMessage());
                        }
                    }

                    totalImported += imported;
                    Map<String, Object> detail = new LinkedHashMap<>();
                    detail.put("table", table);
                    detail.put("status", "success");
                    detail.put("count", imported);
                    detail.put("total", rows.size());
                    detail.put("message", "成功导入 " + imported + "/" + rows.size() + " 行");
                    details.add(detail);
                    log.info("批量导入: {} 成功 {}/{}", table, imported, rows.size());

                } catch (Exception tableEx) {
                    Map<String, Object> detail = new LinkedHashMap<>();
                    detail.put("table", table);
                    detail.put("status", "error");
                    detail.put("message", "导入失败：" + tableEx.getMessage());
                    details.add(detail);
                    log.error("批量导入表 {} 失败: {}", table, tableEx.getMessage());
                }
            }

            importResult.put("totalImported", totalImported);
            importResult.put("tableCount", tables.size());
            importResult.put("details", details);
            return Result.success("批量导入完成，共导入 " + totalImported + " 行数据", importResult);

        } catch (Exception e) {
            log.error("批量导入失败: {}", e.getMessage());
            return Result.fail("批量导入失败：" + e.getMessage());
        }
    }

    private boolean isValidTableName(String name) {
        return name != null && name.matches("^[a-zA-Z_][a-zA-Z0-9_]{0,63}$");
    }
}
