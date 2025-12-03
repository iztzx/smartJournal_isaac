import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class UserManager {
    private Connection dbConnection;

    public UserManager() {
        // Tries to get connection from DbManager
        this.dbConnection = DbManager.getConnection();
    }

    /**
     * Authenticates a user against the Supabase database.
     */
    public User login(String email, String password) {
        if (dbConnection == null) return null;
        
        // Searches for user with matching email and password (hash)
        String sql = "SELECT email, display_name, password_hash FROM users WHERE email = ? AND password_hash = ?";
        
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = dbConnection.prepareStatement(sql);
            ps.setString(1, email);
            ps.setString(2, password); 
            
            rs = ps.executeQuery();

            if (rs.next()) {
                String displayName = rs.getString("display_name");
                return new User(email, displayName, password);
            }
        } catch (SQLException e) {
            System.err.println("Login database error: " + e.getMessage());
        } finally {
            DbManager.close(rs);
            DbManager.close(ps);
        }
        return null;
    }
    
    /**
     * Registers a new user in the Supabase database.
     */
    public boolean register(String email, String displayName, String password) {
        if (dbConnection == null) return false;

        if (isUserExist(email)) {
            System.out.println("Registration failed: Email already exists.");
            return false;
        }

        // Inserts new user into the 'users' table
        String sql = "INSERT INTO users (email, display_name, password_hash) VALUES (?, ?, ?)";
        
        PreparedStatement ps = null;
        try {
            ps = dbConnection.prepareStatement(sql);
            ps.setString(1, email);
            ps.setString(2, displayName);
            ps.setString(3, password); 
            
            int affectedRows = ps.executeUpdate();
            
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Registration database error: " + e.getMessage());
        } finally {
            DbManager.close(ps);
        }
        return false;
    }
    
    private boolean isUserExist(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ?";
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = dbConnection.prepareStatement(sql);
            ps.setString(1, email);
            rs = ps.executeQuery();
            return rs.next(); 
        } catch (SQLException e) {
            return false; 
        } finally {
            DbManager.close(rs);
            DbManager.close(ps);
        }
    }
}