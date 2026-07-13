package com.yjh.base.utils.util;

import android.os.Debug;
import android.os.Environment;
import android.util.Log;
import com.yjh.base.utils.config.AppConfig;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by jiahui on 2025/12/15
 */
public class LogUtils {
    private static final int METHOD_COUNT = 2;
    private static final int MIN_STACK_OFFSET = 3;
    private static final int MAX_STACK_TRACE_SIZE = 131071; //128 KB - 1
    private static boolean isDebug = AppConfig.DEBUG_ENABLE;// 是否调试模式
    private static String debugTag = AppConfig.DEBUG_TAG;// LogCat的标记
    private static final String TAG = "VIEW";

    public static void setIsDebug(boolean isDebug) {
        LogUtils.isDebug = isDebug;
    }

    public static boolean isDebug() {
        return isDebug;
    }

    public static void setDebugTag(String debugTag) {
        LogUtils.debugTag = debugTag;
    }

    public static String getDebugTag() {
        return debugTag;
    }

    /**
     * 记录“verbose”级别的信息
     *
     * @param tag tag
     * @param msg message
     */
    public static void verbose(String tag, String msg) {
        if (isDebug) {
            tag = debugTag + (tag == null || tag.trim().isEmpty() ? "" : "-" + tag);
            msg = msg + getTraceElement();
            Log.v(tag, msg);
        }
    }

    public static void verbose(String message) {
        verbose(TAG, message);
    }

    public static void verbose(Object object, String message) {
        verbose(object.getClass().getSimpleName(), message);
    }

    /**
     * 记录“info”级别的信息
     *
     * @param tag tag
     * @param msg message
     */
    public static void info(String tag, String msg) {
        if (isDebug) {
            tag = debugTag + (tag == null || tag.trim().isEmpty() ? "" : "-" + tag);
            msg = msg + getTraceElement();
            Log.i(tag, msg);
        }
    }

    public static void info(String message) {
        info(TAG, message);
    }

    public static void info(Object object, String message) {
        info(object.getClass().getSimpleName(), message);
    }

    /**
     * 记录“debug”级别的信息
     *
     * @param tag tag
     * @param msg message
     */
    public static void debug(String tag, String msg) {
        if (isDebug) {
            tag = debugTag + (tag == null || tag.trim().isEmpty() ? "" : "-" + tag);
            msg = msg + getTraceElement();
            Log.d(tag, msg);
        }
    }

    public static void debug(String message) {
        debug(TAG, message);
    }

    public static void debug(Object object, String message) {
        debug(object.getClass().getSimpleName(), message);
    }

    /**
     * 记录“warn”级别的信息
     *
     * @param tag tag
     * @param msg message
     */
    public static void warn(String tag, String msg) {
        if (isDebug) {
            tag = debugTag + (tag == null || tag.trim().isEmpty() ? "" : "-" + tag);
            msg = msg + getTraceElement();
            Log.w(tag, msg);
        }
    }

    public static void warn(Throwable e) {
        warn(toStackTraceString(e));
    }

    public static void warn(String message) {
        warn(TAG, message);
    }

    public static void warn(Object object, String message) {
        warn(object.getClass().getSimpleName(), message);
    }

    public static void warn(Object object, Throwable e) {
        warn(object.getClass().getSimpleName(), toStackTraceString(e));
    }

    /**
     * 记录“error”级别的信息
     *
     * @param tag tag
     * @param msg message
     */
    public static void error(String tag, String msg) {
        if (isDebug) {
            tag = debugTag + (tag == null || tag.trim().isEmpty() ? "" : "-" + tag);
            msg = msg + getTraceElement();
            Log.e(tag, msg);
        }
    }

    public static void error(Throwable e) {
        error(toStackTraceString(e));
    }

    public static void error(String message) {
        error(TAG, message);
    }

    public static void error(Object object, String message) {
        error(object.getClass().getSimpleName(), message);
    }

    public static void error(Object object, Throwable e) {
        error(object.getClass().getSimpleName(), toStackTraceString(e));
    }

    /**
     * 获取当前日志调用处的代码堆栈信息
     *
     * @return 格式化后的字符串
     */
    private static String getTraceElement() {
        try {
            int methodCount = METHOD_COUNT;
            StackTraceElement[] trace = Thread.currentThread().getStackTrace();
            int stackOffset = _getStackOffset(trace);

            if (methodCount + stackOffset > trace.length) {
                methodCount = trace.length - stackOffset - 1;
            }

            String level = "    ";
            StringBuilder builder = new StringBuilder();
            for (int i = methodCount; i > 0; i--) {
                int stackIndex = i + stackOffset;
                if (stackIndex >= trace.length) {
                    continue;
                }
                builder.append("\n")
                        .append(level)
                        .append(_getSimpleClassName(trace[stackIndex].getClassName()))
                        .append(".")
                        .append(trace[stackIndex].getMethodName())
                        .append(" ")
                        .append("(")
                        .append(trace[stackIndex].getFileName())
                        .append(":")
                        .append(trace[stackIndex].getLineNumber())
                        .append(")");
                level += "    ";
            }
            return builder.toString();
        } catch (Exception e) {
            Log.w(debugTag, e);
            return "";
        }
    }

    /**
     * 计算堆栈偏移量
     *
     * @param trace 堆栈数组
     * @return 返回第一个非LogUtils的堆栈索引（即日志调用方的起始位置）
     */
    private static int _getStackOffset(StackTraceElement[] trace) {
        for (int i = MIN_STACK_OFFSET; i < trace.length; i++) {
            StackTraceElement element = trace[i];
            String name = element.getClassName();
            if (!name.equals(LogUtils.class.getName())) {
                return --i;
            }
        }
        return -1;
    }

    private static String _getSimpleClassName(String name) {
        int lastIndex = name.lastIndexOf(".");
        return name.substring(lastIndex + 1);
    }

    /**
     * 处理异常堆栈信息
     *
     * @param throwable 异常
     * @return 处理完的异常信息
     */
    public static String toStackTraceString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stackTraceString = sw.toString();
        if (stackTraceString.length() > MAX_STACK_TRACE_SIZE) {
            String disclaimer = " [stack trace too large]";
            stackTraceString = stackTraceString.substring(0, MAX_STACK_TRACE_SIZE - disclaimer.length()) + disclaimer;
        }
        pw.close();
        return stackTraceString;
    }

    public static void startMethodTracing() {
        if (isDebug) {
            Debug.startMethodTracing(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + debugTag + ".trace");
        }
    }

    public static void stopMethodTracing() {
        if (isDebug) {
            Debug.stopMethodTracing();
        }
    }

}
