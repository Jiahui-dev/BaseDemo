package com.yjh.blekit

import java.util.UUID

/**
 * 一条蓝牙广播的可移植快照。
 *
 * 不用 Android 的 `BluetoothDevice` 是为了让 [DeviceProfile.isTarget] 可以在
 * 纯 JVM 单测里验证，不必依赖真机。
 */
data class BleAdvertisement(
    /** 设备 MAC 地址，Android 12+ 未声明 BLUETOOTH_CONNECT 时可能为空串 */
    val address: String,
    /** 广播名，可能为空 */
    val name: String?,
    /** 信号强度 */
    val rssi: Int,
    /** 广播中携带的服务 UUID 列表，用于识别设备型号 */
    val serviceUuids: List<UUID> = emptyList(),
)
