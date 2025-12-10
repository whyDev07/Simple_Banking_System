import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/bank_system";
    private static final String USER = "root";
    private static final String PASSWORD = "12345";

    /* By Static it can be direcly get called by its class itself and not by the object
       Also it will be same for every object if its being called by an object */
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.out.println("Connection Failed! Wrong MySQL credentials.");
            e.printStackTrace();
            return null;
        }
    }

    // Testing it
    public static void main(String[] args) {
        Connection conn = getConnection();
        if (conn != null) {
            System.out.println("Connection Successful!");
        }
    }
}