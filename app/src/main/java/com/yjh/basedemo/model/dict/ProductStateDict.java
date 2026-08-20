package com.yjh.basedemo.model.dict;

import java.util.ArrayList;
import java.util.List;

/**
 * 资产状态系统字典
 */
public enum ProductStateDict {

    IN_USE("IN_USE", "使用中"),
    IDLE("IDLE", "闲置中"),
    DISPOSED("DISPOSED", "已变卖"),
    DAMAGED("DAMAGED", "已损坏"),
    LOST("LOST", "已丢失");

    private final String code;
    private final String title;

    ProductStateDict(String code, String title) {
        this.code = code;
        this.title = title;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    /**
     * 获取所有状态的中文名称列表（专门给 ListSelectDialog 弹窗做数据源）
     */
    public static List<String> getTitleList() {
        List<String> list = new ArrayList<>();
        for (ProductStateDict item : values()) {
            list.add(item.getTitle());
        }
        return list;
    }

    /**
     * 根据中文 Title 反查对应的 Code（数据库存储用）
     */
    public static String getCodeByTitle(String title) {
        for (ProductStateDict item : values()) {
            if (item.getTitle().equals(title)) {
                return item.getCode();
            }
        }
        return IN_USE.getCode(); // 默认兜底：使用中
    }

    /**
     * 根据 Code 查询对应的中文 Title（UI 回显用）
     */
    public static String getTitleByCode(String code) {
        if (code == null) return IN_USE.getTitle();
        for (ProductStateDict item : values()) {
            if (item.getCode().equalsIgnoreCase(code)) {
                return item.getTitle();
            }
        }
        return IN_USE.getTitle(); // 默认兜底
    }
}