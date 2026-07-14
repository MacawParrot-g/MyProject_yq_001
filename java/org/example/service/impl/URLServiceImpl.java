package org.example.service.impl;
import org.example.service.URLService;
import org.example.utis.RequestKil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class URLServiceImpl implements URLService {

    private static final Logger log = LoggerFactory.getLogger(URLServiceImpl.class);

    @Autowired
    private CacheStrategyService cacheStrategyService;

    @Autowired
    private RequestKil rk;

    @Override
    public Map<String, Object> proxyTask(){
        String remoteUrl = "https://d-reporter.de123.net/ad/play/task";
        Map<String, Object> response = rk.safeRemoteGet(remoteUrl);

        if (response != null && Boolean.TRUE.equals(response.get("success")) && response.get("data") != null) {
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            String downloadUrl = (String) data.get("downloadUrl");
            String bundleId = (String) data.get("bundleId");

            if (downloadUrl != null && bundleId != null) {
                try {
                    if (cacheStrategyService.exists(downloadUrl, bundleId)) {
                        Map<String, Object> dup = new HashMap<>();
                        dup.put("success", true);
                        dup.put("duplicate", true);
                        dup.put("resultMsg", "检测到重复URL，请点击刷新按钮");
                        return dup;
                    }
                    cacheStrategyService.save(downloadUrl, bundleId);
                } catch (Exception e) {
                    log.warn("缓存去重处理失败: {}", e.getMessage());
                }
            }
        }
        return response != null ? response : rk.errorResponse("远程接口返回空响应，请稍后重试");
    }

    @Override
    public Map<String, Object> proxyObtain(Long appleid) {
        String remoteUrl = "https://d-reporter.de123.net/ad/play/task?appId=" + appleid;
        Map<String, Object> response = rk.safeRemoteGet(remoteUrl);
        return response != null ? response : rk.errorResponse("远程接口返回空响应，请稍后重试");
    }
}
