package com.yjh.blekit.scan

import com.yjh.blekit.BleAdvertisement
import com.yjh.blekit.BleScanFilter

/**
 * 全局扫描调度器（进程内单例）。
 *
 * 解决的问题：
 * 1. Android 对 startScan 有频率限制（30 秒 5 次），多实例各自扫描会互相打断；
 * 2. 每个实例都自己 startScan / stopScan 时，后启动的会把先启动的停掉。
 *
 * 策略：
 * - 只有 1 个目标 → 带过滤扫描（省电、回调少）
 * - 2 个及以上 → 合并成一次无过滤扫描，进程内按 matches 分发
 * - 策略切换前检查频率限制，超限则等窗口过去再切
 *
 * 这个类不依赖任何 Android API，可以直接在 JVM 单测里验证。
 *
 * @param observer 每条扫描结果都会先回调一次，用于调试界面展示“实际扫到了什么”
 */
internal class ScanArbiter(
    private val scanner: Scanner,
    private val clock: Clock,
    private val scheduler: Scheduler,
    private val rateLimit: ScanRateLimit = ScanRateLimit(),
    private val observer: (BleAdvertisement) -> Unit = {},
) {

    private enum class Mode { IDLE, FILTERED, UNFILTERED }

    /** 期望的扫描形态。与 [activePlan] 不一致时需要重启扫描 */
    private data class Plan(
        val mode: Mode,
        val filters: List<BleScanFilter> = emptyList(),
    )

    private val targets = LinkedHashMap<String, ScanTarget>()

    private var activePlan: Plan? = null
    private var session: ScanSession? = null
    private var restartScheduled = false
    private var closed = false

    /** 注册一个扫描目标。同一个 id 重复注册会替换旧目标 */
    fun register(target: ScanTarget) {
        if (closed) return
        targets[target.id] = target
        reconcile()
    }

    /** 注销目标。最后一个目标注销后停止物理扫描 */
    fun unregister(id: String) {
        if (closed) return
        if (targets.remove(id) != null) reconcile()
    }

    /** 释放资源，之后不再启动任何扫描 */
    fun close() {
        closed = true
        targets.clear()
        stopSession()
        activePlan = null
    }

    // ===== 内部 =====

    private fun reconcile() {
        val plan = desiredPlan()
        if (plan == activePlan) return

        if (plan.mode == Mode.IDLE) {
            stopSession()
            activePlan = plan
            return
        }

        val now = clock.now()
        if (!rateLimit.canStart(now)) {
            scheduleRestart(now)
            return
        }
        restart(plan, now)
    }

    private fun scheduleRestart(now: Long) {
        if (restartScheduled) return
        restartScheduled = true
        val delay = (rateLimit.nextAvailableAt(now) - now).coerceAtLeast(0L)
        scheduler.schedule(delay) {
            restartScheduled = false
            if (!closed) reconcile()
        }
    }

    private fun restart(plan: Plan, now: Long) {
        stopSession()
        rateLimit.record(now)
        activePlan = plan
        session = when (plan.mode) {
            Mode.FILTERED -> scanner.startFiltered(plan.filters) { dispatch(it) }
            Mode.UNFILTERED -> scanner.startUnfiltered { dispatch(it) }
            Mode.IDLE -> null
        }
    }

    private fun stopSession() {
        session?.stop()
        session = null
    }

    private fun dispatch(advertisement: BleAdvertisement) {
        observer(advertisement)
        // 快照一份，避免 onFound 里注销目标导致 ConcurrentModificationException
        val snapshot = targets.values.toList()
        when (activePlan?.mode) {
            // 系统已经过滤过，结果都属于唯一的那个目标
            Mode.FILTERED -> snapshot.firstOrNull()?.onFound?.invoke(advertisement)
            Mode.UNFILTERED -> snapshot.forEach { target ->
                if (target.matches(advertisement)) target.onFound(advertisement)
            }
            else -> Unit
        }
    }

    private fun desiredPlan(): Plan = when {
        targets.isEmpty() -> Plan(Mode.IDLE)
        targets.size == 1 -> {
            val only = targets.values.first()
            if (only.filters.isNotEmpty()) Plan(Mode.FILTERED, only.filters) else Plan(Mode.UNFILTERED)
        }
        else -> Plan(Mode.UNFILTERED)
    }
}
