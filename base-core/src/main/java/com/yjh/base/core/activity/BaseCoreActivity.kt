package com.yjh.base.core.activity

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.yjh.base.core.annotation.InjectPresenter
import com.yjh.base.core.contract.IBasePresenter
import com.yjh.base.core.contract.IBaseView
import com.yjh.base.core.lifecycle.Lifecycle
import com.yjh.base.core.lifecycle.LifecycleEvent
import com.yjh.base.core.router.BaseRouter
import com.yjh.base.utils.util.LogUtils

/**
 * 逻辑基类，管理 Presenter 和 Controller 生命周期的 Activity 基类，基于“组合优于继承”思想
 * Created by youjiahui on 2026/10/5
 */
abstract class BaseCoreActivity<VB : ViewBinding> : AppCompatActivity(), IBaseView {

    private val TAG: String = "BaseCoreActivity"

    var binding: VB? = null

    private val mPresenterList = ArrayList<IBasePresenter<*>>()

    private val mControllers = HashMap<String, Lifecycle>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 非反射的初始化方法
        binding = initBinding(LayoutInflater.from(this))
        binding?.let {
            setContentView(it.root)
        } ?: LogUtils.error(TAG, "ViewBinding 绑定失败")

        // 让子类在这个时机去注册它需要的 Controller（此时布局已经塞进去了，可以放心拿 rootView）
        onRegisterControllers()

        // 路由注入与 Presenter 自动绑定
        BaseRouter.inject(this)
        injectPresenters()

        // 驱动状态机：内存初始化就绪
        dispatchLifecycleEvent(LifecycleEvent.ON_INIT)

        // Activity 自身的 View 初始化
        initView()
        // 驱动状态机：通知所有插件——View 已经绑定完毕！
        // 此时 Controller 内部的 case ON_VIEW_CREATED 绝对可以安全地拿到各个控件！
        dispatchLifecycleEvent(LifecycleEvent.ON_VIEW_CREATED)

        // 专门处理本地控件点击事件
        initListener()
        // 专门处理全局、跨组件消息
        initEvent()
        initData()
        // 驱动状态机：数据流就绪
        dispatchLifecycleEvent(LifecycleEvent.ON_DATA_INIT)

    }

    /**
     * 供子类调用：往插排上“插插件”
     */
    protected fun registerController(key: String, controller: Lifecycle) {
        if (!mControllers.containsKey(key)) {
            mControllers[key] = controller
        }
    }

    protected fun <T : Lifecycle> registerController(clazz: Class<T>?, controller: T) {
        if (clazz != null) {
            registerController(clazz.name, controller)
        }
    }

    /**
     * 供子类调用：可以通过 Key 拿到具体的插件实例（比如在 Activity 里控制加载 Footer 的样式）
     */
    @Suppress("UNCHECKED_CAST")
    protected fun <T : Lifecycle> getController(key: String): T? {
        return mControllers[key] as? T
    }

    /**
     * 供子类调用：根据 Class 拿到具体的插件实例
     */
    @Suppress("UNCHECKED_CAST")
    protected fun <T : Lifecycle> getController(clazz: Class<T>?): T? {
        if (clazz == null) return null
        return getController<Lifecycle>(clazz.name) as T
    }

    /**
     * 分发逻辑：遍历所有控制器，驱动他们的状态机流转
     */
    private fun dispatchLifecycleEvent(event: LifecycleEvent) {
        mControllers.values.forEach { controller ->
            controller.onLifecycleChanged(event)
        }
    }

    /**
     * 在这里子类可以写：setContentView(R.layout.activity_main);
     * 或者写 ViewBinding 的加载。
     */
    protected abstract fun onRegisterControllers()

    protected abstract fun initBinding(inflater: LayoutInflater): VB

    protected open fun initView() {}

    protected open fun initListener() {}

    protected open fun initEvent() {}

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

    override fun getActivityContext(): Activity {
        return this
    }

    override fun onDestroy() {

        // 通知所有控制器进行最后的销毁和内存清理
        dispatchLifecycleEvent(LifecycleEvent.ON_DESTROY);

        // 强行清空集合，彻底断开Activity对所有控制器的强引用，确保0内存泄漏
        mControllers.clear();

        // Presenter 解绑
        mPresenterList.forEach { presenter ->
            presenter.detachView()
        }

        mPresenterList.clear();
        binding = null
        super.onDestroy();
    }

    private fun injectPresenters() {
        val fields = this.javaClass.declaredFields
        for (field in fields) {
            if (field.isAnnotationPresent(InjectPresenter::class.java)) {
                try {
                    val type = field.type
                    if (IBasePresenter::class.java.isAssignableFrom(type)) {
                        field.isAccessible = true
                        @Suppress("UNCHECKED_CAST")
                        val presenter =
                            type.getDeclaredConstructor().newInstance() as IBasePresenter<IBaseView>
                        presenter.attachView(this)
                        field.set(this, presenter)
                        mPresenterList.add(presenter)
                    }
                } catch (e: Exception) {
                    LogUtils.error(TAG, "@InjectPresenter 失败：$e")
                }
            }
        }
    }

}