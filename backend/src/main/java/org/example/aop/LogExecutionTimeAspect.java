package org.example.aop;

import jakarta.servlet.http.HttpServletResponse;
import org.example.annotation.LogExecutionTime;
import org.example.common.UserContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.example.entity.AuditLog;
import org.example.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
@Component
public class LogExecutionTimeAspect {

    private static final Logger log = LoggerFactory.getLogger(LogExecutionTimeAspect.class);

    @Autowired
    private AuditLogService auditLogService;

    @Pointcut("@annotation(logExecutionTime)")
    public void loggableMethod(LogExecutionTime logExecutionTime) {}

    @Around("@annotation(logExecutionTime)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint, LogExecutionTime logExecutionTime) throws Throwable {
        long start = System.currentTimeMillis();

        String methodName = joinPoint.getSignature().toShortString();
        log.info("🚀 [{}] 开始执行", logExecutionTime.value());

        Object[] args = joinPoint.getArgs();
        String operator = "anonymous";
        String operatorType = "UNKNOWN";
        String ip = "unknown";

        for (Object arg : args) {
            if (arg instanceof HttpServletRequest) {
                HttpServletRequest request = (HttpServletRequest) arg;
                operator = request.getHeader("X-User-Name");
                if (operator == null) operator = "anonymous";
                HttpSession session = request.getSession(false);
                if (session != null && session.getAttribute("type") != null) {
                    operatorType = (String) session.getAttribute("type");
                }
                ip = getClientIp(request);
                break;
            }
        }

        Object result = null;
        boolean success = true;
        String errorMsg = null;
        try {
            result = joinPoint.proceed(args);
            if (result instanceof org.example.common.Result) {
                success = ((org.example.common.Result) result).isSuccess();
                if (!success) {
                    errorMsg = ((org.example.common.Result) result).getMessage();
                }
            }
            log.info("✅ [{}] 执行完成，耗时: {}ms", logExecutionTime.value(), System.currentTimeMillis() - start);
        } catch (Throwable t) {
            success = false;
            errorMsg = t.getMessage();
            log.error("❌ [{}] 执行异常: {}", logExecutionTime.value(), t.getMessage());
            throw t;
        } finally {
            long duration = System.currentTimeMillis() - start;
            String uri = methodName;
            for (Object arg : args) {
                if (arg instanceof HttpServletRequest) {
                    uri = ((HttpServletRequest) arg).getRequestURI();
                    break;
                }
            }

            String params = Arrays.stream(args)
                    .filter(a -> !(a instanceof HttpServletRequest) && !(a instanceof HttpServletResponse))
                    .map(a -> {
                        try { return a.toString().substring(0, Math.min(a.toString().length(), 200)); }
                        catch (Exception e) { return a.getClass().getSimpleName(); }
                    })
                    .collect(Collectors.joining(", "));

            AuditLog auditLog = new AuditLog();
            auditLog.setOperator(operator);
            auditLog.setOperatorType(operatorType);
            auditLog.setAction(logExecutionTime.value());
            auditLog.setMethod(joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName());
            auditLog.setUri(uri);
            auditLog.setParams(params.length() > 500 ? params.substring(0, 500) : params);
            auditLog.setIp(ip);
            auditLog.setSuccess(success);
            auditLog.setErrorMessage(errorMsg);
            auditLog.setDurationMs(duration);
            auditLog.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            auditLogService.recordAuditLogAsync(auditLog);
        }
        return result;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

}
