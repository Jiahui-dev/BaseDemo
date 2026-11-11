package com.yjh.blekit.scan

import com.yjh.blekit.BleAdvertisement
import com.yjh.blekit.BleScanFilter

/**
 * 一个等待连接的目标。BleKit 实例 connect() 时向 [ScanArbiter] 注册一个 Target。
 */
internal class ScanTarget(
    val id: String,
    val filters: List<BleScanFilter>,
    val matches: (BleAdvertisement) -> Boolean,
    val onFound: (BleAdvertisement) -> Unit,
)

/** 一次物理扫描的句柄。stop() 之后不应再有回调。 */
internal interface ScanSession {
    fun stop()
}

/**
 * 对底层 `BluetoothLeScanner` 的最小抽象。
 *
 * 抽这一层是为了单测：测试里用 FakeScanner 记录“用的是过滤扫描还是合扫”、
 * 以及结果分发到了哪些 target。
 */
internal interface Scanner {
    /** 带系统过滤的扫描（省电，单设备时使用） */
    fun startFiltered(
        filters: List<BleScanFilter>,
        onResult: (BleAdvertisement) -> Unit,
    ): ScanSession

    /** 无过滤扫描（多设备时使用，结果由上层分发） */
    fun startUnfiltered(onResult: (BleAdvertisement) -> Unit): ScanSession
}

/** 时间来源。单测里用可控时钟，避免真的等 30 秒。 */
internal fun interface Clock {
    fun now(): Long
}

/** 延迟执行。单测里配合 [Clock] 快进时间。 */
internal fun interface Scheduler {
    fun schedule(delayMs: Long, action: () -> Unit)
}
