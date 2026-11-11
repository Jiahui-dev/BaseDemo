package com.yjh.blekit.scan

import com.yjh.blekit.BleAdvertisement
import com.yjh.blekit.BleScanFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ScanArbiterTest {

    private lateinit var clock: FakeClock
    private lateinit var scheduler: FakeScheduler
    private lateinit var scanner: FakeScanner
    private lateinit var arbiter: ScanArbiter

    private val found = mutableListOf<Pair<String, BleAdvertisement>>()

    @Before
    fun setUp() {
        clock = FakeClock()
        scheduler = FakeScheduler(clock)
        scanner = FakeScanner()
        arbiter = ScanArbiter(scanner, clock, scheduler)
        found.clear()
    }

    private fun target(
        id: String,
        filters: List<BleScanFilter> = listOf(BleScanFilter(address = "AA:BB:CC:DD:EE:0$id")),
        matches: (BleAdvertisement) -> Boolean = { true },
    ) = ScanTarget(
        id = id,
        filters = filters,
        matches = matches,
        onFound = { found += id to it },
    )

    // ===== 基础状态机 =====

    @Test
    fun `没有目标时不启动扫描`() {
        assertEquals(0, scanner.startCount)
        assertNull(scanner.active)
    }

    @Test
    fun `只有一个目标时使用过滤扫描`() {
        arbiter.register(target("1"))

        assertEquals(1, scanner.startCount)
        assertTrue(scanner.active!!.filtered)
        assertEquals(listOf(BleScanFilter(address = "AA:BB:CC:DD:EE:01")), scanner.active!!.filters)
    }

    @Test
    fun `第二个目标接入时切换为无过滤合扫`() {
        arbiter.register(target("1"))
        arbiter.register(target("2"))

        assertEquals(2, scanner.startCount)
        assertFalse(scanner.active!!.filtered)
        // 旧的过滤扫描必须被停掉，否则两个扫描会互相打断
        assertTrue(scanner.sessions[0].stopped)
    }

    @Test
    fun `注销一个目标后回到过滤扫描`() {
        arbiter.register(target("1"))
        arbiter.register(target("2"))
        arbiter.unregister("2")

        assertTrue(scanner.active!!.filtered)
        assertEquals(3, scanner.startCount)
        assertEquals(listOf(BleScanFilter(address = "AA:BB:CC:DD:EE:01")), scanner.active!!.filters)
    }

    @Test
    fun `注销全部目标后停止物理扫描`() {
        arbiter.register(target("1"))
        arbiter.unregister("1")

        assertNull(scanner.active)
        assertTrue(scanner.sessions.all { it.stopped })
    }

    @Test
    fun `同一目标重复注册不会重启扫描`() {
        val t = target("1")
        arbiter.register(t)
        arbiter.register(t)

        assertEquals(1, scanner.startCount)
    }

    // ===== 结果分发 =====

    @Test
    fun `过滤模式下结果直接交给唯一目标`() {
        arbiter.register(target("1"))
        val adv = BleAdvertisement("AA:BB:CC:DD:EE:01", "BP-01", -60)
        scanner.emit(adv)

        assertEquals(1, found.size)
        assertEquals("1", found[0].first)
    }

    @Test
    fun `合扫模式下只分发给匹配的目标`() {
        arbiter.register(target("1", matches = { it.name == "BP" }))
        arbiter.register(target("2", matches = { it.name == "GLU" }))

        scanner.emit(BleAdvertisement("AA:BB:CC:DD:EE:01", "BP", -50))
        scanner.emit(BleAdvertisement("AA:BB:CC:DD:EE:02", "GLU", -50))
        scanner.emit(BleAdvertisement("AA:BB:CC:DD:EE:03", "OTHER", -50))

        assertEquals(2, found.size)
        assertEquals("1", found[0].first)
        assertEquals("2", found[1].first)
    }

    @Test
    fun `合扫模式下同一条广播可命中多个目标`() {
        arbiter.register(target("1", matches = { true }))
        arbiter.register(target("2", matches = { true }))

        scanner.emit(BleAdvertisement("AA:BB:CC:DD:EE:01", "BP", -50))

        assertEquals(listOf("1", "2"), found.map { it.first })
    }

    // ===== 限流 =====

    @Test
    fun `窗口内启动次数超限时延迟切换而不是硬启`() {
        // 反复增删制造 5 次真实启动（Android 的上限）
        arbiter.register(target("1"))
        arbiter.register(target("2"))
        arbiter.unregister("2")
        arbiter.register(target("2"))
        arbiter.unregister("2")
        assertEquals(5, scanner.startCount)

        // 第 6 次需要切换，应该被推迟
        arbiter.register(target("2"))
        assertEquals("超限时不应立刻启动", 5, scanner.startCount)
        assertEquals(1, scheduler.pendingCount)

        // 窗口过去后自动补上
        clock.time = 30_000
        scheduler.advance(0)
        assertEquals(6, scanner.startCount)
        assertFalse(scanner.active!!.filtered)
    }

    @Test
    fun `延迟切换期间不会重复排队`() {
        arbiter.register(target("1"))
        arbiter.register(target("2"))
        arbiter.unregister("2")
        arbiter.register(target("2"))
        arbiter.unregister("2")

        arbiter.register(target("2"))
        arbiter.register(target("3"))

        assertEquals(1, scheduler.pendingCount)
    }

    // ===== 生命周期 =====

    @Test
    fun `close 之后不再响应注册`() {
        arbiter.register(target("1"))
        arbiter.close()

        assertNull(scanner.active)

        arbiter.register(target("2"))
        assertNotNull(scanner.sessions)
        assertNull(scanner.active)
    }
}

