package engine;

import model.*;
import java.util.*;

public class SynergyEngine {
    private final List<Synergy> synergies;

    public SynergyEngine() {
        synergies = DataManager.get().getAllSynergies();
    }

    /** Recalculate synergies from deployed units */
    public Map<Tag, Integer> countTags(List<Unit> deployedUnits) {
        Map<Tag, Integer> counts = new EnumMap<>(Tag.class);
        for (Unit u : deployedUnits) {
            if (!u.isAlive)
                continue;
            for (Tag tag : u.template.tags) {
                counts.merge(tag, 1, Integer::sum);
            }
        }
        return counts;
    }

    public void updateSynergies(List<Unit> deployedUnits) {
        Map<Tag, Integer> counts = countTags(deployedUnits);
        for (Synergy s : synergies) {
            int count = counts.getOrDefault(s.tag, 0);
            s.update(count);
        }
    }

    public List<Synergy> getActiveSynergies() {
        return synergies.stream().filter(Synergy::isActive).toList();
    }

    public List<Synergy> getAllSynergies() {
        return synergies;
    }

    /** Apply synergy stat bonuses to a list of units */
    public void applySynergyBuffs(List<Unit> units) {
        for (Synergy s : synergies) {
            if (!s.isActive())
                continue;
            switch (s.tag) {
                case WARRIOR -> applyWarriorBuff(units, s.activeLevel);
                case MAGE -> applyMageBuff(units, s.activeLevel);
                case ASSASSIN -> applyAssassinBuff(units, s.activeLevel);
                case RANGER -> applyRangerBuff(units, s.activeLevel);
                case ELF -> applyElfBuff(units, s.activeLevel);
                case BEAST -> applyBeastBuff(units, s.activeLevel);
                case UNDEAD -> applyUndeadBuff(units, s.activeLevel);
                case HUMAN -> applyHumanBuff(units, s.activeLevel);
                case SUPPORT -> applySupportBuff(units, s.activeLevel);
                default -> {
                }
            }
        }
    }

    private void applyWarriorBuff(List<Unit> units, int level) {
        for (Unit u : units) {
            if (!hasTag(u, Tag.WARRIOR))
                continue;
            if (level >= 1)
                u.armor *= 1.20;
            if (level >= 2) {
                u.armor *= 1.35 / 1.20;
                u.maxHp *= 1.20;
                u.hp = Math.min(u.hp * 1.20, u.maxHp);
            }
            if (level >= 3) {
                u.armor *= 1.55 / 1.35;
            }
        }
    }

    private void applyMageBuff(List<Unit> units, int level) {
        for (Unit u : units) {
            if (!hasTag(u, Tag.MAGE))
                continue;
            if (level >= 1)
                u.ap *= 1.30;
            if (level >= 2) {
                u.ap *= 1.60 / 1.30;
                u.abilityRadiusBonus = 1;
            }
        }
    }

    private void applyAssassinBuff(List<Unit> units, int level) {
        for (Unit u : units) {
            if (!hasTag(u, Tag.ASSASSIN))
                continue;
            if (level >= 1)
                u.critDamageBonus = 0.30;
            if (level >= 2)
                u.critArmorReduction = 0.25;
        }
    }

    private void applyRangerBuff(List<Unit> units, int level) {
        for (Unit u : units) {
            if (!hasTag(u, Tag.RANGER))
                continue;
            if (level >= 1)
                u.attackSpeed *= 1.25;
            if (level >= 2)
                u.piercing = true;
        }
    }

    private void applyElfBuff(List<Unit> units, int level) {
        for (Unit u : units) {
            if (!hasTag(u, Tag.ELF))
                continue;
            if (level >= 1)
                u.dodgeChance += 0.20;
            if (level >= 2) {
                u.dodgeChance += 0.20;
                u.attackSpeed *= 1.20;
            }
        }
    }

    private void applyBeastBuff(List<Unit> units, int level) {
        for (Unit u : units) {
            if (!hasTag(u, Tag.BEAST))
                continue;
            if (level >= 1)
                u.attackSpeed *= 1.25;
            if (level >= 2)
                u.beastRage = true;
        }
    }

    private void applyUndeadBuff(List<Unit> units, int level) {
        for (Unit u : units) {
            if (!hasTag(u, Tag.UNDEAD))
                continue;
            if (level >= 1)
                u.lifeSteal += 0.20;
            if (level >= 2 && !u.hasRebirth) {
                u.hasRebirth = true;
                u.hasRevived = false;
            }
        }
    }

    private void applyHumanBuff(List<Unit> units, int level) {
        // Gold bonus handled in GameState; death bonus is combat-time
        // For now, human buff = nothing pre-combat (handled at runtime)
    }

    private void applySupportBuff(List<Unit> units, int level) {
        for (Unit u : units) {
            u.supportRegen = true;
        }
    }

    private boolean hasTag(Unit u, Tag tag) {
        for (Tag t : u.template.tags)
            if (t == tag)
                return true;
        return false;
    }

    /** How much extra gold per round from Human synergy */
    public int getHumanGoldBonus() {
        for (Synergy s : synergies)
            if (s.tag == Tag.HUMAN && s.isActive())
                return 2;
        return 0;
    }
}
