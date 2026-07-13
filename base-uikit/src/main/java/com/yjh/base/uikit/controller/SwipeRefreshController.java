package com.yjh.base.uikit.controller;

import android.app.Activity;
import android.view.View;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.yjh.base.uikit.listener.IRefreshListener;
import com.yjh.base.utils.util.LogUtils;

/**
 * 封装官方的 swiperefreshlayout
 * Created by jiahui on 2026/01/29
 */

public class SwipeRefreshController {

    private SwipeRefreshLayout mSwipeRefreshLayout;

    //记录开始刷新的时间戳
    private long mStartTime;

    //定义刷新 UI 最少要转多久
    private static final int MIN_DURATION = 500;

    private IRefreshListener mListener;

    // 构造函数：传入 Activity 和 控件ID
    public SwipeRefreshController(Activity activity, int refreshLayoutId) {
        View view = activity.findViewById(refreshLayoutId);
        if (view instanceof SwipeRefreshLayout) {
            mSwipeRefreshLayout = (SwipeRefreshLayout) view;
            initConfig();
        } else {
            LogUtils.error("ID对应的控件必须是 SwipeRefreshLayout");
        }
    }

    // 构造函数：直接传入 View (用于 Fragment 或 动态布局)
    public SwipeRefreshController(View rootView, int refreshLayoutId) {
        View view = rootView.findViewById(refreshLayoutId);
        if (view instanceof SwipeRefreshLayout) {
            mSwipeRefreshLayout = (SwipeRefreshLayout) view;
            initConfig();
        } else {
            LogUtils.error("ID对应的控件必须是 SwipeRefreshLayout");
        }
    }

    private void initConfig() {
        mSwipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );
    }

    /**
     * 设置刷新监听
     */
    public void setOnRefreshListener(IRefreshListener listener) {
        //将 listener保存为全局变量
        this.mListener = listener;
        if (mSwipeRefreshLayout != null && listener != null) {
            //手动下拉用的
            mSwipeRefreshLayout.setOnRefreshListener(() -> {
                mStartTime = System.currentTimeMillis();
                listener.onRefresh();
            });
        }
    }

    /**
     * 自动刷新
     */
    public void autoRefresh() {
        if (mSwipeRefreshLayout != null) {
            mStartTime = System.currentTimeMillis();
            mSwipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefreshLayout.setRefreshing(true);
                    if (mListener != null) {
                        mListener.onRefresh();
                    }
                }
            });
        }
    }

    /**
     * 结束刷新
     */
    public void finishRefresh() {
        if (mSwipeRefreshLayout != null) {
            long duration = System.currentTimeMillis() - mStartTime;
            long delay = 0;

            if (duration < MIN_DURATION) {
                delay = MIN_DURATION - duration;
            }
            mSwipeRefreshLayout.postDelayed(() -> {
                mSwipeRefreshLayout.setRefreshing(false);
            }, delay);
        }
    }

    /**
     * 设置是否启用
     */
    public void setEnableRefresh(boolean enable) {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setEnabled(enable);
        }
    }


}
