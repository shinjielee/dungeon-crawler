package model;

public enum Tag {
    // Races
    HUMAN("人類"),
    UNDEAD("亡靈"),
    ELF("精靈"),
    BEAST("野獸"),
    // Classes
    WARRIOR("戰士"),
    MAGE("法師"),
    ASSASSIN("刺客"),
    RANGER("遊俠"),
    SUPPORT("輔助");

    public final String displayName;

    Tag(String displayName) {
        this.displayName = displayName;
    }
}
