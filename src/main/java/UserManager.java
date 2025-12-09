import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class UserManager {
    private Connection dbConnection;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    public UserManager() {
        this.dbConnection = DbManager.getConnection();
    }

    // --- AUTHENTICATION ---
    public User login(String email, String password) {
        if (dbConnection == null)
            return null;

        String hashedPassword = hashPassword(password);
        if (hashedPassword == null)
            return null; // Hashing fail

        String sql = "SELECT email, display_name FROM users WHERE email = ? AND password_hash = ?";
        try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, hashedPassword);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String displayName = rs.getString("display_name");
                    // Return User object with hashed password (or handle it safely)
                    return new User(email, displayName, hashedPassword);
                }
            }
        } catch (SQLException e) {
            System.err.println("Login database error: " + e.getMessage());
        }
        return null;
    }

    public void register(String email, String displayName, String password) {
        if (dbConnection == null)
            throw new RuntimeException("Database connection unavailable.");

        // VALIDATION
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format.");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password too short (<6 chars).");
        }

        if (isUserExist(email))
            throw new IllegalArgumentException("Email already taken.");

        String hashedPassword = hashPassword(password);
        if (hashedPassword == null)
            throw new RuntimeException("Secure hashing failed.");

        String sql = "INSERT INTO users (email, display_name, password_hash) VALUES (?, ?, ?)";
        try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, displayName);
            ps.setString(3, hashedPassword);
            int rows = ps.executeUpdate();
            if (rows <= 0)
                throw new RuntimeException("Database insert failed.");
        } catch (SQLException e) {
            throw new RuntimeException("Registration error: " + e.getMessage());
        }
    }

    public boolean updateProfile(User user, String newName, String newPassword) {
        if (dbConnection == null)
            return false;

        String sql = "UPDATE users SET display_name = ?, password_hash = ? WHERE email = ?";

        String finalPassHash = user.getPassword(); // Default to old hash
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            if (newPassword.length() < 6)
                return false;
            finalPassHash = hashPassword(newPassword);
        }

        try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
            ps.setString(1, newName);
            ps.setString(2, finalPassHash);
            ps.setString(3, user.getEmail());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                user.setDisplayName(newName);
                user.setPassword(finalPassHash);
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // --- HELPERS ---
    private boolean isUserExist(String email) {
        if (dbConnection == null)
            return false;
        try (PreparedStatement ps = dbConnection.prepareStatement("SELECT 1 FROM users WHERE email = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    private String hashPassword(String originalPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(originalPassword.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}