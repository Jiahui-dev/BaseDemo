package com.yjh.blekit

import java.util.UUID

/**
 * 描述一台设备长什么样。
 *
 * 业务方实现这个接口，把 UUID、匹配规则、固件兼容开关告诉库；
 * 库负责扫描、连接、重连、分包，不关心报文内容。
 */
interface DeviceProfile {

    // ===== 通信通道 =====

    /** 通信主服务 UUID */
    val serviceUuid: UUID

    /** 接收通知（Notify）的特征 UUID —— 设备上报数据走这里 */
    val notifyUuid: UUID

    /** 写入命令的特征 UUID —— 注意可能和 [notifyUuid] 不在同一个 Service 里 */
    val writeUuid: UUID

    // ===== 扫描规则 =====

    /**
     * 下发给系统的扫描过滤条件。返回空列表表示只能无过滤扫描。
     * 填了 [BleScanFilter.serviceUuid] 时，很多设备只广播服务 UUID 不广播名称，识别更可靠。
     */
    val scanFilters: List<BleScanFilter>
        get() = emptyList()

    /**
     * 判断这条广播是不是我要找的设备。
     *
     * 默认按 [scanAddress] 精确匹配；子类可以实现成按广播名或服务 UUID 匹配。
     */
    fun isTarget(advertisement: BleAdvertisement): Boolean {
        val target = scanAddress ?: return false
        return advertisement.address.equals(target, ignoreCase = true)
    }

    /** 已知 MAC 地址时直接填，配合默认的 [isTarget] 即可 */
    val scanAddress: String?
        get() = null

    // ===== 保活 =====

    /** 周期心跳包，不需要就返回 null */
    val handshake: ByteArray?
        get() = null

    /** 心跳间隔（毫秒） */
    val handshakeIntervalMs: Long
        get() = 2_000L

    /**
     * 收到数据后的自动应答。
     *
     * 有些设备要求 App 在收到特定报文后立刻回包（例如 JK100 的握手包 0x92），
     * 不回就会被设备主动断开。返回 null 表示这条数据不需要应答。
     */
    val autoReply: ((ByteArray) -> ByteArray?)?
        get() = null

    // ===== 固件兼容开关 =====

    /**
     * 是否请求 MTU 协商。
     *
     * 默认 false —— 部分固件收到 MTU 请求后会停止回包，导致链路超时断开。
     * 只有确认自己的设备支持时才打开。
     */
    val autoRequestMtu: Boolean
        get() = false

    /** MTU 协商目标值，仅 [autoRequestMtu] 为 true 时生效 */
    val targetMtu: Int
        get() = 247
}
