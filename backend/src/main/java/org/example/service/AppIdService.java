package org.example.service;

public interface AppIdService {
    boolean isAppIdExists(Long appId);
    void saveAppId(String bundleId, Long appId);
    void warmUpAppIdCache();
}
