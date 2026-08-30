package org.example.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) // 作用于方法
@Retention(RetentionPolicy.RUNTIME)// 运行时可见
public @interface LogExecutionTime {
    String value() default "";//干嘛的
    boolean logParams() default false;
}