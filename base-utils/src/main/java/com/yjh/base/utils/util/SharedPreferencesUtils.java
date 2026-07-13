package com.yjh.base.utils.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

/**
 * Created by jiahui on 2025/12/15
 */
public class SharedPreferencesUtils {
    /**
     * 默认文件名
     */
    private static final String DEFAULT_FILE_NAME = "base";

    /**
     * 保存数据
     *
     * @param context context
     * @param key     保存的键
     * @param object  保存的值
     */
    public static void setParam(Context context, String key, Object object) {
        setParam(context, DEFAULT_FILE_NAME, key, object);
    }

    /**
     * 保存数据到指定文件
     *
     * @param context  context
     * @param fileName 文件名
     * @param key      键
     * @param object   值
     */
    public static void setParam(Context context, String fileName, String key, Object object) {
        if (context == null || TextUtils.isEmpty(key) || object == null) {
            return;
        }

        SharedPreferences sp = getSharedPreferences(context, fileName);
        SharedPreferences.Editor editor = sp.edit();

        String type = object.getClass().getSimpleName();
        if ("String".equals(type)) {
            editor.putString(key, (String) object);
        } else if ("Integer".equals(type)) {
            editor.putInt(key, (Integer) object);
        } else if ("Boolean".equals(type)) {
            editor.putBoolean(key, (Boolean) object);
        } else if ("Float".equals(type)) {
            editor.putFloat(key, (Float) object);
        } else if ("Long".equals(type)) {
            editor.putLong(key, (Long) object);
        }

        editor.apply();
    }

    /**
     * 获取保存的数据
     *
     * @param context       context
     * @param key           键
     * @param defaultObject 默认值（不能为null）
     * @param <T>           泛型
     * @return 保存的值或默认值
     */
    public static <T> T getParam(Context context, String key, T defaultObject) {
        return getParam(context, DEFAULT_FILE_NAME, key, defaultObject);
    }

    /**
     * 从指定文件获取保存的数据
     *
     * @param context       context
     * @param fileName      文件名
     * @param key           键
     * @param defaultObject 默认值（不能为null）
     * @param <T>           泛型
     * @return 保存的值或默认值
     */
    public static <T> T getParam(Context context, String fileName, String key, T defaultObject) {
        if (context == null || TextUtils.isEmpty(key) || defaultObject == null) {
            return defaultObject;
        }

        SharedPreferences sp = getSharedPreferences(context, fileName);
        String type = defaultObject.getClass().getSimpleName();

        switch (type) {
            case "String":
                return (T) sp.getString(key, (String) defaultObject);
            case "Integer":
                return (T) Integer.valueOf(sp.getInt(key, (Integer) defaultObject));
            case "Boolean":
                return (T) Boolean.valueOf(sp.getBoolean(key, (Boolean) defaultObject));
            case "Float":
                return (T) Float.valueOf(sp.getFloat(key, (Float) defaultObject));
            case "Long":
                return (T) Long.valueOf(sp.getLong(key, (Long) defaultObject));
        }

        return defaultObject;
    }

    /**
     * 移除指定键的值
     *
     * @param context context
     * @param key     要移除的键
     */
    public static void removeParam(Context context, String key) {
        removeParam(context, DEFAULT_FILE_NAME, key);
    }

    /**
     * 从指定文件移除指定键的值
     *
     * @param context  context
     * @param fileName 文件名
     * @param key      要移除的键
     */
    public static void removeParam(Context context, String fileName, String key) {
        if (context == null || TextUtils.isEmpty(key)) {
            return;
        }

        SharedPreferences sp = getSharedPreferences(context, fileName);
        sp.edit().remove(key).apply();
    }

    /**
     * 清空指定文件的所有数据
     *
     * @param context  context
     * @param fileName 文件名
     */
    public static void clearAll(Context context, String fileName) {
        if (context == null) {
            return;
        }

        SharedPreferences sp = getSharedPreferences(context, fileName);
        sp.edit().clear().apply();
    }

    /**
     * 清空默认文件的所有数据
     *
     * @param context context
     */
    public static void clearAll(Context context) {
        clearAll(context, DEFAULT_FILE_NAME);
    }

    /**
     * 获取SharedPreferences实例
     *
     * @param context  context
     * @param fileName 文件名
     * @return SharedPreferences实例
     */
    private static SharedPreferences getSharedPreferences(Context context, String fileName) {
        return context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
    }

    /**
     * 检查是否包含某个键
     *
     * @param context context
     * @param key     键
     * @return 是否包含
     */
    public static boolean contains(Context context, String key) {
        return contains(context, DEFAULT_FILE_NAME, key);
    }

    /**
     * 检查指定文件是否包含某个键
     *
     * @param context  context
     * @param fileName 文件名
     * @param key      键
     * @return 是否包含
     */
    public static boolean contains(Context context, String fileName, String key) {
        if (context == null || TextUtils.isEmpty(key)) {
            return false;
        }

        SharedPreferences sp = getSharedPreferences(context, fileName);
        return sp.contains(key);
    }
}