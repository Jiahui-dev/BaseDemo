package com.yjh.blekit.scan

import com.yjh.blekit.BleAdvertisement
import com.yjh.blekit.BleScanFilter

/** 可控时钟，单测里直接快进 */
internal class FakeClock(var time: Long = 0L) : Clock {
    override fun now(): Long = time
}

/** 虚拟调度器：任务按到期时间排序，advance() 时依次执行 */
internal class FakeScheduler(private val clock: FakeClock) : Scheduler {

    private data class Task(val dueAt: Long, val action: () -> Unit)

    private val tasks = mutableListOf<Task>()

    override fun schedule(delayMs: Long, action: () -> Unit) {
        tasks += Task(clock.now() + delayMs, action)
    }

    fun advance(ms: Long) {
        val target = clock.time + ms
        while (true) {
            val next = tasks.filter { it.dueAt <= target }.minByOrNull { it.dueAt } ?: break
            tasks.remove(next)
            clock.time = next.dueAt
            next.action()
        }
        clock.time = target
    }

    val pendingCount: Int get() = tasks.size
}

/** 记录扫描调用的假 Scanner */
internal class FakeScanner : Scanner {

    internal class Session(
        val filtered: Boolean,
        val filters: List<BleScanFilter>,
        val onResult: (BleAdvertisement) -> Unit,
    ) : ScanSession {
        var stopped = false
            private set

        override fun stop() {
            stopped = true
        }
    }

    val sessions = mutableListOf<Session>()

    val active: Session? get() = sessions.lastOrNull()?.takeIf { !it.stopped }

    /** 只统计仍然有效的扫描启动次数 */
    val startCount: Int get() = sessions.size

    override fun startFiltered(
        filters: List<BleScanFilter>,
        onResult: (BleAdvertisement) -> Unit,
    ): ScanSession = Session(filtered = true, filters = filters, onResult = onResult)
        .also { sessions += it }

    override fun startUnfiltered(onResult: (BleAdvertisement) -> Unit): ScanSession =
        Session(filtered = false, filters = emptyList(), onResult = onResult)
            .also { sessions += it }

    fun emit(advertisement: BleAdvertisement) {
        active?.onResult?.invoke(advertisement)
    }
}

