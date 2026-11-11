package com.yjh.blekit.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanRateLimitTest {

    @Test
    fun `窗口内最多启动 5 次`() {
        val limit = ScanRateLimit()

        repeat(5) { i ->
            assertTrue("第 ${i + 1} 次应当允许", limit.canStart(0))
            limit.record(0)
        }

        assertFalse(limit.canStart(0))
        assertEquals(30_000L, limit.nextAvailableAt(0))
    }

    @Test
    fun `窗口滑过后重新可用`() {
        val limit = ScanRateLimit()
        repeat(5) { limit.record(0) }

        assertTrue(limit.canStart(30_000))
        assertTrue(limit.canStart(60_000))
    }

    @Test
    fun `窗口内的部分启动过期后只占用剩余名额`() {
        val limit = ScanRateLimit()
        limit.record(0)
        repeat(4) { limit.record(10_000) }

        // t=30_000 时，第一个（t=0）已过期，剩 4 个，还能启动 1 次
        assertTrue(limit.canStart(30_000))
        limit.record(30_000)

        assertFalse(limit.canStart(30_000))
        assertEquals(40_000L, limit.nextAvailableAt(30_000))
    }

    @Test
    fun `未达上限时 nextAvailableAt 返回当前时间`() {
        val limit = ScanRateLimit()
        limit.record(0)

        assertEquals(5_000L, limit.nextAvailableAt(5_000))
    }
}
