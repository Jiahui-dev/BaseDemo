package com.yjh.base.core.contract

/**
 * 所有 Presenter 的基类接口
 * Created by youjiahui on 2026/10/3
 */
interface IBasePresenter<V : IBaseView> {

    //绑定 View
    fun attachView(view: V)

    //解绑 View
    fun detachView()

    //获取 View 的引用，通常由实现类处理
    val view: V?

}