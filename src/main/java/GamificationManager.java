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
    public static List<Achievement> getAchievements(User user) {
        List<Achievement> achievements = new ArrayList<>();
        // In reality, query "user_achievements" table
        // For now, hardcoded demo set
        achievements.add(new Achievement("ach_1", "First Step", "Write your first entry", "📝", true));
        achievements.add(new Achievement("ach_7", "Consistency", "7 Day Streak", "🔥", false));
        achievements.add(new Achievement("ach_100", "Century", "Reach Level 100", "💯", false));
        return achievements;
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
