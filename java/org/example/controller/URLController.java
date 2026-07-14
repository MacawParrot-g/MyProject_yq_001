package org.example.controller;
import org.example.service.URLService;
import org.example.utis.RequestKil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class URLController {

    @Autowired
    private URLService urlService;

    @Autowired
    private RequestKil rk;

    @GetMapping("/api/proxy/task")
    public Map<String, Object> proxyTask() {
        return urlService.proxyTask();
    }

    @GetMapping("/api/proxy/obtain")
    public Map<String, Object> proxyObtain(@RequestParam Long appleid) {
        return urlService.proxyObtain(appleid);
    }

    @GetMapping("/api/proxy/event")
    public Map<String, Object> proxyEvent(@RequestParam String bundleId) {
        String url = "https://d-reporter.de123.net/ad/event/target?bundleId=" + bundleId;
        Map<String, Object> response = rk.safeRemoteGet(url);
        return response != null ? response : rk.errorResponse("事件接口返回空响应");
    }

    @GetMapping("/api/proxy/attribution")
    public Map<String, Object> proxyAttribution(@RequestParam String bundleId, @RequestParam String type) {
        String url = "https://d-reporter.de123.net/ad/" + type + "/event/?bundleId=" + bundleId;
        Map<String, Object> response = rk.safeRemoteGet(url);
        return response != null ? response : rk.errorResponse("归因接口返回空响应");
    }

    @GetMapping("/api/proxy/frozen")
    public Map<String, Object> proxyFrozen(@RequestParam Long id) {
        String url = "https://d-reporter.de123.net/ad/play/task/frozen?id=" + id;
        Map<String, Object> response = rk.safeRemoteGet(url);
        return response != null ? response : rk.errorResponse("冻结接口返回空响应");
    }

//    private Map<String, Object> safeRemoteGet(String url) {
//        try {
//            return restTemplate.getForObject(url, Map.class);
//        } catch (Exception e) {
//            log.error("远程请求失败 [{}]: {}", url, e.getMessage());
//            return rk.errorResponse("远程请求失败：" + e.getMessage());
//        }
//    }
//
//    private Map<String, Object> errorResponse(String msg) {
//        Map<String, Object> map = new HashMap<>();
//        map.put("success", false);
//        map.put("resultMsg", msg);
//        return map;
//    }
}
