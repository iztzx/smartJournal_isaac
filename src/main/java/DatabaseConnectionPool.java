import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnectionPool {

    private static HikariDataSource dataSource;

    private static synchronized void initDataSource() {
        if (dataSource != null && !dataSource.isClosed())
            return;

        try {
            String dbUrl = EnvLoader.get("DB_URL");
            String dbUser = EnvLoader.get("DB_USER");
            String dbPassword = EnvLoader.get("DB_PASSWORD");

            if (dbUrl == null || dbUser == null || dbPassword == null) {
                System.err.println("FATAL: Database credentials missing in .env");
                return;
            }

            HikariConfig config = new HikariConfig();

            // Construct JDBC URL with SSL safe defaults for Supabase
            String separator = dbUrl.contains("?") ? "&" : "?";
            String jdbcUrl = String.format("%s%suser=%s&password=%s&sslmode=require", dbUrl, separator, dbUser,
                    dbPassword);

            config.setJdbcUrl(jdbcUrl);
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setIdleTimeout(30000);
            config.setConnectionTimeout(20000);
            config.setDriverClassName("org.postgresql.Driver");

            dataSource = new HikariDataSource(config);
            System.out.println("[DatabaseConnectionPool] Pool initialized successfully.");

        } catch (Exception e) {
            System.err.println("[DatabaseConnectionPool] Initialization Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            initDataSource();
        }
        if (dataSource == null) {
            throw new SQLException("DataSource is null. Check .env configuration.");
        }
        return dataSource.getConnection();
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("[DatabaseConnectionPool] Pool closed.");
        }
    }
}
