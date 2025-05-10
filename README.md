## Description
Given two JSON files:
- `orders.json`: list of orders (id, value, promotions)
- `paymentmethods.json`: list of payment methods (id, discount, limit)

Compute an assignment of payments (cards and loyalty points) to orders that maximizes total discount, minimizes card usage, and respects payment limits.

## Thought process
### Błędne algorytmy
#### Sortowanie orders wg. ich ceny
1. Posortowanie orders od największego do najmniejszego
2. Sprawdzenie jaki jest najlepszy możliwy discount dla danego orderu, dzięki posortowaniu największy order będzie obniżony o maksymalną sumę.

problemy:
- 

#### Sortowanie paymentmethods wg. discount
1. Posegregowanie metod według discount
2. Sprawdzenie jaki jest największy możliwy zbiór orderów dla metody płatności - gwarantuje, że największa zniża będzie użyta na największej sumie
3. Przeiterować tak przez wszsytkie metody płatności

problem:
- punkty mogą tak się rozkładać, że bardziej opłaca się je rozdysponować na mniejsze ordery  które nie mają, żadnego discount/ żaden discount nie pasuje

rozwiązanie:
- przeprowadzić, dwa scenariusze, jeden dla punktów jako 




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