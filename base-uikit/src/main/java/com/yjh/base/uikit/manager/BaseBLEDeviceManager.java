package com.yjh.base.uikit.manager;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.yjh.base.uikit.controller.BLEController;
import com.yjh.base.uikit.listener.BLEConnectionListener;

import java.util.UUID;

/**
 * 抽象蓝牙业务中间层
 * 职责：
 * 1. 统一管理扫描、匹配、连接、断线重连、心跳保活生命周期。
 * 2. 隔离底层 BleController 的 GATT 细节，采用模板方法模式供子类实现具体业务。
 * Created by jiahui on 2026/11/07
 */
@SuppressLint("MissingPermission")
public abstract class BaseBLEDeviceManager implements BLEConnectionListener {

    protected final String TAG = getClass().getSimpleName();

    protected final BLEController bleController;
    protected final Handler mainHandler = new Handler(Looper.getMainLooper());

    // 状态标志
    protected volatile boolean isStarted = false;
    protected volatile boolean isConnected = false;
    protected volatile boolean isConnecting = false;
    protected volatile boolean isScanning = false;

    // 配置参数（子类可在构造函数或子类初始化时修改）
    protected long handshakeIntervalMs = 2000;   // 默认 2 秒防息屏心跳包
    protected long reconnectDelayMs = 20000;     // 默认 20 秒断线重连
    protected long scanTimeoutMs = 15000;        // 扫描超时重试

    // 内部任务 Runnable
    private Runnable handshakeRunnable;
    private Runnable reconnectRunnable;
    private Runnable scanTimeoutRunnable;

    public BaseBLEDeviceManager() {
        this.bleController = BLEController.getInstance();
    }

    // ==================== 留给子类实现的抽象方法 (模板方法模式) ====================

    /**
     * 目标设备过滤规则（如判断广播名或 MAC 地址）
     */
    protected abstract boolean isTargetDevice(BluetoothDevice device, int rssi);

    /**
     * 提供通信所需的 Service UUID
     */
    protected abstract UUID getServiceUuid();

    /**
     * 提供接收通知 (Notify) 的 Characteristic UUID
     */
    protected abstract UUID getNotifyUuid();

    /**
     * 提供写入 (Write) 的 Characteristic UUID
     */
    protected abstract UUID getWriteUuid();

    /**
     * 提供心跳/握手包数据（如果不需要返回 null）
     */
    protected abstract byte[] buildHandshakePacket();

    /**
     * 解析底层上报的业务数据
     */
    protected abstract void onBleDataReceived(byte[] data);

    /**
     * 状态或提示回调（供 UI 层绑定观察）
     */
    protected abstract void onStatusChanged(boolean isConnected, String status);

    // ==================== 公共生命周期控制 ====================

    /**
     * 启动设备管理器（开始扫描并自动配对连接）
     */
    public void start() {
        if (isStarted) {
            Log.w(TAG, "Already started.");
            return;
        }
        isStarted = true;
        Log.d(TAG, "Manager started.");

        // 绑定底层监听
        bleController.setBleListener(this);
        startScan();
    }

    /**
     * 停止并清理所有状态与定时任务
     */
    public void stop() {
        isStarted = false;
        isConnected = false;
        isConnecting = false;
        isScanning = false;

        cancelAllTasks();
        bleController.disconnect();
        Log.d(TAG, "Manager stopped and cleaned up.");
    }

    protected void startScan() {
        if (!isStarted) return;
        if (!bleController.isBluetoothEnabled()) {
            onStatusChanged(false, "系统蓝牙未开启");
            return;
        }

        isScanning = true;
        onStatusChanged(false, "正在扫描目标设备...");
        bleController.startScan();

        // 扫描超时保护：超时未找到则重新开始扫描
        if (scanTimeoutRunnable == null) {
            scanTimeoutRunnable = () -> {
                if (isScanning && !isConnected) {
                    Log.d(TAG, "Scan timeout, restarting scan...");
                    bleController.stopScan();
                    mainHandler.postDelayed(this::startScan, 2000);
                }
            };
        }
        mainHandler.removeCallbacks(scanTimeoutRunnable);
        mainHandler.postDelayed(scanTimeoutRunnable, scanTimeoutMs);
    }

    protected void scheduleReconnect() {
        if (!isStarted) return;
        cancelReconnectTask();

        reconnectRunnable = () -> {
            if (!isStarted || isConnected) return;
            Log.d(TAG, "Executing scheduled reconnect...");
            onStatusChanged(false, "正在尝试重新连接...");

            // 直接调用无参的 reconnect，Context 由 BleController 内部维护
            boolean success = bleController.reconnect();

            // 如果内部重连失败，转为重新扫描
            if (!success) {
                startScan();
            }
        };
        mainHandler.postDelayed(reconnectRunnable, reconnectDelayMs);
    }

    private void startHandshakeLoop() {
        cancelHandshakeTask();
        byte[] handshakePacket = buildHandshakePacket();
        if (handshakePacket == null) return;

        handshakeRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isConnected || !isStarted) return;
                // 发送防息屏心跳包
                bleController.writeDataChunked(handshakePacket);
                mainHandler.postDelayed(this, handshakeIntervalMs);
            }
        };
        mainHandler.postDelayed(handshakeRunnable, handshakeIntervalMs);
    }

    private void cancelAllTasks() {
        mainHandler.removeCallbacks(scanTimeoutRunnable);
        cancelHandshakeTask();
        cancelReconnectTask();
    }

    private void cancelHandshakeTask() {
        if (handshakeRunnable != null) {
            mainHandler.removeCallbacks(handshakeRunnable);
            handshakeRunnable = null;
        }
    }

    private void cancelReconnectTask() {
        if (reconnectRunnable != null) {
            mainHandler.removeCallbacks(reconnectRunnable);
            reconnectRunnable = null;
        }
    }

    @Override
    public void onScanResult(BluetoothDevice device, int rssi) {
        if (!isScanning || !isStarted) return;

        // 调用子类实现的过滤规则
        if (isTargetDevice(device, rssi)) {
            Log.d(TAG, "Target device found: " + device.getName() + " [" + device.getAddress() + "]");
            isScanning = false;
            mainHandler.removeCallbacks(scanTimeoutRunnable);
            bleController.stopScan();

            isConnecting = true;
            onStatusChanged(false, "发现设备，正在连接...");

            // 发起连接
            bleController.connect(null, device.getAddress(), getServiceUuid(), getNotifyUuid(), getWriteUuid());
        }
    }

    @Override
    public void onScanFinished() {
        // 如果扫描结束但还没连上，且处于启动状态，则再次触发扫描
        if (isScanning && isStarted && !isConnected) {
            mainHandler.postDelayed(this::startScan, 1000);
        }
    }

    @Override
    public void onConnected() {
        isConnecting = false;
        onStatusChanged(false, "已建立底层连接，正在发现服务...");
    }

    @Override
    public void onServicesDiscovered() {
        isConnected = true;
        cancelReconnectTask();
        onStatusChanged(true, "连接成功，链路就绪");

        // 开启心跳循环
        startHandshakeLoop();
    }

    @Override
    public void onDisconnected() {
        boolean wasConnected = isConnected;
        isConnected = false;
        isConnecting = false;
        cancelHandshakeTask();

        onStatusChanged(false, "设备已断开连接");

        if (isStarted) {
            // 触发 20 秒断线重连
            onStatusChanged(false, "将在 " + (reconnectDelayMs / 1000) + " 秒后尝试重连...");
            scheduleReconnect();
        }
    }

    @Override
    public void onConnectionFailed(String error) {
        isConnected = false;
        isConnecting = false;
        onStatusChanged(false, "连接失败: " + error);

        if (isStarted) {
            // 连接失败后转入重连或重新扫描
            scheduleReconnect();
        }
    }

    @Override
    public void onMtuChanged(int mtuPayload) {
        Log.d(TAG, "MTU changed payload: " + mtuPayload);
    }

    @Override
    public void onDataReceived(byte[] data) {
        if (data != null && data.length > 0) {
            // 分发给子类解析具体业务报文
            onBleDataReceived(data);
        }
    }
}