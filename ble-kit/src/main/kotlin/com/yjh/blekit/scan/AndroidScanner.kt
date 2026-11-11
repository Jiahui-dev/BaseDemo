package com.yjh.blekit.scan

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import com.yjh.blekit.BleAdvertisement
import com.yjh.blekit.BleScanFilter

/**
 * [Scanner] 的 Android 实现。
 *
 * 只做“启动/停止扫描 + 结果转换”，频率限制与多目标调度都在 [ScanArbiter] 里。
 */
internal class AndroidScanner(private val context: Context) : Scanner {

    override fun startFiltered(
        filters: List<BleScanFilter>,
        onResult: (BleAdvertisement) -> Unit,
    ): ScanSession = start(filters.map { it.toPlatform() }, onResult)

    override fun startUnfiltered(onResult: (BleAdvertisement) -> Unit): ScanSession =
        start(emptyList(), onResult)

    private fun start(
        filters: List<ScanFilter>,
        onResult: (BleAdvertisement) -> Unit,
    ): ScanSession {
        if (!hasScanPermission()) return NoopSession

        val scanner = adapter()?.bluetoothLeScanner ?: return NoopSession
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                onResult(result.toAdvertisement())
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { onResult(it.toAdvertisement()) }
            }

            override fun onScanFailed(errorCode: Int) {
                // 失败（含频率限制）由上层重试，这里不抛异常
            }
        }

        return try {
            scanner.startScan(filters, settings, callback)
            object : ScanSession {
                override fun stop() {
                    runCatching { scanner.stopScan(callback) }
                }
            }
        } catch (e: SecurityException) {
            NoopSession
        }
    }

    private fun adapter(): BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private fun hasScanPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_SCAN
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun BleScanFilter.toPlatform(): ScanFilter {
        val builder = ScanFilter.Builder()
        address?.let { builder.setDeviceAddress(it) }
        namePrefix?.let { builder.setDeviceName(it) }
        serviceUuid?.let { builder.setServiceUuid(ParcelUuid(it)) }
        return builder.build()
    }

    private fun ScanResult.toAdvertisement(): BleAdvertisement {
        val deviceName = runCatching { device.name }.getOrNull()
        return BleAdvertisement(
            // Android 12+ 未声明 BLUETOOTH_CONNECT 时 address 会抛异常，兜底为空串
            address = runCatching { device.address }.getOrDefault(""),
            name = deviceName ?: scanRecord?.deviceName,
            rssi = rssi,
            serviceUuids = scanRecord?.serviceUuids?.map { it.uuid }.orEmpty(),
        )
    }

    private object NoopSession : ScanSession {
        override fun stop() = Unit
    }
}
