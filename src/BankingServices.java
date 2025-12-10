import java.sql.*;

public class BankingServices {

    //REGISTRATION
    public int registerAccount(String fullName, String email, String pin) {
        // Just to make sure that user is entering the right input for the pin
        if (fullName.trim().isEmpty() || email.trim().isEmpty() || pin.length() < 4) {
            //trim() will clear the spaces
            System.out.println("Invalid credentials!! PIN must be at least 4 digits.");
            return -1;
        }

        String query = "INSERT INTO users (full_name, email, security_pin, balance) VALUES (?, ?, ?, 0.00)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) //RET..keys will return the acc. no.
        {
            pstmt.setString(1, fullName);
            pstmt.setString(2, email);
            pstmt.setString(3, pin);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                //For Retrieving the generated account number
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int newAccountNumber = generatedKeys.getInt(1);
                    System.out.println("\n===========================================");
                    System.out.println("{  *** Account Created Successfully! ***  }");
                    System.out.println("===========================================");
                    System.out.println(" Account Holder : " + fullName);
                    System.out.println(" Email : " + email);
                    System.out.println(" Your Account Number : " + newAccountNumber);
                    System.out.println(" Initial Balance : ₹0.00");
                    System.out.println("============================================");
                    System.out.println(" IMPORTANT: Please save your account number!");
                    System.out.println("============================================\n");
                    return newAccountNumber;
                }
            }

            return -1;

        } catch (SQLException e) { // This will get caught for any duplicacy
            if (e.getMessage().contains("Duplicate entry")) {
                System.out.println("Email already exists! Please use a different email.");
            } else {
                System.out.println("Registration failed. Please try again later.");
                e.printStackTrace();
            }
            return -1;
        }
    }


    // LOGIN
    public User login(int accountNumber, String pin) {
        String query = "SELECT * FROM users WHERE account_number = ? AND security_pin = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, accountNumber);
            pstmt.setString(2, pin);


            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new User(
                        rs.getInt("account_number"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getDouble("balance")
                );
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // CHECKING BALANCE
    public double getBalance(int accountNumber) {
        String query = "SELECT balance FROM users WHERE account_number = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, accountNumber);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble("balance");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    //DEPOSITING MONEY
    public boolean deposit(int accountNumber, double amount) {
        if (amount <= 0) {
            System.out.println("Amount must be positive!");
            return false;
        }

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); //Starting  the transaction

            // Updating balance
            String updateQuery = "UPDATE users SET balance = balance + ? WHERE account_number = ?";
            PreparedStatement pstmt1 = conn.prepareStatement(updateQuery);
            pstmt1.setDouble(1, amount);
            pstmt1.setInt(2, accountNumber);
            pstmt1.executeUpdate();

            //For updating the Log transactions
            String logQuery = "INSERT INTO transactions (account_number, transaction_type, amount) VALUES (?, 'DEPOSIT', ?)";
            PreparedStatement pstmt2 = conn.prepareStatement(logQuery);
            pstmt2.setInt(1, accountNumber);
            pstmt2.setDouble(2, amount);
            pstmt2.executeUpdate();

            conn.commit(); // Commiting transaction

            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); //Will do a Rollback on any type of error
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            // This block will run no matter the upper blocks returns true or false
            try {
                if (conn != null) {
                    conn.close(); // connection is closed now
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }

    // FOR WITHDRAWING
    public boolean withdraw(int accountNumber, double amount) {
        //If -ve value
        if (amount <= 0) {
            System.out.println("Amount must be positive!");
            return false;
        }

        // Checking balance first
        double currentBalance = getBalance(accountNumber);
        if (currentBalance < amount) {
            System.out.println("Insufficient balance!");
            return false;
        }

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            String updateQuery = "UPDATE users SET balance = balance - ? WHERE account_number = ?";
            PreparedStatement pstmt1 = conn.prepareStatement(updateQuery);
            pstmt1.setDouble(1, amount);
            pstmt1.setInt(2, accountNumber);
            pstmt1.executeUpdate();

            String logQuery = "INSERT INTO transactions (account_number, transaction_type, amount) VALUES (?, 'WITHDRAWAL', ?)";
            PreparedStatement pstmt2 = conn.prepareStatement(logQuery);
            pstmt2.setInt(1, accountNumber);
            pstmt2.setDouble(2, amount);
            pstmt2.executeUpdate();

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        }
    }

    //FOR TRANSFERRING
    public boolean transfer(int fromAccount, int toAccount, double amount) {
        // before starting the transaction , ensuring for positive amt. , no self transfers , and checking funds

        // amount must be +ve
        if (amount <= 0) {
            System.out.println("Transfer amount must be positive!");
            return false;
        }

        // ensuring the amount is not transferring to the same account
        if (fromAccount == toAccount) {
            System.out.println("Cannot transfer to the same account!");
            return false;
        }

        // Checking sender's balance first
        double senderBalance = getBalance(fromAccount);
        if (senderBalance < amount) {
            System.out.println("Insufficient balance! Available: ₹" + String.format("%.2f", senderBalance));
            return false;
        }

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            //turning off autosave now
            conn.setAutoCommit(false);   // starting transaction

            //Step 1: First deducing the amount from sender
            String debitQuery = "UPDATE users SET balance = balance - ? WHERE account_number = ?";
            PreparedStatement pstmt1 = conn.prepareStatement(debitQuery);
            pstmt1.setDouble(1, amount);
            pstmt1.setInt(2, fromAccount);
            pstmt1.executeUpdate();

            //Step 2: adding to the receiver's account
            String creditQuery = "UPDATE users SET balance = balance + ? WHERE account_number = ?";
            PreparedStatement pstmt2 = conn.prepareStatement(creditQuery);
            pstmt2.setDouble(1, amount);
            pstmt2.setInt(2, toAccount);
            int rowsAffected = pstmt2.executeUpdate();

            // checking if receiver account actually exists
            if (rowsAffected == 0) {
                conn.rollback();
                System.out.println("Receiver account #" + toAccount + " not found!");
                return false;
            }

            //Step 3: let's prepare log transactions for both accounts
            String logQuery = "INSERT INTO transactions (account_number, transaction_type, amount) VALUES (?, ?, ?)";

            //log for sender's transaction
            PreparedStatement pstmt3 = conn.prepareStatement(logQuery);
            pstmt3.setInt(1, fromAccount);
            pstmt3.setString(2, "TRANSFER_OUT");
            pstmt3.setDouble(3, amount);
            pstmt3.executeUpdate();

            //log for receiver's transaction
            pstmt3.setInt(1, toAccount);
            pstmt3.setString(2, "TRANSFER_IN");
            pstmt3.setDouble(3, amount);
            pstmt3.executeUpdate();

            //at last committing everything
            conn.commit();
            return true;

        } catch (SQLException e) {
            //If anything fails, rolling back everything
            if (conn != null) {
                try {
                    conn.rollback();
                    System.out.println("Oh Sheesh!! Transfer failed! Transaction rolled back.");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            //resetting the auto-commit
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    // For Displaying last 10 transactions history
    public void showTransactionHistory(int accountNumber) {
        String query = "SELECT * FROM transactions WHERE account_number = ? ORDER BY transaction_date DESC LIMIT 10";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, accountNumber);
            ResultSet rs = pstmt.executeQuery();

            System.out.println("\n╔════════════════════════════════════════╗");
            System.out.println("║      TRANSACTION HISTORY               ║");
            System.out.println("╚════════════════════════════════════════╝");

            boolean hasTransactions = false;//to have a track of transactions
            int count = 1;

            while (rs.next()) {
                hasTransactions = true;
                System.out.println("\n[" + count + "] ────────────────────────────────");
                System.out.println("Type: " + rs.getString("transaction_type"));
                System.out.println("Amount: ₹" + String.format("%.2f", rs.getDouble("amount")));
                System.out.println("Date: " + rs.getTimestamp("transaction_date"));
                count++;
            }

            if (!hasTransactions) {
                System.out.println("\nNo transactions found.");
            }

            System.out.println("\n═════════════════════════════════════════\n");

        } catch (SQLException e) {
            System.out.println("Unable to fetch transaction history.");
            e.printStackTrace();
        }
    }
}