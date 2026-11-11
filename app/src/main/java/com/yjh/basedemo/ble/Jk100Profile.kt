package com.yjh.basedemo.ble

import com.yjh.blekit.BleAdvertisement
import com.yjh.blekit.BleScanFilter
import com.yjh.blekit.DeviceProfile
import java.util.UUID

/**
 * JK100（长桑 / 众志 UART 协议 v2.52.4-2023.1.6）的链路描述。
 *
 * 取自协议文档「蓝牙服务」一节：
 * - Service     0xFFF0
 * - Notify      0xFFF2（收数据）
 * - Write       0xFFF3（写数据，Write without response）
 *
 * 注意：文档里写特征的 128 位 UUID 印成了 FFF2，属于笔误；以 16 位值 FFF3 为准。
 *
 * @param macAddress 绑定的设备 MAC。传空串表示“只扫描不自动连接”，
 *   用于首次打开时让用户从列表里挑一台——两台同名 JK100 只能靠 MAC 区分。
 */
class Jk100Profile(
    private val macAddress: String = "",
) : DeviceProfile {

    override val serviceUuid: UUID = SERVICE
    override val notifyUuid: UUID = NOTIFY
    override val writeUuid: UUID = WRITE

    /**
     * 必须留空，不能用 `listOf(BleScanFilter(serviceUuid = SERVICE))`。
     *
     * 原因：ScanFilter.setServiceUuid 匹配的是**广播包**里声明的服务 UUID
     * （AD Type 0x02/0x03），JK100 的广播包里并没有 FFF0——它连上之后才暴露
     * 该服务。加了系统过滤会导致一条广播都收不到，isTarget 的名字兜底也无法生效。
     */
    override val scanFilters: List<BleScanFilter> = emptyList()

    override val scanAddress: String?
        get() = macAddress.ifBlank { null }

    /**
     * 已绑定 MAC 时精确匹配；未绑定时返回 false，
     * 这样库会一直扫描、界面持续展示设备列表，但不会误连某一台。
     */
    override fun isTarget(advertisement: BleAdvertisement): Boolean {
        if (macAddress.isBlank()) return false
        return advertisement.address.equals(macAddress, ignoreCase = true)
    }

    /**
     * 握手包：设备 -> APP  `AA 55 01 92 xx CHK`
     * 应答   ：APP -> 设备 `AA 55 01 13 00 14`
     * （CHK = DATALEN + CMDCODE + DATA = 0x01 + 0x13 + 0x00 = 0x14）
     *
     * 不回这一包，设备会主动断开，所以自动连接必须带它。
     */
    private val HANDSHAKE_REPLY = byteArrayOf(0xAA.toByte(), 0x55, 0x01, 0x13, 0x00, 0x14)

    override val autoReply: (ByteArray) -> ByteArray? = { bytes ->
        if (isHandshake(bytes)) HANDSHAKE_REPLY else null
    }

    private fun isHandshake(bytes: ByteArray): Boolean =
        bytes.size >= 5 &&
            bytes[0] == 0xAA.toByte() &&
            bytes[1] == 0x55.toByte() &&
            bytes[3] == 0x92.toByte()

    private companion object {
        val SERVICE: UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
        val NOTIFY: UUID = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb")
        val WRITE: UUID = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb")
    }
}
