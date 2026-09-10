package org.example.service;

import org.example.common.Result;

import java.util.Map;

public interface TableManageService {

    Result listTables();

    Result describeTable(String tableName);

    Result createTable(String tableName, String columnDefinitions);

    Result dropTable(String tableName);

    Result addColumn(String tableName, String columnDefinition);

    Result modifyColumn(String tableName, String columnDefinition);

    Result dropColumn(String tableName, String columnName);

    Result executeSQL(String sql, String tableName);

    Result batchImport(Map<String, Object> params);
}
