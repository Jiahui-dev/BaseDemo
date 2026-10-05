package com.yjh.base.core.event.hub

import android.os.Looper
import android.os.Handler
import com.yjh.base.core.lifecycle.Lifecycle
import com.yjh.base.core.lifecycle.LifecycleEvent
import com.yjh.base.utils.util.LogUtils
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 生命周期感知事件枢纽 (EventHub)
 *
 * 特性：
 * 0 内存泄漏：基于 Controller 插槽与 BaseCore 生命周期绑定，自动解绑。
 * 0 数据倒灌：纯即时响应模式。
 * 线程安全：底层采用 ConcurrentHashMap + CopyOnWriteArrayList。
 * 主线程调度：支持在任意子线程 post，观察者回调统一安全的切回主线程执行。
 * 异常隔离：单个 Observer 执行报错不会中断其他 Observer，更不会引发 App 崩溃。
 *
 * Created by youjiahui on 2026/10/4
 */
object EventHub {

    private const val TAG = "EventHub"

    // 全局事件路由器
    private val OBSERVERS = ConcurrentHashMap<Class<*>, CopyOnWriteArrayList<Observer<*>>>()

    // 主线程 Handler，用于安全分发
    private val MAIN_HANDLER = Handler(Looper.getMainLooper())

    /**
     * 事件观察者接口
     */
    interface Observer<T> {
        fun onEvent(event: T)
    }

    class Subscription(
        private val eventClass: Class<*>,
        private val observer: Observer<*>
    ) {
        fun unsubscribe() {
            OBSERVERS[eventClass]?.let { list ->
                list.remove(observer)
                if (list.isEmpty()) {
                    OBSERVERS.remove(eventClass)
                }
            }
        }
    }

    fun <T> subscribe(eventClass: Class<T>, observer: Observer<T>): Subscription {
        val list = OBSERVERS.computeIfAbsent(eventClass) { CopyOnWriteArrayList() }
        list.add(observer)
        return Subscription(eventClass, observer)
    }

    fun <T> post(event: T?) {
        event ?: return
        val clazz = event.javaClass
        OBSERVERS[clazz]?.takeIf { it.isNotEmpty() }?.let { list ->
            val task = Runnable {
                list.forEach {
                    try {
                        @Suppress("UNCHECKED_CAST")
                        (it as Observer<T>).onEvent(event)
                    } catch (e: Exception) {
                        LogUtils.error(TAG, "Error delivering event: ${clazz.simpleName}")
                    }
                }
            }
            if (Looper.myLooper() == Looper.getMainLooper()) task.run() else MAIN_HANDLER.post(task)
        }
    }

    class Controller : Lifecycle {

        private val mSubscriptions = ArrayList<Subscription>()

        override fun onLifecycleChanged(event: LifecycleEvent) {
            if (event == LifecycleEvent.ON_DESTROY) {
                mSubscriptions.forEach {
                    it.unsubscribe()
                }
                mSubscriptions.clear()
            }
        }

        fun <T> observe(eventClass: Class<T>, observer: Observer<T>): Controller {
            mSubscriptions += subscribe(eventClass, observer)
            return this
        }
    }
}