package com.yjh.blekit.diagnostics

/**
 * 环形缓冲：保留最近 N 条记录，供现场问题复盘。
 *
 * 线程安全由调用方保证（当前只在 BLE 回调线程内写入）。
 */
internal class DiagnosticsBuffer(
    private val capacity: Int,
    private val captureBytes: CaptureBytes,
    private val logLevel: () -> BleLogLevel,
    private val logger: (BleRecord) -> Unit,
) {

    private val records = ArrayDeque<BleRecord>()
    private val recentBytes = ArrayDeque<ByteArray>()

    fun record(
        timeMs: Long,
        level: BleLogLevel,
        event: BleEvent,
        address: String? = null,
    ) {
        when (event) {
            is BleEvent.Tx -> rememberBytes(event.bytes)
            is BleEvent.Rx -> rememberBytes(event.bytes)
            else -> Unit
        }

        val attached = if (captureBytes == CaptureBytes.OnError && event.isError()) {
            recentBytes.toList()
        } else {
            emptyList()
        }

        val record = BleRecord(
            timeMs = timeMs,
            level = level,
            event = event,
            address = address,
            attachedBytes = attached,
        )

        records.addLast(record)
        while (records.size > capacity) records.removeFirst()

        if (level.ordinal >= logLevel().ordinal) logger(record)
    }

    private fun rememberBytes(bytes: ByteArray) {
        if (captureBytes == CaptureBytes.Never) return
        recentBytes.addLast(bytes)
        while (recentBytes.size > MAX_PENDING_BYTES) recentBytes.removeFirst()
    }

    fun snapshot(count: Int): List<BleRecord> = records.takeLast(count)

    private fun BleEvent.isError(): Boolean =
        this is BleEvent.Error || this is BleEvent.Disconnected || this is BleEvent.ScanFailed

    private companion object {
        const val MAX_PENDING_BYTES = 20
    }
}
