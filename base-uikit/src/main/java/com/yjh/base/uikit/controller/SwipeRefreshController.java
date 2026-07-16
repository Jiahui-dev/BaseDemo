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

    // 记录上一次刷新的第一个颜色，用来做去重校验
    private int mLastFirstColor = -1;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    //记录开始刷新的时间戳
    private long mStartTime;

    //定义刷新 UI 最少要转多久
    private static final int MIN_DURATION = 2000;

    private IRefreshListener mListener;

    private static final int[] COLOR_RESOURCES = {
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light,
            android.R.color.holo_blue_dark,
            android.R.color.holo_green_dark,
            android.R.color.holo_orange_dark,
            android.R.color.holo_purple
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

        List<Integer> colorList = new ArrayList<>(COLOR_RESOURCES.length);
        for (int color : COLOR_RESOURCES) {
            colorList.add(color);
        }

        // 强随机：如果打乱后的第一个颜色和上次一样，就再打乱一次，确保每次出来的首发颜色不同
        int attempts = 0;
        do {
            Collections.shuffle(colorList);
            attempts++;
        } while (colorList.get(0) == mLastFirstColor && attempts < 5);

        // 记录这次的第一个颜色，留作下次对比
        mLastFirstColor = colorList.get(0);

        // 转换为 int[] 设置给 SwipeRefreshLayout
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
