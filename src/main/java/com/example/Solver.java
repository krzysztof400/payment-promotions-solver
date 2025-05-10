package com.example;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.example.model.Order;
import com.example.model.PaymentMethod;

/**
 * Solver assigns payment methods and loyalty points to orders,
 * using a heuristic:
 * 1. For each non-point payment method (sorted by discount desc),
 *    find the largest subset of eligible orders (knapsack) and pay them fully.
 * 2. For loyalty points, run two scenarios:
 *    A) Points used only as full payment for orders where loyalty discount > card discount.
 *    B) Points distributed as 10% payment across orders where card discount < 10%.
 * 3. Fallback: remaining orders paid by best card or any card.
 * Finally pick scenario with lowest total spend.
 */
public class Solver {
    private final List<Order> orders;
    private final Map<String, PaymentMethod> initialMethods;

    public Solver(List<Order> orders, List<PaymentMethod> methods) {
        this.orders = new ArrayList<>(orders);
        this.initialMethods = new HashMap<>();
        for (PaymentMethod m : methods) {
            PaymentMethod copy = new PaymentMethod();
            copy.setId(m.getId());
            copy.setDiscount(m.getDiscount());
            copy.setLimit(m.getLimit());
            this.initialMethods.put(copy.getId(), copy);
        }
    }

    /**
     * Runs both point strategies and returns the spent map for the cheaper total.
     */
    public Map<String, BigDecimal> solve() {
        Map<String, BigDecimal> resultA = solveScenario(true);
        Map<String, BigDecimal> resultB = solveScenario(false);
        BigDecimal totalA = totalSpent(resultA);
        BigDecimal totalB = totalSpent(resultB);
        return totalA.compareTo(totalB) <= 0 ? resultA : resultB;
    }

    /**
     * Processes orders under one loyalty-points strategy.
     * @param pointsAsOnly if true, use points only as full payment; otherwise, use 10% points mixed.
     */
    private Map<String, BigDecimal> solveScenario(boolean pointsAsOnly) {
        // 1. Clone limits map
        Map<String, BigDecimal> limits = new HashMap<>();
        initialMethods.forEach((id, pm) -> limits.put(id, pm.getLimit()));

        // 2. Prepare sorted lists
        List<Order> sortedOrders = new ArrayList<>(orders);
        sortedOrders.sort(Comparator.comparing(Order::getValue).reversed());

        List<PaymentMethod> sortedMethods = new ArrayList<>(initialMethods.values());
        sortedMethods.sort(Comparator.comparingInt(PaymentMethod::getDiscount).reversed());
        if(pointsAsOnly) {
            sortedMethods.removeIf(pm -> "PUNKTY".equals(pm.getId()));
        }
        List<PaymentMethod> deadMethods = new ArrayList<>();

        // Initialize spent tracking
        Map<String, BigDecimal> spent = new HashMap<>();
        limits.keySet().forEach(id -> spent.put(id, BigDecimal.ZERO));

        // 4. Iterate through cards
        while (!sortedOrders.isEmpty() && !sortedMethods.isEmpty()) {
            PaymentMethod bestCard = sortedMethods.get(0);
            List<Order> applicable = new ArrayList<>();
            for (Order o : sortedOrders) {
                List<String> promos = o.getPromotions();
                if (promos != null && promos.contains(bestCard.getId())) {
                    applicable.add(o);
                }
            }
            if (applicable.isEmpty()) {
                // remove unusable card
                deadMethods.add(sortedMethods.remove(0));
                continue;
            }

            // Scenario B: mix points if card discount <10
            if (bestCard.getDiscount() < 10) {
                List<Order> subset = maxSubsetOrders(applicable, limits.get("PUNKTY").multiply(BigDecimal.valueOf(10)));
                if (!subset.isEmpty()) {
                    for (Order o : subset) {
                        partialCardPayment(o, bestCard, spent, limits);
                        sortedOrders.remove(o);
                        applicable.remove(o);
                    }
                }
            }

            // pay as many as possible with card
            List<Order> payList = maxSubsetOrders(applicable, bestCard.getLimit());
            for (Order o : payList) {
                fullCardPayment(o, bestCard, spent, limits);
                sortedOrders.remove(o);
            }
        }

        // 5. Handle remaining orders
        for (Order o : sortedOrders) {
            PaymentMethod card = anyCard(o);
            if (limits.get("PUNKTY").multiply(BigDecimal.valueOf(10)).compareTo(o.getValue()) >= 0) {
                partialCardPayment(o, card, spent, limits);
            }
            else {
                fullCardPayment(o, card, spent, limits);
            }
        }
        return spent;
    }

    private Optional<PaymentMethod> bestCard(Order o) {
        return Optional.ofNullable(o.getPromotions()).orElse(Collections.emptyList()).stream()
            .filter(id -> !"PUNKTY".equals(id))
            .map(initialMethods::get)
            .filter(Objects::nonNull)
            .filter(pm -> initialMethods.get(pm.getId()).getLimit().compareTo(o.getValue()) >= 0)
            .max(Comparator.comparingInt(PaymentMethod::getDiscount));
    }

    private PaymentMethod anyCard(Order o) {
        return initialMethods.values().stream()
            .filter(pm -> !"PUNKTY".equals(pm.getId()))
            .filter(pm -> initialMethods.get(pm.getId()).getLimit().compareTo(o.getValue()) >= 0)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No card for order " + o.getId()));
    }

    private void apply(String methodId, BigDecimal amount,
                       Map<String, BigDecimal> spent,
                       Map<String, BigDecimal> limits) {
        limits.put(methodId, limits.get(methodId).subtract(amount));
        spent.merge(methodId, amount, BigDecimal::add);
    }

    /**
     * Applies full-card payment with discount.
     */
    private void fullCardPayment(Order o, PaymentMethod card,
                                 Map<String, BigDecimal> spent,
                                 Map<String, BigDecimal> limits) {
        BigDecimal pay = applyDiscount(o.getValue(), card.getDiscount());
        apply(card.getId(), pay, spent, limits);
        System.out.println("Full payment: " + o.getId() + " " + card.getId() + " " + pay);
    }

    /**
     * Applies partial payment: 10% with loyalty points, rest with card.
     */
    private void partialCardPayment(Order o, PaymentMethod card,
                                    Map<String, BigDecimal> spent,
                                    Map<String, BigDecimal> limits) {
        BigDecimal pct = BigDecimal.valueOf(0.10);
        BigDecimal usedPoints = o.getValue().multiply(pct);
        apply("PUNKTY", usedPoints, spent, limits);
        BigDecimal remaining = o.getValue().subtract(usedPoints);
        BigDecimal cardPay = applyDiscount(o.getValue(), 10).subtract(usedPoints);
        apply(card.getId(), cardPay, spent, limits);
        System.out.println("Partial payment: " + o.getId() + " " + card.getId() + " " + cardPay + " PUNKTY " + usedPoints);
    }

    private BigDecimal applyDiscount(BigDecimal amount, int discount) {
        BigDecimal factor = BigDecimal.ONE.subtract(
            BigDecimal.valueOf(discount).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP)
        );
        return amount.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal totalSpent(Map<String, BigDecimal> spent) {
        return spent.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 0/1 Knapsack to maximize number of orders under a limit.
     */
    private static List<Order> maxSubsetOrders(List<Order> orders, BigDecimal limit) {
        int scale = 100;
        int n = orders.size();
        int max = limit.multiply(BigDecimal.valueOf(scale)).intValueExact();
        boolean[][] dp = new boolean[n + 1][max + 1];
        dp[0][0] = true;
        for (int i = 1; i <= n; i++) {
            int val = orders.get(i - 1).getValue().multiply(BigDecimal.valueOf(scale)).intValueExact();
            for (int j = 0; j <= max; j++) {
                dp[i][j] = dp[i - 1][j] || (j >= val && dp[i - 1][j - val]);
            }
        }
        int best = 0;
        for (int j = max; j >= 0; j--) if (dp[n][j]) { best = j; break; }
        List<Order> result = new ArrayList<>();
        int w = best;
        for (int i = n; i > 0 && w > 0; i--) {
            int val = orders.get(i - 1).getValue().multiply(BigDecimal.valueOf(scale)).intValueExact();
            if (w >= val && dp[i - 1][w - val]) {
                result.add(orders.get(i - 1));
                w -= val;
            }
        }
        return result;
    }
}
