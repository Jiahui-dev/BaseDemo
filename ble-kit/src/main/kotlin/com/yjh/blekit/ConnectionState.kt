package com.yjh.blekit

/**
 * 链路状态。
 *
 * 用 sealed interface 而不是 enum，是因为 [Ready] 需要携带 mtu，
 * 而 [Idle] 不需要携带任何东西。`when` 上可以穷举所有分支。
 */
sealed interface ConnectionState {

    /** 还没开始 */
    data object Idle : ConnectionState

    /** 正在扫描目标设备 */
    data object Scanning : ConnectionState

    /** 已发起连接，等待连接建立 */
    data object Connecting : ConnectionState

    /** 连接已建立，正在发现服务 */
    data object Discovering : ConnectionState

    /** 可以收发数据了。mtu 是当前可用 MTU（单包上限为 mtu - 3） */
    data class Ready(val mtu: Int) : ConnectionState

    /** 断线，等待第 attempt 次重连，delayMs 毫秒后开始 */
    data class Retrying(val attempt: Int, val delayMs: Long) : ConnectionState

    /** 失败且不再自动重试（例如主动 disconnect 之后） */
    data class Failed(val reason: String) : ConnectionState
}
