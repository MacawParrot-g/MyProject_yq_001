package org.example.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class RequestKil {

    private static final Logger log = LoggerFactory.getLogger(RequestKil.class);
    private static final int MAX_RETRIES = 2;

    @Autowired
    private RestTemplate restTemplate;

    public Map<String, Object> safeRemoteGet(String url) {
        for (int i = 0; i <= MAX_RETRIES; i++) {
            try {
                return restTemplate.getForObject(url, Map.class);
            } catch (Exception e) {
                if (i < MAX_RETRIES) {
                    log.warn("远程请求失败 [{}]，第{}次重试: {}", url, i + 1, e.getMessage());
                    try {
                        Thread.sleep(500L * (i + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return errorResponse("请求被中断");
                    }
                } else {
                    log.error("远程请求最终失败 [{}]: {}", url, e.getMessage());
                    return errorResponse("远程请求失败：" + e.getMessage());
                }
            }
        }
        return errorResponse("远程请求失败");
    }

    public Map<String, Object> errorResponse(String msg) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", false);
        map.put("resultMsg", msg);
        return map;
    }
}
