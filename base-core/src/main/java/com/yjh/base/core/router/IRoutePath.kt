package com.yjh.base.core.router

import android.app.Activity

/**
 * 路由安全通行证，通过接口类型来寻找真正的Activity
 * Created by youjiahui on 2026/10/2
 */
interface IRoutePath<T : Activity> {
    val targetClass: Class<T>
}