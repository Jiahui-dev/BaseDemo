package com.yjh.base.uikit.widget.dialog;

import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.yjh.base.uikit.R;

/**
 * 通用输入弹窗
 * Created by jiahui on 2026/04/06
 */
public class InputDialog extends BaseDialog {

    private String title;
    private String hint;
    private String defaultText; // 默认回显的文字
    private int inputType = InputType.TYPE_CLASS_TEXT; // 默认是普通文本键盘
    private String cancelText = "取消";
    private String confirmText = "确定";

    private OnConfirmClickListener onConfirmListener;
    private OnCancelClickListener onCancelListener;

    public interface OnConfirmClickListener {
        void onClick(InputDialog dialog, String inputText);
    }

    public interface OnCancelClickListener {
        void onClick(InputDialog dialog);
    }

    public static InputDialog newInstance() {
        return new InputDialog();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.uikit_dialog_input;
    }

    @Override
    protected void initView(View root) {
        TextView tvTitle = root.findViewById(R.id.tv_dialog_title);
        EditText etInput = root.findViewById(R.id.et_dialog_input);
        TextView btnCancel = root.findViewById(R.id.btn_dialog_cancel);
        TextView btnConfirm = root.findViewById(R.id.btn_dialog_confirm);

        tvTitle.setText(TextUtils.isEmpty(title) ? "请输入" : title);
        tvTitle.setVisibility(TextUtils.isEmpty(title) ? View.GONE : View.VISIBLE);

        btnCancel.setText(cancelText);
        btnConfirm.setText(confirmText);

        etInput.setHint(TextUtils.isEmpty(hint) ? "" : hint);
        etInput.setInputType(inputType);

        // 如果有默认文本，设置上去并把光标移到最后
        if (!TextUtils.isEmpty(defaultText)) {
            etInput.setText(defaultText);
            etInput.setSelection(defaultText.length());
        }

        // 2. 按钮点击事件
        btnCancel.setOnClickListener(v -> {
            if (onCancelListener != null) {
                onCancelListener.onClick(this);
            } else {
                dismiss();
            }
        });

        btnConfirm.setOnClickListener(v -> {
            if (onConfirmListener != null) {
                String inputText = etInput.getText().toString().trim();
                onConfirmListener.onClick(this, inputText);
            }
            dismiss();
        });
    }

    public InputDialog setTitle(String title) {
        this.title = title;
        return this;
    }

    public InputDialog setHint(String hint) {
        this.hint = hint;
        return this;
    }

    public InputDialog setDefaultText(String defaultText) {
        this.defaultText = defaultText;
        return this;
    }

    /**
     * 设置键盘输入类型，例如只允许输入数字：
     * InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
     */
    public InputDialog setInputType(int inputType) {
        this.inputType = inputType;
        return this;
    }

    public InputDialog setButtons(String cancelText, String confirmText) {
        this.cancelText = cancelText;
        this.confirmText = confirmText;
        return this;
    }

    public InputDialog setOnConfirmListener(OnConfirmClickListener listener) {
        this.onConfirmListener = listener;
        return this;
    }

    public InputDialog setOnCancelListener(OnCancelClickListener listener) {
        this.onCancelListener = listener;
        return this;
    }
}