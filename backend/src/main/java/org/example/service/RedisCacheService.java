package org.example.service;

public interface RedisCacheService {
    boolean exists(String url, String bundleId);
    void save(String url, String bundleId);
    boolean isAvailable();
    void warmUpFromMySQL(java.util.List<org.example.entity.TestStatic> records);
}