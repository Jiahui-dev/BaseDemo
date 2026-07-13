package com.yjh.base.core.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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
import androidx.viewbinding.ViewBinding;

/**
 * 逻辑基类，管理 Presenter 和 Controller 生命周期的 Activity 基类，基于“组合优于继承”思想
 * Created by jiahui on 2026/07/13
 */
public abstract class BaseCoreFragment<VB extends ViewBinding> extends Fragment implements IBaseView {

    private static final String TAG = "BaseCoreFragment";

    protected VB binding;

    protected Activity mActivity;

    protected View mRootView;

    private boolean mIsFirstInit = true;

    //存放所有自动注入的Presenter
    private final List<IBasePresenter> mPresenterList = new ArrayList<>();

    //核心“排插插槽”
    private final Map<String, Lifecycle> mControllers = new HashMap<>();

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            this.mActivity = (Activity) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (mRootView == null) {
            mIsFirstInit = true;
            // 利用反射自动创建 ViewBinding 实例，干掉 LayoutId
            binding = initViewBinding(inflater, container);
            if (binding != null) {
                mRootView = binding.getRoot();
            }
        } else {
            mIsFirstInit = false;
            binding = initViewBinding(inflater, container);
        }
        // 缓存机制：防止 Fragment 切换时重复把同一个 View 添加到 Container 导致崩溃
        if (mRootView != null) {
            ViewGroup parent = (ViewGroup) mRootView.getParent();
            if (parent != null) {
                parent.removeView(mRootView);
            }
        }
        return mRootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 只有第一次初始化视图时，才走状态机的核心初始化流程
        if (mIsFirstInit) {
            // 子类去注册 Controller 插件
            onRegisterControllers();

            // 注入路由参数与 Presenter
            BaseRouter.getInstance().inject(this);
            injectPresenters();

            // 驱动状态机：初始化就绪
            dispatchLifecycleEvent(LifecycleEvent.ON_INIT);

            // 驱动自身与子类 View 初始化
            initView();
            dispatchLifecycleEvent(LifecycleEvent.ON_VIEW_CREATED);

            // 驱动自身与子类数据流就绪
            initListener();
            initData();
            dispatchLifecycleEvent(LifecycleEvent.ON_DATA_INIT);

            mIsFirstInit = false;
        }
    }

    protected void registerController(String key, Lifecycle controller) {
        if (controller != null && !mControllers.containsKey(key)) {
            mControllers.put(key, controller);
        }
    }

    @SuppressWarnings("unchecked")
    protected <T extends Lifecycle> T getController(String key) {
        return (T) mControllers.get(key);
    }

    private void dispatchLifecycleEvent(LifecycleEvent event) {
        for (Lifecycle controller : mControllers.values()) {
            if (controller != null) {
                controller.onLifecycleChanged(event);
            }
        }
    }

    protected abstract void onRegisterControllers();

    protected void initView() {
    }

    protected void initListener() {
    }

    protected void initData() {
    }

    @Override
    public void onStart() {
        super.onStart();
        dispatchLifecycleEvent(LifecycleEvent.ON_START);
    }

    @Override
    public void onResume() {
        super.onResume();
        dispatchLifecycleEvent(LifecycleEvent.ON_RESUME);
    }

    @Override
    public void onPause() {
        super.onPause();
        dispatchLifecycleEvent(LifecycleEvent.ON_PAUSE);
    }

    @Override
    public void onStop() {
        super.onStop();
        dispatchLifecycleEvent(LifecycleEvent.ON_STOP);
    }

    @Override
    public void onDestroy() {
        // 通知所有插件彻底清理内存
        dispatchLifecycleEvent(LifecycleEvent.ON_DESTROY);
        mControllers.clear();

        // Presenter 彻底解绑
        for (IBasePresenter presenter : mPresenterList) {
            presenter.detachView();
        }
        mPresenterList.clear();

        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 为了防止 Fragment 实例存活但视图销毁时的内存泄漏，必须在此处把 binding 强行置空！
        binding = null;
    }

    @Override
    public Activity getActivityContext() {
        return mActivity;
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

    /**
     * 自动解析泛型 VB 的 inflate 方法
     */
    @SuppressWarnings("unchecked")
    private VB initViewBinding(LayoutInflater inflater, ViewGroup container) {
        try {
            // 获取当前类的父类泛型信息
            Type type = getClass().getGenericSuperclass();
            if (type instanceof ParameterizedType) {
                Type[] actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
                // 拿到 VB 具体的 Class 对象
                Class<VB> clazz = (Class<VB>) actualTypeArguments[0];
                // 找到 VB 的 inflate(LayoutInflater, ViewGroup, boolean) 方法
                Method method = clazz.getMethod("inflate", LayoutInflater.class, ViewGroup.class, boolean.class);
                // 调用并返回 ViewBinding 实例
                return (VB) method.invoke(null, inflater, container, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
