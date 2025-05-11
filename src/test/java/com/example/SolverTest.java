package com.example;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.model.Order;
import com.example.model.PaymentMethod;

class SolverTest {

    private PaymentMethod card20;
    private PaymentMethod card10;
    private PaymentMethod points50;
    private List<PaymentMethod> methods;

    @BeforeEach
    void initMethods() {
        // Card with 20% discount, limit 100
        card20 = new PaymentMethod();
        card20.setId("CARD20");
        card20.setDiscount(20);
        card20.setLimit(new BigDecimal("100.00"));

        // Card with 10% discount, limit 100
        card10 = new PaymentMethod();
        card10.setId("CARD10");
        card10.setDiscount(10);
        card10.setLimit(new BigDecimal("100.00"));

        // Loyalty points with 50% discount, limit 100
        points50 = new PaymentMethod();
        points50.setId(Solver.POINTS_METHOD);
        points50.setDiscount(50);
        points50.setLimit(new BigDecimal("100.00"));

        methods = Arrays.asList(card20, card10, points50);
    }

    private Order makeOrder(String id, String value, String... promos) {
        Order o = new Order();
        o.setId(id);
        o.setValue(new BigDecimal(value));
        o.setPromotions(Arrays.asList(promos));
        return o;
    }

    @Test
    void testAllCardPayment_whenPointsAreWorse() {
        // points50 discount=5 instead of 50, so card20 is best
        points50.setDiscount(5);
        List<Order> orders = Collections.singletonList(
            makeOrder("O1", "100.00", "CARD20", Solver.POINTS_METHOD)
        );

        Solver solver = new Solver(orders, methods);
        Map<String, BigDecimal> spent = solver.solve();

        // Expect CARD20=80, others zero
        assertEquals(new BigDecimal("80.00"), spent.get("CARD20"));
        assertEquals(BigDecimal.ZERO, spent.get("CARD10"));
        assertEquals(BigDecimal.ZERO, spent.get(Solver.POINTS_METHOD));
    }

    @Test
    void testFullPoints_whenPointsAreBest() {
        // 50% off via points vs 20% via card
        List<Order> orders = Collections.singletonList(
            makeOrder("O1", "100.00", "CARD20", Solver.POINTS_METHOD)
        );

        Solver solver = new Solver(orders, methods);
        Map<String, BigDecimal> spent = solver.solve();

        // Should pick full points: 100 * .5 = 50
        assertEquals(BigDecimal.ZERO, spent.get("CARD20"));
        assertEquals(BigDecimal.ZERO, spent.get("CARD10"));
        assertEquals(new BigDecimal("50.00"), spent.get(Solver.POINTS_METHOD));
    }

    @Test
    void testPartialPointsPlusCard_whenPointsLimitLow() {
        // Lower points limit so full‐points impossible
        points50.setLimit(new BigDecimal("5.00"));
        List<Order> orders = Collections.singletonList(
            makeOrder("O1", "100.00", "CARD20", Solver.POINTS_METHOD)
        );

        Solver solver = new Solver(orders, methods);
        Map<String, BigDecimal> spent = solver.solve();

        // Full points fail; partial points + card yields same as full card 20%: 80
        assertEquals(new BigDecimal("80.00"), spent.get("CARD20"));
        assertEquals(BigDecimal.ZERO, spent.get("CARD10"));
        assertEquals(BigDecimal.ZERO, spent.get(Solver.POINTS_METHOD));
    }
}
