package model;

public enum AbilityType {
    AOE_MAGIC, // 範圍魔法傷害
    AOE_PHYSICAL, // 範圍物理傷害
    SINGLE_MAGIC, // 單體魔法傷害
    SINGLE_PHYSICAL, // 單體物理傷害
    CC_STUN, // 眩暈
    CC_FREEZE, // 冰凍
    HEAL, // 治療
    BUFF_SELF, // 自身增益
    BUFF_TEAM, // 全隊增益
    DEBUFF, // 減益
    PIERCE // 穿刺
}
