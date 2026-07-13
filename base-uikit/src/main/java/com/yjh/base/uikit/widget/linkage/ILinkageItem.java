package com.yjh.base.uikit.widget.linkage;

public interface ILinkageItem {
    // 标记是否是标题（分组头）
    boolean isHeader();

    // 获取所属的分类名称（用于和左侧对应）
    String getHeaderName();
}
