package engine;

import model.*;
import java.awt.Color;
import java.util.*;

public class DataManager {
    private static DataManager instance;
    private final List<UnitTemplate> allUnits = new ArrayList<>();
    private final List<Item> allItems = new ArrayList<>();
    private final List<Synergy> allSynergies = new ArrayList<>();

    private DataManager() {
        initUnits();
        initItems();
        initSynergies();
    }

    public static DataManager get() {
        if (instance == null)
            instance = new DataManager();
        return instance;
    }

    private void initUnits() {
        // ── Rarity 1 ──────────────────────────────────────────────────────────
        allUnits.add(new UnitTemplate("knight", "騎士",
                new Tag[] { Tag.HUMAN, Tag.WARRIOR }, 1,
                620, 60, 40, 20, 0.7, 1, 0.10, 0.05, false, false,
                "震擊", "眩暈相鄰敵人1.5秒，並造成120%AP物理傷害",
                AbilityType.CC_STUN, 80, 1.2, 1,
                new Color(70, 130, 180), "騎"));

        allUnits.add(new UnitTemplate("archer", "弓箭手",
                new Tag[] { Tag.ELF, Tag.RANGER }, 1,
                420, 75, 15, 20, 1.0, 4, 0.12, 0.08, false, false,
                "連射", "向前方同列射出3箭各造成80%AP傷害",
                AbilityType.PIERCE, 80, 0.8, 0,
                new Color(34, 139, 34), "弓"));

        allUnits.add(new UnitTemplate("rogue", "遊俠",
                new Tag[] { Tag.HUMAN, Tag.ASSASSIN }, 1,
                380, 95, 15, 15, 0.9, 1, 0.25, 0.10, true, false,
                "背刺", "瞬移至敵方後排造成200%AP傷害，降低目標30%護甲3秒",
                AbilityType.DEBUFF, 80, 2.0, 0,
                new Color(105, 105, 105), "刺"));

        // ── Rarity 2 ──────────────────────────────────────────────────────────
        allUnits.add(new UnitTemplate("wizard", "法師",
                new Tag[] { Tag.ELF, Tag.MAGE }, 2,
                500, 120, 10, 35, 0.7, 3, 0.08, 0.08, false, true,
                "火球術", "對目標3×3區域造成150%AP魔法傷害",
                AbilityType.AOE_MAGIC, 85, 1.5, 1,
                new Color(148, 0, 211), "法"));

        allUnits.add(new UnitTemplate("berserker", "狂戰士",
                new Tag[] { Tag.BEAST, Tag.WARRIOR }, 2,
                720, 105, 25, 15, 1.0, 1, 0.15, 0.05, false, false,
                "狂怒", "自身攻擊速度+120%，持續4秒",
                AbilityType.BUFF_SELF, 80, 0, 0,
                new Color(220, 80, 30), "狂"));

        allUnits.add(new UnitTemplate("paladin", "聖騎士",
                new Tag[] { Tag.HUMAN, Tag.WARRIOR, Tag.SUPPORT }, 2,
                800, 55, 55, 45, 0.6, 1, 0.08, 0.05, false, false,
                "神聖之光", "治療生命最低的友方單位200%AP HP",
                AbilityType.HEAL, 75, 2.0, 0,
                new Color(255, 215, 0), "聖"));

        // ── Rarity 3 ──────────────────────────────────────────────────────────
        allUnits.add(new UnitTemplate("necromancer", "死靈法師",
                new Tag[] { Tag.UNDEAD, Tag.MAGE }, 3,
                560, 140, 10, 40, 0.75, 3, 0.08, 0.08, false, true,
                "死亡波動", "對一列敵人造成130%AP魔法傷害並減速2秒",
                AbilityType.AOE_MAGIC, 90, 1.3, 0,
                new Color(80, 0, 120), "死"));

        allUnits.add(new UnitTemplate("shadow", "暗影",
                new Tag[] { Tag.UNDEAD, Tag.ASSASSIN }, 3,
                510, 130, 20, 30, 0.95, 1, 0.35, 0.20, true, false,
                "暗影步", "瞬移至後排敵人，造成260%AP傷害，隱身1.5秒",
                AbilityType.SINGLE_PHYSICAL, 85, 2.6, 0,
                new Color(20, 20, 50), "影"));

        allUnits.add(new UnitTemplate("sniper", "狙擊手",
                new Tag[] { Tag.ELF, Tag.RANGER }, 3,
                540, 145, 15, 25, 0.65, 6, 0.18, 0.12, false, false,
                "爆裂箭", "超遠程對目標造成220%AP傷害，並波及相鄰單位100%AP傷害",
                AbilityType.AOE_PHYSICAL, 85, 2.2, 1,
                new Color(107, 142, 35), "狙"));

        // ── Rarity 4 ──────────────────────────────────────────────────────────
        allUnits.add(new UnitTemplate("guardian", "守護者",
                new Tag[] { Tag.HUMAN, Tag.WARRIOR }, 4,
                1300, 80, 90, 55, 0.5, 1, 0.08, 0.05, false, false,
                "戰爭怒吼", "全體友軍護甲+40，攻擊力+30%，持續5秒",
                AbilityType.BUFF_TEAM, 80, 0, 0,
                new Color(150, 150, 160), "守"));

        allUnits.add(new UnitTemplate("sorcerer", "術士",
                new Tag[] { Tag.ELF, Tag.MAGE }, 4,
                610, 175, 15, 35, 0.85, 3, 0.10, 0.10, false, true,
                "暴風雪", "對目標5×5區域造成160%AP魔法傷害並冰凍2秒",
                AbilityType.CC_FREEZE, 90, 1.6, 2,
                new Color(0, 200, 230), "術"));

        // ── Rarity 5 ──────────────────────────────────────────────────────────
        allUnits.add(new UnitTemplate("dragonknight", "龍騎士",
                new Tag[] { Tag.BEAST, Tag.WARRIOR, Tag.MAGE }, 5,
                1100, 170, 65, 65, 0.80, 2, 0.20, 0.08, false, true,
                "龍息", "噴火錐形，對3格敵人造成190%AP魔法傷害",
                AbilityType.AOE_MAGIC, 85, 1.9, 0,
                new Color(255, 120, 30), "龍"));
    }

    private void initItems() {
        allItems.add(new Item("sword", "鐵劍", "+30 攻擊力",
                Item.Effect.BONUS_AP, 30, new Color(180, 180, 190), "⚔"));
        allItems.add(new Item("shield", "鋼盾", "+35 護甲",
                Item.Effect.BONUS_ARMOR, 35, new Color(100, 100, 130), "🛡"));
        allItems.add(new Item("orb", "魔法寶珠", "+40 魔抗",
                Item.Effect.BONUS_MAGIC_RESIST, 40, new Color(180, 100, 255), "◎"));
        allItems.add(new Item("boots", "疾風靴", "+50% 攻擊速度",
                Item.Effect.BONUS_ATTACK_SPEED, 0.5, new Color(255, 200, 50), "↯"));
        allItems.add(new Item("ring", "吸血戒指", "+25% 生命吸取",
                Item.Effect.LIFESTEAL, 0.25, new Color(220, 50, 50), "♥"));
        allItems.add(new Item("thorn", "荊棘甲", "反彈15%物理傷害",
                Item.Effect.THORN_ARMOR, 0.15, new Color(50, 150, 50), "♦"));
        allItems.add(new Item("rebirth", "重生之石", "陣亡後以50%HP復活一次",
                Item.Effect.REBIRTH, 0.5, new Color(255, 255, 100), "✦"));
        allItems.add(new Item("belt", "巨人腰帶", "+300 最大HP",
                Item.Effect.BONUS_HP, 300, new Color(180, 120, 50), "●"));
        allItems.add(new Item("pierce", "穿甲弓", "+25% 護甲穿透",
                Item.Effect.ARMOR_PEN, 0.25, new Color(200, 200, 100), "→"));
        allItems.add(new Item("magicpen", "魔貫杖", "+25% 魔法穿透",
                Item.Effect.MAGIC_PEN, 0.25, new Color(150, 100, 220), "↬"));
    }

    private void initSynergies() {
        allSynergies.add(new Synergy("人類", Tag.HUMAN,
                new int[] { 2, 4 },
                new String[] { "每回合+2金幣", "隊友陣亡時其他人攻擊+25%（5秒）" }));
        allSynergies.add(new Synergy("亡靈", Tag.UNDEAD,
                new int[] { 2, 4 },
                new String[] { "+20% 吸血", "所有單位陣亡後以25%HP復活一次" }));
        allSynergies.add(new Synergy("精靈", Tag.ELF,
                new int[] { 2, 4 },
                new String[] { "+20% 閃避", "+40% 閃避，+20% 攻擊速度" }));
        allSynergies.add(new Synergy("野獸", Tag.BEAST,
                new int[] { 2, 4 },
                new String[] { "+25% 攻擊速度", "HP低於50%時攻速再+50%" }));
        allSynergies.add(new Synergy("戰士", Tag.WARRIOR,
                new int[] { 2, 4, 6 },
                new String[] { "+20% 護甲", "+35% 護甲，+20% HP", "+55% 護甲，嘲諷最近敵人" }));
        allSynergies.add(new Synergy("法師", Tag.MAGE,
                new int[] { 2, 4 },
                new String[] { "+30% 魔法傷害", "+60% 魔法傷害，AOE半徑+1" }));
        allSynergies.add(new Synergy("刺客", Tag.ASSASSIN,
                new int[] { 2, 4 },
                new String[] { "+30% 暴擊傷害", "暴擊降低目標25%護甲（3秒）" }));
        allSynergies.add(new Synergy("遊俠", Tag.RANGER,
                new int[] { 2, 4 },
                new String[] { "+25% 攻擊速度", "攻擊可穿透1個額外敵人" }));
        allSynergies.add(new Synergy("輔助", Tag.SUPPORT,
                new int[] { 2 },
                new String[] { "所有友軍每秒回復3%最大HP" }));
    }

    public List<UnitTemplate> getAllUnits() {
        return allUnits;
    }

    public List<Item> getAllItems() {
        return allItems;
    }

    public List<Synergy> getAllSynergies() {
        return allSynergies;
    }

    public UnitTemplate getById(String id) {
        return allUnits.stream().filter(u -> u.id.equals(id)).findFirst().orElse(null);
    }

    public List<UnitTemplate> getByRarity(int rarity) {
        return allUnits.stream().filter(u -> u.rarity == rarity).toList();
    }
}
