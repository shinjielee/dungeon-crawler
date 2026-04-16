package model;

import java.awt.Color;

public class Item {
    public enum Effect {
        BONUS_AP, BONUS_ARMOR, BONUS_MAGIC_RESIST, BONUS_ATTACK_SPEED,
        LIFESTEAL, THORN_ARMOR, REBIRTH, BONUS_HP, ARMOR_PEN, MAGIC_PEN
    }

    public final String id;
    public final String name;
    public final String description;
    public final Effect effect;
    public final double value; // numeric value of the effect
    public final Color color;
    public final String symbol;

    public Item(String id, String name, String description, Effect effect, double value, Color color, String symbol) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.effect = effect;
        this.value = value;
        this.color = color;
        this.symbol = symbol;
    }

    @Override
    public String toString() {
        return name;
    }
}
