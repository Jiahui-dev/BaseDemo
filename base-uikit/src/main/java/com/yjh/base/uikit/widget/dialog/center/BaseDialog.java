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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 去掉默认标题栏
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
            // 1. 设置背景透明 (为了让 xml 中的圆角生效)
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // 2. 设置弹窗宽度为屏幕宽度的 85%
            DisplayMetrics dm = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = (int) (dm.widthPixels * 0.85); // 宽度占比
            params.height = WindowManager.LayoutParams.WRAP_CONTENT; // 高度自适应
            window.setAttributes(params);
        }
    }

    protected abstract int getLayoutId();

    protected abstract void initView(View root);
}