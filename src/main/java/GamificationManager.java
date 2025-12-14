import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class GamificationManager {

    // --- XP & LEVELING ---
    public static int calculateLevel(int xp) {
        // Simple linear progression: Level = 1 + (XP / 500)
        return 1 + (xp / 500);
    }

    public static int getXpNextLevel(int currentLevel) {
        return currentLevel * 500;
    }

    // --- QUESTS ---
    public static List<Quest> getDailyQuests(User user) {
        // In a real app, these would be fetched/generated from DB based on date
        List<Quest> quests = new ArrayList<>();
        quests.add(new Quest("q_entry", "Write a journal entry", 1, 50));
        quests.add(new Quest("q_long", "Write over 100 characters", 1, 100));
        quests.add(new Quest("q_night", "Journal after 8 PM", 1, 150));
        return quests;
    }

    // --- ACHIEVEMENTS ---
    // --- ACHIEVEMENTS ---
    public static List<Achievement> getAchievements(User user) {
        List<Achievement> achievements = new ArrayList<>();
        if (DbManager.getConnection() == null)
            return achievements;

        // Fetch all definitions + unlock status for this user
        // LEFT JOIN ensures we get all achievements, and unlocked_at is non-null if
        // user has it
        String sql = "SELECT ad.id, ad.title, ad.description, ad.icon_char, ua.unlocked_at " +
                "FROM achievement_definitions ad " +
                "LEFT JOIN user_achievements ua ON ad.id = ua.achievement_id AND ua.user_email = ?";

        try (Connection conn = DbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getEmail());
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String title = rs.getString("title");
                    String desc = rs.getString("description");
                    String icon = rs.getString("icon_char");
                    boolean unlocked = rs.getTimestamp("unlocked_at") != null;

                    achievements.add(new Achievement(id, title, desc, icon, unlocked));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return achievements;
    }

    public static void unlockAchievement(User user, String achievementId) {
        // Insert if not exists
        String sql = "INSERT INTO user_achievements (user_email, achievement_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (Connection conn = DbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, achievementId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- DB OPERATIONS ---
    public static void grantXp(User user, int amount) {
        String sql = "UPDATE user_progress SET xp = xp + ? WHERE user_email = ?";
        try (Connection conn = DbManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, amount);
            ps.setString(2, user.getEmail());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
