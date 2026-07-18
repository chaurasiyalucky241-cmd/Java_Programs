import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

class Item {
    String name;
    int quantity;
    double price;

    Item(String name, int quantity, double price) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
    }

    double totalValue() {
        return quantity * price;
    }
}

public class Day01_InventoryManager {

    static int getValidInt(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("it's not a numerical value ::: Please Try Again");
            }
        }
    }

    static double getValidDouble(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                System.out.println("it's not a numerical value ::: Please Try Again");
            }
        }
    }

    static List<Item> getItemInput(Scanner sc) {
        List<Item> inventory = new ArrayList<>();
        int n = getValidInt(sc, "Total number of items: ");

        for (int i = 0; i < n; i++) {
            System.out.println("\n--- Item " + (i + 1) + " ---");
            System.out.print("Item name: ");
            String name = sc.nextLine().trim();

            int quantity = getValidInt(sc, "Quantity: ");
            double price = getValidDouble(sc, "Price per unit: ");

            inventory.add(new Item(name, quantity, price));
        }
        return inventory;
    }

    static void showReport(List<Item> inventory) {
        double grandTotal = 0;
        System.out.printf("%n%-15s%-10s%-10s%-10s%n", "Item", "Qty", "Price", "Value");
        for (Item item : inventory) {
            System.out.printf("%-15s%-10d%-10.2f%-10.2f%n",
                    item.name, item.quantity, item.price, item.totalValue());
            grandTotal += item.totalValue();
        }
        System.out.printf("%nTotal Inventory Value: %.2f%n", grandTotal);
    }

    static void saveToDB(List<Item> inventory) {
        String sql = "INSERT INTO items(Name, Quantity, Price) VALUES(?,?,?)";

        try (Connection conn = DBConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (Item item : inventory) {
                stmt.setString(1, item.name);
                stmt.setInt(2, item.quantity);
                stmt.setDouble(3, item.price);
                stmt.executeUpdate();   // ← yeh missing tha, ab add kiya
            }

            System.out.println("\n✅ Data MySQL mein store ho gaya");

        } catch (SQLException e) {
            System.out.println("❌ Database error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("=== Inventory Manager ===");
        List<Item> inventory = getItemInput(sc);
        showReport(inventory);
        saveToDB(inventory);   // ← yeh call missing tha, ab add kiya
        sc.close();
    }
}