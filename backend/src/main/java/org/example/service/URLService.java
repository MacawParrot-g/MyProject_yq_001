package org.example.service;
import java.util.Map;

public interface URLService {
    Map<String, Object> proxyTask();
    Map<String, Object> proxyObtain(Long appleid);
    Map<String, Object> proxyEvent(String bundleId);
    Map<String, Object> proxyAttribution(String bundleId, String type);
    Map<String, Object> proxyFrozen(Long id);
}
