public class JournalEntry {
    private String date;
    private String content;
    private String weather;
    private String mood;

    public JournalEntry(String date, String content, String weather, String mood) {
        this.date = date;
        this.content = content;
        this.weather = weather;
        this.mood = mood;
    }

    public String getDate() { return date; }
    public String getContent() { return content; }
    public String getWeather() { return weather; }
    public String getMood() { return mood; }
    
    public void setContent(String content) { this.content = content; }
}