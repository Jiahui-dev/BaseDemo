package com.yjh.base.uikit.controller;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewStub;
import com.yjh.base.core.lifecycle.Lifecycle;
import com.yjh.base.core.lifecycle.LifecycleEvent;
import java.util.List;

/**
 * 状态控制器：负责切换 内容/空数据/错误页
 * Created by jiahui on 2026/01/28
 */
public class StateController implements Lifecycle {

    private Activity mActivity;
    private Context mContext;
    private View mContentView;//正常显示的内容
    private View mEmptyView;//空布局
    private ViewStub mEmptyViewStub;//懒加载空布局
    private View mErrorView;//出错布局
    private ViewStub mErrorViewStub;//懒加载出错布局

    public StateController(Activity activity, View contentView) {
        this.mActivity = activity;
        this.mContentView = contentView;
    }

    public StateController(View rootView, View contentView) {
        this.mContext = rootView.getContext();
        this.mContentView = contentView;
    }

    /**
     * 设置空布局的 ViewStub
     */
    public void setEmptyViewStub(ViewStub viewStub) {
        this.mEmptyViewStub = viewStub;
    }

    /**
     * 设置错误布局的 ViewStub
     */
    public void setErrorViewStub(ViewStub viewStub) {
        this.mErrorViewStub = viewStub;
    }

    public void handleData(List<?> data){
        if(data==null||data.isEmpty()){
            showEmpty();
        }else{
            showContent();
        }
    }

    public void showContent() {
        if (mContentView != null) mContentView.setVisibility(View.VISIBLE);
        if (mEmptyView != null) mEmptyView.setVisibility(View.GONE);
        if (mErrorView != null) mErrorView.setVisibility(View.GONE);
    }

    public void showEmpty(){
        if(mEmptyView==null){
            if(mEmptyViewStub!=null){
                mEmptyView=mEmptyViewStub.inflate();
            }else{
                return;
            }
        }
        // 展示空布局时，内容和错误布局必须彻底隐藏
        if (mEmptyView != null) mEmptyView.setVisibility(View.VISIBLE);
        if (mContentView != null) mContentView.setVisibility(View.GONE);
        if (mErrorView != null) mErrorView.setVisibility(View.GONE);
    }

    public void showError() {
        if (mErrorView == null) {
            if (mErrorViewStub != null) {
                mErrorView = mErrorViewStub.inflate();
            } else {
                return;
            }
        }
        // 展示错误布局时，内容和空布局需要彻底隐藏
        if (mErrorView != null) mErrorView.setVisibility(View.VISIBLE);
        if (mContentView != null) mContentView.setVisibility(View.GONE);
        if (mEmptyView != null) mEmptyView.setVisibility(View.GONE);
    }

    @Override
    public void onLifecycleChanged(LifecycleEvent event) {
        switch (event) {
            case ON_INIT:
                // 内存初始化时想做的事
                break;
            case ON_DATA_INIT:
                // 数据开始流转时的初始化
                break;
            case ON_START:
            case ON_RESUME:
            case ON_PAUSE:
            case ON_STOP:
                // 原生的基础生命周期
                break;
            case ON_DESTROY:
                // 内存熔断与释放，防止 Activity 销毁后 Controller 持有 View 导致内存泄漏
                mActivity = null;
                mContext = null;
                mContentView = null;
                mEmptyView = null;
                mErrorView = null;
                mEmptyViewStub = null;
                mErrorViewStub = null;
                break;
            default:
                break;
        }
    }
}
