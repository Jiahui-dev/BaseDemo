package com.yjh.base.uikit.listener;

import android.bluetooth.BluetoothDevice;

/**
 * Created by jiahui on 2026/11/04
 */
public interface BLEConnectionListener {

    void onScanResult(BluetoothDevice device, int rssi);

    void onScanFinished();

    void onConnected();

    void onDisconnected();

    void onConnectionFailed(String reason);

    void onServicesDiscovered();

    void onMtuChanged(int mtuPayload);

    void onDataReceived(byte[] data);
}
