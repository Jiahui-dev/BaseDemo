package com.yjh.base.core.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewbinding.ViewBinding;
import com.yjh.base.core.annotation.InjectPresenter;
import com.yjh.base.core.contract.IBasePresenter;
import com.yjh.base.core.contract.IBaseView;
import com.yjh.base.core.lifecycle.Lifecycle;
import com.yjh.base.core.lifecycle.LifecycleEvent;
import com.yjh.base.core.router.BaseRouter;
import com.yjh.base.utils.util.LogUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 逻辑基类，管理 Presenter 和 Controller 生命周期的 Activity 基类，基于“组合优于继承”思想
 * Created by jiahui on 2026/07/11
 */
public abstract class BaseCoreActivity<VB extends ViewBinding> extends AppCompatActivity implements IBaseView {

    private static final String TAG = "BaseCoreActivity";

    protected VB binding;

    //存放所有自动注入的Presenter
    private final List<IBasePresenter> mPresenterList = new ArrayList<>();

    //核心“排插插槽”
    private final Map<String, Lifecycle> mControllers = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 非反射的初始化方法
        binding = initBinding(getLayoutInflater());
        if (binding != null) {
            setContentView(binding.getRoot());
        } else {
            LogUtils.error(TAG,"ViewBinding 绑定失败，请检查 onBindingInflate 实现！");
        }

        // 让子类在这个时机去注册它需要的 Controller（此时布局已经塞进去了，可以放心拿 rootView）
        onRegisterControllers();

        // 路由注入与 Presenter 自动绑定
        BaseRouter.getInstance().inject(this);
        injectPresenters();

        // 驱动状态机：内存初始化就绪
        dispatchLifecycleEvent(LifecycleEvent.ON_INIT);

        // Activity 自身的 View 初始化
        initView();
        // 驱动状态机：通知所有插件——View 已经绑定完毕！
        // 此时 Controller 内部的 case ON_VIEW_CREATED 绝对可以安全地拿到各个控件！
        dispatchLifecycleEvent(LifecycleEvent.ON_VIEW_CREATED);

        initListener();
        initData();
        // 驱动状态机：数据流就绪
        dispatchLifecycleEvent(LifecycleEvent.ON_DATA_INIT);

    }

    /**
     * 供子类调用：往插排上“插插件”
     */
    protected void registerController(String key, Lifecycle controller) {
        if (controller != null && !mControllers.containsKey(key)) {
            mControllers.put(key, controller);
        }
    }

    /**
     * 供子类调用：可以通过 Key 拿到具体的插件实例（比如在 Activity 里控制加载 Footer 的样式）
     */
    @SuppressWarnings("unchecked")
    protected <T extends Lifecycle> T getController(String key) {
        return (T) mControllers.get(key);
    }

    /**
     * 分发逻辑：遍历所有控制器，驱动他们的状态机流转
     */
    private void dispatchLifecycleEvent(LifecycleEvent event) {
        for (Lifecycle controller : mControllers.values()) {
            if (controller != null) {
                controller.onLifecycleChanged(event);
            }
        }
    }

    /**
     * 在这里子类可以写：setContentView(R.layout.activity_main);
     * 或者写 ViewBinding 的加载。
     */
    protected abstract void onRegisterControllers();

    protected abstract VB initBinding(LayoutInflater inflater);

    protected void initView() {
    }

    protected void initListener() {
    }

    protected void initData() {
    }


    @Override
    protected void onStart() {
        super.onStart();
        dispatchLifecycleEvent(LifecycleEvent.ON_START);
    }

    @Override
    protected void onResume() {
        super.onResume();
        dispatchLifecycleEvent(LifecycleEvent.ON_RESUME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        dispatchLifecycleEvent(LifecycleEvent.ON_PAUSE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        dispatchLifecycleEvent(LifecycleEvent.ON_STOP);
    }

    @Override
    protected void onDestroy() {
        // 通知所有控制器进行最后的销毁和内存清理
        dispatchLifecycleEvent(LifecycleEvent.ON_DESTROY);

        // 强行清空集合，彻底断开Activity对所有控制器的强引用，确保0内存泄漏
        mControllers.clear();

        // Presenter 解绑
        for (IBasePresenter presenter : mPresenterList) {
            presenter.detachView();
        }
        mPresenterList.clear();
        binding = null;
        super.onDestroy();

    }

    @Override
    public Activity getActivityContext() {
        return this;
    }

    private void injectPresenters() {
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(InjectPresenter.class)) {
                try {
                    Class<?> type = field.getType();
                    if (IBasePresenter.class.isAssignableFrom(type)) {
                        field.setAccessible(true);
                        IBasePresenter presenter = (IBasePresenter) type.newInstance();
                        presenter.attachView(this);
                        field.set(this, presenter);
                        mPresenterList.add(presenter);
                    }
                } catch (Exception e) {
                    LogUtils.error("@InjectPresenter 失败：", e);
                }
            }
        }
    }

}
