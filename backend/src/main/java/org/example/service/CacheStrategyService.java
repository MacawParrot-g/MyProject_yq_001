package org.example.service;

public interface CacheStrategyService {

    boolean exists(String downloadUrl, String bundleId);
    void save(String downloadUrl, String bundleId);
    boolean isRedisAvailable();
    boolean isDedupEnabled();
}
