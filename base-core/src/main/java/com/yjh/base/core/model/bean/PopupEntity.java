package com.yjh.base.core.model.bean;

import android.graphics.Color;

public class PopupEntity {
    public int id;
    public String text;
    public int iconRes; // 0 代表没有图标
    public int textColor = Color.parseColor("#0a0700");

    public PopupEntity(int id, String text) {
        this.id = id;
        this.text = text;
    }

    public PopupEntity(int id, String text, int iconRes) {
        this.id = id;
        this.text = text;
        this.iconRes = iconRes;
    }
}
