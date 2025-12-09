import java.sql.Connection;
import java.sql.SQLException;

public class DbManager {

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("FATAL: PostgreSQL JDBC Driver not found.");
        }
    }

    // Old single connection logic removed in favor of Pooling

    public static Connection getConnection() {
        try {
            return DatabaseConnectionPool.getConnection();
        } catch (SQLException e) {
            System.err.println("[DbManager] Could not get connection from pool: " + e.getMessage());
            return null;
        }
    }

    public static void close(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                /* Ignore */ }
        }
    }
}