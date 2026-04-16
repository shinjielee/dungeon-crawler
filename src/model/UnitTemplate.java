package model;

import java.awt.Color;

public class UnitTemplate {
    public final String id;
    public final String name;
    public final Tag[] tags;
    public final int rarity; // 1-5 (also cost)
    public final int baseHp;
    public final int baseAp;
    public final int baseArmor;
    public final int baseMagicResist;
    public final double baseAttackSpeed; // attacks per second
    public final int attackRange; // in grid cells (1=melee)
    public final double baseCritChance;
    public final double baseDodgeChance;
    public final boolean preferBackline; // assassins target backline
    public final boolean isMagicDamage; // basic attack is magic
    public final String abilityName;
    public final String abilityDesc;
    public final AbilityType abilityType;
    public final int manaRequired; // 0-100
    public final double abilityPower; // multiplier to AP
    public final int abilityRadius; // AOE radius in cells
    public final Color color;
    public final String symbol; // 2-char display symbol

    public UnitTemplate(String id, String name, Tag[] tags, int rarity,
            int baseHp, int baseAp, int baseArmor, int baseMagicResist,
            double baseAttackSpeed, int attackRange,
            double baseCritChance, double baseDodgeChance,
            boolean preferBackline, boolean isMagicDamage,
            String abilityName, String abilityDesc,
            AbilityType abilityType, int manaRequired,
            double abilityPower, int abilityRadius,
            Color color, String symbol) {
        this.id = id;
        this.name = name;
        this.tags = tags;
        this.rarity = rarity;
        this.baseHp = baseHp;
        this.baseAp = baseAp;
        this.baseArmor = baseArmor;
        this.baseMagicResist = baseMagicResist;
        this.baseAttackSpeed = baseAttackSpeed;
        this.attackRange = attackRange;
        this.baseCritChance = baseCritChance;
        this.baseDodgeChance = baseDodgeChance;
        this.preferBackline = preferBackline;
        this.isMagicDamage = isMagicDamage;
        this.abilityName = abilityName;
        this.abilityDesc = abilityDesc;
        this.abilityType = abilityType;
        this.manaRequired = manaRequired;
        this.abilityPower = abilityPower;
        this.abilityRadius = abilityRadius;
        this.color = color;
        this.symbol = symbol;
    }
}
