package com.yjh.basedemo.ble

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.yjh.base.utils.util.SharedPreferencesUtils
import com.yjh.blekit.BleAdvertisement
import com.yjh.blekit.BleKit
import com.yjh.blekit.ConnectionState
import com.yjh.blekit.diagnostics.BleEvent
import com.yjh.blekit.diagnostics.BleLogger
import com.yjh.blekit.diagnostics.BleLogLevel
import com.yjh.blekit.diagnostics.BleRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * JK100 自动连接调试页。
 *
 * 只做三件事：申请权限、自动连接、把库的状态和诊断事件打出来。
 * 不实现任何业务指令。
 */
class BleDebugActivity : AppCompatActivity() {

    private lateinit var stateView: TextView
    private lateinit var deviceList: LinearLayout
    private lateinit var logView: TextView

    private var ble: BleKit? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val seen = LinkedHashMap<String, BleAdvertisement>()

    /** 已绑定的 MAC，空串表示还没选设备 */
    private var boundMac: String = ""

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val denied = result.filterValues { !it }.keys
            if (denied.isEmpty()) {
                log("权限已授予，开始自动连接")
                startConnect()
            } else {
                log("权限被拒绝：$denied")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())

        BleKit.logLevel = BleLogLevel.VERBOSE
        BleKit.logger = BleLogger { record -> runOnUiThread { renderRecord(record) } }

        ble = BleKit.create(this)
        boundMac = SharedPreferencesUtils.getParam(this, KEY_BOUND_MAC, "")
        if (boundMac.isBlank()) {
            log("未绑定设备，请点击下方列表中的 JK100")
        } else {
            log("已绑定设备 $boundMac")
        }
        observe()
        ensurePermissionAndConnect()
    }

    override fun onDestroy() {
        BleKit.logger = null
        ble?.close()
        ble = null
        scope.cancel()
        super.onDestroy()
    }

    // ===== 流程 =====

    private fun ensurePermissionAndConnect() {
        val missing = requiredPermissions().filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            log("权限已具备，开始自动连接")
            startConnect()
        } else {
            log("请求权限：$missing")
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun startConnect() {
        ble?.connect(Jk100Profile(boundMac))
    }

    /** 点击设备行：记住 MAC，然后按这个 MAC 重连 */
    private fun bindDevice(advertisement: BleAdvertisement) {
        if (advertisement.address.isBlank()) {
            log("该设备没有 MAC（缺少 BLUETOOTH_CONNECT 权限），无法绑定")
            return
        }
        boundMac = advertisement.address
        SharedPreferencesUtils.setParam(this, KEY_BOUND_MAC, boundMac)
        log("已绑定 ${advertisement.name ?: boundMac} -> $boundMac")
        ble?.disconnect()
        startConnect()
    }

    private fun requiredPermissions(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    // ===== 订阅 =====

    private fun observe() {
        val kit = ble ?: return

        scope.launch {
            kit.state.collect { state ->
                stateView.text = "状态：${describe(state)}"
                log("状态 -> ${describe(state)}")
            }
        }

        scope.launch {
            kit.advertisements.collect { advertisement ->
                val key = advertisement.address.ifEmpty { advertisement.name ?: "unknown" }
                seen[key] = advertisement
                renderDevices()
            }
        }

        scope.launch {
            kit.packets.collect { bytes ->
                log("RX ${toHex(bytes)}")
            }
        }
    }

    private fun describe(state: ConnectionState): String = when (state) {
        ConnectionState.Idle -> "Idle 未连接"
        ConnectionState.Scanning -> "Scanning 扫描中"
        ConnectionState.Connecting -> "Connecting 连接中"
        ConnectionState.Discovering -> "Discovering 发现服务"
        is ConnectionState.Ready -> "Ready 已连接（mtu=${state.mtu}）"
        is ConnectionState.Retrying -> "Retrying 第 ${state.attempt} 次，${state.delayMs}ms 后"
        is ConnectionState.Failed -> "Failed ${state.reason}"
    }

    // ===== 渲染 =====

    private fun renderDevices() {
        deviceList.removeAllViews()
        seen.values
            .sortedWith(
                compareByDescending<BleAdvertisement> {
                    it.address.equals(boundMac, ignoreCase = true)
                }.thenByDescending { it.rssi },
            )
            .forEach { advertisement ->
                deviceList.addView(
                    TextView(this).apply {
                        val mark = if (advertisement.address.equals(boundMac, true)) "★ " else "  "
                        text = "$mark${advertisement.name ?: "(无名)"}  " +
                            "${advertisement.address}  ${advertisement.rssi}dBm"
                        textSize = 13f
                        setPadding(0, dp(6), 0, dp(6))
                        setOnClickListener { bindDevice(advertisement) }
                    },
                )
            }
    }

    private fun renderRecord(record: BleRecord) {
        val event = when (val e = record.event) {
            BleEvent.ScanStarted -> "SCAN_START"
            is BleEvent.ScanFailed -> "SCAN_FAIL code=${e.errorCode}"
            is BleEvent.Connected -> "CONNECTED ${e.address}"
            is BleEvent.Disconnected -> "DISCONNECTED status=${e.status}"
            is BleEvent.ServicesDiscovered -> "SERVICES=${e.count}"
            is BleEvent.MtuChanged -> "MTU=${e.mtu}"
            is BleEvent.Tx -> "TX ${toHex(e.bytes)}"
            is BleEvent.Rx -> "RX ${toHex(e.bytes)}"
            is BleEvent.Error -> "ERROR ${e.reason}"
        }
        log("[${record.level}] $event")
    }

    private fun log(message: String) {
        logView.append("${timeFormat.format(Date())}  $message\n")
    }

    private fun toHex(bytes: ByteArray): String =
        bytes.joinToString(" ") { String.format(Locale.US, "%02X", it) }

    // ===== UI =====

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(24), dp(16), dp(16))
        }

        root.addView(
            TextView(this).apply {
                text = "JK100 自动连接调试"
                textSize = 20f
            },
        )

        stateView = TextView(this).apply {
            text = "状态：Idle 未连接"
            textSize = 16f
            setPadding(0, dp(8), 0, dp(8))
        }
        root.addView(stateView)

        root.addView(
            button("断开") {
                ble?.disconnect()
                log("手动断开")
            },
        )
        root.addView(
            button("重新连接") {
                log("手动重新连接")
                startConnect()
            },
        )

        root.addView(
            label("扫描到的设备（点击绑定）"),
        )
        deviceList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(
            ScrollView(this).apply {
                addView(
                    deviceList,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(120),
                )
            },
        )

        root.addView(label("日志"))
        logView = TextView(this).apply {
            textSize = 11f
            movementMethod = ScrollingMovementMethod()
            setPadding(0, dp(4), 0, 0)
        }
        root.addView(
            ScrollView(this).apply {
                addView(
                    logView,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f,
                )
            },
        )

        return root
    }

    private fun label(text: String) = TextView(this).apply {
        this.text = text
        textSize = 14f
        gravity = Gravity.START
        setPadding(0, dp(8), 0, 0)
    }

    private fun button(text: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        setOnClickListener { onClick() }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val KEY_BOUND_MAC = "ble_debug_bound_mac"
    }
}
