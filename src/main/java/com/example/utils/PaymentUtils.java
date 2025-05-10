package com.example.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import com.example.model.PaymentMethod;

/**
 * Utility class for payment-related calculations and operations.
 */
public class PaymentUtils {

    private static final int CALCULATION_SCALE = 8;
    private static final int MONEY_SCALE = 2;

    private PaymentUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Apply discount to an amount based on percentage.
     * 
     * @param amount The original amount
     * @param discountPercentage The discount percentage (0-100)
     * @return The discounted amount
     */
    public static BigDecimal applyDiscount(BigDecimal amount, int discountPercentage) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (discountPercentage < 0 || discountPercentage > 100) {
            throw new IllegalArgumentException("Discount percentage must be between 0 and 100");
        }

        BigDecimal discountFactor = BigDecimal.ONE.subtract(
            BigDecimal.valueOf(discountPercentage).divide(BigDecimal.valueOf(100), CALCULATION_SCALE, RoundingMode.HALF_UP)
        );
        return amount.multiply(discountFactor).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Reverse a discount calculation to find the original amount.
     * 
     * @param discountedAmount The amount after discount
     * @param discountPercentage The discount percentage (0-100)
     * @return The original amount before discount
     */
    public static BigDecimal reverseDiscount(BigDecimal discountedAmount, int discountPercentage) {
        if (discountedAmount == null) {
            throw new IllegalArgumentException("Discounted amount cannot be null");
        }
        if (discountPercentage < 0 || discountPercentage >= 100) {
            throw new IllegalArgumentException("Discount percentage must be between 0 and 100");
        }

        BigDecimal discountFactor = BigDecimal.ONE.subtract(
            BigDecimal.valueOf(discountPercentage).divide(BigDecimal.valueOf(100), CALCULATION_SCALE, RoundingMode.HALF_UP)
        );
        return discountedAmount.divide(discountFactor, CALCULATION_SCALE, RoundingMode.HALF_UP)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Apply a payment to the spent and limits tracking.
     * 
     * @param methodId The payment method ID
     * @param amount The amount to apply
     * @param spent The map tracking spent amounts
     * @param limits The map tracking remaining limits
     */
    public static void applyPayment(String methodId, BigDecimal amount, 
                                  Map<String, BigDecimal> spent, 
                                  Map<String, BigDecimal> limits) {
        if (methodId == null || amount == null || spent == null || limits == null) {
            throw new IllegalArgumentException("None of the parameters can be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        if (!limits.containsKey(methodId)) {
            throw new IllegalArgumentException("Payment method not found: " + methodId);
        }

        limits.put(methodId, limits.get(methodId).subtract(amount));
        spent.merge(methodId, amount, BigDecimal::add);
        
        if (limits.get(methodId).compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Limit exceeded for " + methodId + ": " + limits.get(methodId));
        }
    }

    /**
     * Initialize the limits map from payment methods.
     * 
     * @param paymentMethods The map of payment methods
     * @return A new map with payment method IDs to their limits
     */
    public static Map<String, BigDecimal> initializeLimits(Map<String, PaymentMethod> paymentMethods) {
        if (paymentMethods == null) {
            throw new IllegalArgumentException("Payment methods map cannot be null");
        }

        Map<String, BigDecimal> limits = new HashMap<>();
        for (PaymentMethod method : paymentMethods.values()) {
            limits.put(method.getId(), method.getLimit());
        }
        return limits;
    }

    /**
     * Initialize the spent map with zero values.
     * 
     * @param paymentMethods The map of payment methods
     * @return A new map with payment method IDs to zero values
     */
    public static Map<String, BigDecimal> initializeSpent(Map<String, PaymentMethod> paymentMethods) {
        if (paymentMethods == null) {
            throw new IllegalArgumentException("Payment methods map cannot be null");
        }

        Map<String, BigDecimal> spent = new HashMap<>();
        for (String methodId : paymentMethods.keySet()) {
            spent.put(methodId, BigDecimal.ZERO);
        }
        return spent;
    }

    /**
     * Calculate total amount spent across all payment methods.
     * 
     * @param spent The map of spent amounts by payment method
     * @return The total spent amount
     */
    public static BigDecimal calculateTotalSpent(Map<String, BigDecimal> spent) {
        if (spent == null) {
            throw new IllegalArgumentException("Spent map cannot be null");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal value : spent.values()) {
            total = total.add(value);
        }
        return total;
    }
}