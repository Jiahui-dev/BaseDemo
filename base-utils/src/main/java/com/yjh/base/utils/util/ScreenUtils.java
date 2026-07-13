package com.yjh.base.utils.util;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Window;
import android.view.WindowManager;

/**
 * 获取屏幕宽高等信息，全屏切换，屏幕常亮切换
 * Created by jiahui on 2025/12/15
 */
public final class ScreenUtils {
    private static DisplayMetrics dm = null;

    public static DisplayMetrics displayMetrics(Context context) {
        if (dm != null) {
            return dm;
        }
        dm = new DisplayMetrics();
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(dm);
        LogUtils.verbose("screen width=" + dm.widthPixels + "px, screen height=" + dm.heightPixels
                + "px, densityDpi=" + dm.densityDpi + ", density=" + dm.density);
        return dm;
    }

    public static int widthPixels(Context context) {
        return displayMetrics(context).widthPixels;
    }

    public static int heightPixels(Context context) {
        return displayMetrics(context).heightPixels;
    }

    public static float density(Context context) {
        return displayMetrics(context).density;
    }

    public static int densityDpi(Context context) {
        return displayMetrics(context).densityDpi;
    }

    public static boolean isFullScreen(Activity activity) {
        if (activity == null) {
            return false;
        }
        Window window = activity.getWindow();
        int windowFlags = window.getAttributes().flags;
        return (windowFlags & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0;
    }

    /**
     * 切换全屏
     *
     * @param activity activity
     */
    public static void toggleFullScreen(Activity activity) {
        if (activity == null) {
            return;
        }
        Window window = activity.getWindow();
        int flagFullscreen = WindowManager.LayoutParams.FLAG_FULLSCREEN;

        if (isFullScreen(activity)) {
            window.clearFlags(flagFullscreen);
        } else {
            window.setFlags(flagFullscreen, flagFullscreen);
        }
    }

    public static boolean isKeepBright(Activity activity) {
        if (activity == null) {
            return false;
        }
        int windowFlags = activity.getWindow().getAttributes().flags;
        return (windowFlags & WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0;
    }

    /**
     * 切换常亮
     *
     * @param activity activity
     */
    public static void toggleKeepBright(Activity activity) {
        if (activity == null) {
            return;
        }
        Window window = activity.getWindow();
        int flagKeepBright = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

        if (isKeepBright(activity)) {
            window.clearFlags(flagKeepBright);
        } else {
            window.setFlags(flagKeepBright, flagKeepBright);
        }
    }
}
