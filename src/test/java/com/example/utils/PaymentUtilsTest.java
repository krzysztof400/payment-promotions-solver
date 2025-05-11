package com.example.utils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.model.PaymentMethod;

public class PaymentUtilsTest {

    private Map<String, PaymentMethod> paymentMethods;

    @BeforeEach
    public void setup() {
        paymentMethods = new HashMap<>();

        PaymentMethod card = new PaymentMethod();
        card.setId("card");
        card.setLimit(new BigDecimal("100.00"));
        card.setDiscount(10);

        PaymentMethod cash = new PaymentMethod();
        cash.setId("cash");
        cash.setLimit(new BigDecimal("50.00"));
        cash.setDiscount(5);

        paymentMethods.put("card", card);
        paymentMethods.put("cash", cash);
    }

    @Test
    public void testApplyDiscount() {
        BigDecimal result = PaymentUtils.applyDiscount(new BigDecimal("100"), 20);
        assertEquals(new BigDecimal("80.00"), result);
    }

    @Test
    public void testApplyDiscount_zeroPercent() {
        BigDecimal result = PaymentUtils.applyDiscount(new BigDecimal("100"), 0);
        assertEquals(new BigDecimal("100.00"), result);
    }

    @Test
    public void testReverseDiscount() {
        BigDecimal result = PaymentUtils.reverseDiscount(new BigDecimal("80.00"), 20);
        assertEquals(new BigDecimal("100.00"), result);
    }

    @Test
    public void testApplyPayment_normalFlow() {
        Map<String, BigDecimal> spent = PaymentUtils.initializeSpent(paymentMethods);
        Map<String, BigDecimal> limits = PaymentUtils.initializeLimits(paymentMethods);

        PaymentUtils.applyPayment("card", new BigDecimal("30.00"), spent, limits);

        assertEquals(new BigDecimal("30.00"), spent.get("card"));
        assertEquals(new BigDecimal("70.00"), limits.get("card"));
    }

    @Test
    public void testApplyPayment_exceedLimit_throwsException() {
        Map<String, BigDecimal> spent = PaymentUtils.initializeSpent(paymentMethods);
        Map<String, BigDecimal> limits = PaymentUtils.initializeLimits(paymentMethods);

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            PaymentUtils.applyPayment("cash", new BigDecimal("60.00"), spent, limits);
        });

        assertTrue(exception.getMessage().contains("Limit exceeded"));
    }

    @Test
    public void testInitializeLimits() {
        Map<String, BigDecimal> limits = PaymentUtils.initializeLimits(paymentMethods);
        assertEquals(new BigDecimal("100.00"), limits.get("card"));
        assertEquals(new BigDecimal("50.00"), limits.get("cash"));
    }

    @Test
    public void testInitializeSpent() {
        Map<String, BigDecimal> spent = PaymentUtils.initializeSpent(paymentMethods);
        assertEquals(BigDecimal.ZERO, spent.get("card"));
        assertEquals(BigDecimal.ZERO, spent.get("cash"));
    }

    @Test
    public void testCalculateTotalSpent() {
        Map<String, BigDecimal> spent = new HashMap<>();
        spent.put("card", new BigDecimal("25.00"));
        spent.put("cash", new BigDecimal("15.00"));

        BigDecimal total = PaymentUtils.calculateTotalSpent(spent);
        assertEquals(new BigDecimal("40.00"), total);
    }

    @Test
    public void testApplyDiscount_invalidPercentage_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            PaymentUtils.applyDiscount(new BigDecimal("100"), -1);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PaymentUtils.applyDiscount(new BigDecimal("100"), 101);
        });
    }

    @Test
    public void testReverseDiscount_invalidPercentage_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            PaymentUtils.reverseDiscount(new BigDecimal("100"), -1);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            PaymentUtils.reverseDiscount(new BigDecimal("100"), 100);
        });
    }
}
