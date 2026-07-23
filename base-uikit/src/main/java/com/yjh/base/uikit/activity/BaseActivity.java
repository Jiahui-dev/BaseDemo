package com.yjh.base.uikit.activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;
import com.gyf.immersionbar.ImmersionBar;
import com.yjh.base.core.activity.BaseCoreActivity;
import com.yjh.base.uikit.R;
import com.yjh.base.utils.util.ToastUtils;
import java.util.Objects;

/**
 * UI视觉样式基类
 * 逻辑基类：{@link BaseCoreActivity}
 * Created by jiahui on 2026/07/13
 */
public abstract class BaseActivity<VB extends ViewBinding> extends BaseCoreActivity<VB> {

    private static final String TAG = "BaseActivity";

    private AlertDialog mLoadingDialog;

    private TextView mTvLoadingMsg;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化沉浸式
        initImmersionBar();
    }

    @Override
    protected void onRegisterControllers() {

    }

    protected View getTitleBar() { return null; }
    protected int getStatusBarColor() { return R.color.uikit_grey_backGround; } // 规范资源前缀
    protected boolean isStatusBarDarkFont() { return true; }

    protected void initImmersionBar() {
        ImmersionBar.with(this)
                .statusBarColor(getStatusBarColor())
                .statusBarDarkFont(isStatusBarDarkFont())
                .titleBar(getTitleBar())
                //.fitsSystemWindows(true)
                .keyboardEnable(true)
                .init();
    }

    @Override
    public void showLoading(String msg) {
        if (mLoadingDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            // 资源随代码走：uikit_view_dialog_loading.xml 存放在 base-uikit 模块中
            View view = LayoutInflater.from(this).inflate(R.layout.uikit_view_dialog_loading, null);
            mTvLoadingMsg = view.findViewById(R.id.tv_dialogMsg);
            builder.setView(view);
            builder.setCancelable(false);
            mLoadingDialog = builder.create();
            Objects.requireNonNull(mLoadingDialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        }
        if (mTvLoadingMsg != null) {
            mTvLoadingMsg.setText(msg);
        }
        if (!mLoadingDialog.isShowing() && !isFinishing()) {
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
        ToastUtils.show(this, msg);
    }

    @Override
    protected void onDestroy() {
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
