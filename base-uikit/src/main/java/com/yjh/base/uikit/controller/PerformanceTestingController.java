package com.yjh.base.uikit.controller;

import com.yjh.base.core.lifecycle.Lifecycle;
import com.yjh.base.core.lifecycle.LifecycleEvent;
import com.yjh.base.utils.util.LogUtils;

/**
 * Created by jiahui on 2026/08/07
 * 监控 Activity 从内存创建到数据渲染完毕的全链路耗时
 */
public class PerformanceTestingController implements Lifecycle {

    private static final String TAG = "PerformanceController";

    // 预设告警阈值，超过此时间将警告
    private static final long WARN_THRESHOLD_MS = 300;

    private final String mPageName;
    private long mStartTime = 0;
    private long mViewCreatedTime = 0;

    public PerformanceTestingController(String pageName) {
        this.mPageName = pageName;
    }

    @Override
    public void onLifecycleChanged(LifecycleEvent event) {
        switch (event) {
            case ON_INIT:
                mStartTime = System.currentTimeMillis();
                break;

            case ON_VIEW_CREATED:
                mViewCreatedTime = System.currentTimeMillis();
                break;

            case ON_DATA_INIT:
                long dataInitTime = System.currentTimeMillis();

                // UI 布局与控件初始化耗时 (ON_VIEW_CREATED - ON_INIT)
                long calculatedUiDuration = mViewCreatedTime - mStartTime;

                // 数据与业务初始化耗时 (ON_DATA_INIT - ON_VIEW_CREATED)
                long dataDuration = dataInitTime - mViewCreatedTime;

                // 总耗时 (ON_DATA_INIT - ON_INIT)
                long totalDuration = dataInitTime - mStartTime;

                StringBuilder sb = new StringBuilder();
                sb.append("页面名称: ").append(mPageName).append("\n")
                        .append("UI 布局耗时: ").append(calculatedUiDuration).append(" ms\t")
                        .append("数据准备耗时: ").append(dataDuration).append(" ms\t")
                        .append("总耗时: ").append(totalDuration).append(" ms");

                if (totalDuration < WARN_THRESHOLD_MS) {
                    sb.append(" ⚠️\n");
                    LogUtils.warn(TAG, sb.toString());
                } else {
                    LogUtils.info(TAG, sb.toString());
                }
                break;

            case ON_DESTROY:
                mStartTime = 0;
                mViewCreatedTime = 0;
                break;

            default:
                break;
        }
    }
}