package com.yjh.base.core.annotation

/**
 * Created by youjiahui on 2026/10/3
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class IntentParam(
    /**
     * Intent传值对应的Key
     * 如果保持默认（空字符串），框架会自动使用对应的变量名作为Key
     */
    val name: String = ""
)
