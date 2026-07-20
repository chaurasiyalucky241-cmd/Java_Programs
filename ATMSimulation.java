import java.sql.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

class InsufficientBalanceException extends Exception {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}

class InvalidPinException extends Exception {
    public InvalidPinException(String message) {
        super(message);
    }
}

class AccountNotFoundException extends Exception {
    public AccountNotFoundException(String message) {
        super(message);
    }
}

class BankDB {
    private static final String URL = "jdbc:mysql://localhost:3306/atm_db";
    private static final String DB_USER = "root";       // change if needed
    private static final String DB_PASSWORD = "";        // put your MySQL password here

    private final Random random = new Random();

    public BankDB() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL JDBC Driver not found. Check your classpath.");
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, DB_USER, DB_PASSWORD);
    }

    private String generateAccNo(Connection con) throws SQLException {
        String accNo;
        String checkSql = "SELECT acc_no FROM accounts WHERE acc_no = ?";
        do {
            int digits = 100000 + random.nextInt(900000); // always 6 digits
            accNo = "ACC" + digits;
            try (PreparedStatement ps = con.prepareStatement(checkSql)) {
                ps.setString(1, accNo);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) break; // unique, exit loop
            }
        } while (true);
        return accNo;
    }

    public String createAccount(String name, int pin, double deposit) throws SQLException {
        try (Connection con = getConnection()) {
            String accNo = generateAccNo(con);

            String insertAcc = "INSERT INTO accounts (acc_no, name, pin, balance) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(insertAcc)) {
                ps.setString(1, accNo);
                ps.setString(2, name);
                ps.setInt(3, pin);
                ps.setDouble(4, deposit);
                ps.executeUpdate();
            }

            logTransaction(con, accNo, "Account opened with balance: ₹" + deposit);
            return accNo;
        }
    }

    public double getBalance(String accNo) throws SQLException, AccountNotFoundException {
        try (Connection con = getConnection()) {
            String sql = "SELECT balance FROM accounts WHERE acc_no = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, accNo);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getDouble("balance");
                }
                throw new AccountNotFoundException("No account found with number: " + accNo);
            }
        }
    }

    public String getName(String accNo) throws SQLException, AccountNotFoundException {
        try (Connection con = getConnection()) {
            String sql = "SELECT name FROM accounts WHERE acc_no = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, accNo);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getString("name");
                }
                throw new AccountNotFoundException("No account found with number: " + accNo);
            }
        }
    }

    public void checkPin(String accNo, int enteredPin) throws SQLException, AccountNotFoundException, InvalidPinException {
        try (Connection con = getConnection()) {
            String sql = "SELECT pin FROM accounts WHERE acc_no = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, accNo);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    throw new AccountNotFoundException("No account found with number: " + accNo);
                }
                if (rs.getInt("pin") != enteredPin) {
                    throw new InvalidPinException("Incorrect PIN. Access denied.");
                }
            }
        }
    }

    public void deposit(String accNo, double amount) throws SQLException {
        try (Connection con = getConnection()) {
            String sql = "UPDATE accounts SET balance = balance + ? WHERE acc_no = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setDouble(1, amount);
                ps.setString(2, accNo);
                ps.executeUpdate();
            }
            logTransaction(con, accNo, "Deposited: ₹" + amount);
        }
    }

    public void withdraw(String accNo, double amount) throws SQLException, InsufficientBalanceException {
        try (Connection con = getConnection()) {
            double currentBalance = getBalance(accNo);
            if (amount > currentBalance) {
                throw new InsufficientBalanceException("Insufficient balance! Available: ₹" + currentBalance);
            }
            String sql = "UPDATE accounts SET balance = balance - ? WHERE acc_no = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setDouble(1, amount);
                ps.setString(2, accNo);
                ps.executeUpdate();
            }
            logTransaction(con, accNo, "Withdrew: ₹" + amount);
        }
    }

    private void logTransaction(Connection con, String accNo, String description) throws SQLException {
        String sql = "INSERT INTO transactions (acc_no, description) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, accNo);
            ps.setString(2, description);
            ps.executeUpdate();
        }
    }

    public ArrayList<String> getHistory(String accNo) throws SQLException {
        ArrayList<String> history = new ArrayList<>();
        try (Connection con = getConnection()) {
            String sql = "SELECT description, txn_time FROM transactions WHERE acc_no = ? ORDER BY txn_time";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, accNo);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    history.add("[" + rs.getTimestamp("txn_time") + "] " + rs.getString("description"));
                }
            }
        }
        return history;
    }
}

public class ATMSimulation {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        BankDB bank = new BankDB();
        boolean running = true;

        System.out.println("==== Welcome to Java ATM (MySQL) ====");

        while (running) {
            System.out.println("\n1. Create New Account");
            System.out.println("2. Login to Existing Account");
            System.out.println("3. Exit");
            System.out.print("Enter Choice: ");
            int choice = sc.nextInt();

            if (choice == 1) {
                sc.nextLine();
                System.out.print("Enter your name: ");
                String name = sc.nextLine();
                System.out.print("Set a 4-digit PIN: ");
                int pin = sc.nextInt();
                System.out.print("Enter initial deposit amount: ");
                double deposit = sc.nextDouble();

                try {
                    String accNo = bank.createAccount(name, pin, deposit);
                    System.out.println("\nAccount created successfully!");
                    System.out.println("Your Account Number: " + accNo);
                    System.out.println("(Save this number, you'll need it to login)");
                } catch (SQLException e) {
                    System.out.println("Database error: " + e.getMessage());
                }

            } else if (choice == 2) {
                sc.nextLine();
                System.out.print("Enter your Account Number: ");
                String accNo = sc.nextLine();
                System.out.print("Enter your PIN: ");
                int pin = sc.nextInt();

                try {
                    bank.checkPin(accNo, pin);
                    String name = bank.getName(accNo);
                    System.out.println("Login successful! Welcome, " + name);
                    showMenu(sc, bank, accNo);
                } catch (AccountNotFoundException | InvalidPinException e) {
                    System.out.println("Error: " + e.getMessage());
                } catch (SQLException e) {
                    System.out.println("Database error: " + e.getMessage());
                }

            } else if (choice == 3) {
                running = false;
                System.out.println("Thank you for using Java ATM. Goodbye, Brody!");

            } else {
                System.out.println("Invalid choice, try again.");
            }
        }
        sc.close();
    }

    private static void showMenu(Scanner sc, BankDB bank, String accNo) {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n--- " + accNo + " Menu ---");
            System.out.println("1. Check Balance");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Transaction History");
            System.out.println("5. Logout");
            System.out.print("Enter Choice: ");
            int choice = sc.nextInt();

            try {
                switch (choice) {
                    case 1:
                        System.out.println("Current Balance: ₹" + bank.getBalance(accNo));
                        break;

                    case 2:
                        System.out.print("Enter amount to deposit: ");
                        double dep = sc.nextDouble();
                        bank.deposit(accNo, dep);
                        System.out.println("Deposit successful. New Balance: ₹" + bank.getBalance(accNo));
                        break;

                    case 3:
                        System.out.print("Enter amount to withdraw: ");
                        double wd = sc.nextDouble();
                        try {
                            bank.withdraw(accNo, wd);
                            System.out.println("Withdrawal successful. New Balance: ₹" + bank.getBalance(accNo));
                        } catch (InsufficientBalanceException e) {
                            System.out.println("Error: " + e.getMessage());
                        }
                        break;

                    case 4:
                        System.out.println("\n--- Transaction History ---");
                        for (String entry : bank.getHistory(accNo)) {
                            System.out.println(entry);
                        }
                        break;

                    case 5:
                        loggedIn = false;
                        System.out.println("Logged out successfully.");
                        break;

                    default:
                        System.out.println("Invalid choice, try again.");
                }
            } catch (SQLException | AccountNotFoundException e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }
    }
}
