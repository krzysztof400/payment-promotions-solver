package com.example;

import com.example.model.Order;
import com.example.model.PaymentMethod;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        try {
            if (args.length < 2) {
                System.err.println("Usage: java -jar app.jar <orders.json> <paymentmethods.json>");
                System.exit(1);
            }
            
            ObjectMapper mapper = new ObjectMapper();
            List<Order> orders = mapper.readValue(new File(args[0]), new TypeReference<List<Order>>() {});
            List<PaymentMethod> methods = mapper.readValue(new File(args[1]), new TypeReference<List<PaymentMethod>>() {});

            Solver solver = new Solver(orders, methods);
            Map<String, BigDecimal> result = solver.solve();

            // Output results in required format
            result.forEach((id, amount) ->
                System.out.printf("%s %s%n", id, amount.setScale(2, RoundingMode.HALF_UP))
            );
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            System.exit(1);
        }
    }
}