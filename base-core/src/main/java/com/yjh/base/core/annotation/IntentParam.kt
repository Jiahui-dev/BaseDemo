package com.yjh.base.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by jiahui on 2026/07/11
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IntentParam {
    /**
     * Intent传值对应的Key
     * 如果保持默认（空字符串），框架会自动使用对应的变量名作为Key
     */
    String name() default "";
}
