package com.example.model;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PaymentMethod {
    private String id;
    private int discount;
    private BigDecimal limit;

    @JsonProperty("id") public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @JsonProperty("discount") public int getDiscount() { return discount; }
    public void setDiscount(int discount) { this.discount = discount; }

    @JsonProperty("limit") public BigDecimal getLimit() { return limit; }
    public void setLimit(BigDecimal limit) { this.limit = limit; }
}