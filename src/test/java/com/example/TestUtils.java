package com.example;

import com.example.model.Order;
import com.example.model.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;

public class TestUtils {
    public static Order order(String id, String value, List<String> promos) {
        Order o = new Order();
        o.setId(id);
        o.setValue(new BigDecimal(value));
        o.setPromotions(promos);
        return o;
    }

    public static PaymentMethod method(String id, int discount, String limit) {
        PaymentMethod m = new PaymentMethod();
        m.setId(id);
        m.setDiscount(discount);
        m.setLimit(new BigDecimal(limit));
        return m;
    }
}