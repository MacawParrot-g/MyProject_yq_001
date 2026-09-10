package org.example.service;

import java.util.Map;

public interface AppIdService {
    boolean isAppIdExists(Long appId);
    void saveAppId(String bundleId, Long appId);
    void warmUpAppIdCache();
    Map<String, Object> lookupByBundleId(String bundleId);
    boolean saveIfNotExist(String bundleId, Long appId);
}
