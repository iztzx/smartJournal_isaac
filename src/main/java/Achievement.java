public class Achievement {
    private String id;
    private String title;
    private String description;
    private String icon; // Text emoji or resource path
    private boolean isUnlocked;

    public Achievement(String id, String title, String description, String icon, boolean isUnlocked) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.isUnlocked = isUnlocked;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }

    public boolean isUnlocked() {
        return isUnlocked;
    }

    public void setUnlocked(boolean unlocked) {
        isUnlocked = unlocked;
    }
}
