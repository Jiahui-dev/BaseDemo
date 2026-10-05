package com.yjh.base.core.lifecycle;

/**
 * Created by jiahui on 2026/07/11
 */
public enum LifecycleEvent {

    /**
     * 1.内存、数据结构初始化阶段
     */
    ON_INIT,

    /**
     * 2.视图绑定完毕，可以获取View、设置监听器
     */
    ON_VIEW_CREATED,

    /**
     * 3.触发首次数据请求的阶段
     */
    ON_DATA_INIT,

    /**
     * 4.页面即将可见
     */
    ON_START,

    /**
     * 5.页面完全可见、获得焦点
     */
    ON_RESUME,

    /**
     * 6.页面失去焦点
     */
    ON_PAUSE,

    /**
     * 7.页面不可见
     */
    ON_STOP,

    /**
     * 8.页面销毁，必须强制清理内存、切断引用
     */
    ON_DESTROY

}

