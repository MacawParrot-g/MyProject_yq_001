package org.example.service.impl;

import org.example.common.DataViewType;
import org.example.common.Result;
import org.example.entity.TestStatic;
import org.example.mapper.GeneranMapper;
import org.example.mq.RecordMessageProducer;
import org.example.mq.RecordOperateMessageProducer;
import org.example.service.DatabaseService;
import org.example.service.GradeService;
import org.example.service.HashCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
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

    @Autowired
    private GradeService gradeService;

    @Autowired
    private HashCacheService hashCacheService;

    private final AtomicBoolean rabbitAvailable = new AtomicBoolean(false);

    private String generateHash(TestStatic record) {
        try {
            long ts = System.currentTimeMillis();
            int rand = ThreadLocalRandom.current().nextInt(100000, 999999);
            String raw = (record.getUrl() != null ? record.getUrl() : "") +
                    (record.getBundleId() != null ? record.getBundleId() : "") +
                    ts + rand;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.substring(0, 32);
        } catch (Exception e) {
            return java.util.UUID.randomUUID().toString().replace("-", "");
        }
    }

    private String generateUniqueHash(TestStatic record) {
        int maxRetries = 10;
        for (int i = 0; i < maxRetries; i++) {
            String hash = generateHash(record);
            if (!hashCacheService.exists(hash)) {
                return hash;
            }
            log.warn("⚠️ Hash碰撞，第{}次重新生成, 原hash: {}", i + 1, hash);
        }
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

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
    public Result queryByPage(String ascribe, boolean frozenOnly, String recorder, String dateFrom, String dateTo, int page, int size) {
        int offset = (page - 1) * size;
        List<TestStatic> list = generanMapper.selectByConditionPaged(ascribe, frozenOnly, recorder, dateFrom, dateTo, size, offset);
        long total = generanMapper.countByCondition(ascribe, frozenOnly, recorder, dateFrom, dateTo);

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
        warmUpHashCache();
        gradeService.warmUpGradeCache();
    }

    private void warmUpHashCache() {
        try {
            log.info("🔄 开始预热hash缓存到Redis db10...");
            List<String> hashes = generanMapper.selectAllHashes();
            if (hashes != null && !hashes.isEmpty()) {
                hashCacheService.warmUp(hashes);
                log.info("✅ Hash缓存预热完成，共缓存 {} 条hash到Redis db10", hashes.size());
            } else {
                log.info("ℹ️ 数据库中暂无hash数据，跳过预热");
            }
        } catch (Exception e) {
            log.error("❌ Hash缓存预热失败: {}", e.getMessage());
        }
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
        if (record.getHash() == null || record.getHash().isEmpty()) {
            String hash = generateUniqueHash(record);
            record.setHash(hash);
        }
        int result = generanMapper.insertRecord(record);
        hashCacheService.save(record.getHash());
        return result;
    }

    @Override
    public void submitRecordAsync(TestStatic record) {
        String hash = generateUniqueHash(record);
        record.setHash(hash);

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
            log.warn("⚠️ RabbitMQ投递失败，触发运行时降级，本次改为MySQL直连更新, hash: {}", record.getHash());
            rabbitAvailable.set(false);
        }
        try {
            generanMapper.updateRecord(record);
            log.info("📥 MySQL直连更新成功（降级模式）, hash: {}", record.getHash());
        } catch (Exception e) {
            log.error("❌ MySQL直连更新也失败, hash: {}, 原因: {}", record.getHash(), e.getMessage());
            throw new RuntimeException("更新失败：" + e.getMessage(), e);
        }
    }

    @Override
    public void submitDeleteAsync(String hash) {
        if (rabbitAvailable.get()) {
            boolean sent = recordOperateMessageProducer.sendDeleteMessage(hash);
            if (sent) {
                return;
            }
            log.warn("⚠️ RabbitMQ投递失败，触发运行时降级，本次改为MySQL直连删除, hash: {}", hash);
            rabbitAvailable.set(false);
        }
        try {
            generanMapper.deleteByHash(hash);
            hashCacheService.remove(hash);
            log.info("📥 MySQL直连删除成功（降级模式）, hash: {}", hash);
        } catch (Exception e) {
            log.error("❌ MySQL直连删除也失败, hash: {}, 原因: {}", hash, e.getMessage());
            throw new RuntimeException("删除失败：" + e.getMessage(), e);
        }
    }

    private void directInsert(TestStatic record) {
        try {
            generanMapper.insertRecord(record);
            hashCacheService.save(record.getHash());
            log.info("📥 MySQL直连入库成功（降级模式）, hash: {}", record.getHash());
        } catch (Exception e) {
            log.error("❌ MySQL直连入库也失败, hash: {}, 原因: {}", record.getHash(), e.getMessage());
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
    public int deleteRecord(String hash) {
        int count = generanMapper.deleteByHash(hash);
        if (count > 0) {
            hashCacheService.remove(hash);
        }
        return count;
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
    public Result adminBatchDelete(List<String> hashes) {
        if (hashes == null || hashes.isEmpty()) {
            return Result.fail("hash列表不能为空");
        }
        int count = 0;
        for (String hash : hashes) {
            count += generanMapper.deleteByHash(hash);
            hashCacheService.remove(hash);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result batchImportRecords(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return Result.fail("导入数据不能为空");
        }

        String[] lines = rawText.split("\\r?\\n");
        List<TestStatic> records = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            String[] fields = line.split("\t", -1);
            if (fields.length < 7) {
                errors.add("第" + (i + 1) + "行：列数不足（需要至少7列Tab分隔，实际" + fields.length + "列）");
                continue;
            }

            try {
                TestStatic record = new TestStatic();
                String hash = generateUniqueHash(record);
                record.setHash(hash);
                record.setUrl(fields[0].trim().isEmpty() ? null : fields[0].trim());
                record.setBundleId(fields[1].trim().isEmpty() ? null : fields[1].trim());
                record.setAscribe(fields[2].trim().isEmpty() ? null : fields[2].trim());

                String eventNumStr = fields[3].trim();
                if (!eventNumStr.isEmpty()) {
                    try {
                        record.setEventNumber(Integer.parseInt(eventNumStr));
                    } catch (NumberFormatException e) {
                        record.setEventNumber(null);
                    }
                }

                record.setExceptionType(fields[4].trim().isEmpty() ? null : fields[4].trim());
                record.setRecordData(fields[5].trim().isEmpty() ? null : fields[5].trim());
                record.setRecorder(fields[6].trim().isEmpty() ? null : fields[6].trim());

                if (fields.length > 7) {
                    String extra = fields[7].trim();
                    if (extra.contains("已冻结")) {
                        record.setRemark("已冻结");
                    }
                }

                record.setIsOutput(1);
                records.add(record);
            } catch (Exception e) {
                errors.add("第" + (i + 1) + "行解析失败：" + e.getMessage());
            }
        }

        if (records.isEmpty()) {
            return Result.fail("没有成功解析到任何有效数据行。" + (errors.isEmpty() ? "" : " 错误：" + String.join("; ", errors)));
        }

        int batchSize = 50;
        int successCount = 0;
        for (int i = 0; i < records.size(); i += batchSize) {
            List<TestStatic> batch = records.subList(i, Math.min(i + batchSize, records.size()));
            try {
                generanMapper.batchInsertRecords(batch);
                for (TestStatic r : batch) {
                    hashCacheService.save(r.getHash());
                }
                successCount += batch.size();
            } catch (Exception e) {
                log.error("批量插入失败(batch start={}): {}", i, e.getMessage());
                for (TestStatic r : batch) {
                    try {
                        generanMapper.insertRecord(r);
                        hashCacheService.save(r.getHash());
                        successCount++;
                    } catch (Exception ex) {
                        log.error("单条插入也失败, hash={}: {}", r.getHash(), ex.getMessage());
                    }
                }
            }
        }

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("totalLines", lines.length);
        result.put("parsedCount", records.size());
        result.put("successCount", successCount);
        result.put("errorCount", errors.size());
        if (!errors.isEmpty()) {
            result.put("errors", errors);
        }

        String msg = "成功导入 " + successCount + " 条数据";
        if (!errors.isEmpty()) {
            msg += "，" + errors.size() + " 行解析失败";
        }
        return Result.success(msg, result);
    }
}