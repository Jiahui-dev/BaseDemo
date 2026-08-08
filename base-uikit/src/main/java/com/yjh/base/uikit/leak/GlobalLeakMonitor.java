package com.yjh.base.uikit.leak;

import android.app.Activity;
import android.app.Application;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yjh.base.utils.util.LogUtils;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * Created by jiahui on 2026/08/08
 */
public class GlobalLeakMonitor implements Application.ActivityLifecycleCallbacks {

    private static final String TAG = "GlobalLeakMonitor";
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    // 记录当前屏幕正展示的前台 Activity
    private static WeakReference<Activity> sTopActivityRef;

    /**
     * 在 Application 中初始化开启
     */
    public static void init(Application application) {
        if (application != null) {
            application.registerActivityLifecycleCallbacks(new GlobalLeakMonitor());
            LogUtils.debug(TAG, "GlobalLeakMonitor 启动成功，全自动内存泄漏监控已就位。");
        }
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        sTopActivityRef = new WeakReference<>(activity);
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        checkLeakOnDestroy(activity);
    }

    /**
     * 核心检测逻辑
     */
    private void checkLeakOnDestroy(Activity destroyedActivity) {
        final String pageName = destroyedActivity.getClass().getSimpleName();
        final ReferenceQueue<Object> queue = new ReferenceQueue<>();
        final WeakReference<Activity> weakRef = new WeakReference<>(destroyedActivity, queue);

        // 延迟 5 秒检测（给 JVM 充分的 GC 时间）
        MAIN_HANDLER.postDelayed(() -> {
            // 建议 JVM 执行一次 GC
            Runtime.getRuntime().gc();
            System.runFinalization();

            // 如果 5 秒后 weakRef 依然未被回收，说明发生泄漏！
            if (queue.poll() == null && weakRef.get() != null) {
                LogUtils.error(TAG, "🚨 [内存泄漏警告] 页面 [" + pageName + "] 未被 GC 正常回收！");

                // 在当前用户正在看的前台 Activity 上展示悬浮卡片
                showLeakBadgeOnTopActivity(pageName);
            } else {
                LogUtils.debug(TAG, "✅ 页面 [" + pageName + "] 正常回收。");
            }
        }, 5000);
    }

    /**
     * 在当前前台 Activity 贴上红色悬浮警报卡片
     */
    private void showLeakBadgeOnTopActivity(String leakedPageName) {
        if (sTopActivityRef == null) return;
        Activity topActivity = sTopActivityRef.get();

        if (topActivity == null || topActivity.isFinishing() || topActivity.isDestroyed()) {
            return;
        }

        try {
            ViewGroup decorView = (ViewGroup) topActivity.getWindow().getDecorView();

            // 警报悬浮卡片
            TextView badge = new TextView(topActivity);
            badge.setText("🚨 发现泄漏: " + leakedPageName);
            badge.setTextColor(Color.WHITE);
            badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            badge.setBackgroundColor(Color.parseColor("#E53935")); // 高亮红色
            badge.setPadding(28, 14, 28, 14);

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.gravity = Gravity.TOP | Gravity.END;
            params.topMargin = 120; // 避开状态栏
            params.rightMargin = 20;
            badge.setLayoutParams(params);

            // 点击卡片：弹 Toast 提醒并消除悬浮条
            badge.setOnClickListener(v -> {
                String tip = "页面 [" + leakedPageName + "] 未被 GC 回收！\n请检查 Handler、静态变量、单例 Listener 或 RxJava 订阅！";
                Toast.makeText(topActivity, tip, Toast.LENGTH_LONG).show();

                // 移除当前卡片
                decorView.removeView(badge);
            });

            decorView.addView(badge);

        } catch (Exception e) {
            LogUtils.error(TAG, "挂载泄漏悬浮卡片失败");
        }
    }

    // 生命周期空实现
    @Override public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}
    @Override public void onActivityStarted(@NonNull Activity activity) {}
    @Override public void onActivityPaused(@NonNull Activity activity) {}
    @Override public void onActivityStopped(@NonNull Activity activity) {}
    @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
}