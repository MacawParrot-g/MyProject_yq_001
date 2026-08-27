package org.example.aop;

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
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

@Aspect
@Component
public class LogExecutionTimeAspect {

    private static final Logger logger = LoggerFactory.getLogger(LogExecutionTimeAspect.class);

    @Pointcut("@annotation(logExecutionTime)")
    public void loggableMethod(LogExecutionTime logExecutionTime) {}

    @Around("loggableMethod(logExecutionTime)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint, LogExecutionTime logExecutionTime) throws Throwable {
        long startTime = System.currentTimeMillis();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();
        String customDescription = logExecutionTime.value();

        String username = UserContext.getUsername();
        String ip = UserContext.getIp();

        MDC.put("username", username != null ? username : "anonymous");

        Object result;
        boolean success = true;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            success = false;
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            StringBuilder logMessage = new StringBuilder();
            logMessage.append("【日志】");

            if (customDescription != null && !customDescription.isEmpty()) {
                logMessage.append(customDescription).append(" | ");
            }

            logMessage.append("用户: ").append(username != null ? username : "anonymous")
                    .append(" | ip: ").append(ip != null ? ip : "unknown")
                    .append(" | 操作所属类: ").append(className)
                    .append(" | 方法: ").append(methodName)
                    .append(" | 状态: ").append(success ? "SUCCESS" : "FAILED")
                    .append(" | 耗时(ms): ").append(duration).append("ms");

            if (logExecutionTime.logParams()) {
                Object[] args = joinPoint.getArgs();
                if (args != null && args.length > 0) {
                    logMessage.append(" | params: ").append(Arrays.toString(args));
                }
            }

            if (!success) {
                logger.error(logMessage.toString());
            } else if (duration > 1000) {
                logger.warn(logMessage.toString());
            } else {
                logger.info(logMessage.toString());
            }

            MDC.remove("username");
        }
    }
}
