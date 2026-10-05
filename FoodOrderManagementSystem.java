package com.food;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class FoodOrderManagementSystem {

    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("====================================================");
        System.out.println("          FOOD ORDER MANAGEMENT SYSTEM");
        System.out.println("====================================================");
        System.out.println("Student : PAVAN KUMAR");
        System.out.println("USN     : 1VJ25CD006");

        while (true) {
            printMenu();
            int choice = readInt("Enter your choice: ");

            try {
                switch (choice) {
                    case 1 -> addFoodItem();
                    case 2 -> viewMenu();
                    case 3 -> searchFoodItem();
                    case 4 -> addCustomer();
                    case 5 -> viewCustomers();
                    case 6 -> placeOrder();
                    case 7 -> viewAllOrders();
                    case 8 -> searchOrder();
                    case 9 -> cancelOrder();
                    case 10 -> {
                        System.out.println("Thank you for using Food Order Management System.");
                        scanner.close();
                        return;
                    }
                    default -> System.out.println("Invalid choice. Please try again.");
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }

            System.out.println();
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("---------------- MAIN MENU ----------------");
        System.out.println("1. Add Food Item");
        System.out.println("2. View Menu");
        System.out.println("3. Search Food Item");
        System.out.println("4. Add Customer");
        System.out.println("5. View Customers");
        System.out.println("6. Place Order");
        System.out.println("7. View All Orders");
        System.out.println("8. Search Order");
        System.out.println("9. Cancel Order");
        System.out.println("10. Exit");
        System.out.println("-------------------------------------------");
    }

    private static void addFoodItem() throws SQLException {
        String name = readRequired("Food name: ");
        String category = readRequired("Category: ");
        BigDecimal price = readDecimal("Price: ");

        String sql = "INSERT INTO food_items (food_name, category, price, available) VALUES (?, ?, ?, TRUE)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setBigDecimal(3, price);
            ps.executeUpdate();
            System.out.println("Food item added successfully.");
        }
    }

    private static void viewMenu() throws SQLException {
        String sql = "SELECT food_id, food_name, category, price, available FROM food_items ORDER BY food_id";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.printf("%-8s %-25s %-18s %-12s %-10s%n",
                    "ID", "Food Name", "Category", "Price", "Available");
            System.out.println("--------------------------------------------------------------------------");

            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%-8d %-25s %-18s %-12s %-10s%n",
                        rs.getInt("food_id"),
                        rs.getString("food_name"),
                        rs.getString("category"),
                        rs.getBigDecimal("price"),
                        rs.getBoolean("available") ? "YES" : "NO");
            }

            if (!found) System.out.println("No food items found.");
        }
    }

    private static void searchFoodItem() throws SQLException {
        String keyword = readRequired("Enter food name/category to search: ");

        String sql = """
                SELECT food_id, food_name, category, price, available
                FROM food_items
                WHERE food_name LIKE ? OR category LIKE ?
                ORDER BY food_id
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String pattern = "%" + keyword + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf("ID: %d | %s | %s | ₹%s | %s%n",
                            rs.getInt("food_id"),
                            rs.getString("food_name"),
                            rs.getString("category"),
                            rs.getBigDecimal("price"),
                            rs.getBoolean("available") ? "Available" : "Not Available");
                }
                if (!found) System.out.println("No matching food item found.");
            }
        }
    }

    private static void addCustomer() throws SQLException {
        String name = readRequired("Customer name: ");
        String phone = readRequired("Phone: ");
        String email = readOptional("Email: ");

        String sql = "INSERT INTO customers (customer_name, phone, email) VALUES (?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, email.isBlank() ? null : email);
            ps.executeUpdate();
            System.out.println("Customer added successfully.");
        }
    }

    private static void viewCustomers() throws SQLException {
        String sql = "SELECT customer_id, customer_name, phone, email FROM customers ORDER BY customer_id";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.printf("%-8s %-25s %-18s %-30s%n",
                    "ID", "Customer Name", "Phone", "Email");
            System.out.println("--------------------------------------------------------------------------");

            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%-8d %-25s %-18s %-30s%n",
                        rs.getInt("customer_id"),
                        rs.getString("customer_name"),
                        rs.getString("phone"),
                        rs.getString("email") == null ? "-" : rs.getString("email"));
            }

            if (!found) System.out.println("No customers found.");
        }
    }

    private static void placeOrder() throws SQLException {
        viewCustomers();
        int customerId = readInt("Customer ID: ");

        if (!customerExists(customerId)) {
            System.out.println("Customer not found.");
            return;
        }

        List<OrderLine> lines = new ArrayList<>();

        while (true) {
            viewMenu();
            int foodId = readInt("Food ID (0 to finish): ");
            if (foodId == 0) break;

            FoodItem food = findFood(foodId);
            if (food == null || !food.available()) {
                System.out.println("Food item not found or unavailable.");
                continue;
            }

            int quantity = readPositiveInt("Quantity: ");
            lines.add(new OrderLine(food.id(), quantity, food.price()));
        }

        if (lines.isEmpty()) {
            System.out.println("No items selected. Order cancelled.");
            return;
        }

        BigDecimal total = BigDecimal.ZERO;
        for (OrderLine line : lines) {
            total = total.add(line.unitPrice().multiply(BigDecimal.valueOf(line.quantity())));
        }

        System.out.println("Total amount: ₹" + total);

        if (!readYesNo("Confirm order? (Y/N): ")) {
            System.out.println("Order cancelled.");
            return;
        }

        String orderSql = "INSERT INTO orders (customer_id, total_amount, status) VALUES (?, ?, 'PLACED')";
        String itemSql = "INSERT INTO order_items (order_id, food_id, quantity, unit_price) VALUES (?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                int orderId;

                try (PreparedStatement ps = con.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, customerId);
                    ps.setBigDecimal(2, total);
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Could not create order.");
                        orderId = keys.getInt(1);
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(itemSql)) {
                    for (OrderLine line : lines) {
                        ps.setInt(1, orderId);
                        ps.setInt(2, line.foodId());
                        ps.setInt(3, line.quantity());
                        ps.setBigDecimal(4, line.unitPrice());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                con.commit();
                System.out.println("Order placed successfully. Order ID: " + orderId);
                System.out.println("Order total: ₹" + total);
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    private static void viewAllOrders() throws SQLException {
        String sql = """
                SELECT o.order_id, c.customer_name, o.order_date,
                       o.total_amount, o.status
                FROM orders o
                JOIN customers c ON o.customer_id = c.customer_id
                ORDER BY o.order_id DESC
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("Order ID: %d | Customer: %s | Date: %s | Total: ₹%s | Status: %s%n",
                        rs.getInt("order_id"),
                        rs.getString("customer_name"),
                        rs.getTimestamp("order_date"),
                        rs.getBigDecimal("total_amount"),
                        rs.getString("status"));
            }

            if (!found) System.out.println("No orders found.");
        }
    }

    private static void searchOrder() throws SQLException {
        int orderId = readInt("Order ID: ");

        String orderSql = """
                SELECT o.order_id, c.customer_name, c.phone, o.order_date,
                       o.total_amount, o.status
                FROM orders o
                JOIN customers c ON o.customer_id = c.customer_id
                WHERE o.order_id = ?
                """;

        String itemSql = """
                SELECT f.food_name, oi.quantity, oi.unit_price,
                       (oi.quantity * oi.unit_price) AS line_total
                FROM order_items oi
                JOIN food_items f ON oi.food_id = f.food_id
                WHERE oi.order_id = ?
                """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(orderSql)) {

            ps.setInt(1, orderId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Order not found.");
                    return;
                }

                System.out.println("Order ID: " + rs.getInt("order_id"));
                System.out.println("Customer: " + rs.getString("customer_name"));
                System.out.println("Phone: " + rs.getString("phone"));
                System.out.println("Date: " + rs.getTimestamp("order_date"));
                System.out.println("Status: " + rs.getString("status"));
                System.out.println("Total: ₹" + rs.getBigDecimal("total_amount"));
            }

            System.out.println("Items:");
            try (PreparedStatement itemPs = con.prepareStatement(itemSql)) {
                itemPs.setInt(1, orderId);
                try (ResultSet rs = itemPs.executeQuery()) {
                    while (rs.next()) {
                        System.out.printf("  %s x%d @ ₹%s = ₹%s%n",
                                rs.getString("food_name"),
                                rs.getInt("quantity"),
                                rs.getBigDecimal("unit_price"),
                                rs.getBigDecimal("line_total"));
                    }
                }
            }
        }
    }

    private static void cancelOrder() throws SQLException {
        int orderId = readInt("Order ID to cancel: ");

        String sql = "UPDATE orders SET status = 'CANCELLED' WHERE order_id = ? AND status = 'PLACED'";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, orderId);
            int updated = ps.executeUpdate();

            if (updated > 0) {
                System.out.println("Order cancelled successfully.");
            } else {
                System.out.println("Order not found or already cancelled.");
            }
        }
    }

    private static boolean customerExists(int customerId) throws SQLException {
        String sql = "SELECT 1 FROM customers WHERE customer_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static FoodItem findFood(int foodId) throws SQLException {
        String sql = "SELECT food_id, food_name, price, available FROM food_items WHERE food_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, foodId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new FoodItem(
                            rs.getInt("food_id"),
                            rs.getString("food_name"),
                            rs.getBigDecimal("price"),
                            rs.getBoolean("available")
                    );
                }
            }
        }
        return null;
    }

    private static String readRequired(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            if (!value.isEmpty()) return value;
            System.out.println("This field cannot be empty.");
        }
    }

    private static String readOptional(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static int readInt(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }

    private static int readPositiveInt(String prompt) {
        while (true) {
            int value = readInt(prompt);
            if (value > 0) return value;
            System.out.println("Value must be greater than zero.");
        }
    }

    private static BigDecimal readDecimal(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                BigDecimal value = new BigDecimal(scanner.nextLine().trim());
                if (value.compareTo(BigDecimal.ZERO) > 0) return value;
                System.out.println("Price must be greater than zero.");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private static boolean readYesNo(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            if (value.equalsIgnoreCase("Y")) return true;
            if (value.equalsIgnoreCase("N")) return false;
            System.out.println("Please enter Y or N.");
        }
    }

    private record FoodItem(int id, String name, BigDecimal price, boolean available) {}
    private record OrderLine(int foodId, int quantity, BigDecimal unitPrice) {}
}
