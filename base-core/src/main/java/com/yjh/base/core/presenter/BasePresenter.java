package com.yjh.base.core.presenter;

import com.yjh.base.core.contract.IBasePresenter;
import com.yjh.base.core.contract.IBaseView;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

/**
 * Created by jiahui on 2025/12/26
 */
public class BasePresenter<V extends IBaseView> implements IBasePresenter<V> {

    private WeakReference<V> mViewRef;
    private V mProxyView;
    public CompositeDisposable mCompositeDisposable;

    @Override
    @SuppressWarnings("unchecked")
    public void attachView(V view) {
        mViewRef=new WeakReference<>(view);
        mCompositeDisposable=new CompositeDisposable();

        //当 View 被销毁后，调用 View 的方法不会空指针，而是直接“空过”
        MvpViewHandler viewHandler=new MvpViewHandler(mViewRef);
        mProxyView=(V) Proxy.newProxyInstance(
                view.getClass().getClassLoader(),
                view.getClass().getInterfaces(),
                viewHandler
        );
    }

    @Override
    public void detachView() {
        //1.取消所有正在进行的 RxJava 任务
        if(mCompositeDisposable!=null){
            mCompositeDisposable.clear();
        }

        //2.释放 View 引用
        if (mViewRef != null) {
            mViewRef.clear();
            mViewRef=null;
        }
        mProxyView=null;
    }

    /**
     * 子类永远调用这个方法来获取 View
     * @return 代理后的 View，绝不会为 null
     */
    @Override
    public V getView() {
        return mProxyView;
    }

    /**
     * 将 RxJava 的 Disposable 添加到管理列表
     */
    protected void addDisposable(Disposable disposable){
        if(mCompositeDisposable!=null){
            mCompositeDisposable.add(disposable);
        }
    }

    private boolean isViewAttached() {
        return mViewRef != null && mViewRef.get() != null;
    }

    // 动态代理处理器
    private class MvpViewHandler implements InvocationHandler {
        private final WeakReference<V> viewRef;

        MvpViewHandler(WeakReference<V> viewRef) {
            this.viewRef = viewRef;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 如果 View 还在，就正常执行 UI 操作
            if (isViewAttached()) {
                return method.invoke(viewRef.get(), args);
            }

            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }

            // 处理基本数据类型（防止自动拆箱导致 NPE）
            // 如果方法返回 boolean，返回 false
            if (method.getReturnType() == boolean.class) {
                return false;
            }
            // 如果方法返回 int/long/float/double，返回 0
            if (method.getReturnType() == int.class || method.getReturnType() == long.class ||
                    method.getReturnType() == float.class || method.getReturnType() == double.class) {
                return 0;
            }

            // 如果 View 已经销毁，什么都不做，返回 null
            return null;
        }
    }

}
