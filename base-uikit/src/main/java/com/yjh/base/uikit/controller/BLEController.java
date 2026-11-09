package com.yjh.base.uikit.controller;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.yjh.base.uikit.listener.BLEConnectionListener;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 通用 BLE 基础控制器
 * 提供扫描、连接、服务发现、数据收发等基础 GATT 操作。
 * 业务层（如血压计、血糖仪）在此基础上构建协议解析、心跳循环、数据同步等逻辑。
 * Created by jiahui on 2026/11/07
 */
@SuppressLint("MissingPermission")
public class BLEController {

    private static final String TAG = "BleController";

    private static volatile BLEController instance;
    private final Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;

    private UUID currentServiceUuid;
    private UUID currentNotifyUuid;
    private UUID currentWriteUuid;
    private BluetoothGattCharacteristic writeCharacteristic;

    private BLEConnectionListener bleListener;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isScanning = false;
    private volatile boolean isConnected = false;
    private volatile boolean isReady = false;

    // 当前 MTU payload（默认 20 = 23 - 3）
    private int currentMtuPayload = 20;

    /**
     * 是否自动请求 MTU 协商（某些设备固件不支持，设为 false）
     */
    private boolean autoRequestMtu = false;
    /**
     * MTU 协商时请求的目标值
     */
    private int targetMtu = 512;

    /**
     * discoverServices 延迟（毫秒）—— 连接成功后等一会儿再 discover，提高成功率
     */
    private long discoverDelayMs = 300;
    /**
     * discoverServices 重试间隔
     */
    private long discoverRetryMs = 3000;
    /**
     * discoverServices 最大重试次数
     */
    private int discoverMaxRetry = 2;
    private int discoverRetryCount = 0;
    private Runnable discoverRetryRunnable;

    /**
     * 扫描窗口：单次注册扫描的时长（毫秒）。窗口越长，stop/start 次数越少，越不容易被系统限流
     */
    private static final long DEFAULT_SCAN_WINDOW = 30000;
    private long scanWindowMs = DEFAULT_SCAN_WINDOW;

    /**
     * 低延迟 + 全回调：占空比最高，目标设备广播一到就能发现
     */
    private final ScanSettings scanSettings = new ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setReportDelay(0)
            .build();

    private final Runnable scanTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            stopScan();
        }
    };

    private final ConcurrentLinkedQueue<byte[]> sendQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean isSending = new AtomicBoolean(false);
    /**
     * 无响应写的包间隔（毫秒）
     */
    private static final long WRITE_INTERVAL_MS = 20;
    private boolean waitingWriteCallback = false;
    private Runnable writeFallback;
    private boolean writeTypeNoResponse = true;

    private String lastDeviceAddress;
    private UUID lastServiceUuid;
    private UUID lastNotifyUuid;
    private UUID lastWriteUuid;

    private BLEController(Context context) {
        this.context = context.getApplicationContext();
        BluetoothManager bluetoothManager = (BluetoothManager) this.context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
        Log.d(TAG, "BleController init OK, BT adapter=" + (bluetoothAdapter != null));
    }

    public static BLEController getInstance() {
        if (instance == null) {
            throw new IllegalStateException("BleController must be initialized in Application first!");
        }
        return instance;
    }

    public static void init(Context context) {
        if (instance == null) {
            synchronized (BLEController.class) {
                if (instance == null) {
                    instance = new BLEController(context);
                }
            }
        }
    }

    /**
     * 是否在服务发现后自动请求 MTU 协商。
     * <p>
     * 某些设备固件（如 101Lite 新固件）在收到 MTU 请求后会停止回复握手包，
     * 导致链路超时断开。这种情况下应设为 false（默认值）。
     */
    public void setAutoRequestMtu(boolean enabled) {
        this.autoRequestMtu = enabled;
        Log.d(TAG, "setAutoRequestMtu=" + enabled);
    }

    /**
     * 设置 MTU 协商目标值（默认 512）
     */
    public void setTargetMtu(int mtu) {
        this.targetMtu = mtu;
    }

    /**
     * 设置 discoverServices 延迟（默认 300ms）
     */
    public void setDiscoverDelayMs(long ms) {
        this.discoverDelayMs = ms;
    }

    /**
     * 设置是否使用 WRITE_TYPE_NO_RESPONSE（默认 true）
     */
    public void setWriteTypeNoResponse(boolean noResponse) {
        this.writeTypeNoResponse = noResponse;
    }

    public void setBleListener(BLEConnectionListener listener) {
        this.bleListener = listener;
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public boolean isConnected() {
        return isConnected;
    }

    /**
     * 是否已连接并准备好收发数据（服务发现 + 特征订阅完成）
     */
    public boolean isReadyForSend() {
        return isReady && bluetoothGatt != null && writeCharacteristic != null;
    }

    /**
     * 当前 MTU payload（字节数），默认 20
     */
    public int getMtuPayload() {
        return currentMtuPayload;
    }

    public BluetoothDevice getConnectedDevice() {
        if (bluetoothGatt != null) {
            return bluetoothGatt.getDevice();
        }
        return null;
    }

    public String getLastDeviceAddress() {
        return lastDeviceAddress;
    }

    public void startScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.w(TAG, "Bluetooth not enabled");
            if (bleListener != null) bleListener.onConnectionFailed("Bluetooth is disabled");
            return;
        }

        // 注意：绝不能对"未注册"的 callback 调用 stopScan 后再立刻 startScan ——
        // Android 9 及更早的 BT 栈异步处理注销，同 callback 的 stop+start 竞态会导致
        // onScannerRegistered status=133 → onScanFailed errorCode=2 (registration failed)。
        // 只有确实在扫描时才先停，并延迟重启，让注销完成。
        if (isScanning) {
            BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
            if (scanner != null) {
                try {
                    scanner.stopScan(scanCallback);
                } catch (Exception ignored) {
                }
            }
            isScanning = false;
            mainHandler.removeCallbacks(scanTimeoutRunnable);
            mainHandler.postDelayed(this::doStartScan, 300);
            return;
        }

        doStartScan();
    }

    private void doStartScan() {
        BluetoothLeScanner scanner = bluetoothAdapter != null ? bluetoothAdapter.getBluetoothLeScanner() : null;
        if (scanner == null) {
            Log.w(TAG, "BluetoothLeScanner is null");
            if (bleListener != null) bleListener.onConnectionFailed("Scanner unavailable");
            return;
        }

        isScanning = true;
        mainHandler.removeCallbacks(scanTimeoutRunnable);

        try {
            scanner.startScan(null, scanSettings, scanCallback);
            Log.d(TAG, "startScan requested successfully, window=" + scanWindowMs + "ms");
        } catch (Exception e) {
            Log.e(TAG, "startScan exception: " + e.getMessage());
            isScanning = false;
            if (bleListener != null)
                bleListener.onConnectionFailed("Scan exception: " + e.getMessage());
            return;
        }

        mainHandler.postDelayed(scanTimeoutRunnable, scanWindowMs);
    }

    public void stopScan() {
        if (!isScanning) return;
        mainHandler.removeCallbacks(scanTimeoutRunnable);

        if (bluetoothAdapter != null) {
            BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
            if (scanner != null) {
                scanner.stopScan(scanCallback);
            }
        }
        isScanning = false;
        mainHandler.post(() -> {
            if (bleListener != null) bleListener.onScanFinished();
        });
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            if (device != null && bleListener != null) {
                mainHandler.post(() -> bleListener.onScanResult(device, result.getRssi()));
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "onScanFailed errorCode=" + errorCode);
            isScanning = false;
            // 如果遇到 errorCode = 2 (Application registration failed)，说明被系统限流或冲突
            // 此时清理超时回调并向外抛出明确提示
            mainHandler.removeCallbacks(scanTimeoutRunnable);
            if (bleListener != null) bleListener.onConnectionFailed("Scan failed: " + errorCode);
        }
    };

    public void connect(Context context, String address, UUID serviceUuid, UUID notifyUuid, UUID writeUuid) {
        stopScan();
        if (bluetoothAdapter == null || address == null) {
            if (bleListener != null) bleListener.onConnectionFailed("Invalid adapter or address");
            return;
        }

        // 保存连接参数（断线重连用）
        this.lastDeviceAddress = address;
        this.lastServiceUuid = serviceUuid;
        this.lastNotifyUuid = notifyUuid;
        this.lastWriteUuid = writeUuid;

        this.currentServiceUuid = serviceUuid;
        this.currentNotifyUuid = notifyUuid;
        this.currentWriteUuid = writeUuid;

        // 如果之前有 gatt，先断开
        if (bluetoothGatt != null) {
            closeInternal();
        }

        currentMtuPayload = 20;
        discoverRetryCount = 0;
        cancelDiscoverRetry();
        isConnected = false;
        isReady = false;

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device != null) {
            Log.d(TAG, "connect to " + address);
            bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
        } else {
            if (bleListener != null) bleListener.onConnectionFailed("Device not found: " + address);
        }
    }

    /**
     * 使用上次的连接参数重连（无需外部传入 Context，由内部维护）
     */
    public boolean reconnect() {
        if (lastDeviceAddress == null) return false;
        connect(this.context, lastDeviceAddress, lastServiceUuid, lastNotifyUuid, lastWriteUuid);
        return true;
    }

    public void disconnect() {
        isConnected = false;
        isReady = false;
        stopLoops();
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
        }
    }

    public void close() {
        closeInternal();
    }

    private void closeInternal() {
        isConnected = false;
        isReady = false;
        stopLoops();
        if (bluetoothGatt != null) {
            try {
                bluetoothGatt.close();
            } catch (Exception ignored) {
            }
            bluetoothGatt = null;
        }
        writeCharacteristic = null;
        sendQueue.clear();
        isSending.set(false);
    }

    private void stopLoops() {
        cancelDiscoverRetry();
        if (writeFallback != null) {
            mainHandler.removeCallbacks(writeFallback);
            writeFallback = null;
        }
    }

    // ==================== GATT 回调 ====================

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "onConnectionStateChange error status=" + status);
                closeInternal();
                mainHandler.post(() -> {
                    if (bleListener != null)
                        bleListener.onConnectionFailed("GATT error status: " + status);
                });
                return;
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "STATE_CONNECTED");
                isConnected = true;
                mainHandler.post(() -> {
                    if (bleListener != null) bleListener.onConnected();
                });

                // 延迟 discoverServices，提高成功率
                discoverRetryCount = 0;
                mainHandler.postDelayed(() -> {
                    if (bluetoothGatt != gatt) return;
                    boolean ok = gatt.discoverServices();
                    Log.d(TAG, "discoverServices returned " + ok);
                    scheduleDiscoverRetry(gatt);
                }, discoverDelayMs);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "STATE_DISCONNECTED");
                closeInternal();
                mainHandler.post(() -> {
                    if (bleListener != null) bleListener.onDisconnected();
                });
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "onServicesDiscovered failed status=" + status + ", will retry");
                scheduleDiscoverRetry(gatt);
                return;
            }
            cancelDiscoverRetry();
            Log.d(TAG, "onServicesDiscovered OK");

            BluetoothGattService service = currentServiceUuid != null ? gatt.getService(currentServiceUuid) : null;
            if (service != null) {
                if (currentWriteUuid != null) {
                    BluetoothGattCharacteristic c = service.getCharacteristic(currentWriteUuid);
                    if (c != null) {
                        writeCharacteristic = c;
                        int props = c.getProperties();
                        boolean supportsNoResp = (props & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0;
                        if (writeTypeNoResponse && supportsNoResp) {
                            c.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                        } else {
                            c.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                        }
                        Log.d(TAG, "writeChar=" + currentWriteUuid + " writeType=" + c.getWriteType());
                    }
                }
            } else {
                Log.w(TAG, "service " + currentServiceUuid + " not found");
            }

            // 部分设备（如 WS20A/PF12）把写和通知拆到两个服务（FFE5 写 / FFE0 通知）。
            // 主服务里找不到 notify 特征时，继续在全部服务里查找，避免漏订阅通知。
            if (currentNotifyUuid != null) {
                BluetoothGattCharacteristic notifyChar = service != null
                        ? service.getCharacteristic(currentNotifyUuid) : null;
                if (notifyChar == null) {
                    notifyChar = findCharacteristicInAllServices(gatt, currentNotifyUuid);
                }
                if (notifyChar != null) {
                    Log.d(TAG, "notifyChar=" + currentNotifyUuid
                            + " service=" + notifyChar.getService().getUuid());
                    setCharacteristicNotification(gatt, notifyChar, true);
                } else {
                    Log.w(TAG, "notifyChar " + currentNotifyUuid + " not found in any service");
                }
            }

            isReady = true;
            mainHandler.post(() -> {
                if (bleListener != null) bleListener.onServicesDiscovered();
            });

            // 可选：MTU 协商
            if (autoRequestMtu) {
                gatt.requestMtu(targetMtu);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onDescriptorWrite status=" + status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                currentMtuPayload = mtu - 3;
                Log.d(TAG, "onMtuChanged mtu=" + mtu + " payload=" + currentMtuPayload);
                mainHandler.post(() -> {
                    if (bleListener != null) bleListener.onMtuChanged(currentMtuPayload);
                });
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (currentNotifyUuid != null && currentNotifyUuid.equals(characteristic.getUuid())) {
                byte[] data = characteristic.getValue();
                if (data != null && bleListener != null) {
                    mainHandler.post(() -> bleListener.onDataReceived(data));
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (waitingWriteCallback) {
                waitingWriteCallback = false;
                if (writeFallback != null) mainHandler.removeCallbacks(writeFallback);
                mainHandler.postDelayed(BLEController.this::sendNextPacket, 5);
            }
        }
    };

    /**
     * 在全部已发现服务中按 UUID 查找特征（用于读写服务分离的设备）
     */
    private BluetoothGattCharacteristic findCharacteristicInAllServices(BluetoothGatt gatt, UUID charUuid) {
        if (gatt == null || charUuid == null) return null;
        for (BluetoothGattService s : gatt.getServices()) {
            if (s == null) continue;
            BluetoothGattCharacteristic c = s.getCharacteristic(charUuid);
            if (c != null) return c;
        }
        return null;
    }

    private void setCharacteristicNotification(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean enabled) {
        gatt.setCharacteristicNotification(characteristic, enabled);
        for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
            descriptor.setValue(enabled
                    ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);
        }
    }

    // ==================== discoverServices 重试 ====================

    private void scheduleDiscoverRetry(BluetoothGatt gatt) {
        cancelDiscoverRetry();
        discoverRetryRunnable = () -> {
            if (bluetoothGatt != gatt) return;
            if (discoverRetryCount >= discoverMaxRetry) {
                Log.e(TAG, "discoverServices retry exhausted");
                return;
            }
            discoverRetryCount++;
            boolean ok = gatt.discoverServices();
            Log.d(TAG, "discoverServices retry#" + discoverRetryCount + " returned " + ok);
            scheduleDiscoverRetry(gatt);
        };
        mainHandler.postDelayed(discoverRetryRunnable, discoverRetryMs);
    }

    private void cancelDiscoverRetry() {
        if (discoverRetryRunnable != null) {
            mainHandler.removeCallbacks(discoverRetryRunnable);
            discoverRetryRunnable = null;
        }
    }

    // ==================== 数据发送 ====================

    /**
     * 直接发送完整数据（数据长度不能超过 MTU payload）
     */
    public boolean writeData(byte[] data) {
        if (bluetoothGatt == null || writeCharacteristic == null || data == null) return false;
        writeCharacteristic.setValue(data);
        writeCharacteristic.setWriteType(writeTypeNoResponse
                ? BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                : BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        return bluetoothGatt.writeCharacteristic(writeCharacteristic);
    }

    /**
     * 分包发送：根据当前 MTU payload 自动切分
     */
    public void writeDataChunked(byte[] data) {
        writeDataChunked(data, currentMtuPayload);
    }

    /**
     * 分包发送：指定 chunkSize
     */
    public void writeDataChunked(byte[] data, int chunkSize) {
        if (data == null || data.length == 0) return;
        if (bluetoothGatt == null || writeCharacteristic == null) {
            Log.e(TAG, "writeDataChunked: Gatt or writeCharacteristic is NULL");
            return;
        }

        int maxChunk = Math.max(1, chunkSize);
        for (int offset = 0; offset < data.length; offset += maxChunk) {
            int chunkLen = Math.min(maxChunk, data.length - offset);
            byte[] chunk = new byte[chunkLen];
            System.arraycopy(data, offset, chunk, 0, chunkLen);
            sendQueue.offer(chunk);
        }

        if (!isSending.get()) {
            isSending.set(true);
            mainHandler.post(this::sendNextPacket);
        }
    }

    private void sendNextPacket() {
        if (bluetoothGatt == null || writeCharacteristic == null) {
            isSending.set(false);
            return;
        }

        byte[] data = sendQueue.poll();
        if (data == null) {
            isSending.set(false);
            return;
        }

        try {
            writeCharacteristic.setValue(data);
            writeCharacteristic.setWriteType(writeTypeNoResponse
                    ? BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    : BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            boolean ok = bluetoothGatt.writeCharacteristic(writeCharacteristic);
            if (!ok) {
                Log.w(TAG, "writeCharacteristic returned FALSE, retrying");
                sendQueue.offer(data);
                mainHandler.postDelayed(this::sendNextPacket, 100);
                return;
            }

            if (!writeTypeNoResponse) {
                // 带响应写：等回调
                waitingWriteCallback = true;
                writeFallback = () -> {
                    if (waitingWriteCallback) {
                        waitingWriteCallback = false;
                        sendNextPacket();
                    }
                };
                mainHandler.postDelayed(writeFallback, 500);
            } else {
                // 无响应写：定时发下一包
                mainHandler.postDelayed(this::sendNextPacket, WRITE_INTERVAL_MS);
            }
        } catch (Exception e) {
            Log.e(TAG, "sendNextPacket exception: " + e.getMessage());
            sendQueue.offer(data);
            mainHandler.postDelayed(this::sendNextPacket, 200);
        }
    }
}
