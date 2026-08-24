package org.example.service.impl;

import org.example.common.DataViewType;
import org.example.common.Result;
import org.example.entity.TestStatic;
import org.example.mapper.GeneranMapper;
import org.example.mq.RecordMessageProducer;
import org.example.mq.RecordOperateMessageProducer;
import org.example.service.DatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Order(1)
public class DatabaseServiceImpl implements DatabaseService, CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseServiceImpl.class);

    @Autowired
    private GeneranMapper generanMapper;

    @Autowired
    private RecordMessageProducer recordMessageProducer;

    @Autowired
    private RecordOperateMessageProducer recordOperateMessageProducer;

    @Autowired
    private ConnectionFactory rabbitConnectionFactory;

    private final AtomicBoolean rabbitAvailable = new AtomicBoolean(false);

    @Override
    public Result getNumberByName(TestStatic record){
        int number=generanMapper.count(record.getRecorder(),record.getRecordData());
        return Result.success("已返回当前数量",number);
    };

    @Override
    @Transactional(readOnly = true)
    public Result getDailyReport(String recordData) {
        if (recordData == null || recordData.isEmpty()) {
            return Result.fail("日期不能为空");
        }
        int totalCount = generanMapper.countByRecordData(recordData);
        int qualifiedCount = generanMapper.countQualifiedByRecordData(recordData);
        int appflyerCount = generanMapper.countAppflyerByDate(recordData);
        int adjustCount = generanMapper.countAdjustByDate(recordData);
        int singularCount = generanMapper.countSingularByDate(recordData);
        int tenjinCount = generanMapper.countTenjinByDate(recordData);

        Map<String, Object> result = Map.of(
                "totalCount", totalCount,
                "qualifiedCount", qualifiedCount,
                "unqualifiedCount", totalCount - qualifiedCount,
                "qualifyRate", totalCount > 0 ? Math.round(qualifiedCount * 10000.0 / totalCount) / 100.0 : 0.0,
                "attributions", Map.of(
                        "appflyer", appflyerCount,
                        "adjust", adjustCount,
                        "singular", singularCount,
                        "tenjin", tenjinCount
                )
        );
        return Result.success("查询日报成功", result);
    }


    @Override
    @Transactional(readOnly = true)
    public Result queryByPage(String ascribe, boolean frozenOnly, String recorder, int page, int size) {
        int offset = (page - 1) * size;
        List<TestStatic> list = generanMapper.selectByConditionPaged(ascribe, frozenOnly, recorder, size, offset);
        long total = generanMapper.countByCondition(ascribe, frozenOnly, recorder);

        DataViewType viewType = DataViewType.ALL;
        boolean hasAscribe = ascribe != null && !ascribe.isEmpty();
        if (hasAscribe && frozenOnly) {
            try { viewType = DataViewType.valueOf(ascribe.toUpperCase() + "_FROZEN"); }
            catch (IllegalArgumentException e) { viewType = DataViewType.FROZEN; }
        } else if (frozenOnly) {
            viewType = DataViewType.FROZEN;
        } else if (hasAscribe) {
            try { viewType = DataViewType.valueOf(ascribe.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }

        return Result.success("查询成功，共 " + total + " 条", list, viewType, total, page, size);
    }

    @Override
    public void run(String... args) {
        checkRabbitMQConnection();
    }

    @Scheduled(fixedDelay = 30000, initialDelay = 30000)
    public void scheduledRabbitMQCheck() {
        if (!rabbitAvailable.get()) {
            checkRabbitMQConnection();
        }
    }

    private void checkRabbitMQConnection() {
        log.info("正在检测RabbitMQ连接...");
        try {
            rabbitConnectionFactory.createConnection().close();
            rabbitAvailable.set(true);
            log.info("✅ RabbitMQ连接成功，数据操作模式：异步消息队列");
        } catch (Exception e) {
            rabbitAvailable.set(false);
            log.warn("⚠️ RabbitMQ连接失败，数据操作模式：降级为MySQL直连。原因: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertRecord(TestStatic record) {
        return generanMapper.insertRecord(record);
    }

    @Override
    public void submitRecordAsync(TestStatic record) {
        if (rabbitAvailable.get()) {
            boolean sent = recordMessageProducer.sendInsertMessage(record);
            if (sent) {
                return;
            }
            log.warn("⚠️ RabbitMQ投递失败，触发运行时降级，本次改为MySQL直连入库, URL: {}", record.getUrl());
            rabbitAvailable.set(false);
        }
        directInsert(record);
    }

    @Override
    public void submitUpdateAsync(TestStatic record) {
        if (rabbitAvailable.get()) {
            boolean sent = recordOperateMessageProducer.sendUpdateMessage(record);
            if (sent) {
                return;
            }
            log.warn("⚠️ RabbitMQ投递失败，触发运行时降级，本次改为MySQL直连更新, URL: {}", record.getUrl());
            rabbitAvailable.set(false);
        }
        try {
            generanMapper.updateRecord(record);
            log.info("📥 MySQL直连更新成功（降级模式）, URL: {}", record.getUrl());
        } catch (Exception e) {
            log.error("❌ MySQL直连更新也失败, URL: {}, 原因: {}", record.getUrl(), e.getMessage());
            throw new RuntimeException("更新失败：" + e.getMessage(), e);
        }
    }

    @Override
    public void submitDeleteAsync(String url) {
        if (rabbitAvailable.get()) {
            boolean sent = recordOperateMessageProducer.sendDeleteMessage(url);
            if (sent) {
                return;
            }
            log.warn("⚠️ RabbitMQ投递失败，触发运行时降级，本次改为MySQL直连删除, URL: {}", url);
            rabbitAvailable.set(false);
        }
        try {
            generanMapper.deleteByURL(url);
            log.info("📥 MySQL直连删除成功（降级模式）, URL: {}", url);
        } catch (Exception e) {
            log.error("❌ MySQL直连删除也失败, URL: {}, 原因: {}", url, e.getMessage());
            throw new RuntimeException("删除失败：" + e.getMessage(), e);
        }
    }

    private void directInsert(TestStatic record) {
        try {
            generanMapper.insertRecord(record);
            log.info("📥 MySQL直连入库成功（降级模式）, URL: {}", record.getUrl());
        } catch (Exception e) {
            log.error("❌ MySQL直连入库也失败, URL: {}, 原因: {}", record.getUrl(), e.getMessage());
            throw new RuntimeException("入库失败：" + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<TestStatic> getUnexportedRecords() {
        return generanMapper.selectUnexported();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<TestStatic> queryByViewType(String ascribe, boolean frozenOnly) {
        return generanMapper.selectByCondition(ascribe, frozenOnly);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateRecord(TestStatic record) {
        return generanMapper.updateRecord(record);
    }

    @Override
    public Result getRandomRecordForRetest(List<String> dates) {
        if (dates == null || dates.isEmpty()) {
            return Result.fail("日期列表不能为空");
        }
        TestStatic record = generanMapper.selectRandomByDates(dates);
        if (record == null) {
            return Result.fail("过去3天内没有可复测的数据");
        }
        Map<String, String> data = Map.of(
                "downloadUrl", record.getUrl(),
                "bundleId", record.getBundleId()
        );
        return Result.success("随机获取复测数据成功", data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRecord(String url) {
        return generanMapper.deleteByURL(url);
    }

    @Override
    @Transactional(readOnly = true)
    public Result adminSearch(String dateFrom, String dateTo, String bundleId, String keyword,
                              String exceptionType, String ascribe, boolean frozenOnly,
                              String recorder, Integer isOutput, int page, int size) {
        int offset = (page - 1) * size;
        List<TestStatic> list = generanMapper.adminSearch(dateFrom, dateTo, bundleId, keyword,
                exceptionType, ascribe, frozenOnly, recorder, isOutput, size, offset);
        long total = generanMapper.adminSearchCount(dateFrom, dateTo, bundleId, keyword,
                exceptionType, ascribe, frozenOnly, recorder, isOutput);
        return Result.success("查询成功，共 " + total + " 条", list, DataViewType.ALL, total, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public Result adminStats() {
        long totalCount = generanMapper.countAll();
        long exportedCount = generanMapper.countExported();
        long frozenCount = generanMapper.countFrozen();
        long unexportedCount = totalCount - exportedCount;
        List<String> exceptionTypes = generanMapper.selectDistinctExceptionTypes();
        List<String> recorders = generanMapper.selectDistinctRecorders();
        java.util.Map<String, Object> stats = java.util.Map.of(
                "totalCount", totalCount,
                "exportedCount", exportedCount,
                "unexportedCount", unexportedCount,
                "frozenCount", frozenCount,
                "exceptionTypes", exceptionTypes,
                "recorders", recorders
        );
        return Result.success("查询成功", stats);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result adminBatchDelete(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return Result.fail("URL列表不能为空");
        }
        int count = 0;
        for (String url : urls) {
            count += generanMapper.deleteByURL(url);
        }
        return Result.success("成功删除 " + count + " 条记录", count);
    }

    @Override
    @Transactional(readOnly = true)
    public Result adminSummary(String dateFrom, String dateTo, String bundleId, String keyword,
                               String exceptionType, String ascribe, boolean frozenOnly,
                               String recorder, Integer isOutput) {
        long totalCount = generanMapper.adminSearchCount(dateFrom, dateTo, bundleId, keyword,
                exceptionType, ascribe, frozenOnly, recorder, isOutput);
        long qualifiedCount = generanMapper.adminSearchQualifiedCount(dateFrom, dateTo, bundleId, keyword,
                exceptionType, ascribe, frozenOnly, recorder, isOutput);
        long appflyerCount = generanMapper.adminSearchAttrCount("appflyer", dateFrom, dateTo, bundleId, keyword,
                exceptionType, ascribe, frozenOnly, recorder, isOutput);
        long adjustCount = generanMapper.adminSearchAttrCount("adjust", dateFrom, dateTo, bundleId, keyword,
                exceptionType, ascribe, frozenOnly, recorder, isOutput);
        long singularCount = generanMapper.adminSearchAttrCount("singular", dateFrom, dateTo, bundleId, keyword,
                exceptionType, ascribe, frozenOnly, recorder, isOutput);
        long tenjinCount = generanMapper.adminSearchAttrCount("tenjin", dateFrom, dateTo, bundleId, keyword,
                exceptionType, ascribe, frozenOnly, recorder, isOutput);
        Map<String, Object> result = Map.of(
                "totalCount", totalCount,
                "qualifiedCount", qualifiedCount,
                "unqualifiedCount", totalCount - qualifiedCount,
                "qualifyRate", totalCount > 0 ? Math.round(qualifiedCount * 10000.0 / totalCount) / 100.0 : 0.0,
                "attributions", Map.of(
                        "appflyer", appflyerCount,
                        "adjust", adjustCount,
                        "singular", singularCount,
                        "tenjin", tenjinCount
                )
        );
        return Result.success("查询成功", result);
    }
}
