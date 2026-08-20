package com.yjh.base.uikit.widget.dialog.center;

import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.yjh.base.uikit.R;

/**
 * 通用确认/取消弹窗
 * Created by jiahui on 2026/02/02
 */
public class CommonDialog extends BaseDialog {

    private String title;
    private String content;
    private String cancelText = "取消";
    private String confirmText = "确定";

    // 监听回调
    private OnClickListener onConfirmListener;
    private OnClickListener onCancelListener;

    public interface OnClickListener {
        void onClick(CommonDialog dialog);
    }

    public static CommonDialog newInstance() {
        return new CommonDialog();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.uikit_dialog_tip;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        // 释放回调引用，切断 Activity 持有链
        onConfirmListener = null;
        onCancelListener = null;
    }

    @Override
    protected void initView(View root) {
        TextView tvTitle = root.findViewById(R.id.tv_dialog_title);
        TextView tvContent = root.findViewById(R.id.tv_dialog_content);
        TextView btnCancel = root.findViewById(R.id.btn_dialog_cancel);
        TextView btnConfirm = root.findViewById(R.id.btn_dialog_confirm);

        // 1. 设置文本
        tvTitle.setText(TextUtils.isEmpty(title) ? "提示" : title);
        tvContent.setText(TextUtils.isEmpty(content) ? "" : content);
        btnCancel.setText(cancelText);
        btnConfirm.setText(confirmText);

        tvTitle.setVisibility(TextUtils.isEmpty(title) ? View.GONE : View.VISIBLE);

        // 2. 设置点击事件
        btnCancel.setOnClickListener(v -> {
            if (onCancelListener != null) {
                onCancelListener.onClick(this);
            } else {
                dismiss(); // 默认行为是关闭
            }
        });

        btnConfirm.setOnClickListener(v -> {
            if (onConfirmListener != null) {
                onConfirmListener.onClick(this);
            }
            dismiss(); // 点击确定通常也关闭
        });
    }

    public CommonDialog setTitle(String title) {
        this.title = title;
        return this;
    }

    public CommonDialog setContent(String content) {
        this.content = content;
        return this;
    }

    public CommonDialog setButtons(String confirmText, String cancelText) {
        this.confirmText = confirmText;
        this.cancelText = cancelText;
        return this;
    }

    public CommonDialog setOnConfirmListener(OnClickListener listener) {
        this.onConfirmListener = listener;
        return this;
    }

    public CommonDialog setOnCancelListener(OnClickListener listener) {
        this.onCancelListener = listener;
        return this;
    }

}