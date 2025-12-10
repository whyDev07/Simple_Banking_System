import java.util.Scanner;
import java.sql.*;

/**
 * Banking Application - Main Interface
 * Author: Dev Adhikari
 * Purpose: Console-based banking system demonstrating JDBC operations
 * Features: Account management, transactions, secure authentication
 */

public class BankingApp {
    private static Scanner scanner = new Scanner(System.in);
    private static BankingServices service = new BankingServices();
    private static User currentUser = null;//If it is null, it means nobody is logged in (show the Main Menu)

    // first :- welcome menus and banners

    // For Displaying the startup banner with database connection status
    // and also validates database connectivity before allowing app usage

    private static void welcomeBanner() {

        System.out.print("\033[H\033[2J"); // ANSI escape code to clean everything from the console
        System.out.flush();

        //copying alt codes for banners and stuff
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║       SECURE BANKING SYSTEM v2.0       ║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║                                        ║");

        // Testing the database connection
        Connection testConn = DatabaseConnection.getConnection();
        if (testConn != null) {
            try {
                DatabaseMetaData metaData = testConn.getMetaData(); // SPecial object to question the database about it
                String dbProduct = metaData.getDatabaseProductName();
                String dbVersion = metaData.getDatabaseProductVersion();

                System.out.println("║ [✓] Database: " + dbProduct + " " + dbVersion.substring(0, 3) + "                ║");
                System.out.println("║ [✓] ACID Transactions: ENABLED         ║");
                System.out.println("║ [✓] SQL Injection Protection: ACTIVE   ║");
                System.out.println("║ [✓] PreparedStatement: ENFORCED        ║");

                testConn.close();

            } catch (SQLException e) {
                System.out.println("║ [!] Database Status: WARNING           ║");
            }
        } else {
            System.out.println("║ [✗] Database Connection: FAILED        ║");
            System.out.println("║ [!] Please check MySQL credentials     ║");
            System.out.println("╚════════════════════════════════════════╝\n");
            System.exit(1);
        }

        System.out.println("║                                        ║");
        System.out.println("╚════════════════════════════════════════╝\n");

        // Small pause for effect
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // Ignore
        }
    }

    // Main menu
    // Options: Register for new account , Login and , Exit
    private static void showMainMenu() {
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║          MAIN MENU                   ║");
        System.out.println("╠══════════════════════════════════════╣");
        System.out.println("║  1. Register New Account             ║");
        System.out.println("║  2. Login                            ║");
        System.out.println("║  3. Exit                             ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.print("\n→ Choose option: ");

        //To prevent crash if user types letters
        if (!scanner.hasNextInt()) {
            System.out.println(" Invalid input. Please enter a number.");
            scanner.next(); // it will consume bad inputs
            return;
        }

        int menuChoice = scanner.nextInt();

        switch (menuChoice) {
            case 1:
                handleRegistration();
                break;
            case 2:
                handleLogin();
                break;
            case 3:
                System.out.println("\n╔══════════════════════════════════════╗");
                System.out.println("║  Thank you for using our service!    ║");
                System.out.println("║            Goodbye!                  ║");
                System.out.println("╚══════════════════════════════════════╝\n");
                System.exit(0);
            default:
                System.out.println(" Invalid option! Please choose 1-3.");
        }
    }

    // Account menu :- checkBalance, deposit money , withdraw and logout
    private static void accountMenu() {
        System.out.println("\n═════════════════════════════════════════");
        System.out.println("│         ACCOUNT MENU                  │");
        System.out.println("       Welcome, " + currentUser.getFullName() + "!");
        System.out.println("═════════════════════════════════════════");
        System.out.println("│   1.    Check Balance                 │");
        System.out.println("│   2.    Deposit Money                 │");
        System.out.println("│   3.    Withdraw Money                │");
        System.out.println("│   4.    Transfer Money                │");
        System.out.println("│   5.    Transaction History           │");
        System.out.println("│   6.    Logout                        │");
        System.out.println("═════════════════════════════════════════");
        System.out.print("\n→ Choose option: ");


        if (!scanner.hasNextInt()) {
            System.out.println(" Invalid input.");
            scanner.next();
            return;
        }

        int aMenuChoice = scanner.nextInt();

        switch (aMenuChoice) {
            case 1:
                double balance = service.getBalance(currentUser.getAccountNumber());
                System.out.println("\n┌─────────────────────────────────────┐");
                System.out.println("│       BALANCE INQUIRY               │");
                System.out.println("├─────────────────────────────────────┤");
                System.out.println("│  Account: " + currentUser.getAccountNumber());
                System.out.println("│  Balance: ₹" + String.format("%.2f", balance));
                System.out.println("└─────────────────────────────────────┘");
                break;

            case 2:
                System.out.println("\n┌─────────────────────────────────────┐");
                System.out.println("│       DEPOSIT TRANSACTION           │");
                System.out.println("└─────────────────────────────────────┘");
                System.out.print("→ Enter amount to deposit: ₹");
                if (scanner.hasNextDouble()) {
                    double depositAmount = scanner.nextDouble();
                    if (service.deposit(currentUser.getAccountNumber(), depositAmount)) {
                        double newBal = service.getBalance(currentUser.getAccountNumber());
                        System.out.println("\n Deposit Successful!");
                        System.out.println("   Amount Deposited: ₹" + String.format("%.2f", depositAmount));
                        System.out.println("   New Balance: ₹" + String.format("%.2f", newBal));
                    } else {
                        System.out.println(" Deposit failed! Amount must be positive.");
                    }
                } else {
                    System.out.println(" Invalid amount!");
                    scanner.next();
                }
                break;

            case 3:
                System.out.println("\n┌─────────────────────────────────────┐");
                System.out.println("│       WITHDRAWAL TRANSACTION        │");
                System.out.println("└─────────────────────────────────────┘");
                System.out.print("→ Enter amount to withdraw: ₹");
                if (scanner.hasNextDouble()) {
                    double withdrawAmount = scanner.nextDouble();
                    if (service.withdraw(currentUser.getAccountNumber(), withdrawAmount)) {
                        double newBal = service.getBalance(currentUser.getAccountNumber());
                        System.out.println("\n Withdrawal Successful!");
                        System.out.println("   Amount Withdrawn: ₹" + String.format("%.2f", withdrawAmount));
                        System.out.println("   New Balance: ₹" + String.format("%.2f", newBal));
                    } else {
                        System.out.println(" Withdrawal failed! Check balance or amount.");
                    }
                } else {
                    System.out.println(" Invalid amount!");
                    scanner.next();
                }
                break;
                // For transfer options
            case 4:
                handleTransfer();
                break;
            case 5:  // Transaction History
                loadingEffect("\n⏳ Fetching your transaction history.");
                service.showTransactionHistory(currentUser.getAccountNumber());
                break;

            case 6:
                currentUser = null;
                System.out.println("\n Logged out successfully!");
                System.out.println("   Returning to main menu...\n");
                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {}
                break;

            default:
                System.out.println(" Invalid option! Please choose 1-6.");
        }
    }

    // login and Registration handlers
    // to handle user login authentication
    private static void handleLogin() {
        System.out.println("\n════════════════════════════════════════");
        System.out.println("           LOGIN TO YOUR ACCOUNT");
        System.out.println("════════════════════════════════════════\n");

        System.out.print("→ Account Number: ");
        if (!scanner.hasNextInt()) {
            System.out.println(" Account Number must be numeric.");
            scanner.next();
            return;
        }
        int accNum = scanner.nextInt();

        System.out.print("→ PIN: ");
        String pin = scanner.next();

        loadingEffect("⏳ Verifying credentials");

        currentUser = service.login(accNum, pin);

        if (currentUser != null) {
            System.out.println("      Login Successful!");
            System.out.println("   Welcome back, " + currentUser.getFullName() + "! \n");
            try {
                Thread.sleep(800);
            } catch (InterruptedException e) {}
        } else {
            System.out.println(" Oops! That doesn't match our records. Please check your credentials!");
        }
    }

    // registration menu for new users , and will take infos like name , email, and pin
    private static void handleRegistration() {
        System.out.println("\n════════════════════════════════════════");
        System.out.println("     NEW ACCOUNT REGISTRATION");
        System.out.println("════════════════════════════════════════\n");

        scanner.nextLine(); // Clear buffer

        System.out.print("→ Full Name: ");
        String name = scanner.nextLine();

        System.out.print("→ Email: ");
        String email = scanner.nextLine();

        System.out.print("→ Create 4-digit PIN: ");
        String pin = scanner.next();

        System.out.print("→ Confirm PIN: ");
        String confirmPin = scanner.next();
        // to confirm the actual pin
        if (!pin.equals(confirmPin)) {
            System.out.println("\n PINs don't match! Registration cancelled.");
            return;
        }

        loadingEffect("⏳ Creating your account");
        int accountNumber = service.registerAccount(name, email, pin);

        //direct login after account registration
        if (accountNumber != -1) {
            System.out.print("\n Would you like to login now? (y/n): ");
            String choice = scanner.next();
            if (choice.equalsIgnoreCase("y")) {
                currentUser = service.login(accountNumber, pin);
                if (currentUser != null) {
                    System.out.println(" Logged in successfully!");
                }
            } else {
                System.out.println(" Registration complete! You can login from the main menu.");
            }
        }
    }

    // to handle the transfers of amount b/w acc.s
    private static void handleTransfer() {
        System.out.println("\n┌─────────────────────────────────────┐");
        System.out.println("│       MONEY TRANSFER                │");
        System.out.println("└─────────────────────────────────────┘");

        //showing the current balance
        double currentBalance = service.getBalance(currentUser.getAccountNumber());
        System.out.println("Your Current Balance: ₹" + String.format("%.2f", currentBalance));
        System.out.println();

        //getting the receiver's account
        System.out.print("→ Enter Receiver's Account Number: ");
        if (!scanner.hasNextInt()) {
            System.out.println("Account number must be numeric!");
            scanner.next();
            return;
        }
        int receiverAccount = scanner.nextInt();

        // preventing self-transfers early
        if (receiverAccount == currentUser.getAccountNumber()) {
            System.out.println("You cannot transfer money to yourself!");
            return;
        }

        //getting the amt.
        System.out.print("→ Enter Amount to Transfer: ₹");
        if (!scanner.hasNextDouble()) {
            System.out.println("Invalid amount!");
            scanner.next();
            return;
        }
        double transferAmount = scanner.nextDouble();

        //just confirming the transaction like actual one's
        System.out.println("\n!!!!!! TRANSFER CONFIRMATION !!!!!!");
        System.out.println("─────────────────────────────────────");
        System.out.println("From Account: " + currentUser.getAccountNumber());
        System.out.println("To Account: " + receiverAccount);
        System.out.println("Amount: ₹" + String.format("%.2f", transferAmount));
        System.out.println("─────────────────────────────────────");
        System.out.print("Confirm transfer? (y/n): ");

        String confirmation = scanner.next();
        if (!confirmation.equalsIgnoreCase("y")) {
            System.out.println("\nTransfer cancelled.");
            return;
        }

        //Processing transfer with the loading effect
        loadingEffect("\n⏳ Processing transfer");

        if (service.transfer(currentUser.getAccountNumber(), receiverAccount, transferAmount)) {
            double newBalance = service.getBalance(currentUser.getAccountNumber());

            System.out.println("\n┌─────────────────────────────────────┐");
            System.out.println("│       TRANSFER SUCCESSFUL           │");
            System.out.println("├─────────────────────────────────────┤");
            System.out.println("│  Amount Transferred: ₹" + String.format("%.2f", transferAmount));
            System.out.println("│  To Account: " + receiverAccount);
            System.out.println("│  Your New Balance: ₹" + String.format("%.2f", newBalance));
            System.out.println("└─────────────────────────────────────┘");
        }
        //error messages are already handled in the service method so need here
    }

    // for a quick loading Effect
    private static void loadingEffect(String message) {
        System.out.print(message);
        for (int i = 0; i < 5; i++) {
            try {
                Thread.sleep(300);
                System.out.print(".");
            } catch (InterruptedException e) {}
        }
        System.out.println();
    }


    // MAIN method
    public static void main(String[] args) {
        // Show welcome banner once
        welcomeBanner();

        // Main application loop
        while (true) {
            if (currentUser == null) {
                showMainMenu();
            } else {
                accountMenu();
            }
        }
    }



}