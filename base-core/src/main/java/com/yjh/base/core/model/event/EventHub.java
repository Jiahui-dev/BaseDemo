package com.yjh.base.core.model.event;

import android.os.Handler;
import android.os.Looper;

import com.yjh.base.core.lifecycle.Lifecycle;
import com.yjh.base.core.lifecycle.LifecycleEvent;
import com.yjh.base.utils.util.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 生命周期感知事件枢纽 (EventHub)
 * <p>
 * 特性：
 * 0 内存泄漏：基于 Controller 插槽与 BaseCore 生命周期绑定，自动解绑。
 * 0 数据倒灌：纯即时响应模式。
 * 线程安全：底层采用 ConcurrentHashMap + CopyOnWriteArrayList。
 * 主线程调度：支持在任意子线程 post，观察者回调统一安全的切回主线程执行。
 * 异常隔离：单个 Observer 执行报错不会中断其他 Observer，更不会引发 App 崩溃。
 * <p>
 * Created by jiahui on 2026/08/08
 */
public class EventHub {

    private static final String TAG = "EventHub";

    // 全局事件路由器
    private static final Map<Class<?>, List<Observer<?>>> OBSERVERS = new ConcurrentHashMap<>();

    // 主线程 Handler，用于安全分发
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    /**
     * 事件观察者接口
     */
    public interface Observer<T> {
        void onEvent(T event);
    }

    public static class Subscription {
        private final Class<?> eventClass;
        private final Observer<?> observer;

        public Subscription(Class<?> eventClass, Observer<?> observer) {
            this.eventClass = eventClass;
            this.observer = observer;
        }

        public void unsubscribe() {
            List<Observer<?>> list = OBSERVERS.get(eventClass);
            if (list != null) {
                list.remove(observer);
                if (list.isEmpty()) {
                    OBSERVERS.remove(eventClass);
                }
            }
        }
    }

    /**
     * 发送事件
     */
    public static <T> void post(final T event) {
        if (event == null) {
            return;
        }
        Class<?> clazz = event.getClass();
        List<Observer<?>> list = OBSERVERS.get(clazz);
        if (list != null && !list.isEmpty()) {
            Runnable dispatchTask = () -> {
                for (Observer<?> observer : list) {
                    try {
                        // 异常隔离，防止单个 Observer 崩溃影响整体
                        ((Observer<T>) observer).onEvent(event);
                    } catch (Exception e) {
                        LogUtils.error(TAG, "Error delivering event: " + clazz.getSimpleName());
                    }
                }
            };
            if (Looper.myLooper() == Looper.getMainLooper()) {
                dispatchTask.run();
            } else {
                MAIN_HANDLER.post(dispatchTask);
            }
        }
    }

    public static <T> Subscription subscribe(Class<T> eventClass, Observer<T> observer) {
        List<Observer<?>> list = OBSERVERS.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>());
        list.add(observer);
        return new Subscription(eventClass, observer);
    }

    /**
     * 自动解绑的生命周期 Controller 插件
     */
    public static class Controller implements Lifecycle {
        private final List<Subscription> mSubscriptions = new ArrayList<>();

        /**
         * 链式订阅事件
         */
        public <T> Controller observe(Class<T> eventClass, Observer<T> observer) {
            if (eventClass != null && observer != null) {
                Subscription subscription = EventHub.subscribe(eventClass, observer);
                mSubscriptions.add(subscription);
            }
            return this;
        }

        @Override
        public void onLifecycleChanged(LifecycleEvent event) {
            if (event == LifecycleEvent.ON_DESTROY) {
                // 页面销毁时自动解绑当前 Controller 挂载的所有事件
                for (Subscription sub : mSubscriptions) {
                    sub.unsubscribe();
                }
                mSubscriptions.clear();
            }
        }
    }

}
