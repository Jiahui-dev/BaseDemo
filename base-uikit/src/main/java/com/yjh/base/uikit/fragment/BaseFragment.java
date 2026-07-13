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
 * Created by jiahui on 2026/07/13
 */
public abstract class BaseFragment<VB extends ViewBinding> extends BaseCoreFragment<VB> {

    private static final String TAG="BaseFragment";

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
        ImmersionBar.with(this)
                .statusBarColor(getStatusBarColor())
                .statusBarDarkFont(isStatusBarDarkFont())
                .titleBar(getTitleBarView())
                .keyboardEnable(true)
                .init();
    }

    protected View getTitleBarView() { return null; }

    protected int getStatusBarColor() { return R.color.uikit_grey_backGround; }

    protected boolean isStatusBarDarkFont() { return true; }

    @Override
    public void showLoading(String msg) {
        if (mActivity == null || mActivity.isFinishing()) return;

        if (mLoadingDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
            View view = LayoutInflater.from(mActivity).inflate(R.layout.uikit_view_dialog_loading, null);
            mTvLoadingMsg = view.findViewById(R.id.tv_dialogMsg);
            builder.setView(view);
            builder.setCancelable(false);
            mLoadingDialog = builder.create();
            Objects.requireNonNull(mLoadingDialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        }
        if (mTvLoadingMsg != null) mTvLoadingMsg.setText(msg);
        if (!mLoadingDialog.isShowing()) mLoadingDialog.show();
    }

    @Override
    public void hideLoading() {
        if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
            mLoadingDialog.dismiss();
        }
    }

    @Override
    public void showError(String msg) {
        ToastUtils.show(mActivity, msg);
    }

    @Override
    public void onDestroy() {
        if (mLoadingDialog != null) {
            mLoadingDialog.dismiss();
            mLoadingDialog = null;
        }
        super.onDestroy();
    }

}
