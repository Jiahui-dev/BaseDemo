package com.yjh.base.core.contract;

/**
 * 所有 Presenter 的基类接口
 * Created by jiahui on 2026/01/28
 */
public interface IBasePresenter<V extends IBaseView> {
    
    //绑定 View
    void attachView(V view);

    //解绑 View
    void detachView();

    //获取 View 引用，通常由实现类处理
    V getView();

}
