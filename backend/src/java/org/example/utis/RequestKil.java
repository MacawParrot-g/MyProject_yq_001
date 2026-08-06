package org.example.utis;
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

    @Autowired
    private RestTemplate restTemplate;

    public Map<String, Object> safeRemoteGet(String url) {
        try {
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            log.error("远程请求失败 [{}]: {}", url, e.getMessage());
            return errorResponse("远程请求失败：" + e.getMessage());
        }
    }

    public Map<String, Object> errorResponse(String msg) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", false);
        map.put("resultMsg", msg);
        return map;
    }
}
