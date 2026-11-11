package com.yjh.blekit.diagnostics

/** 日志级别，数值越大越严重 */
enum class BleLogLevel {
    VERBOSE, DEBUG, INFO, WARN, ERROR
}

/** 原始字节（Tx / Rx）的记录策略 */
enum class CaptureBytes {
    /** 完全不记录字节，只记录事件 */
    Never,

    /** 默认：平时不记，发生异常时把此前最近几条字节附在错误记录上 */
    OnError,

    /** 全量记录，仅用于抓包排查 */
    Always,
}

/** 一条诊断事件。结构化而不是字符串，方便上报后做聚合统计 */
sealed interface BleEvent {
    data object ScanStarted : BleEvent
    data class ScanFailed(val errorCode: Int) : BleEvent
    data class Connected(val address: String) : BleEvent
    data class Disconnected(val status: Int) : BleEvent
    data class ServicesDiscovered(val count: Int) : BleEvent
    data class MtuChanged(val mtu: Int) : BleEvent
    data class Tx(val bytes: ByteArray) : BleEvent
    data class Rx(val bytes: ByteArray) : BleEvent
    data class Error(val reason: String) : BleEvent
}

/**
 * 一条带时间戳的记录。
 *
 * @param attachedBytes 仅在 [CaptureBytes.OnError] 模式下、且本条是异常记录时才有值
 */
data class BleRecord(
    val timeMs: Long,
    val level: BleLogLevel,
    val event: BleEvent,
    val address: String? = null,
    val attachedBytes: List<ByteArray> = emptyList(),
)

/**
 * 日志出口。用 `fun interface`，所以 Java 和 Kotlin 都能直接传 lambda。
 *
 * 约定：实现方必须立刻返回，不能阻塞。库内部在单独的协程里调用它，
 * 不会占用 GATT 回调线程。
 */
fun interface BleLogger {
    fun onLog(record: BleRecord)
}
