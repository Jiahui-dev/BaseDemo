package com.yjh.basedemo.activity;

import android.annotation.SuppressLint;
import com.yjh.base.uikit.activity.BaseActivity;
import com.yjh.basedemo.databinding.AcMainBinding;

public class MainActivity extends BaseActivity<AcMainBinding>{

    @SuppressLint("SetTextI18n")
    @Override
    protected void initView() {
        binding.tvContent.setText("主Activity");
    }

}
