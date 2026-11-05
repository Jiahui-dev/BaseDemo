package com.yjh.base.uikit.fragment;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.viewbinding.ViewBinding;

import com.gyf.immersionbar.ImmersionBar;
import com.yjh.base.core.fragment.BaseCoreFragment;
import com.yjh.base.uikit.R;
import com.yjh.base.utils.util.ToastUtils;

import java.util.Objects;

/**
 * UI视觉样式基类
 * 逻辑基类：{@link BaseCoreFragment}
 * Created by jiahui on 2026/07/13
 */
public abstract class BaseFragment<VB extends ViewBinding> extends BaseCoreFragment<VB> {

    private static final String TAG = "BaseFragment";

    private AlertDialog mLoadingDialog;

    private TextView mTvLoadingMsg;

    @Override
    protected void onRegisterControllers() {

    }

    @Override
    public void onResume() {
        super.onResume();
        initImmersionBar();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            initImmersionBar();
        }
    }

    protected void initImmersionBar() {
        if (getActivity() == null) return;
        ImmersionBar.with(this)
                .statusBarColor(getStatusBarColor())
                .statusBarDarkFont(isStatusBarDarkFont())
                .titleBar(getTitleBarView())
                .keyboardEnable(true)
                .init();
    }

    protected View getTitleBarView() {
        return null;
    }

    protected int getStatusBarColor() {
        return R.color.uikit_grey_100;
    }

    protected boolean isStatusBarDarkFont() {
        return true;
    }

    // ==================== Loading 相关 ====================

    @Override
    public void showLoading(String msg) {
        // 增加 Fragment 状态判断，防止视图已销毁或已脱离 Activity 时弹窗崩溃
        if (getActivity() == null || getActivity().isFinishing() || !isAdded()) {
            return;
        }

        if (mLoadingDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            View view = LayoutInflater.from(getActivity()).inflate(R.layout.uikit_view_loading_bar, null);
            mTvLoadingMsg = view.findViewById(R.id.tv_dialogMsg);
            builder.setView(view);
            builder.setCancelable(false);
            mLoadingDialog = builder.create();
            Objects.requireNonNull(mLoadingDialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        }

        if (mTvLoadingMsg != null) {
            mTvLoadingMsg.setText(msg);
        }
        if (!mLoadingDialog.isShowing()) {
            mLoadingDialog.show();
        }
    }

    @Override
    public void hideLoading() {
        if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
            mLoadingDialog.dismiss();
        }
    }

    @Override
    public void showError(String msg) {
        ToastUtils.showShort(msg);
    }

    @Override
    public void onDestroyView() {
        // 在视图销毁时关闭弹窗，防止内存泄漏
        hideLoading();
        mLoadingDialog = null;
        mTvLoadingMsg = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (mLoadingDialog != null) {
            mLoadingDialog.dismiss();
            mLoadingDialog = null;
        }
        super.onDestroy();
    }

    protected void setClick(View.OnClickListener listener, View... views) {
        setClick(500, listener, views);
    }

    protected void setClick(final long delayMilliseconds, final View.OnClickListener listener, View... views) {
        if (views == null || listener == null) return;
        for (View view : views) {
            if (view != null) {
                view.setOnClickListener(new View.OnClickListener() {
                    private long lastClickTime = 0;

                    @Override
                    public void onClick(View v) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastClickTime >= delayMilliseconds) {
                            lastClickTime = currentTime;
                            listener.onClick(v);
                        }
                    }
                });
            }
        }
    }
}