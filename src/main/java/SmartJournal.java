import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import javafx.util.Pair;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class SmartJournal {

    private final ObservableList<JournalEntry> entries = FXCollections.observableArrayList();
    private final ObservableList<JournalEntry> weeklyStats = FXCollections.observableArrayList();

    // Gamification properties bound to UI
    private final IntegerProperty xp = new SimpleIntegerProperty(0);
    private final IntegerProperty level = new SimpleIntegerProperty(1);
    private final IntegerProperty streak = new SimpleIntegerProperty(0);

    // Thread pool for background tasks
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Gson gson = new Gson();

    private User currentUser;

    // Callback for UI
    private Runnable onLevelUpCallback;

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public void setOnLevelUp(Runnable callback) {
        this.onLevelUpCallback = callback;
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    // --- GREETING & QUOTES ---
    public String getGreeting() {
        int hour = LocalTime.now(ZoneId.of("GMT+8")).getHour();
        return (hour < 12) ? "Good Morning" : (hour < 17) ? "Good Afternoon" : "Good Evening";
    }

    public String getDailyQuote(String mood) {
        if (mood == null)
            mood = "Neutral";
        String[] quotes = switch (mood.toLowerCase()) {
            case "positive" -> new String[] {
                    "Keep your face always toward the sunshine—and shadows will fall behind you.",
                    "Success is not final, failure is not fatal: it is the courage to continue that counts."
            };
            case "negative" -> new String[] {
                    "Tough times never last, but tough people do.",
                    "The best way out is always through. Keep going!",
                    "Stars can't shine without darkness."
            };
            default -> new String[] {
                    "Every day is a fresh start.",
                    "Simplicity is the ultimate sophistication.",
                    "Do what you can, with what you have, where you are."
            };
        };
        return quotes[new Random().nextInt(quotes.length)];
    }

    public boolean isThemeUnlocked(String theme) {
        int lvl = level.get();
        return switch (theme) {
            case "Light" -> true;
            case "Dark" -> lvl >= 5;
            case "Nature" -> lvl >= 10;
            case "Ocean" -> lvl >= 15;
            default -> false;
        };
    }

    // --- DATA LOADING ---
    public void loadUserData() {
        if (currentUser == null)
            return;
        executor.submit(() -> {
            int[] stats = JournalManager.loadUserProgress(currentUser);
            Platform.runLater(() -> {
                streak.set(stats[0]);
                xp.set(stats[1]);
                level.set(stats[2]);
            });
        });
    }

    public void loadHistory() {
        if (currentUser == null)
            return;
        executor.submit(() -> {
            List<JournalEntry> history = JournalManager.getRecentEntries(currentUser);
            Platform.runLater(() -> {
                entries.clear();
                entries.addAll(history);
                refreshWeeklyStats(); // NEW: Update stats when history loads
            });
        });
    }

    private void refreshWeeklyStats() {
        LocalDate oneWeekAgo = LocalDate.now().minusDays(7);
        List<JournalEntry> lastWeek = entries.stream()
                .filter(e -> !e.getDate().isBefore(oneWeekAgo))
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .toList();
        weeklyStats.setAll(lastWeek);
    }

    // --- LOGIC ---
    public void processEntry(LocalDate date, String text, String weather) {
        if (text == null || text.trim().isEmpty())
            return;

        executor.submit(() -> {
            boolean isUpdate = false;
            JournalEntry existing = getEntryForDate(date);
            isUpdate = (existing != null);

            String mood = analyzeSentiment(text);
            JournalEntry entryObj = new JournalEntry(date, text, mood, weather);

            // 1. Save Journal Entry (Content)
            JournalManager.saveJournal(currentUser, entryObj);

            // 2. Calculate New Stats Locally
            // Logic: 10 XP for update, 50+ chars for new.
            int xpGained = isUpdate ? 10 : (50 + text.length());
            int currentTotalXp = xp.get();
            int newTotalXp = currentTotalXp + xpGained;

            // Logic: Level = 1 + (XP / 500)
            int currentLvl = level.get();
            int newLvl = GamificationManager.calculateLevel(newTotalXp);

            // Logic: Streak increments only if not updating existing
            int newStreak = streak.get() + (isUpdate ? 0 : 1);

            // 3. SYNC FULL PROGRESS TO DB
            JournalManager.saveUserProgress(currentUser, newStreak, newTotalXp, newLvl);

            boolean finalIsUpdate = isUpdate;
            Platform.runLater(() -> {
                // Update UI List
                if (finalIsUpdate) {
                    entries.removeIf(e -> e.getDate().equals(date));
                }
                entries.add(0, entryObj);
                FXCollections.sort(entries, (a, b) -> b.getDate().compareTo(a.getDate()));

                // Update UI Stats
                xp.set(newTotalXp);
                streak.set(newStreak);
                level.set(newLvl);

                if (newLvl > currentLvl && onLevelUpCallback != null) {
                    onLevelUpCallback.run();
                }
            });
        });
    }

    public JournalEntry getTodayEntry() {
        return getEntryForDate(LocalDate.now());
    }

    public JournalEntry getEntryForDate(LocalDate date) {
        for (JournalEntry entry : entries) {
            if (entry.getDate().equals(date)) {
                return entry;
            }
        }
        return null;
    }

    private String analyzeSentiment(String text) {
        try {
            JsonObject jsonBody = new JsonObject();
            jsonBody.addProperty("inputs", text);

            String responseBody = API.post(API.MOOD_API_URL, gson.toJson(jsonBody));
            if (responseBody == null)
                return "Unknown";
            return parseBestSentiment(responseBody);
        } catch (Exception e) {
            return "Neutral";
        }
    }

    private String parseBestSentiment(String json) {
        String bestLabel = "Neutral";
        double maxScore = -1.0;
        java.util.regex.Pattern pattern = java.util.regex.Pattern
                .compile("\\{\"label\":\"(.*?)\",\"score\":([\\d\\.]+)\\}");
        java.util.regex.Matcher matcher = pattern.matcher(json);
        while (matcher.find()) {
            String label = matcher.group(1);
            try {
                double score = Double.parseDouble(matcher.group(2));
                if (score > maxScore) {
                    maxScore = score;
                    bestLabel = label;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return bestLabel;
    }

    // --- INNER CLASSES ---
    public static class JournalEntry {
        private final LocalDate date;
        private final String content;
        private final String aiMood;
        private final String weather;

        public JournalEntry(LocalDate date, String content, String mood, String weather) {
            this.date = date;
            this.content = content;
            this.aiMood = mood;
            this.weather = weather;
        }

        public LocalDate getDate() {
            return date;
        }

        public String getContent() {
            return content;
        }

        public String getAiMood() {
            return aiMood;
        }

        public String getWeather() {
            return weather;
        }
    }

    // --- GETTERS FOR UI BINDING ---
    public ObservableList<JournalEntry> getEntries() {
        return entries;
    }

    // Wrapper for getWeeklyStats that calculates the correct range
    public ObservableList<JournalEntry> getWeeklyStats() {
        Pair<LocalDate, LocalDate> range = getWeeklyDateRange(currentUser.getStartOfWeek());
        List<JournalEntry> rawStats = JournalManager.getWeeklyStats(currentUser, range.getKey(), range.getValue());
        return FXCollections.observableArrayList(rawStats);
    }

    // Helper: Determine the start and end of the current "week" based on user
    // preference
    public static Pair<LocalDate, LocalDate> getWeeklyDateRange(String startDayStr) {
        LocalDate today = LocalDate.now();

        DayOfWeek startDay = DayOfWeek.SUNDAY; // Default
        try {
            startDay = DayOfWeek.valueOf(startDayStr.toUpperCase());
        } catch (Exception e) {
            /* ignore */ }

        // Logic: Find the most recent occurrence of the startDay (or today if today is
        // startDay)
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(startDay));
        LocalDate weekEnd = today; // Analysis usually up to "now", or could be weekStart.plusDays(6)

        return new Pair<>(weekStart, weekEnd);
    }

    public int getLevel() {
        return level.get();
    }

    public IntegerProperty levelProperty() {
        return level;
    }

    public IntegerProperty streakProperty() {
        return streak;
    }

    public IntegerProperty xpProperty() {
        return xp;
    }
}