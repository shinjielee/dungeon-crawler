package model;

public class Synergy {
    public final String name;
    public final Tag tag;
    public final int[] thresholds; // e.g. {2, 4, 6}
    public final String[] descriptions; // effect at each threshold

    // Runtime state
    public int currentCount;
    public int activeLevel; // which threshold is currently active (0=none, 1=first, etc.)

    public Synergy(String name, Tag tag, int[] thresholds, String[] descriptions) {
        this.name = name;
        this.tag = tag;
        this.thresholds = thresholds;
        this.descriptions = descriptions;
        this.currentCount = 0;
        this.activeLevel = 0;
    }

    public void update(int count) {
        this.currentCount = count;
        this.activeLevel = 0;
        for (int i = thresholds.length - 1; i >= 0; i--) {
            if (count >= thresholds[i]) {
                this.activeLevel = i + 1;
                break;
            }
        }
    }

    public boolean isActive() {
        return activeLevel > 0;
    }

    public String getActiveDescription() {
        if (activeLevel == 0)
            return "";
        return descriptions[activeLevel - 1];
    }

    public int getNextThreshold() {
        if (activeLevel >= thresholds.length)
            return -1;
        return thresholds[activeLevel];
    }
}
