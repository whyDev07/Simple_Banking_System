public class TestMain {
    public static void main(String[] args) {

        BankingServices bankService = new BankingServices();

        int myAccountNumber = 1;
        String myPin = "1234";

        System.out.println("--- TESTING BANKING APP ---");

        System.out.println("\n1. Testing Login...");
        User user = bankService.login(myAccountNumber, myPin);

        if (user != null) {
            System.out.println(" Login Successful! Welcome " + user.getFullName());
            System.out.println("Current Balance: $" + user.getBalance());
        } else {
            System.out.println(" Login Failed.");
            return;
        }

        System.out.println("\n2. Testing Deposit...");
        boolean depositSuccess = bankService.deposit(myAccountNumber, 500.00);
        if (depositSuccess) {
            System.out.println(" Deposit of $500.00 successful.");
        } else {
            System.out.println(" Deposit failed.");
        }

        System.out.println("\n3. Verifying New Balance...");
        double newBalance = bankService.getBalance(myAccountNumber);
        System.out.println("   New Balance is: $" + newBalance);

        System.out.println("\n4. Testing Withdrawal...");
        boolean withdrawSuccess = bankService.withdraw(myAccountNumber, 200.00);
        if (withdrawSuccess) {
            System.out.println(" Withdraw of $200.00 successful.");
        } else {
            System.out.println(" Withdraw failed.");
        }

        System.out.println("\n5. Final Balance Check...");
        System.out.println("   Final Balance is: $" + bankService.getBalance(myAccountNumber));
    }
}