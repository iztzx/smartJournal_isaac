import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class JournalManager {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    // Connects only once when the class is loaded
    private static final Connection dbConnection = DbManager.getConnection();

    /**
     * Saves or updates a journal entry in the Supabase database (UPSERT logic).
     */
    public static void saveJournal(User user, JournalEntry entry) {
        if (dbConnection == null) {
            System.err.println("Cannot save journal. Database connection failed.");
            return;
        }

        String sql = "INSERT INTO journals (user_email, entry_date, content, weather, mood) "
                   + "VALUES (?, ?, ?, ?, ?) "
                   + "ON CONFLICT (user_email, entry_date) DO UPDATE "
                   + "SET content = EXCLUDED.content, weather = EXCLUDED.weather, mood = EXCLUDED.mood";

        PreparedStatement ps = null;
        try {
            ps = dbConnection.prepareStatement(sql);
            ps.setString(1, user.getEmail());
            ps.setDate(2, Date.valueOf(entry.getDate()));
            ps.setString(3, entry.getContent());
            ps.setString(4, entry.getWeather());
            ps.setString(5, entry.getMood());
            
            ps.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error saving journal to database: " + e.getMessage());
        } finally {
            DbManager.close(ps);
        }
    }

    /**
     * Loads a journal entry from the Supabase database.
     */
    public static JournalEntry loadJournal(User user, String date) {
        if (dbConnection == null) return null; 

        String sql = "SELECT content, weather, mood FROM journals WHERE user_email = ? AND entry_date = ?";
        
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = dbConnection.prepareStatement(sql);
            ps.setString(1, user.getEmail());
            ps.setDate(2, Date.valueOf(date));
            
            rs = ps.executeQuery();
            
            if (rs.next()) {
                String content = rs.getString("content");
                String weather = rs.getString("weather");
                String mood = rs.getString("mood");
                return new JournalEntry(date, content, weather, mood);
            }
        } catch (SQLException e) {
            System.err.println("Error loading journal from database: " + e.getMessage());
        } finally {
            DbManager.close(rs);
            DbManager.close(ps);
        }
        return null;
    }

    /**
     * Generates a list of dates for the last 8 days (including today).
     */
    public static List<String> listJournalDates(User user) {
        List<String> dates = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        // Add dates from 7 days ago up to today
        for (int i = 7; i >= 0; i--) {
            dates.add(today.minusDays(i).format(DATE_FORMATTER));
        }
        
        return dates;
    }
}