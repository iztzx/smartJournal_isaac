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

    public static void initializeDatabase() {
        System.out.println("[DbManager] Initializing database schema...");
        try (Connection conn = getConnection()) {
            if (conn == null)
                return;

            // Read schema.sql from resources
            String schemaSql = loadSchemaSql();
            if (schemaSql == null || schemaSql.isEmpty()) {
                System.err.println("[DbManager] schema.sql not found or empty.");
                return;
            }

            // Split by semicolon (naive split, but suffices for simple schema files)
            // Ideally use a dedicated runner, but for this app a simple split works
            // Note: valid sql blocks like DO $$ ... $$; contain semicolons inside.
            // Better approach: Execute the whole file as one block if DB supports it, or
            // specific delimiter.
            // Postgres JDBC driver can handle multiple statements if allowMultiQueries=true
            // (check connection string)
            // Or we just execute the whole string.

            try (java.sql.Statement stmt = conn.createStatement()) {
                stmt.execute(schemaSql);
                System.out.println("[DbManager] Database schema initialized successfully.");
            }
        } catch (Exception e) {
            System.err.println("[DbManager] Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String loadSchemaSql() {
        try (java.io.InputStream is = DbManager.class.getResourceAsStream("/schema.sql")) {
            if (is == null)
                return null;
            return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            e.printStackTrace();
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