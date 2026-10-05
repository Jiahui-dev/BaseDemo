package com.yjh.base.core.router

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment
import com.yjh.base.core.annotation.IntentParam
import com.yjh.base.utils.util.LogUtils
import java.io.Serializable

/**
 * Created by youjiahui on 2026/10/2
 */
object BaseRouter {

    private const val TAG = "BaseRouter"

    /**
     * 通过反射把 Intent 的值赋给加了 @IntentParam 的字段
     */
    fun inject(target: Any?) {
        if (target == null) return
        val extras = getBundle(target) ?: return

        // 反射注入逻辑
        val fields = target.javaClass.declaredFields
        for (field in fields) {
            if (field.isAnnotationPresent(IntentParam::class.java)) {
                val intentParam = field.getAnnotation(IntentParam::class.java)!!
                val key = intentParam.name.ifEmpty { field.name }

                if (extras.containsKey(key)) {
                    try {
                        field.isAccessible = true
                        field.set(target, extras.get(key))
                    } catch (e: Exception) {
                        LogUtils.error(TAG, "参数注入失败：${field.name} -> ${e.message}")
                    }
                }

            }
        }

    }

    /**
     * 获取跳转建造者（传入强类型契约）
     */
    fun <T : Activity> build(routePath: IRoutePath<T>): Postcard<T> =
        Postcard(routePath.targetClass)

    fun <T : Activity> navigation(context: Context, postcard: Postcard<T>) {
        val intent = Intent(context, postcard.targetClass).apply {
            putExtras(postcard.bundle)
            if (postcard.flags != -1) {
                flags = postcard.flags
            }
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(intent)
    }

    class Postcard<T : Activity>(val targetClass: Class<T>) {
        val bundle = Bundle()
        var flags = -1

        fun with(extras: Bundle?): Postcard<T> = apply {
            extras?.let { bundle.putAll(it) }
        }

        fun withString(key: String, value: String?): Postcard<T> = apply {
            if (value != null) {
                bundle.putString(key, value)
            } else {
                bundle.remove(key)
            }
        }

        fun withInt(key: String, value: Int): Postcard<T> = apply {
            bundle.putInt(key, value)
        }

        fun withLong(key: String, value: Long): Postcard<T> = apply {
            bundle.putLong(key, value)
        }

        fun withDouble(key: String, value: Double): Postcard<T> = apply {
            bundle.putDouble(key, value)
        }

        fun withBoolean(key: String, value: Boolean): Postcard<T> = apply {
            bundle.putBoolean(key, value)
        }

        fun withFlags(flags: Int): Postcard<T> = apply {
            this.flags = flags
        }

        fun withSerializable(key: String, value: Serializable?): Postcard<T> = apply {
            bundle.putSerializable(key, value)
        }

        fun withParcelable(key: String, value: Parcelable?): Postcard<T> = apply {
            bundle.putParcelable(key, value)
        }

        fun navigation(context: Context) {
            BaseRouter.navigation(context, this)
        }

    }

    private fun getBundle(target: Any?): Bundle? = when (target) {
        is Activity -> target.intent?.extras

        is Fragment -> {
            target.arguments ?: target.activity?.intent?.extras
        }

        else -> null
    }

}