package com.yjh.base.uikit.controller;

import android.app.Activity;
import android.view.View;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.yjh.base.utils.util.LogUtils;
import com.yjh.base.core.lifecycle.Lifecycle;
import com.yjh.base.core.lifecycle.LifecycleEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 封装官方的 swiperefreshlayout
 * Created by jiahui on 2026/01/29
 */
public class SwipeRefreshController implements Lifecycle {

    private SwipeRefreshLayout mSwipeRefreshLayout;

    //记录开始刷新的时间戳
    private long mStartTime;

    //定义刷新 UI 最少要转多久
    private static final int MIN_DURATION = 500;

    private IRefreshListener mListener;

    private static final int[] COLOR_RESOURCES = {
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
    };

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
        setRandomColors();
    }

    private void setRandomColors() {
        if (mSwipeRefreshLayout == null) return;

        // 将数组放入 List 中用于打乱顺序
        List<Integer> colorList = new ArrayList<>(COLOR_RESOURCES.length);
        for (int color : COLOR_RESOURCES) {
            colorList.add(color);
        }
        // 打乱顺序
        Collections.shuffle(colorList);

        // 转换为 int[] 重新设置给 SwipeRefreshLayout
        int[] randomColors = new int[colorList.size()];
        for (int i = 0; i < colorList.size(); i++) {
            randomColors[i] = colorList.get(i);
        }

        mSwipeRefreshLayout.setColorSchemeResources(randomColors);
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
                // 手动下拉触发时，随机换一次颜色
                setRandomColors();
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


    @Override
    public void onLifecycleChanged(LifecycleEvent event) {
        if (event == LifecycleEvent.ON_DESTROY) {
            mListener = null;
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setOnRefreshListener(null);
            }
        }
    }
}
