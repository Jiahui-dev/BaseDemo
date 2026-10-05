package com.yjh.base.core.lifecycle

/**
 * Created by youjiahui on 2026/10/2
 */
interface Lifecycle {
    /**
     * 状态机唯一入口：当生命周期发生状态流转时，此方法会被触发
     * @param event 当前流转到的生命周期阶段
     */
    fun onLifecycleChanged(event: LifecycleEvent)
}