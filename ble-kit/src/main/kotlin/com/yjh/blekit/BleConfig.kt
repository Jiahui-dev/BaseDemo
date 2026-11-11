package com.yjh.blekit

import com.yjh.blekit.diagnostics.BleLogLevel
import com.yjh.blekit.diagnostics.CaptureBytes

/**
 * 库的全局配置。
 *
 * @param logLevel release 建议 [BleLogLevel.WARN]，Debug 用 [BleLogLevel.VERBOSE]
 * @param bufferSize 环形缓冲保留的记录条数
 * @param captureBytes 原始字节记录策略，默认只在异常时附带
 */
data class BleConfig(
    val logLevel: BleLogLevel = BleLogLevel.WARN,
    val bufferSize: Int = 200,
    val captureBytes: CaptureBytes = CaptureBytes.OnError,
)
