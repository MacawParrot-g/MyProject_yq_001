package org.example.service;

import org.example.common.Result;

public interface DedupSessionService {
    Result enableDedup(String username);
    Result disableDedup();
    Result getStatus();
    boolean isDedupEnabled();
    void checkAndRegenerateSession(String username);
}
