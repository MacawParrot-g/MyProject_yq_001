package org.example.service;

import org.example.entity.TestStatic;

import java.util.List;

public interface DatabaseService {
    int insertRecord(TestStatic record);

    List<TestStatic> queryByViewType(String ascribe, boolean frozenOnly);

    int updateRecord(TestStatic record);

    int deleteRecord(String url);
}
