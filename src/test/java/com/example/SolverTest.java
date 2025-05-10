package com.example;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.example.model.Order;
import com.example.model.PaymentMethod;

public class SolverTest {

    @Test
    public void exampleTest() {
        List<Order> orders = List.of(
            // TestUtils.order("ORDER1", "100.00", List.of("mZysk")),
            // TestUtils.order("ORDER2", "200.00", List.of("BosBankrut")),
            // TestUtils.order("ORDER3", "150.00", List.of("mZysk", "BosBankrut")),
            // TestUtils.order("ORDER4", "50.00", null)
        );
        List<PaymentMethod> methods = List.of(
            // TestUtils.method("PUNKTY", 15, "100.00"),
            // TestUtils.method("mZysk", 10, "180.00"),
            // TestUtils.method("BosBankrut", 5, "200.00")
        );
        Solver solver = new Solver(orders, methods);
        Map<String, BigDecimal> result = solver.solve();
        // Assertions based on expected example
        // assertEquals(new BigDecimal("165.00"), result.get("mZysk"));
        // assertEquals(new BigDecimal("190.00"), result.get("BosBankrut"));
        // assertEquals(new BigDecimal("100.00"), result.get("PUNKTY"));
    }
}