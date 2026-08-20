package com.yjh.basedemo.model.bean;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.yjh.basedemo.model.dict.ProductStateDict;

import java.io.Serializable;


@Entity(tableName = "products")
public class ProductBean implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "price")
    private double price;

    @ColumnInfo(name = "purchase_date")
    private String purchaseDate;

    @ColumnInfo(name = "category_id")
    private int categoryId;

    @ColumnInfo(name = "icon_code")
    private String iconCode;

    @ColumnInfo(name = "state_code")
    private String stateCode; // 存 "IN_USE", "IDLE" 等

    public ProductBean() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(String purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getIconCode() {
        return iconCode;
    }

    public void setIconCode(String iconCode) {
        this.iconCode = iconCode;
    }

    public String getStateCode() {
        return stateCode;
    }

    public void setStateCode(String stateCode) {
        this.stateCode = stateCode;
    }

    /**
     * 获取展示给用户看的状态中文名称（不入库，仅 UI 显示）
     */
    @Ignore
    public String getStateTitle() {
        return ProductStateDict.getTitleByCode(this.stateCode);
    }
}
