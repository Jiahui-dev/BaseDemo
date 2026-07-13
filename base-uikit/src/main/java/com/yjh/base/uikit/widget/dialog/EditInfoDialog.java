package com.yjh.base.uikit.widget.dialog;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.yjh.base.uikit.R;
import com.yjh.base.uikit.widget.titleBar.TitleBar;

/**
 * Created by jiahui on 2026/04/06
 */

public class EditInfoDialog extends BaseBottomSheetDialog {

    private TitleBar titleBar;
    private EditText etContent;
    private TextView tvHint;
    private Button btnConfirm;

    private String mTitle = "修改信息";
    private String mContent = "";
    private String mHintText = "";
    private int mInputType = InputType.TYPE_CLASS_TEXT; // 默认文本类型

    private OnConfirmListener mListener;

    public interface OnConfirmListener {
        void onConfirm(String content);
    }

    public void setOnConfirmListener(OnConfirmListener listener) {
        this.mListener = listener;
    }

    /**
     * 静态工厂方法，方便创建
     * @param title 标题
     * @param content 初始内容 (比如旧昵称)
     */
    public static EditInfoDialog newInstance(String title, String content) {
        EditInfoDialog fragment = new EditInfoDialog();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("content", content);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * 设置输入类型 (例如 InputType.TYPE_TEXT_VARIATION_PASSWORD)
     */
    public void setInputType(int inputType) {
        this.mInputType = inputType;
    }

    /**
     * 设置下方小提示文字
     */
    public void setHintText(String hint) {
        this.mHintText = hint;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.uikit_dialog_edit_info;
    }

    @Override
    protected void initView(View root) {
        // 1. 获取参数
        if (getArguments() != null) {
            mTitle = getArguments().getString("title", "修改信息");
            mContent = getArguments().getString("content", "");
        }

        titleBar = root.findViewById(R.id.title_bar);
        etContent = root.findViewById(R.id.et_content);
        tvHint = root.findViewById(R.id.tv_hint);
        btnConfirm = root.findViewById(R.id.btn_confirm);

        // 2. 设置 TitleBar
        if (titleBar != null) {
            titleBar.setTitle(mTitle);
            titleBar.setOnBackListener(v -> dismiss());
        }

        // 3. 设置输入框
        etContent.setText(mContent);
        etContent.setInputType(mInputType);
        // 将光标移动到末尾
        if (!TextUtils.isEmpty(mContent)) {
            etContent.setSelection(mContent.length());
        }

        // 4. 设置提示文字
        if (TextUtils.isEmpty(mHintText)) {
            tvHint.setVisibility(View.GONE);
        } else {
            tvHint.setText(mHintText);
            tvHint.setVisibility(View.VISIBLE);
        }

        // 5. 确认按钮点击
        btnConfirm.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onConfirm(etContent.getText().toString().trim());
            }
            dismiss();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // 强制设置高度为屏幕的 3/4
        if (getDialog() != null) {
            View bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                int screenHeight = getResources().getDisplayMetrics().heightPixels;
                ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
                layoutParams.height = screenHeight * 3 / 4;
                bottomSheet.setLayoutParams(layoutParams);

                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setPeekHeight(screenHeight * 3 / 4);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        }
    }
}