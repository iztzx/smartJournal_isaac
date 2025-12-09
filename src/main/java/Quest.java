public class Quest {
    private String id;
    private String description;
    private int target;
    private int progress;
    private int xpReward;
    private boolean isCompleted;

    public Quest(String id, String description, int target, int xpReward) {
        this.id = id;
        this.description = description;
        this.target = target;
        this.xpReward = xpReward;
        this.progress = 0;
        this.isCompleted = false;
    }

    public void addProgress(int amount) {
        if (isCompleted)
            return;
        this.progress += amount;
        if (this.progress >= this.target) {
            this.progress = this.target;
            this.isCompleted = true;
        }
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public int getTarget() {
        return target;
    }

    public int getProgress() {
        return progress;
    }

    public int getXpReward() {
        return xpReward;
    }

    public boolean isCompleted() {
        return isCompleted;
    }
}
