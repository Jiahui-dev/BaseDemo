package com.yjh.base.core.fragment

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.yjh.base.core.annotation.InjectPresenter
import com.yjh.base.core.contract.IBasePresenter
import com.yjh.base.core.contract.IBaseView
import com.yjh.base.core.lifecycle.Lifecycle
import com.yjh.base.core.lifecycle.LifecycleEvent
import com.yjh.base.core.router.BaseRouter
import com.yjh.base.utils.util.LogUtils
import java.lang.reflect.ParameterizedType

/**
 * 逻辑基类，管理 Presenter 和 Controller 生命周期的 Activity 基类，基于“组合优于继承”思想
 * Created by jiahui on 2026/10/05
 */
abstract class BaseCoreFragment<VB : ViewBinding> : Fragment(), IBaseView {

    companion object {
        private const val TAG = "BaseCoreFragment"
    }

    protected var binding: VB? = null

    protected var mActivity: Activity? = null

    protected var mRootView: View? = null

    private var mIsFirstInit = true

    //存放所有自动注入的Presenter
    private val mPresenterList = mutableListOf<IBasePresenter<*>>()

    //核心“排插插槽”
    private val mControllers = LinkedHashMap<String, Lifecycle>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Activity) {
            this.mActivity = context
        }
    }

    @Nullable
    override fun onCreateView(
        inflater: LayoutInflater,
        @Nullable container: ViewGroup?,
        @Nullable savedInstanceState: Bundle?
    ): View? {
        if (mRootView == null) {
            mIsFirstInit = true
            // 利用反射自动创建 ViewBinding 实例，干掉 LayoutId
            binding = initViewBinding(inflater, container)
            binding?.let {
                mRootView = it.root
            }
        } else {
            mIsFirstInit = false
            binding = initViewBinding(inflater, container)
        }
        // 缓存机制：防止 Fragment 切换时重复把同一个 View 添加到 Container 导致崩溃
        mRootView?.let { root ->
            (root.parent as? ViewGroup)?.removeView(root)
        }
        return mRootView
    }

    override fun onViewCreated(@NonNull view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 只有第一次初始化视图时，才走状态机的核心初始化流程
        if (mIsFirstInit) {
            // 子类去注册 Controller 插件
            onRegisterControllers()

            // 注入路由参数与 Presenter
            BaseRouter.inject(this)
            injectPresenters()

            // 驱动状态机：初始化就绪
            dispatchLifecycleEvent(LifecycleEvent.ON_INIT)

            // 驱动自身与子类 View 初始化
            initView()
            dispatchLifecycleEvent(LifecycleEvent.ON_VIEW_CREATED)

            // 驱动自身与子类数据流就绪
            initListener()
            initData()
            dispatchLifecycleEvent(LifecycleEvent.ON_DATA_INIT)

            mIsFirstInit = false
        }
    }

    protected fun registerController(key: String, controller: Lifecycle?) {
        controller ?: return
        if (!mControllers.containsKey(key)) {
            mControllers[key] = controller
        }
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <T : Lifecycle> getController(key: String): T? {
        return mControllers[key] as? T
    }

    private fun dispatchLifecycleEvent(event: LifecycleEvent) {
        mControllers.values.forEach { controller ->
            controller?.onLifecycleChanged(event)
        }
    }

    protected abstract fun onRegisterControllers()

    protected open fun initView() {}

    protected open fun initListener() {}

    protected open fun initData() {}

    override fun onStart() {
        super.onStart()
        dispatchLifecycleEvent(LifecycleEvent.ON_START)
    }

    override fun onResume() {
        super.onResume()
        dispatchLifecycleEvent(LifecycleEvent.ON_RESUME)
    }

    override fun onPause() {
        super.onPause()
        dispatchLifecycleEvent(LifecycleEvent.ON_PAUSE)
    }

    override fun onStop() {
        super.onStop()
        dispatchLifecycleEvent(LifecycleEvent.ON_STOP)
    }

    override fun onDestroy() {
        // 通知所有插件彻底清理内存
        dispatchLifecycleEvent(LifecycleEvent.ON_DESTROY)
        mControllers.clear()

        // Presenter 彻底解绑
        mPresenterList.forEach { presenter ->
            runCatching { presenter.detachView() }
                .onFailure { LogUtils.error(TAG, "detachView 失败：$it") }
        }
        mPresenterList.clear()

        super.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 为了防止 Fragment 实例存活但视图销毁时的内存泄漏，必须在此处把 binding 强行置空！
        binding = null
    }

    override fun getActivityContext(): Activity? = mActivity

    private fun injectPresenters() {
        javaClass.declaredFields
            .filter { it.isAnnotationPresent(InjectPresenter::class.java) }
            .forEach { field ->
                runCatching {
                    val type = field.type
                    if (IBasePresenter::class.java.isAssignableFrom(type)) {
                        @Suppress("UNCHECKED_CAST")
                        val presenter = type
                            .getDeclaredConstructor()
                            .newInstance() as IBasePresenter<IBaseView>

                        presenter.attachView(this)

                        field.isAccessible = true
                        field.set(this, presenter)
                        mPresenterList.add(presenter)
                    }
                }.onFailure { e ->
                    LogUtils.error(TAG, "@InjectPresenter 失败：$e")
                }
            }
    }

    /**
     * 自动解析泛型 VB 的 inflate 方法
     */
    @Suppress("UNCHECKED_CAST")
    private fun initViewBinding(inflater: LayoutInflater, container: ViewGroup?): VB? {
        return try {
            val type = javaClass.genericSuperclass
            if (type is ParameterizedType) {
                val actualTypeArguments = type.actualTypeArguments
                val clazz = actualTypeArguments[0] as Class<VB>
                val method = clazz.getMethod(
                    "inflate",
                    LayoutInflater::class.java,
                    ViewGroup::class.java,
                    Boolean::class.javaPrimitiveType
                )
                method.invoke(null, inflater, container, false) as? VB
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}