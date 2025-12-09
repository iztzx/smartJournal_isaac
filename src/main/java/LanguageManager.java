import java.util.HashMap;
import java.util.Map;

public class LanguageManager {
    private static String currentLanguage = "English";
    private static final Map<String, Map<String, String>> translations = new HashMap<>();

    static {
        // ENGLISH
        Map<String, String> en = new HashMap<>();
        en.put("app.title", "SmartJournal");
        en.put("menu.profile", "👤 Profile");
        en.put("menu.settings", "⚙ Settings");
        en.put("menu.summary", "📊 Weekly Summary");
        en.put("menu.logout", "🚪 Logout");
        en.put("login.title", "SmartJournal Access");
        en.put("login.signin", "Sign In");
        en.put("login.create", "Create an account");
        en.put("login.register", "Register");
        en.put("login.back", "Already have an account? Sign In");
        en.put("btn.signin", "Sign In");
        en.put("btn.register", "Register");
        en.put("prompt.email", "Email");
        en.put("prompt.password", "Password");
        en.put("prompt.displayname", "Display Name");
        en.put("timeline.welcome", "Welcome");
        en.put("timeline.empty", "No memories yet. Start your journey today!");
        en.put("btn.newentry", "+ New Entry");
        en.put("btn.editentry", "Edit Today's");
        en.put("gamification.progress", "Your Progress");
        en.put("gamification.level", "Current Level");
        en.put("gamification.streak", "🔥 %d Day Streak");
        en.put("gamification.xp", "XP Progress");
        en.put("gamification.quests", "Daily Quests");
        en.put("gamification.achievements", "Achievements");
        en.put("settings.title", "Settings");
        en.put("settings.header", "Personalize Experience");
        en.put("settings.theme", "Theme:");
        en.put("settings.language", "Language:");
        en.put("summary.title", "Weekly Summary");
        en.put("summary.header", "Your Mood Last 7 Days");
        en.put("summary.noentries", "No entries found for the past week. Journal to see analytics!");
        en.put("summary.assessment", "AI Assessment");
        en.put("editor.new", "New Entry");
        en.put("editor.edit", "Edit Entry");
        en.put("editor.prompt.new", "How was your day?");
        en.put("editor.prompt.edit", "Refine your memory:");
        en.put("btn.save", "Save");
        en.put("col.date", "Date");
        en.put("col.mood", "Mood");

        // ACHIEVEMENTS (English)
        en.put("ach.ach_1.title", "First Step");
        en.put("ach.ach_1.desc", "Write your first entry");
        en.put("ach.ach_7.title", "Consistency");
        en.put("ach.ach_7.desc", "7 Day Streak");
        en.put("ach.ach_100.title", "Century");
        en.put("ach.ach_100.desc", "Reach Level 100");

        translations.put("English", en);

        // BAHASA MALAYSIA
        Map<String, String> bm = new HashMap<>();
        bm.put("app.title", "SmartJournal");
        bm.put("menu.profile", "👤 Profil");
        bm.put("menu.settings", "⚙ Tetapan");
        bm.put("menu.summary", "📊 Ringkasan Mingguan");
        bm.put("menu.logout", "🚪 Log Keluar");
        bm.put("login.title", "Akses SmartJournal");
        bm.put("login.signin", "Log Masuk");
        bm.put("login.create", "Cipta akaun");
        bm.put("login.register", "Daftar");
        bm.put("login.back", "Sudah mempunyai akaun? Log Masuk");
        bm.put("btn.signin", "Log Masuk");
        bm.put("btn.register", "Daftar");
        bm.put("prompt.email", "Emel");
        bm.put("prompt.password", "Kata Laluan");
        bm.put("prompt.displayname", "Nama Paparan");
        bm.put("timeline.welcome", "Selamat Datang");
        bm.put("timeline.empty", "Tiada kenangan lagi. Mulakan perjalanan anda hari ini!");
        bm.put("btn.newentry", "Entri Baru");
        bm.put("btn.editentry", "Sunting Hari Ini");
        bm.put("gamification.progress", "Kemajuan Anda");
        bm.put("gamification.level", "Tahap Semasa");
        bm.put("gamification.streak", "🔥 %d Hari Berturut");
        bm.put("gamification.xp", "Kemajuan XP");
        bm.put("gamification.quests", "Misi Harian");
        bm.put("gamification.achievements", "Pencapaian");
        bm.put("settings.title", "Tetapan");
        bm.put("settings.header", "Peribadikan Pengalaman");
        bm.put("settings.theme", "Tema:");
        bm.put("settings.language", "Bahasa:");
        bm.put("summary.title", "Ringkasan Mingguan");
        bm.put("summary.header", "Mood 7 Hari Lepas");
        bm.put("summary.noentries", "Tiada entri minggu lepas. Tulis jurnal untuk lihat analitik!");
        bm.put("summary.assessment", "Penilaian AI");
        bm.put("editor.new", "Entri Baru");
        bm.put("editor.edit", "Sunting Entri");
        bm.put("editor.prompt.new", "Bagaimana hari anda?");
        bm.put("editor.prompt.edit", "Perhalusi memori anda:");
        bm.put("btn.save", "Simpan");
        bm.put("col.date", "Tarikh");
        bm.put("col.mood", "Mood");

        // GREETINGS
        bm.put("Good Morning", "Selamat Pagi");
        bm.put("Good Afternoon", "Selamat Petang");
        bm.put("Good Evening", "Selamat Malam");

        // MOODS
        bm.put("mood.neutral", "Neutral");
        bm.put("mood.positive", "Positif");
        bm.put("mood.negative", "Negatif");

        // QUOTES (Mapping full sentences)
        bm.put("Keep your face always toward the sunshine—and shadows will fall behind you.",
                "Pastikan muka anda menghadap matahari—dan bayang-bayang akan jatuh di belakang anda.");
        bm.put("Success is not final, failure is not fatal: it is the courage to continue that counts.",
                "Kejayaan bukan muktamad, kegagalan bukan penamat: keberanian untuk meneruskan yang penting.");
        bm.put("Tough times never last, but tough people do.",
                "Masa sukar tidak kekal, tetapi orang yang tabah kekal.");
        bm.put("The best way out is always through. Keep going!", "Jalan keluar terbaik adalah melaluinya. Teruskan!");
        bm.put("Stars can't shine without darkness.", "Bintang tidak boleh bersinar tanpa kegelapan.");
        bm.put("Every day is a fresh start.", "Setiap hari adalah permulaan baharu.");
        bm.put("Simplicity is the ultimate sophistication.", "Kesederhanaan adalah kecanggihan yang muktamad.");
        bm.put("Do what you can, with what you have, where you are.",
                "Buat apa yang anda mampu, dengan apa yang anda ada, di mana anda berada.");

        // ACHIEVEMENTS (Bahasa Malaysia)
        bm.put("ach.ach_1.title", "Langkah Pertama");
        bm.put("ach.ach_1.desc", "Tulis entri pertama anda");
        bm.put("ach.ach_7.title", "Konsistensi");
        bm.put("ach.ach_7.desc", "7 Hari Berturut");
        bm.put("ach.ach_100.title", "Abad");
        bm.put("ach.ach_100.desc", "Capai Tahap 100");

        translations.put("Bahasa Malaysia", bm);
    }

    public static String get(String key) {
        if (!translations.containsKey(currentLanguage))
            return key;
        return translations.get(currentLanguage).getOrDefault(key, key);
    }

    public static void setLanguage(String lang) {
        if (translations.containsKey(lang)) {
            currentLanguage = lang;
        }
    }

    public static String getCurrentLanguage() {
        return currentLanguage;
    }
}
