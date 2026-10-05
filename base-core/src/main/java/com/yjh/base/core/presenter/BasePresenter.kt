package com.yjh.base.core.presenter

import com.yjh.base.core.contract.IBasePresenter
import com.yjh.base.core.contract.IBaseView
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import java.lang.ref.WeakReference
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Created by youjiahui on 2026/10/4
 */
open class BasePresenter<V : IBaseView> : IBasePresenter<V> {

    private var mViewRef: WeakReference<V>? = null
    private var mProxyView: V? = null
    protected var mCompositeDisposable: CompositeDisposable? = null

    @Suppress("UNCHECKED_CAST")
    override fun attachView(view: V) {
        mViewRef = WeakReference(view)
        mCompositeDisposable = CompositeDisposable()

        // 当 View 被销毁后，调用 View 的方法不会空指针，而是直接“空过”
        val viewHandler = MvpViewHandler(mViewRef)
        mProxyView = Proxy.newProxyInstance(
            view.javaClass.classLoader,
            view.javaClass.interfaces,
            viewHandler
        ) as V
    }

    override fun detachView() {
        // 取消所有正在进行的 RxJava 任务
        mCompositeDisposable?.clear()

        // 释放 View 引用
        mViewRef?.clear()
        mViewRef = null
        mProxyView = null
    }

    override val view: V?
        get() = mProxyView

    /**
     * 将 RxJava 的 Disposable 添加到管理列表
     */
    protected fun addDisposable(disposable: Disposable) {
        mCompositeDisposable?.add(disposable)
    }

    protected fun isViewAttached(): Boolean {
        return mViewRef != null && mViewRef?.get() != null
    }

    private inner class MvpViewHandler(private val viewRef: WeakReference<V>?) : InvocationHandler {

        override fun invoke(
            proxy: Any?,
            method: Method?,
            args: Array<out Any?>?
        ): Any? {
            // 用局部变量 target 统一接住弱引用解出来的对象
            val target = viewRef?.get()

            // 处理 Object 自带的方法（如 toString, equals, hashCode）
            if (method?.declaringClass == Any::class.java) {
                return method.invoke(this, *(args ?: emptyArray()))
            }

            // 如果 View 还在，就正常执行 UI 操作
            if (target != null) {
                return method?.invoke(target, *(args ?: emptyArray()))
            }

            // 处理基本数据类型（防止自动拆箱导致 NPE）
            return when (method?.returnType) {
                Boolean::class.java, Boolean -> false
                Int::class.java, Int -> 0
                Long::class.java, Long -> 0L
                Float::class.java, Float -> 0f
                Double::class.java, Double -> 0.0
                else -> null
            }
        }
    }
}