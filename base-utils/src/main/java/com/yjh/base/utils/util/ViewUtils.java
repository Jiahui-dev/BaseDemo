package com.yjh.base.utils.util;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;
import java.lang.reflect.Field;
import java.util.Objects;

/**
 * Created by jiahui on 2025/12/26
 */
public class ViewUtils {

    public static int getID(Context paramContext, String className, String resName) {
        try {
            Class<?> localClass = Class.forName(paramContext.getPackageName());
            Field localField = localClass.getField(resName);
            return Integer.parseInt(Objects.requireNonNull(localField.get(localField.getName())).toString());
        } catch (Exception e) {
            LogUtils.error("GetIDByReflection Error：", e.getMessage());
        }
        return 0;
    }

    /**
     * 从View对象中找到它所依赖的Activity，如果为Dialog/Toast，则返回null
     *
     * @param view V
     * @return host Activity
     */
    public static Activity getActivityFromView(View view) {
        Context context = view.getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    /**
     * 从View对象中找到它所依赖的Activity，如果为Dialog/Toast，则返回null
     *
     * @param view V
     * @return context
     */
    public static Context getContextFromView(View view) {
        Context context = view.getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    public static int getViewLocationScreenX(View view) {
        return getViewLocationScreenXY(view, 0);
    }

    public static int getViewLocationScreenY(View view) {
        return getViewLocationScreenXY(view, 1);
    }

    public static int getViewLocationWindowX(View view) {
        return getViewLocationWindowXY(view, 0);
    }

    public static int getViewLocationWindowY(View view) {
        return getViewLocationWindowXY(view, 1);
    }

    private static int getViewLocationScreenXY(View view, int index) {
        int[] xy = new int[2];
        view.getLocationOnScreen(xy);
        return xy[index];
    }

    private static int getViewLocationWindowXY(View view, int index) {
        int[] xy = new int[2];
        view.getLocationInWindow(xy);
        return xy[index];
    }
}
