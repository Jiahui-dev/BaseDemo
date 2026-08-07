package com.yjh.base.uikit.controller;

import android.content.pm.PackageManager;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.yjh.base.core.lifecycle.Lifecycle;
import com.yjh.base.core.lifecycle.LifecycleEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by jiahui on 2026/08/07
 * 链式无痕动态权限申请 Controller，彻底消除 onRequestPermissionsResult 模板代码，生命周期感知防泄漏
 */
public class PermissionController implements Lifecycle {

    private final ComponentActivity mActivity;
    private ActivityResultLauncher<String[]> mPermissionLauncher;

    private OnPermissionCallback mCallback;

    public interface OnPermissionCallback {
        void onGranted();

        void onDenied(List<String> deniedPermissions);
    }

    public PermissionController(ComponentActivity activity) {
        this.mActivity = activity;
    }

    @Override
    public void onLifecycleChanged(LifecycleEvent event) {
        switch (event) {
            case ON_INIT:
                // 1. 在 ComponentActivity 初始化阶段注册 ActivityResult 契约 launcher
                // 必须在 ON_CREATE / ON_INIT 阶段完成注册，否则 Android 官方会抛出 IllegalStateException
                mPermissionLauncher = mActivity.registerForActivityResult(
                        new ActivityResultContracts.RequestMultiplePermissions(),
                        this::handlePermissionResult
                );
                break;

            case ON_DESTROY:
                if (mPermissionLauncher != null) {
                    mPermissionLauncher.unregister();
                    mPermissionLauncher = null;
                }
                mCallback = null;
                break;

            default:
                break;
        }
    }

    /**
     * 发起权限请求（支持单个或多个权限）
     *
     * @param permissions 权限数组，如 Manifest.permission.CAMERA
     * @param callback    回调监听
     */
    public void request(String[] permissions, OnPermissionCallback callback) {
        if (mActivity == null || permissions == null || permissions.length == 0) {
            return;
        }
        this.mCallback = callback;

        List<String> deniedList = new ArrayList<>();
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(mActivity, perm) != PackageManager.PERMISSION_GRANTED) {
                deniedList.add(perm);
            }
        }

        if (deniedList.isEmpty()) {
            if (mCallback != null) {
                mCallback.onGranted();
            }
            return;
        }

        // 触发系统权限弹窗
        if (mPermissionLauncher != null) {
            mPermissionLauncher.launch(deniedList.toArray(new String[0]));
        }
    }

    /**
     * 处理系统返回的权限结果
     */
    private void handlePermissionResult(Map<String, Boolean> result) {
        if (mCallback == null) return;

        List<String> deniedList = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : result.entrySet()) {
            if (!entry.getValue()) {
                deniedList.add(entry.getKey());
            }
        }

        if (deniedList.isEmpty()) {
            mCallback.onGranted();
        } else {
            mCallback.onDenied(deniedList);
        }
    }
}
