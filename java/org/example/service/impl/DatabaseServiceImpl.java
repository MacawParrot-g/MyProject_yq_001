package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.entity.TestStatic;
import org.example.mapper.GeneranMapper;
import org.example.service.DatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DatabaseServiceImpl implements DatabaseService {

    @Autowired
    private GeneranMapper generanMapper;

    @Override
    public int insertRecord(TestStatic record) {
        return generanMapper.insert(record);
    }

    @Override
    public List<TestStatic> queryByViewType(String ascribe, boolean frozenOnly) {
        LambdaQueryWrapper<TestStatic> wrapper = new LambdaQueryWrapper<>();

        if (ascribe != null && !ascribe.isEmpty()) {
            wrapper.like(TestStatic::getAscribe, ascribe);
        }

        if (frozenOnly) {
            wrapper.like(TestStatic::getRemark, "已冻结");
        }

        return generanMapper.selectList(wrapper);
    }

    @Override
    public int updateRecord(TestStatic record) {
        return generanMapper.update(null,
                new LambdaQueryWrapper<TestStatic>()
                        .eq(TestStatic::getUrl, record.getUrl())
        );
    }

    @Override
    public int deleteRecord(String url) {
        return generanMapper.delete(
                new LambdaQueryWrapper<TestStatic>()
                        .eq(TestStatic::getUrl, url)
        );
    }
}
