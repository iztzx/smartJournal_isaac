public class User {
    private String email;
    private String displayName;
    private String password;

    private String startOfWeek = "SUNDAY"; // Default

    public User(String email, String displayName, String password) {
        this.email = email;
        this.displayName = displayName;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPassword() {
        return password;
    }

    public String getStartOfWeek() {
        return startOfWeek;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setStartOfWeek(String startOfWeek) {
        this.startOfWeek = startOfWeek;
    }
}