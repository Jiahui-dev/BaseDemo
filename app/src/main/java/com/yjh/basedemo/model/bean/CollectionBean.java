package com.yjh.basedemo.model.bean;

public class CollectionBean {

    public CollectionBean(String name) {
        this.name = name;
    }

    private String name;
    private String price;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }
}
