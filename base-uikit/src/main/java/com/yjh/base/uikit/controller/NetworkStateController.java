package com.yjh.base.uikit.controller;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yjh.base.uikit.R;
import com.yjh.base.utils.util.LogUtils;

import java.lang.ref.WeakReference;

/**
 * 全局网络断网/弱网提示 Controller
 * Created by jiahui on 2026/08/08
 */
public class NetworkStateController implements Application.ActivityLifecycleCallbacks {

    private static final String TAG = "NetworkStateController";
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final int VIEW_ID_NETWORK_BAR = 0X7F0A9999;
    private static WeakReference<Activity> sTopActivityRef;
    private static boolean isNetworkAvailable = true;

    public static void init(Application application) {
        if (application == null) return;

        // 1. 注册生命周期追踪当前前台 Activity
        application.registerActivityLifecycleCallbacks(new NetworkStateController());

        // 2. 注册系统网络监听 API
        registerNetworkCallback(application);

        LogUtils.debug(TAG, "NetworkStateController 初始化成功，已开启全局断网状态监控。");
    }

    private static void registerNetworkCallback(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return;
        }

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        cm.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                LogUtils.debug(TAG, "🌐 网络已恢复连接");
                isNetworkAvailable = true;

                // 切换到主线程移除顶部断网提示条
                MAIN_HANDLER.post(NetworkStateController::dismissOfflineBar);
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                LogUtils.error(TAG, "⚠️ 网络已断开");
                isNetworkAvailable = false;

                // 切换到主线程在当前 Activity 顶部贴上断网提示条
                MAIN_HANDLER.post(NetworkStateController::showOfflineBar);
            }
        });
    }

    private static void showOfflineBar() {
        if (sTopActivityRef == null) {
            return;
        }
        Activity topActivity = sTopActivityRef.get();
        if (topActivity == null || topActivity.isFinishing() || topActivity.isDestroyed()) return;

        try {
            ViewGroup decorView = (ViewGroup) topActivity.getWindow().getDecorView();

            View existingBar = decorView.findViewById(VIEW_ID_NETWORK_BAR);
            if (existingBar != null) {
                existingBar.setVisibility(View.VISIBLE);
                return;
            }

            TextView offlineBar = new TextView(topActivity);
            offlineBar.setId(VIEW_ID_NETWORK_BAR);
            offlineBar.setText("⚠️ 当前网络已断开，请检查网络设置");
            offlineBar.setTextColor(Color.WHITE);
            offlineBar.setGravity(Gravity.CENTER);
            offlineBar.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            offlineBar.setPadding(32, 16, 32, 16);
            offlineBar.setBackgroundColor(Color.parseColor("#E53935")); // 红色背景，更醒目

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.gravity = Gravity.BOTTOM;

            decorView.addView(offlineBar, params);
            offlineBar.bringToFront();

        } catch (Exception e) {
            LogUtils.error(TAG, "挂载断网提示条失败：" + e);
        }
    }

    /**
     * 移除顶部断网提示条
     */
    private static void dismissOfflineBar() {
        if (sTopActivityRef == null) return;
        Activity topActivity = sTopActivityRef.get();

        if (topActivity == null || topActivity.isFinishing() || topActivity.isDestroyed()) {
            return;
        }

        try {
            ViewGroup decorView = (ViewGroup) topActivity.getWindow().getDecorView();
            View existingBar = decorView.findViewById(VIEW_ID_NETWORK_BAR);
            if (existingBar != null) {
                decorView.removeView(existingBar);
            }
        } catch (Exception e) {
            LogUtils.error(TAG, "移除断网提示条失败：" + e);
        }
    }

    /**
     * 测量系统状态栏高度（避免提示条挡住状态栏）
     */
    private static int getStatusBarHeight(Context context) {
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        return resourceId > 0 ? context.getResources().getDimensionPixelSize(resourceId) : 80;
    }

    /**
     * 暴露给外部调用的静态方法：直接获取当前网络是否可用
     */
    public static boolean isConnected() {
        return isNetworkAvailable;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        sTopActivityRef = new WeakReference<>(activity);

        // 如果跳转到新页面时已经是断网状态，自动在这个新页面把警告条补上
        if (!isNetworkAvailable) {
            showOfflineBar();
        }
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {

    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {

    }
}
