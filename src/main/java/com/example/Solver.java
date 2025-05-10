package com.example;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.model.Order;
import com.example.model.PaymentMethod;
import com.example.utils.PaymentUtils;

/**
 * Solver assigns payment methods to orders based on the following rules:
 * 1. Each order has a subset of eligible payment methods (promotions).
 * 2. If the entire order is paid with a bank card, apply the card's discount percentage.
 * 3. If at least 10% of the order value is paid with loyalty points, an additional 10% discount
 *    is applied to the entire order.
 * 4. If the entire order is paid with loyalty points, apply the discount defined for "PUNKTY"
 *    instead of the 10% discount for partial payment.
 * 
 * The solver aims to minimize the total amount spent across all orders.
 */
public class Solver {
    private final List<Order> orders;
    private final Map<String, PaymentMethod> paymentMethods;
    public static final String POINTS_METHOD = "PUNKTY";
    public static final BigDecimal PARTIAL_POINTS_MINIMUM = new BigDecimal("0.10");
    public static final int PARTIAL_POINTS_DISCOUNT = 10;

    /**
     * Constructor that initializes the solver with orders and payment methods.
     * 
     * @param orders The list of orders to process
     * @param methods The available payment methods
     */
    public Solver(List<Order> orders, List<PaymentMethod> methods) {
        this.orders = new ArrayList<>(orders);
        this.paymentMethods = new HashMap<>();
        for (PaymentMethod m : methods) {
            PaymentMethod copy = new PaymentMethod();
            copy.setId(m.getId());
            copy.setDiscount(m.getDiscount());
            copy.setLimit(m.getLimit());
            this.paymentMethods.put(copy.getId(), copy);
        }
    }

    /**
     * Solves the payment optimization problem and returns the optimal payment allocation.
     * 
     * @return A map of payment method IDs to amounts spent
     */
    public Map<String, BigDecimal> solve() {
        // Try all possible payment strategies and select the best one
        Map<String, BigDecimal> result1 = solveWithCardPriority();
        BigDecimal total1 = PaymentUtils.calculateTotalSpent(result1);

        Map<String, BigDecimal> result2 = solveWithPointsPriority();
        BigDecimal total2 = PaymentUtils.calculateTotalSpent(result2);

        Map<String, BigDecimal> result3 = solveWithMixedPayments();
        BigDecimal total3 = PaymentUtils.calculateTotalSpent(result3);

        // Find the lowest cost strategy
        if (total1.compareTo(total2) <= 0 && total1.compareTo(total3) <= 0) {
            return result1;
        } else if (total2.compareTo(total1) <= 0 && total2.compareTo(total3) <= 0) {
            return result2;
        } else {
            return result3;
        }
    }

    /**
     * Strategy that prioritizes full card payments.
     * 
     * @return Spent amounts by payment method
     */
    private Map<String, BigDecimal> solveWithCardPriority() {
        Map<String, BigDecimal> remainingLimits = PaymentUtils.initializeLimits(paymentMethods);
        Map<String, BigDecimal> spent = PaymentUtils.initializeSpent(paymentMethods);
        
        // Sort orders by value (descending)
        List<Order> sortedOrders = new ArrayList<>(orders);
        sortedOrders.sort(Comparator.comparing(Order::getValue).reversed());
        
        // Sort payment methods by discount (descending)
        List<PaymentMethod> sortedCards = new ArrayList<>();
        for (PaymentMethod method : paymentMethods.values()) {
            if (!POINTS_METHOD.equals(method.getId())) {
                sortedCards.add(method);
            }
        }
        sortedCards.sort(Comparator.comparingInt(PaymentMethod::getDiscount).reversed());
        
        // Process orders
        List<Order> remainingOrders = new ArrayList<>();
        for (Order order : sortedOrders) {
            boolean orderProcessed = false;
            
            // Try to pay with best card first
            for (PaymentMethod card : sortedCards) {
                List<String> promotions = order.getPromotions();
                if (promotions != null && promotions.contains(card.getId())) {
                    BigDecimal orderValue = order.getValue();
                    BigDecimal discountedValue = PaymentUtils.applyDiscount(orderValue, card.getDiscount());
                    
                    // Check if we have enough limit
                    if (remainingLimits.get(card.getId()).compareTo(discountedValue) >= 0) {
                        // Pay with card
                        PaymentUtils.applyPayment(card.getId(), discountedValue, spent, remainingLimits);
                        orderProcessed = true;
                        break;
                    }
                }
            }
            
            // If no card payment was possible, try points payment
            if (!orderProcessed) {
                PaymentMethod points = paymentMethods.get(POINTS_METHOD);
                BigDecimal orderValue = order.getValue();
                BigDecimal discountedValue = PaymentUtils.applyDiscount(orderValue, points.getDiscount());
                
                if (remainingLimits.get(POINTS_METHOD).compareTo(discountedValue) >= 0) {
                    PaymentUtils.applyPayment(POINTS_METHOD, discountedValue, spent, remainingLimits);
                    orderProcessed = true;
                }
            }
            
            // If still not processed, add to remaining orders
            if (!orderProcessed) {
                remainingOrders.add(order);
            }
        }
        
        // Process remaining orders with mixed payments if possible
        for (Order order : remainingOrders) {
            processRemainingOrder(order, spent, remainingLimits);
        }
        
        return spent;
    }

    /**
     * Strategy that prioritizes full points payments.
     * 
     * @return Spent amounts by payment method
     */
    private Map<String, BigDecimal> solveWithPointsPriority() {
        Map<String, BigDecimal> remainingLimits = PaymentUtils.initializeLimits(paymentMethods);
        Map<String, BigDecimal> spent = PaymentUtils.initializeSpent(paymentMethods);
        
        // Sort orders by value (descending)
        List<Order> sortedOrders = new ArrayList<>(orders);
        sortedOrders.sort(Comparator.comparing(Order::getValue).reversed());
        
        PaymentMethod points = paymentMethods.get(POINTS_METHOD);
        
        // Process orders
        List<Order> remainingOrders = new ArrayList<>();
        for (Order order : sortedOrders) {
            boolean orderProcessed = false;
            BigDecimal orderValue = order.getValue();
            
            // Try full points payment first
            BigDecimal discountedValue = PaymentUtils.applyDiscount(orderValue, points.getDiscount());
            if (remainingLimits.get(POINTS_METHOD).compareTo(discountedValue) >= 0) {
                PaymentUtils.applyPayment(POINTS_METHOD, discountedValue, spent, remainingLimits);
                orderProcessed = true;
            }
            
            // If not processed with points, try best card
            if (!orderProcessed) {
                for (PaymentMethod card : paymentMethods.values()) {
                    if (POINTS_METHOD.equals(card.getId())) continue;
                    
                    List<String> promotions = order.getPromotions();
                    if (promotions != null && promotions.contains(card.getId())) {
                        discountedValue = PaymentUtils.applyDiscount(orderValue, card.getDiscount());
                        
                        if (remainingLimits.get(card.getId()).compareTo(discountedValue) >= 0) {
                            PaymentUtils.applyPayment(card.getId(), discountedValue, spent, remainingLimits);
                            orderProcessed = true;
                            break;
                        }
                    }
                }
            }
            
            // If still not processed, add to remaining orders
            if (!orderProcessed) {
                remainingOrders.add(order);
            }
        }
        
        // Process remaining orders with mixed payments if possible
        for (Order order : remainingOrders) {
            processRemainingOrder(order, spent, remainingLimits);
        }
        
        return spent;
    }

    /**
     * Strategy that prioritizes partial points payments for additional 10% discount.
     * 
     * @return Spent amounts by payment method
     */
    private Map<String, BigDecimal> solveWithMixedPayments() {
        Map<String, BigDecimal> remainingLimits = PaymentUtils.initializeLimits(paymentMethods);
        Map<String, BigDecimal> spent = PaymentUtils.initializeSpent(paymentMethods);
        
        // Sort orders by value (descending)
        List<Order> sortedOrders = new ArrayList<>(orders);
        sortedOrders.sort(Comparator.comparing(Order::getValue).reversed());
        
        List<Order> remainingOrders = new ArrayList<>();
        
        // First pass: try partial points payment + card for additional 10% discount
        for (Order order : sortedOrders) {
            boolean orderProcessed = false;
            BigDecimal orderValue = order.getValue();
            BigDecimal pointsNeeded = orderValue.multiply(PARTIAL_POINTS_MINIMUM);
            
            // Check if we have enough points for partial payment
            if (remainingLimits.get(POINTS_METHOD).compareTo(pointsNeeded) >= 0) {
                // Find best eligible card
                PaymentMethod bestCard = findBestEligibleCard(order);
                
                if (bestCard != null) {
                    // Calculate costs with partial payment + extra discount
                    BigDecimal discountedValue = PaymentUtils.applyDiscount(orderValue, PARTIAL_POINTS_DISCOUNT);
                    BigDecimal cardPortion = discountedValue.subtract(pointsNeeded);
                    
                    if (remainingLimits.get(bestCard.getId()).compareTo(cardPortion) >= 0) {
                        // Apply partial points payment
                        PaymentUtils.applyPayment(POINTS_METHOD, pointsNeeded, spent, remainingLimits);
                        // Apply card payment for remaining amount
                        PaymentUtils.applyPayment(bestCard.getId(), cardPortion, spent, remainingLimits);
                        orderProcessed = true;
                    }
                }
            }
            
            if (!orderProcessed) {
                remainingOrders.add(order);
            }
        }
        
        // Second pass for remaining orders: try full payment methods
        List<Order> finalRemainingOrders = new ArrayList<>();
        for (Order order : remainingOrders) {
            boolean orderProcessed = processFullPaymentOrder(order, spent, remainingLimits);
            
            if (!orderProcessed) {
                finalRemainingOrders.add(order);
            }
        }
        
        // Final pass: process any remaining orders with best available method
        for (Order order : finalRemainingOrders) {
            processRemainingOrder(order, spent, remainingLimits);
        }
        
        return spent;
    }

    /**
     * Attempts to process an order with full payment using any eligible method.
     * 
     * @param order The order to process
     * @param spent The map tracking spent amounts
     * @param remainingLimits The map tracking remaining limits
     * @return True if the order was processed successfully
     */
    private boolean processFullPaymentOrder(Order order, Map<String, BigDecimal> spent, Map<String, BigDecimal> remainingLimits) {
        BigDecimal orderValue = order.getValue();
        
        // Try points first if available
        PaymentMethod points = paymentMethods.get(POINTS_METHOD);
        BigDecimal pointsDiscountedValue = PaymentUtils.applyDiscount(orderValue, points.getDiscount());
        
        if (remainingLimits.get(POINTS_METHOD).compareTo(pointsDiscountedValue) >= 0) {
            PaymentUtils.applyPayment(POINTS_METHOD, pointsDiscountedValue, spent, remainingLimits);
            return true;
        }
        
        // Try cards sorted by discount
        List<PaymentMethod> sortedCards = new ArrayList<>();
        for (PaymentMethod method : paymentMethods.values()) {
            if (!POINTS_METHOD.equals(method.getId())) {
                List<String> promotions = order.getPromotions();
                if (promotions != null && promotions.contains(method.getId())) {
                    sortedCards.add(method);
                }
            }
        }
        sortedCards.sort(Comparator.comparingInt(PaymentMethod::getDiscount).reversed());
        
        for (PaymentMethod card : sortedCards) {
            BigDecimal cardDiscountedValue = PaymentUtils.applyDiscount(orderValue, card.getDiscount());
            
            if (remainingLimits.get(card.getId()).compareTo(cardDiscountedValue) >= 0) {
                PaymentUtils.applyPayment(card.getId(), cardDiscountedValue, spent, remainingLimits);
                return true;
            }
        }
        
        return false;
    }

    /**
     * Process remaining orders with best available method.
     * When limits are insufficient, use whatever is available.
     * 
     * @param order The order to process
     * @param spent The map tracking spent amounts
     * @param remainingLimits The map tracking remaining limits
     */
    private void processRemainingOrder(Order order, Map<String, BigDecimal> spent, Map<String, BigDecimal> remainingLimits) {
        BigDecimal orderValue = order.getValue();
        
        // Try partial points + card payment if possible
        BigDecimal pointsNeeded = orderValue.multiply(PARTIAL_POINTS_MINIMUM);
        PaymentMethod bestCard = findBestEligibleCard(order);
        
        if (bestCard != null && 
            remainingLimits.get(POINTS_METHOD).compareTo(pointsNeeded) >= 0) {
            
            BigDecimal discountedValue = PaymentUtils.applyDiscount(orderValue, PARTIAL_POINTS_DISCOUNT);
            BigDecimal cardPortion = discountedValue.subtract(pointsNeeded);
            
            if (remainingLimits.get(bestCard.getId()).compareTo(cardPortion) >= 0) {
                PaymentUtils.applyPayment(POINTS_METHOD, pointsNeeded, spent, remainingLimits);
                PaymentUtils.applyPayment(bestCard.getId(), cardPortion, spent, remainingLimits);
                return;
            }
        }
        
        // Try full points if possible
        BigDecimal pointsDiscountedValue = PaymentUtils.applyDiscount(orderValue, 
                                                        paymentMethods.get(POINTS_METHOD).getDiscount());
        
        if (remainingLimits.get(POINTS_METHOD).compareTo(pointsDiscountedValue) >= 0) {
            PaymentUtils.applyPayment(POINTS_METHOD, pointsDiscountedValue, spent, remainingLimits);
            return;
        }
        
        // Last resort: use any available card with sufficient limit
        for (PaymentMethod card : paymentMethods.values()) {
            if (POINTS_METHOD.equals(card.getId())) continue;
            
            List<String> promotions = order.getPromotions();
            if (promotions != null && promotions.contains(card.getId())) {
                BigDecimal cardDiscountedValue = PaymentUtils.applyDiscount(orderValue, card.getDiscount());
                
                if (remainingLimits.get(card.getId()).compareTo(cardDiscountedValue) >= 0) {
                    PaymentUtils.applyPayment(card.getId(), cardDiscountedValue, spent, remainingLimits);
                    return;
                }
            }
        }
        
        // Fallback: split the order across multiple payment methods if needed
        processWithSplitPayment(order, spent, remainingLimits);
    }
    
    /**
     * Process an order by splitting it across multiple payment methods if necessary.
     * 
     * @param order The order to process
     * @param spent The map tracking spent amounts
     * @param remainingLimits The map tracking remaining limits
     */
    private void processWithSplitPayment(Order order, Map<String, BigDecimal> spent, Map<String, BigDecimal> remainingLimits) {
        BigDecimal orderValue = order.getValue();
        BigDecimal remainingAmount = orderValue;
        
        // First, try to use as many points as available
        BigDecimal pointsAvailable = remainingLimits.get(POINTS_METHOD);
        if (pointsAvailable.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal pointsToUse = pointsAvailable.min(remainingAmount);
            PaymentUtils.applyPayment(POINTS_METHOD, pointsToUse, spent, remainingLimits);
            remainingAmount = remainingAmount.subtract(pointsToUse);
        }
        
        // If there's still an amount to pay, use available cards
        if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Sort eligible cards by discount
            List<PaymentMethod> eligibleCards = new ArrayList<>();
            for (PaymentMethod card : paymentMethods.values()) {
                if (POINTS_METHOD.equals(card.getId())) continue;
                
                List<String> promotions = order.getPromotions();
                if (promotions != null && promotions.contains(card.getId())) {
                    eligibleCards.add(card);
                }
            }
            eligibleCards.sort(Comparator.comparingInt(PaymentMethod::getDiscount).reversed());
            
            // Use available cards until the order is fully paid
            for (PaymentMethod card : eligibleCards) {
                if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) break;
                
                BigDecimal cardAvailable = remainingLimits.get(card.getId());
                if (cardAvailable.compareTo(BigDecimal.ZERO) > 0) {
                    // Calculate how much to pay with this card (with discount)
                    BigDecimal amountBeforeDiscount = remainingAmount.min(
                        PaymentUtils.reverseDiscount(cardAvailable, card.getDiscount())
                    );
                    BigDecimal discountedAmount = PaymentUtils.applyDiscount(amountBeforeDiscount, card.getDiscount());
                    
                    // Apply payment
                    PaymentUtils.applyPayment(card.getId(), discountedAmount, spent, remainingLimits);
                    remainingAmount = remainingAmount.subtract(amountBeforeDiscount);
                }
            }
        }
        
        // If there's still remaining amount, use any available payment method
        if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            for (Map.Entry<String, BigDecimal> entry : remainingLimits.entrySet()) {
                String methodId = entry.getKey();
                BigDecimal available = entry.getValue();
                
                if (available.compareTo(BigDecimal.ZERO) > 0) {
                    PaymentMethod method = paymentMethods.get(methodId);
                    BigDecimal amountBeforeDiscount = remainingAmount.min(
                        PaymentUtils.reverseDiscount(available, method.getDiscount())
                    );
                    BigDecimal discountedAmount = PaymentUtils.applyDiscount(amountBeforeDiscount, method.getDiscount());
                    
                    PaymentUtils.applyPayment(methodId, discountedAmount, spent, remainingLimits);
                    remainingAmount = remainingAmount.subtract(amountBeforeDiscount);
                    
                    if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                        break;
                    }
                }
            }
        }
        
        // If we still can't process the order, that's a real error
        if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Truly insufficient payment limits for order: " + 
                                           order.getId() + " - remaining amount: " + remainingAmount);
        }
    }
    
    /**
     * Find the best eligible card for an order.
     * 
     * @param order The order to find a card for
     * @return The best payment method or null if none found
     */
    private PaymentMethod findBestEligibleCard(Order order) {
        PaymentMethod bestCard = null;
        int bestDiscount = -1;
        
        for (PaymentMethod method : paymentMethods.values()) {
            if (POINTS_METHOD.equals(method.getId())) continue;
            
            List<String> promotions = order.getPromotions();
            if (promotions != null && promotions.contains(method.getId())) {
                if (method.getDiscount() > bestDiscount) {
                    bestCard = method;
                    bestDiscount = method.getDiscount();
                }
            }
        }
        
        return bestCard;
    }
}