package com.yjh.blekit

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.yjh.blekit.diagnostics.BleEvent
import com.yjh.blekit.diagnostics.BleLogLevel
import com.yjh.blekit.diagnostics.BleRecord
import com.yjh.blekit.diagnostics.DiagnosticsBuffer
import com.yjh.blekit.scan.AndroidScanner
import com.yjh.blekit.scan.Clock
import com.yjh.blekit.scan.ScanArbiter
import com.yjh.blekit.scan.ScanTarget
import com.yjh.blekit.scan.Scheduler
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

/**
 * [BleKit] 的默认实现。
 *
 * 流程：扫描（交给 [ScanArbiter] 统一调度）→ 连接 → 发现服务 → 订阅通知 → Ready。
 * 断线后按 [BACKOFF_MS] 指数退避重连，直到主动 disconnect() / close()。
 */
internal class DefaultBleKit(
    private val appContext: Context,
    private val config: BleConfig,
) : BleKit {

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    private val _packets = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    private val _advertisements = MutableSharedFlow<BleAdvertisement>(extraBufferCapacity = 64)

    override val state: StateFlow<ConnectionState> = _state.asStateFlow()
    override val packets: SharedFlow<ByteArray> = _packets.asSharedFlow()
    override val advertisements: SharedFlow<BleAdvertisement> = _advertisements.asSharedFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val writeMutex = Mutex()

    private val diagnostics = DiagnosticsBuffer(
        capacity = config.bufferSize,
        captureBytes = config.captureBytes,
        logLevel = { BleKit.logLevel },
        logger = { BleKit.logger?.onLog(it) },
    )

    private val arbiter = ScanArbiter(
        scanner = AndroidScanner(appContext),
        clock = Clock { SystemClock.elapsedRealtime() },
        scheduler = Scheduler { delayMs, action -> mainHandler.postDelayed(action, delayMs) },
        observer = { advertisement -> _advertisements.tryEmit(advertisement) },
    )

    private var profile: DeviceProfile? = null
    private var gatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var scanTargetId: String? = null
    private var lastAddress: String? = null
    private var manualDisconnect = false
    private var retryAttempt = 0
    private var currentMtu = DEFAULT_MTU
    private var pendingResponse: CompletableDeferred<ByteArray>? = null

    // ===== 对外 API =====

    override fun connect(profile: DeviceProfile) {
        val missing = missingPermissions()
        if (missing != null) {
            fail("缺少权限：$missing")
            return
        }
        val adapter = adapter()
        if (adapter == null || !adapter.isEnabled) {
            fail("蓝牙未开启")
            return
        }

        this.profile = profile
        manualDisconnect = false
        retryAttempt = 0
        closeGatt()
        startScanning(profile)
    }

    override fun disconnect() {
        manualDisconnect = true
        stopScanning()
        closeGatt()
        _state.value = ConnectionState.Idle
    }

    override fun close() {
        manualDisconnect = true
        stopScanning()
        closeGatt()
        arbiter.close()
        scope.cancel()
        _state.value = ConnectionState.Idle
    }

    override suspend fun write(data: ByteArray) {
        writeMutex.withLock {
            val gatt = this.gatt ?: error("未连接")
            val characteristic = writeCharacteristic ?: error("未找到可写特征")
            val chunkSize = (currentMtu - ATT_HEADER).coerceAtLeast(MIN_CHUNK)

            var offset = 0
            while (offset < data.size) {
                val end = minOf(offset + chunkSize, data.size)
                val chunk = data.copyOfRange(offset, end)

                val ok = writeChunk(gatt, characteristic, chunk)
                log(BleLogLevel.DEBUG, BleEvent.Tx(chunk))
                check(ok) { "写入失败" }

                offset = end
                if (offset < data.size) delay(WRITE_INTERVAL_MS)
            }
        }
    }

    override suspend fun writeAndAwait(data: ByteArray): ByteArray {
        val deferred = CompletableDeferred<ByteArray>()
        pendingResponse = deferred
        return try {
            write(data)
            withTimeout(RESPONSE_TIMEOUT_MS) { deferred.await() }
        } finally {
            pendingResponse = null
        }
    }

    override fun recentRecords(count: Int): List<BleRecord> = diagnostics.snapshot(count)

    // ===== 扫描 =====

    private fun startScanning(profile: DeviceProfile) {
        _state.value = ConnectionState.Scanning
        log(BleLogLevel.INFO, BleEvent.ScanStarted)

        val id = scanTargetId ?: UUID.randomUUID().toString().also { scanTargetId = it }
        arbiter.register(
            ScanTarget(
                id = id,
                filters = profile.scanFilters,
                matches = { profile.isTarget(it) },
                onFound = { advertisement ->
                    stopScanning()
                    // 关键：stopScan 之后立刻 connectGatt 是 133 的主要诱因，
                    // 控制器还没完成扫描收尾，连接请求会被直接拒绝。
                    mainHandler.postDelayed(
                        { connectGatt(advertisement.address) },
                        CONNECT_AFTER_SCAN_MS,
                    )
                },
            ),
        )
    }

    private fun stopScanning() {
        scanTargetId?.let { arbiter.unregister(it) }
        scanTargetId = null
    }

    // ===== GATT =====

    private fun connectGatt(address: String) {
        val adapter = adapter() ?: run {
            fail("蓝牙不可用")
            return
        }
        val device = runCatching { adapter.getRemoteDevice(address) }.getOrNull() ?: run {
            fail("无效的设备地址：$address")
            return
        }

        closeGatt()
        lastAddress = address
        _state.value = ConnectionState.Connecting
        gatt = device.connectGatt(appContext, false, callback, BluetoothDevice.TRANSPORT_LE)
    }

    private fun closeGatt() {
        writeCharacteristic = null
        val current = gatt ?: return
        gatt = null
        runCatching {
            current.disconnect()
            current.close()
        }
    }

    private val callback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED &&
                status == BluetoothGatt.GATT_SUCCESS
            ) {
                log(BleLogLevel.INFO, BleEvent.Connected(lastAddress.orEmpty()), lastAddress)
                // 部分设备连上后立刻 discover 会失败，延迟一小段更稳
                _state.value = ConnectionState.Discovering
                mainHandler.postDelayed({ runCatching { gatt.discoverServices() } }, 300)
                return
            }

            log(BleLogLevel.WARN, BleEvent.Disconnected(status), lastAddress)
            closeGatt()
            if (manualDisconnect) {
                _state.value = ConnectionState.Idle
            } else {
                onLinkLost()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                log(BleLogLevel.WARN, BleEvent.Error("发现服务失败：$status"))
                onLinkLost()
                return
            }

            val profile = profile ?: return
            log(BleLogLevel.INFO, BleEvent.ServicesDiscovered(gatt.services.size), lastAddress)

            val notifyChar = findNotifyCharacteristic(gatt, profile)
            val writeChar = findWriteCharacteristic(gatt, profile)

            if (notifyChar == null || writeChar == null) {
                log(BleLogLevel.ERROR, BleEvent.Error("未找到通信特征 notify=$notifyChar write=$writeChar"))
                onLinkLost()
                return
            }
            writeCharacteristic = writeChar

            if (!gatt.setCharacteristicNotification(notifyChar, true)) {
                log(BleLogLevel.ERROR, BleEvent.Error("开启通知失败"))
                onLinkLost()
                return
            }

            val cccd = notifyChar.getDescriptor(CCCD_UUID)
            if (cccd == null) {
                // 没有 CCCD 说明只能靠 setCharacteristicNotification 生效
                onReady()
                return
            }

            val written = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) ==
                    BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(cccd)
            }

            if (!written) {
                log(BleLogLevel.ERROR, BleEvent.Error("写 CCCD 失败"))
                onLinkLost()
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                onReady()
            } else {
                log(BleLogLevel.ERROR, BleEvent.Error("CCCD 写入失败：$status"))
                onLinkLost()
            }
        }

        // API 33+ 的新回调，带 value
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            onPacket(value)
        }

        // API 32 及以下的老回调
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            val value = characteristic.value ?: return
            onPacket(value)
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            currentMtu = mtu
            log(BleLogLevel.INFO, BleEvent.MtuChanged(mtu), lastAddress)
        }
    }

    private fun onReady() {
        val profile = profile
        retryAttempt = 0
        if (profile?.autoRequestMtu == true) {
            runCatching { gatt?.requestMtu(profile.targetMtu) }
        } else {
            currentMtu = DEFAULT_MTU
        }
        log(BleLogLevel.INFO, BleEvent.MtuChanged(currentMtu), lastAddress)
        _state.value = ConnectionState.Ready(currentMtu)
    }

    private fun onLinkLost() {
        closeGatt()
        if (manualDisconnect) {
            _state.value = ConnectionState.Idle
            return
        }
        val profile = profile ?: return
        val address = lastAddress
        retryAttempt += 1
        val delayMs = nextBackoff(retryAttempt)
        _state.value = ConnectionState.Retrying(retryAttempt, delayMs)
        mainHandler.postDelayed({
            if (manualDisconnect || profile !== this.profile) return@postDelayed
            // 前几次直接按地址重连，比重新扫描快，也不占用扫描限额；
            // 连续失败多次再退回扫描，避免地址没有广播导致连不上。
            if (address != null && retryAttempt <= DIRECT_RETRY_LIMIT) {
                connectGatt(address)
            } else {
                startScanning(profile)
            }
        }, delayMs)
    }

    private fun onPacket(value: ByteArray) {
        log(BleLogLevel.DEBUG, BleEvent.Rx(value), lastAddress)
        _packets.tryEmit(value)
        pendingResponse?.complete(value)

        // 设备要求应答的报文（如 JK100 握手包 0x92），自动回复，否则会被断开
        val reply = profile?.autoReply?.invoke(value) ?: return
        scope.launch {
            runCatching { write(reply) }
                .onFailure { log(BleLogLevel.ERROR, BleEvent.Error("自动应答失败：${it.message}")) }
        }
    }

    // ===== 特征查找 =====

    private fun findNotifyCharacteristic(
        gatt: BluetoothGatt,
        profile: DeviceProfile,
    ): BluetoothGattCharacteristic? {
        gatt.getService(profile.serviceUuid)?.getCharacteristic(profile.notifyUuid)?.let { return it }
        // 兜底：部分设备把收发拆在别的 Service 里
        return gatt.services.firstNotNullOfOrNull { it.getCharacteristic(profile.notifyUuid) }
    }

    private fun findWriteCharacteristic(
        gatt: BluetoothGatt,
        profile: DeviceProfile,
    ): BluetoothGattCharacteristic? {
        gatt.getService(profile.serviceUuid)?.getCharacteristic(profile.writeUuid)?.let { return it }
        gatt.services.firstNotNullOfOrNull { it.getCharacteristic(profile.writeUuid) }?.let { return it }
        // 再兜底：找第一个可写特征
        return gatt.services
            .flatMap { it.characteristics }
            .firstOrNull { characteristic ->
                characteristic.properties and (
                    BluetoothGattCharacteristic.PROPERTY_WRITE or
                        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
                    ) != 0
            }
    }

    // ===== 写入 =====

    private fun writeChunk(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        chunk: ByteArray,
    ): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        gatt.writeCharacteristic(
            characteristic,
            chunk,
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE,
        ) == BluetoothStatusCodes.SUCCESS
    } else {
        @Suppress("DEPRECATION")
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        @Suppress("DEPRECATION")
        characteristic.value = chunk
        @Suppress("DEPRECATION")
        gatt.writeCharacteristic(characteristic)
    }

    // ===== 工具 =====

    private fun nextBackoff(attempt: Int): Long =
        BACKOFF_MS[(attempt - 1).coerceIn(0, BACKOFF_MS.lastIndex)]

    private fun adapter(): BluetoothAdapter? =
        (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private fun missingPermissions(): String? {
        val required = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return required.firstOrNull {
            appContext.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
    }

    private fun fail(reason: String) {
        log(BleLogLevel.ERROR, BleEvent.Error(reason), lastAddress)
        _state.value = ConnectionState.Failed(reason)
    }

    private fun log(level: BleLogLevel, event: BleEvent, address: String? = null) {
        diagnostics.record(
            timeMs = System.currentTimeMillis(),
            level = level,
            event = event,
            address = address,
        )
    }

    private companion object {
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        const val DEFAULT_MTU = 23
        const val ATT_HEADER = 3
        const val MIN_CHUNK = 20
        const val WRITE_INTERVAL_MS = 20L
        const val RESPONSE_TIMEOUT_MS = 3_000L

        /** stopScan 与 connectGatt 之间的间隔，太短会稳定触发 status=133 */
        const val CONNECT_AFTER_SCAN_MS = 500L

        /** 连续几次按地址直连都失败后，改为重新扫描 */
        const val DIRECT_RETRY_LIMIT = 3

        val BACKOFF_MS = longArrayOf(5_000L, 10_000L, 20_000L, 30_000L)
    }
}
