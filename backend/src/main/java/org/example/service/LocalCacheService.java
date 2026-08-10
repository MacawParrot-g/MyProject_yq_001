package org.example.service;

public interface LocalCacheService {
    boolean exists(String downloadUrl, String bundleId);
    void save(String downloadUrl, String bundleId);
}