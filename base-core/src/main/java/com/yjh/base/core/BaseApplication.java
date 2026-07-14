package com.yjh.base.core;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

/**
 * Created by jiahui on 2025/12/16
 */
public class BaseApplication extends Application {
    // 提供一个静态的 Context，方便在类似 Utils 里使用
    @SuppressLint("StaticFieldLeak")
    private static Context context;

    //单例变量用来存 Application 实例本身
    @SuppressLint("StaticFieldLeak")
    private static BaseApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        instance = this;
    }

    public static Context getAppContext() {
        return context;
    }

    public static BaseApplication getInstance() {
        return instance;
    }

}
