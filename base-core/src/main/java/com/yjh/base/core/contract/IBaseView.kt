package com.yjh.base.core.contract

import android.app.Activity

/**
 * 所有 View 层接口必须继承此接口
 * 这里可以定义通用的 UI 操作
 * Created by jiahui on 2026/10/03
 */
interface IBaseView {

    fun getActivityContext(): Activity?

    fun showLoading(msg: String)

    fun hideLoading()

    fun showError(msg: String)

}