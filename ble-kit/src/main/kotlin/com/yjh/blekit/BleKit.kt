package com.yjh.blekit

import android.content.Context
import com.yjh.blekit.diagnostics.BleLogger
import com.yjh.blekit.diagnostics.BleLogLevel
import com.yjh.blekit.diagnostics.BleRecord
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 库的唯一入口。一个实例管理一台设备，需要多设备就创建多个实例。
 *
 * 对外只暴露“状态”和“数据”两条流，不暴露任何 Android BLE 回调类型，
 * 这样以后换实现（协程化、Compose 化）不会破坏调用方代码。
 */
interface BleKit {

    /**
     * 发起连接。内部流程：注册到全局扫描调度器 → 扫描 → 连接 → 发现服务。
     * 断线后自动指数退避重连，直到调用 [disconnect] 或 [close]。
     */
    fun connect(profile: DeviceProfile)

    /** 主动断开，并停止自动重连 */
    fun disconnect()

    /** 释放全部资源。调用后该实例不可再用 */
    fun close()

    /** 链路状态；StateFlow 会立刻把当前值推给新订阅者 */
    val state: StateFlow<ConnectionState>

    /** 设备上报的数据；SharedFlow 只推新包，不重放历史 */
    val packets: SharedFlow<ByteArray>

    /**
     * 所有扫描到的广播，用于调试界面展示“到底扫到了什么”。
     * 正常业务不需要订阅它。
     */
    val advertisements: SharedFlow<BleAdvertisement>

    /** 写入数据，内部按当前 MTU 分包 */
    suspend fun write(data: ByteArray)

    /** 写入并等待设备响应（带响应写） */
    suspend fun writeAndAwait(data: ByteArray): ByteArray

    /** 取最近 N 条诊断记录，用于现场问题复盘 */
    fun recentRecords(count: Int = 200): List<BleRecord>

    companion object {

        /** 全局日志出口，通常在 Application 里设置一次 */
        @JvmStatic
        var logger: BleLogger? = null

        /** 全局日志级别；低于该级别的事件只进环形缓冲，不回调 [logger] */
        @JvmStatic
        var logLevel: BleLogLevel = BleLogLevel.WARN

        @JvmStatic
        fun create(context: Context, config: BleConfig = BleConfig()): BleKit =
            DefaultBleKit(context.applicationContext, config)
    }
}
