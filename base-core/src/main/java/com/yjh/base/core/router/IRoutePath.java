package com.yjh.base.core.router;

import android.app.Activity;

/**
 * 路由安全通行证，通过接口类型来寻找真正的Activity
 * Created by jiahui on 2026/07/11
 */
public interface IRoutePath<T extends Activity> {
    Class<T> getTargetClass();
}
