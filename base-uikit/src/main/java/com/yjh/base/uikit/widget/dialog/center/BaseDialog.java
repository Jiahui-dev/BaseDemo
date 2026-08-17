package com.yjh.base.uikit.widget.dialog.center;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

/**
 * 通用中心弹窗基类
 * Created by jiahui on 2026/02/02
 */
public abstract class BaseDialog extends DialogFragment {

    protected View mRootView;
    private boolean isDismissing = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getDialog() != null) {
            getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        }

        mRootView = inflater.inflate(getLayoutId(), container, false);
        initView(mRootView);
        return mRootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog() != null ? getDialog().getWindow() : null;
        if (window != null) {
            // 1. 设置背景透明
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setWindowAnimations(0);
            // 2. 设置弹窗宽度为屏幕宽度的 85%
            DisplayMetrics dm = new DisplayMetrics();
            if (getActivity() != null) {
                getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
                WindowManager.LayoutParams params = window.getAttributes();
                params.width = (int) (dm.widthPixels * 0.85);
                params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                window.setAttributes(params);
            }
        }

        // 3. 执行平滑进场动画
        startEnterAnimation();
    }

    /**
     * View 级进场动画（从 40% 缩放放大到 100%，带弹性过度）
     */
    private void startEnterAnimation() {
        if (mRootView == null) return;

        // 如果想从0放大并带淡入，可以这样写（更完整）
        mRootView.setScaleX(0.5f);  // 从50%开始
        mRootView.setScaleY(0.5f);
        mRootView.setAlpha(0f);

        mRootView.animate()
                .scaleX(1.0f)   // 恢复到100%
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(100)
                .start();
    }

    @Override
    public void dismiss() {
        if (isDismissing) return;
        isDismissing = true;

        if (mRootView != null) {
            mRootView.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .alpha(0f)
                    .setDuration(180)
                    .withEndAction(BaseDialog.super::dismiss)
                    .start();
        } else {
            super.dismiss();
        }
    }

    protected abstract int getLayoutId();

    protected abstract void initView(View root);
}