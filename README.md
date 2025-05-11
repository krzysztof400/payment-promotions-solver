## Description
Given two JSON files:
- `orders.json`: list of orders (id, value, promotions)
- `paymentmethods.json`: list of payment methods (id, discount, limit)

Compute an assignment of payments (cards and loyalty points) to orders that maximizes total discount, minimizes card usage, and respects payment limits.

## Thought Process

### Sorting Orders by Value

1. Sort orders in descending order of their value.
2. For each order, apply the best possible discount — by processing high-value orders first, we maximize the impact of available discounts.

**Problem:**
Each method can work the best not for the highest available order but for subset of smaller orders.

### Sorting Payment Methods by Discount

1. Sort payment methods by their discount rates in descending order.
2. For each method, determine the most valuable subset of orders it can be applied to — this ensures that the highest discounts are used on the highest total amounts.
3. Iterate through all payment methods in this order.

**Problem:**
Loyalty points (or similar flexible methods) might yield better results if distributed across multiple smaller orders, especially those that don't qualify for any other discount.
For example, in one of the PDF scenarios, it's more effective to apply a flat 10% discount using points across all orders, achieving a total discount of 50 instead of 45 — which would result from using higher-percentage discounts on fewer, larger orders.

**Solution:**
Explore multiple scenarios:

* In one, treat loyalty points like any other fixed payment method.
* In another, intelligently distribute them among orders that cannot otherwise be discounted by more than 10%, maximizing overall savings.
  In both cases, choose the most valuable subset of orders for point usage
* In one prioritize use of points as a paying method.

**Algorithm mixed approach taking the best from each conclusion and iteration**
## Usage
```bash
java -jar target/payment-promotions-solver.jar /path/to/orders.json /path/to/paymentmethods.json
```

The program outputs to stdout the total spent amount per payment method:
```
<methodId1> <amountSpent>
<methodId2> <amountSpent>
...
```

## Build
This project uses Maven:
```bash
mvn clean package
```
This produces a fat-jar under `target/payment-promotions-solver.jar`.

## Testing
```bash
mvn test
```