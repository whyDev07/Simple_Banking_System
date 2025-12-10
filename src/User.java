public class User {
    private int accountNumber;
    private String fullName;
    private String email;
    private double balance;

    public User(int accountNumber, String fullName ,String email ,double balance){
        this.accountNumber = accountNumber;
        this.fullName = fullName;
        this.email = email;
        this.balance = balance;
    }

    //GEtters
    public  int getAccountNumber(){
        return  accountNumber;
    }
    public  String getFullName(){
        return  fullName;
    }
    public String getEmail(){
        return email;
    }
    public double getBalance() {
        return balance;
    }
    public void setBalance(double balance){
        this.balance = balance;
    }
}
