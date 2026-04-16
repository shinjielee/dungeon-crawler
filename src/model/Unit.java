package model;

import game.GameConstants;

public class Unit {
    private static int idCounter = 0;

    public final int uid;
    public final UnitTemplate template;
    public int starLevel; // 1, 2, 3

    // Grid position (-1 = on bench / in shop)
    public int gridX = -1, gridY = -1;

    // Pixel position for smooth animation
    public double drawX, drawY;

    // Runtime stats (after star scaling + synergy + item)
    public double maxHp, hp;
    public double ap;
    public double armor;
    public double magicResist;
    public double attackSpeed; // attacks per second
    public int attackRange;
    public double critChance;
    public double dodgeChance;
    public double lifeSteal; // fraction
    public double armorPen; // fraction
    public double magicPen; // fraction
    public boolean thornArmor; // reflect damage
    public boolean hasRebirth; // can revive
    public boolean hasRevived; // already revived
    public int bonusHp; // extra hp from items

    // Mana
    public double mana;
    public double maxMana = 100;

    // Combat state
    public boolean isPlayerUnit;
    public boolean isAlive = true;
    public double attackCooldown = 0; // seconds until next attack
    public double moveCooldown = 0;
    public double stunDuration = 0; // seconds remaining stunned
    public double frozenDuration = 0; // seconds remaining frozen
    public double buffDuration = 0; // generic buff timer
    public double debuffArmorReduction = 0; // flat armor reduction from debuff
    public double debuffDuration = 0;
    public boolean isInvisible = false;

    public Item equippedItem;

    // Synergy-applied buffs (reset each battle)
    public int abilityRadiusBonus = 0;
    public double critDamageBonus = 0; // extra crit damage fraction
    public double critArmorReduction = 0; // reduce target armor on crit
    public boolean piercing = false; // ranger: pierce one extra enemy
    public boolean beastRage = false; // beast: extra AS when low HP
    public boolean supportRegen = false; // support: regen HP per tick

    // For animation
    public int targetGridX = -1, targetGridY = -1;
    public boolean isAttacking = false;
    public double attackAnimTimer = 0;
    public boolean isCasting = false;
    public double castAnimTimer = 0;
    public int lastDamageFlash = 0; // frames of red flash remaining

    public Unit(UnitTemplate template, int starLevel, boolean isPlayerUnit) {
        this.uid = idCounter++;
        this.template = template;
        this.starLevel = starLevel;
        this.isPlayerUnit = isPlayerUnit;
        applyBaseStats();
    }

    public void applyBaseStats() {
        double starMult = getStarMultiplier();
        double apMult = getStarApMultiplier();
        this.maxHp = template.baseHp * starMult + bonusHp;
        this.hp = this.maxHp;
        this.ap = template.baseAp * apMult;
        this.armor = template.baseArmor;
        this.magicResist = template.baseMagicResist;
        this.attackSpeed = template.baseAttackSpeed;
        this.attackRange = template.attackRange;
        this.critChance = template.baseCritChance;
        this.dodgeChance = template.baseDodgeChance;
        this.lifeSteal = 0;
        this.armorPen = 0;
        this.magicPen = 0;
        this.thornArmor = false;
        this.hasRebirth = false;
    }

    private double getStarMultiplier() {
        return switch (starLevel) {
            case 1 -> 1.0;
            case 2 -> 2.0;
            case 3 -> 4.0;
            default -> 1.0;
        };
    }

    private double getStarApMultiplier() {
        return switch (starLevel) {
            case 1 -> 1.0;
            case 2 -> 1.8;
            case 3 -> 3.2;
            default -> 1.0;
        };
    }

    public void applyItem() {
        if (equippedItem == null)
            return;
        switch (equippedItem.effect) {
            case BONUS_AP -> ap += equippedItem.value;
            case BONUS_ARMOR -> armor += equippedItem.value;
            case BONUS_MAGIC_RESIST -> magicResist += equippedItem.value;
            case BONUS_ATTACK_SPEED -> attackSpeed *= (1.0 + equippedItem.value);
            case LIFESTEAL -> lifeSteal += equippedItem.value;
            case THORN_ARMOR -> thornArmor = true;
            case REBIRTH -> {
                hasRebirth = true;
                hasRevived = false;
            }
            case BONUS_HP -> {
                bonusHp = (int) equippedItem.value;
                maxHp += equippedItem.value;
                hp = Math.min(hp + equippedItem.value, maxHp);
            }
            case ARMOR_PEN -> armorPen += equippedItem.value;
            case MAGIC_PEN -> magicPen += equippedItem.value;
        }
    }

    /** Reset combat-only state for a new battle */
    public void resetForCombat() {
        applyBaseStats();
        if (equippedItem != null)
            applyItem();
        hp = maxHp;
        mana = 0;
        isAlive = true;
        attackCooldown = 1.0 / attackSpeed;
        moveCooldown = 0.4;
        stunDuration = 0;
        frozenDuration = 0;
        buffDuration = 0;
        debuffArmorReduction = 0;
        debuffDuration = 0;
        isInvisible = false;
        hasRevived = false;
        abilityRadiusBonus = 0;
        critDamageBonus = 0;
        critArmorReduction = 0;
        piercing = false;
        beastRage = false;
        supportRegen = false;
        isAttacking = false;
        isCasting = false;
        lastDamageFlash = 0;
        targetGridX = -1;
        targetGridY = -1;
    }

    public double getHpPercent() {
        return maxHp > 0 ? hp / maxHp : 0;
    }

    public double getManaPercent() {
        return mana / maxMana;
    }

    public int getSellValue() {
        int cost = GameConstants.UNIT_COST[template.rarity - 1];
        return Math.max(1, cost * starLevel / 2);
    }

    public String getStarString() {
        return switch (starLevel) {
            case 1 -> "★";
            case 2 -> "★★";
            case 3 -> "★★★";
            default -> "";
        };
    }

    public String getTagString() {
        var sb = new StringBuilder();
        for (int i = 0; i < template.tags.length; i++) {
            if (i > 0)
                sb.append("/");
            sb.append(template.tags[i].displayName);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return template.name + " " + getStarString();
    }
}
