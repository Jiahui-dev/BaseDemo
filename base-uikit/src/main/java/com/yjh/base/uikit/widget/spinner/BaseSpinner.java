package com.yjh.base.uikit.widget.spinner;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import com.yjh.base.uikit.R;

public abstract class BaseSpinner {
    protected Context context;
    protected Dialog dialog;
    protected OnSpinnerSelectedListener listener;

    public BaseSpinner(Context context) {
        this.context = context;
        initDialog();
    }

    private void initDialog() {
        dialog = new Dialog(context, R.style.uikit_BottomDialogStyle);
        View view = LayoutInflater.from(context).inflate(getLayoutId(), null);
        dialog.setContentView(view);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setGravity(Gravity.BOTTOM);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        initView(view);
    }

    protected abstract int getLayoutId();

    protected abstract void initView(View rootView);

    public void setOnSelectedListener(OnSpinnerSelectedListener listener) {
        this.listener = listener;
    }

    public void show() {
        dialog.show();
    }

    public void dismiss() {
        dialog.dismiss();
    }
}