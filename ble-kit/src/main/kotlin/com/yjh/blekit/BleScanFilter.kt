package com.yjh.blekit

import java.util.UUID

/**
 * 扫描过滤条件。
 *
 * 只有一个目标时会下发给系统做过滤（省电），
 * 同时存在多个目标时改由库在进程内匹配。
 */
data class BleScanFilter(
    /** 精确匹配 MAC 地址 */
    val address: String? = null,
    /** 匹配广播名前缀 */
    val namePrefix: String? = null,
    /** 匹配广播中携带的服务 UUID */
    val serviceUuid: UUID? = null,
)
