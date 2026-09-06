package org.example.entity;

import lombok.Data;

@Data
public class UserSessionInfo {
    private String uid;
    private String name;
    private String type;
    private String sessionId;
    private boolean online;
    private long loginTime;
}
