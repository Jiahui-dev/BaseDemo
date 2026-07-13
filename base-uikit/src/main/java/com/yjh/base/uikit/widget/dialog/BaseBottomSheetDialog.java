package com.yjh.base.uikit.widget.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * 通用底部弹窗基类
 * Created by jiahui on 2026/02/02
 */

public abstract class BaseBottomSheetDialog extends BottomSheetDialogFragment {

    protected View mRootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 加载子类提供的布局
        mRootView = inflater.inflate(getLayoutId(), container, false);
        initView(mRootView);
        return mRootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        // 设置透明背景，为了让圆角显示出来
        Dialog dialog = getDialog();
        if (dialog != null) {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackgroundResource(android.R.color.transparent);
                // 默认全展开
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        }
    }

    // 子类实现
    protected abstract int getLayoutId();
    protected abstract void initView(View root);
}
