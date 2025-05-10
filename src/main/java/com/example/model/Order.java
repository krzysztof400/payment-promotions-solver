package com.example.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

public class Order {
    private String id;
    private BigDecimal value;
    private List<String> promotions;

    // Getters and setters
    @JsonProperty("id")
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @JsonProperty("value")
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }

    @JsonProperty("promotions")
    public List<String> getPromotions() { return promotions; }
    public void setPromotions(List<String> promotions) { this.promotions = promotions; }
}