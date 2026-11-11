package com.yjh.blekit.scan

/**
 * 扫描频率限制器。
 *
 * Android 8.0 起，同一个 App 在 30 秒窗口内启动扫描超过 5 次会返回
 * `SCAN_FAILED_APPLICATION_REGISTRATION_FAILED`（errorCode = 2）。
 * 这里用滑动窗口统计，确保 [ScanArbiter] 切换扫描策略时不会撞上这个限制。
 */
internal class ScanRateLimit(
    private val maxStarts: Int = MAX_STARTS,
    private val windowMs: Long = WINDOW_MS,
) {

    /** 记录每次真实 startScan 的时间戳 */
    private val starts = ArrayDeque<Long>()

    /** 现在还能不能再启动一次扫描 */
    fun canStart(now: Long): Boolean {
        evictExpired(now)
        return starts.size < maxStarts
    }

    /** 启动成功后调用，占一个名额 */
    fun record(now: Long) {
        starts.addLast(now)
    }

    /** 下一次可以启动的最早时间；现在就能启动则返回 now */
    fun nextAvailableAt(now: Long): Long {
        evictExpired(now)
        if (starts.size < maxStarts) return now
        return starts.first() + windowMs
    }

    private fun evictExpired(now: Long) {
        while (starts.isNotEmpty() && now - starts.first() >= windowMs) {
            starts.removeFirst()
        }
    }

    companion object {
        private const val MAX_STARTS = 5
        private const val WINDOW_MS = 30_000L
    }
}
