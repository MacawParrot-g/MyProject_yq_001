package org.example.service;

import java.util.List;

public interface HashCacheService {
    boolean exists(String hash);
    void save(String hash);
    void remove(String hash);
    void warmUp(List<String> hashes);
    boolean isAvailable();
}
