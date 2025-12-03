import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbManager {
    private static Connection connection = null;
    
    static {
        // This is where the PostgreSQL JDBC Driver is loaded
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("FATAL: PostgreSQL JDBC Driver not found. Check your classpath.");
            // Do not print stack trace in final code
        }
    }

    public static Connection getConnection() {
        if (connection == null) {
            // Read credentials from .env
            String dbUrl = EnvLoader.get("DB_URL");
            String dbUser = EnvLoader.get("DB_USER");
            String dbPassword = EnvLoader.get("DB_PASSWORD");
            
            if (dbUrl == null || dbUser == null || dbPassword == null) {
                System.err.println("FATAL: Database credentials missing in .env. Check DB_URL, DB_USER, DB_PASSWORD.");
                return null;
            }

            try {
                // Establishes the connection using credentials
                connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                System.out.println("[DbManager] Connected to Supabase (PostgreSQL) successfully.");
            } catch (SQLException e) {
                // Enhanced error reporting to debug external connection failures
                System.err.println("[DbManager] ERROR: Could not establish database connection.");
                System.err.println("SQL State: " + e.getSQLState());
                System.err.println("Error Code: " + e.getErrorCode());
                System.err.println("Details: " + e.getMessage()); // This will show "The connection attempt failed"
                // Inform user about potential fixes
                System.err.println("TROUBLESHOOTING:");
                System.err.println("1. Check DB_PASSWORD for spaces or typos.");
                System.err.println("2. Check your local firewall is not blocking port 5432.");
                System.err.println("3. Check Supabase firewall/network settings.");
            }
        }
        return connection;
    }
    
    public static void close(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                // Ignore silent close failure
            }
        }
    }
}