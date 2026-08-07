package com.yjh.base.utils.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

/**
 * Created by jiahui
 */
public class ToastUtils {

    private static Context sContext;
    private static Toast sToast;
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private ToastUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    /**
     * 必须在 Application 初始化时调用一次
     */
    public static void init(Context context) {
        if (context != null) {
            sContext = context.getApplicationContext();
        }
    }

    /**
     * 短时间显示
     */
    public static void showShort(final String message) {
        show(message, Toast.LENGTH_SHORT);
    }

    public static void showShort(final int resId) {
        if (sContext != null) {
            show(sContext.getString(resId), Toast.LENGTH_SHORT);
        }
    }

    /**
     * 长时间显示
     */
    public static void showLong(final String message) {
        show(message, Toast.LENGTH_LONG);
    }

    public static void showLong(final int resId) {
        if (sContext != null) {
            show(sContext.getString(resId), Toast.LENGTH_LONG);
        }
    }

    /**
     * 核心显示方法（兼容子线程调用 + 防重复弹窗）
     */
    private static void show(final String message, final int duration) {
        if (TextUtils.isEmpty(message)) {
            return;
        }

        // 如果在子线程，自动 post 到主线程去弹 Toast
        if (Looper.myLooper() != Looper.getMainLooper()) {
            MAIN_HANDLER.post(() -> showInternal(message, duration));
        } else {
            showInternal(message, duration);
        }
    }

    @SuppressLint("ShowToast")
    private static void showInternal(String message, int duration) {
        if (sContext == null) {
            return;
        }

        // 如果当前已有 Toast，先取消掉，实现立即覆盖更新文本的效果，防排队
        if (sToast != null) {
            sToast.cancel();
        }

        sToast = Toast.makeText(sContext, message, duration);
        sToast.show();
    }
}