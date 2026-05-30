package com.shawarma_moods.delivery.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UberManifestItem {

    @JsonProperty("name")
    private String name;

    @JsonProperty("quantity")
    private Integer quantity;

    @JsonProperty("size")
    private String size;

    @JsonProperty("price")
    private Integer price;

    public UberManifestItem(String name, Integer quantity, String size, Integer price) {
        this.name = name;
        this.quantity = quantity;
        this.size = size;
        this.price = price;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }
}