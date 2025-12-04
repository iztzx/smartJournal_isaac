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
            // Maven handles this, so this error means pom.xml is not run or the dependency failed.
        }
    }

    public static Connection getConnection() {
        if (connection == null) {
            // Read credentials from .env
            String dbUrl = EnvLoader.get("DB_URL");
            String dbUser = EnvLoader.get("DB_USER");
            String dbPassword = EnvLoader.get("DB_PASSWORD");
            
            // Defensive check for empty strings
            if (dbUrl == null || dbUrl.isEmpty() || dbUser == null || dbUser.isEmpty() || dbPassword == null || dbPassword.isEmpty()) {
                System.err.println("FATAL: Database credentials missing or empty in .env. Check DB_URL, DB_USER, DB_PASSWORD.");
                return null;
            }

            // CRITICAL FIX ATTEMPT: Construct the full URL string with credentials embedded.
            // This format sometimes resolves parsing issues better than the three-argument method.
            // Assumes DB_URL in .env is: "jdbc:postgresql://host:port/dbname"
            String separator = dbUrl.contains("?") ? "&" : "?";
            String fullUrl = String.format("%s%suser=%s&password=%s&sslmode=require", dbUrl, separator, dbUser, dbPassword);

            try {
                // Use the single-argument connection method
                connection = DriverManager.getConnection(fullUrl); 
                System.out.println("[DbManager] Connected to Supabase (PostgreSQL) successfully.");
            } catch (SQLException e) {
                System.err.println("[DbManager] ERROR: Could not establish database connection.");
                System.err.println("SQL State: " + e.getSQLState());
                System.err.println("Error Code: " + e.getErrorCode());
                System.err.println("Details: " + e.getMessage());
                
                System.err.println("\n--- TROUBLESHOOTING CHECKLIST ---");
                System.err.println("1. Verify Password: The password 'sjFOP@um123' must be correct for port 5432.");
                System.err.println("2. Check .env: Ensure NO whitespace before or after '=' in .env file.");
                System.err.println("3. Supabase Network: Temporarily disable 'Network Restrictions' on Supabase (if active).");
                System.err.println("4. Local Firewall: Ensure local firewall is not blocking outbound port 5432.");
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