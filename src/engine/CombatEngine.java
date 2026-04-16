package engine;

import engine.AStarPathfinder.Point;
import game.GameConstants;
import game.GameLogger;
import model.*;
import java.util.*;
import java.util.function.Consumer;

/**
 * Core combat engine. Runs combat logic tick by tick.
 * Each tick = COMBAT_TICK_MS milliseconds.
 */
public class CombatEngine {
    private final Grid grid;
    private final List<Unit> playerUnits;
    private final List<Unit> enemyUnits;
    private Consumer<CombatEvent> eventListener;

    private final Random rng = new Random();
    private static final double LAVA_DAMAGE_PER_TICK = 15.0;
    private static final double SUPPORT_REGEN_RATE = 0.03; // 3% per second
    private static final double TICK_SECONDS = GameConstants.COMBAT_TICK_MS / 1000.0;
    private static final double MOVE_COOLDOWN = 0.5; // seconds per step

    public CombatEngine(Grid grid, List<Unit> playerUnits, List<Unit> enemyUnits) {
        this.grid = grid;
        this.playerUnits = playerUnits;
        this.enemyUnits = enemyUnits;
    }

    public void setEventListener(Consumer<CombatEvent> listener) {
        this.eventListener = listener;
    }

    public enum EventType {
        ATTACK, ABILITY_CAST, UNIT_DIED, UNIT_REVIVE, DAMAGE_DEALT, HEAL, MOVE, STUN, FREEZE
    }

    public static class CombatEvent {
        public final EventType type;
        public final Unit source;
        public final Unit target;
        public final double value;
        public final int targetX, targetY;

        public CombatEvent(EventType type, Unit source, Unit target, double value, int tx, int ty) {
            this.type = type;
            this.source = source;
            this.target = target;
            this.value = value;
            this.targetX = tx;
            this.targetY = ty;
        }
    }

    private void emit(EventType type, Unit src, Unit target, double val, int tx, int ty) {
        if (eventListener != null)
            eventListener.accept(new CombatEvent(type, src, target, val, tx, ty));
    }

    /** Process one combat tick. Returns true if combat is still ongoing. */
    public boolean tick() {
        List<Unit> all = new ArrayList<>();
        all.addAll(playerUnits);
        all.addAll(enemyUnits);

        // 1. Apply lava damage
        for (Unit u : all) {
            if (!u.isAlive)
                continue;
            if (grid.isLava(u.gridX, u.gridY)) {
                dealRawDamage(u, LAVA_DAMAGE_PER_TICK);
            }
        }

        // 2. Apply support regen
        for (Unit u : all) {
            if (!u.isAlive || !u.supportRegen)
                continue;
            double regen = u.maxHp * SUPPORT_REGEN_RATE * TICK_SECONDS;
            u.hp = Math.min(u.hp + regen, u.maxHp);
        }

        // 3. Beast rage: update attack speed when HP low
        for (Unit u : all) {
            if (!u.isAlive || !u.beastRage)
                continue;
            // handled dynamically in getCooldown
        }

        // Shuffle processing order for fairness
        Collections.shuffle(all, rng);

        // 4. Process each unit
        for (Unit u : all) {
            if (!u.isAlive)
                continue;
            processUnit(u, all);
        }

        // 5. Check deaths
        for (Unit u : all) {
            if (u.isAlive && u.hp <= 0) {
                handleDeath(u, all);
            }
        }

        // 6. Decrement flash timers
        for (Unit u : all) {
            if (u.lastDamageFlash > 0)
                u.lastDamageFlash--;
        }

        return isCombatOngoing();
    }

    private void processUnit(Unit u, List<Unit> all) {
        // Tick cooldowns
        u.attackCooldown = Math.max(0, u.attackCooldown - TICK_SECONDS);
        u.moveCooldown = Math.max(0, u.moveCooldown - TICK_SECONDS);
        if (u.stunDuration > 0) {
            u.stunDuration -= TICK_SECONDS;
            return;
        }
        if (u.frozenDuration > 0) {
            u.frozenDuration -= TICK_SECONDS;
            u.attackCooldown += TICK_SECONDS * 0.5; // slowed
            // can still act at half speed
        }
        if (u.debuffDuration > 0)
            u.debuffDuration = Math.max(0, u.debuffDuration - TICK_SECONDS);
        else
            u.debuffArmorReduction = 0;
        if (u.buffDuration > 0)
            u.buffDuration = Math.max(0, u.buffDuration - TICK_SECONDS);

        // Gain mana passively
        u.mana = Math.min(u.maxMana, u.mana + 8 * TICK_SECONDS);

        List<Unit> enemies = getEnemiesOf(u);
        List<Unit> allies = getAlliesOf(u, all);
        if (enemies.isEmpty())
            return;

        // Cast ability if mana full
        if (u.mana >= u.maxMana) {
            castAbility(u, enemies, allies, all);
            u.mana = 0;
            return;
        }

        // Find target
        Unit target = findTarget(u, enemies);
        if (target == null)
            return;

        int dist = AStarPathfinder.manhattan(u.gridX, u.gridY, target.gridX, target.gridY);

        if (dist <= u.attackRange && u.attackCooldown <= 0) {
            // Attack!
            performAttack(u, target, enemies);
        } else if (u.moveCooldown <= 0) {
            // Move toward target
            moveUnit(u, target, all);
        }
    }

    private Unit findTarget(Unit u, List<Unit> enemies) {
        if (u.template.preferBackline) {
            // Assassin: find enemy in furthest row from enemy start
            Unit best = null;
            int bestBackness = -1;
            for (Unit e : enemies) {
                if (!e.isAlive)
                    continue;
                int backness = u.isPlayerUnit ? e.gridY : (GameConstants.GRID_ROWS - 1 - e.gridY);
                if (best == null || backness > bestBackness ||
                        (backness == bestBackness && distanceTo(u, e) < distanceTo(u, best))) {
                    best = e;
                    bestBackness = backness;
                }
            }
            return best;
        }
        // Default: nearest enemy
        Unit nearest = null;
        int bestDist = Integer.MAX_VALUE;
        for (Unit e : enemies) {
            if (!e.isAlive)
                continue;
            int d = AStarPathfinder.manhattan(u.gridX, u.gridY, e.gridX, e.gridY);
            if (d < bestDist) {
                bestDist = d;
                nearest = e;
            }
        }
        return nearest;
    }

    private void moveUnit(Unit u, Unit target, List<Unit> all) {
        Set<Long> blocked = new HashSet<>();
        for (Unit other : all) {
            if (other == u || !other.isAlive)
                continue;
            blocked.add(AStarPathfinder.key(other.gridX, other.gridY));
        }

        // For assassin: teleport directly if possible
        if (u.template.preferBackline && u.mana >= 60) {
            Unit backTarget = findTarget(u, getEnemiesOf(u));
            if (backTarget != null) {
                int bx = backTarget.gridX, by = backTarget.gridY;
                // Try to land adjacent to the target
                int[][] adj = { { bx - 1, by }, { bx + 1, by }, { bx, by - 1 }, { bx, by + 1 } };
                for (int[] pos : adj) {
                    if (grid.isInBounds(pos[0], pos[1]) && grid.isWalkable(pos[0], pos[1])
                            && !blocked.contains(AStarPathfinder.key(pos[0], pos[1]))) {
                        u.gridX = pos[0];
                        u.gridY = pos[1];
                        u.moveCooldown = MOVE_COOLDOWN;
                        emit(EventType.MOVE, u, null, 0, pos[0], pos[1]);
                        return;
                    }
                }
            }
        }

        Point next = AStarPathfinder.nextStep(grid, u.gridX, u.gridY, target.gridX, target.gridY, blocked);
        if (next != null) {
            u.gridX = next.x();
            u.gridY = next.y();
            u.moveCooldown = MOVE_COOLDOWN;
            emit(EventType.MOVE, u, null, 0, next.x(), next.y());
        }
    }

    private void performAttack(Unit u, Unit target, List<Unit> enemies) {
        double effSpeed = u.attackSpeed;
        if (u.beastRage && u.hp < u.maxHp * 0.5)
            effSpeed *= 1.5;
        u.attackCooldown = 1.0 / effSpeed;
        u.isAttacking = true;
        u.attackAnimTimer = 0.3;

        // Dodge check
        if (rng.nextDouble() < target.dodgeChance) {
            emit(EventType.DAMAGE_DEALT, u, target, 0, target.gridX, target.gridY);
            target.lastDamageFlash = 2;
            gainMana(u, 8);
            return;
        }

        double dmg = u.ap;
        boolean isCrit = rng.nextDouble() < u.critChance;
        if (isCrit) {
            dmg *= (2.0 + u.critDamageBonus);
            // Assassin crit: reduce target armor
            if (u.critArmorReduction > 0) {
                target.debuffArmorReduction += target.armor * u.critArmorReduction;
                target.debuffDuration = 3.0;
            }
        }

        double damageDealt;
        if (u.template.isMagicDamage) {
            double effectiveMR = Math.max(0, target.magicResist * (1 - u.magicPen));
            damageDealt = dmg * 100.0 / (100.0 + effectiveMR);
        } else {
            double effectiveArmor = Math.max(0, (target.armor - target.debuffArmorReduction) * (1 - u.armorPen));
            damageDealt = dmg * 100.0 / (100.0 + effectiveArmor);
        }

        // Thorn armor
        if (target.thornArmor && !u.template.isMagicDamage) {
            dealRawDamage(u, damageDealt * 0.15);
        }

        dealRawDamage(target, damageDealt);
        gainMana(u, 12);
        gainMana(target, 10);

        // Lifesteal
        if (u.lifeSteal > 0) {
            double heal = damageDealt * u.lifeSteal;
            u.hp = Math.min(u.hp + heal, u.maxHp);
        }

        // Ranger piercing
        if (u.piercing) {
            for (Unit e : enemies) {
                if (e == target || !e.isAlive)
                    continue;
                if (AStarPathfinder.manhattan(target.gridX, target.gridY, e.gridX, e.gridY) == 1) {
                    dealRawDamage(e, damageDealt * 0.6);
                    break;
                }
            }
        }

        emit(EventType.ATTACK, u, target, damageDealt, target.gridX, target.gridY);
    }

    private void castAbility(Unit u, List<Unit> enemies, List<Unit> allies, List<Unit> all) {
        GameLogger.combat("CAST: " + u.template.name + " [" + u.template.abilityType + "] player=" + u.isPlayerUnit);
        u.isCasting = true;
        u.castAnimTimer = 0.5;
        emit(EventType.ABILITY_CAST, u, null, 0, u.gridX, u.gridY);

        int radius = u.template.abilityRadius + u.abilityRadiusBonus;
        double power = u.ap * u.template.abilityPower;

        AbilityBehavior behavior = ABILITY_DISPATCH.get(u.template.abilityType);
        if (behavior != null)
            behavior.cast(u, u.template.abilityType, radius, power, enemies, allies, all, this);
    }

    private double calcMagicDmg(double dmg, Unit target, Unit attacker) {
        double mR = Math.max(0, target.magicResist * (1 - attacker.magicPen));
        return dmg * 100.0 / (100.0 + mR);
    }

    private double calcPhysDmg(double dmg, Unit target, Unit attacker) {
        double eff = Math.max(0, (target.armor - target.debuffArmorReduction) * (1 - attacker.armorPen));
        return dmg * 100.0 / (100.0 + eff);
    }

    private void dealRawDamage(Unit target, double amount) {
        target.hp -= amount;
        target.lastDamageFlash = 4;
    }

    private void gainMana(Unit u, double amount) {
        u.mana = Math.min(u.maxMana, u.mana + amount);
    }

    private void handleDeath(Unit u, List<Unit> all) {
        // Rebirth check
        if (u.hasRebirth && !u.hasRevived) {
            u.hp = u.maxHp * 0.5;
            u.hasRevived = true;
            u.mana = 0;
            GameLogger.combat("REVIVE: " + u.template.name + " player=" + u.isPlayerUnit);
            emit(EventType.UNIT_REVIVE, u, null, u.hp, u.gridX, u.gridY);
            return;
        }
        GameLogger.combat(
                "DIED: " + u.template.name + " player=" + u.isPlayerUnit + " hp=" + String.format("%.1f", u.hp));
        u.isAlive = false;
        emit(EventType.UNIT_DIED, u, null, 0, u.gridX, u.gridY);

        // Human synergy: death triggers ally AP buff
        for (Unit ally : getAlliesOf(u, all)) {
            if (!ally.isAlive)
                continue;
            for (Tag t : u.template.tags) {
                if (t == Tag.HUMAN) {
                    ally.ap *= 1.25;
                    ally.buffDuration = Math.max(ally.buffDuration, 5.0);
                }
            }
        }
    }

    private List<Unit> getEnemiesOf(Unit u) {
        List<Unit> result = new ArrayList<>();
        List<Unit> base = u.isPlayerUnit ? enemyUnits : playerUnits;
        for (Unit e : base)
            if (e.isAlive)
                result.add(e);
        return result;
    }

    private List<Unit> getAlliesOf(Unit u, List<Unit> all) {
        List<Unit> result = new ArrayList<>();
        for (Unit a : all) {
            if (a != u && a.isAlive && a.isPlayerUnit == u.isPlayerUnit)
                result.add(a);
        }
        return result;
    }

    private int distanceTo(Unit a, Unit b) {
        return AStarPathfinder.manhattan(a.gridX, a.gridY, b.gridX, b.gridY);
    }

    public boolean isCombatOngoing() {
        boolean anyPlayer = playerUnits.stream().anyMatch(u -> u.isAlive);
        boolean anyEnemy = enemyUnits.stream().anyMatch(u -> u.isAlive);
        return anyPlayer && anyEnemy;
    }

    public boolean isPlayerWin() {
        return enemyUnits.stream().noneMatch(u -> u.isAlive);
    }

    // ── Ability Behaviors ─────────────────────────────────────────────────────
    /**
     * Abstract base for all ability implementations. Static nested classes may
     * access private CombatEngine members via the {@code engine} parameter (JLS
     * §6.6.1).
     * To add a new ability type: extend this class, implement {@code cast}, and
     * register the instance in {@code ABILITY_DISPATCH}.
     */
    private abstract static class AbilityBehavior {
        abstract void cast(Unit caster, AbilityType type, int radius, double power,
                List<Unit> enemies, List<Unit> allies, List<Unit> all,
                CombatEngine engine);
    }

    private static final class AoeBehavior extends AbilityBehavior {
        @Override
        void cast(Unit u, AbilityType type, int radius, double power,
                List<Unit> enemies, List<Unit> allies, List<Unit> all, CombatEngine e) {
            Unit main = e.findTarget(u, enemies);
            if (main == null)
                return;
            int cx = main.gridX, cy = main.gridY;
            for (Unit t : enemies) {
                if (!t.isAlive)
                    continue;
                if (AStarPathfinder.manhattan(t.gridX, t.gridY, cx, cy) <= radius) {
                    double dmg = type == AbilityType.AOE_MAGIC
                            ? e.calcMagicDmg(power, t, u)
                            : e.calcPhysDmg(power, t, u);
                    e.dealRawDamage(t, dmg);
                    e.gainMana(t, 8);
                    e.emit(EventType.DAMAGE_DEALT, u, t, dmg, t.gridX, t.gridY);
                }
            }
            if (u.template.id.equals("necromancer")) {
                for (Unit t : enemies)
                    if (t.isAlive && t.gridX == cx)
                        t.frozenDuration = Math.max(t.frozenDuration, 2.0);
            }
            if (u.template.id.equals("dragonknight")) {
                int dir = u.isPlayerUnit ? -1 : 1;
                for (Unit t : enemies) {
                    if (!t.isAlive)
                        continue;
                    int dy = t.gridY - u.gridY;
                    if (Math.abs(t.gridX - u.gridX) <= 1 && dy * dir > 0 && Math.abs(dy) <= 2)
                        e.dealRawDamage(t, e.calcMagicDmg(power * 0.7, t, u));
                }
            }
        }
    }

    private static final class CcStunBehavior extends AbilityBehavior {
        @Override
        void cast(Unit u, AbilityType type, int radius, double power,
                List<Unit> enemies, List<Unit> allies, List<Unit> all, CombatEngine e) {
            for (Unit t : enemies) {
                if (!t.isAlive)
                    continue;
                if (AStarPathfinder.manhattan(u.gridX, u.gridY, t.gridX, t.gridY) <= 1) {
                    double dmg = e.calcPhysDmg(power, t, u);
                    e.dealRawDamage(t, dmg);
                    t.stunDuration = Math.max(t.stunDuration, 1.5);
                    e.emit(EventType.STUN, u, t, dmg, t.gridX, t.gridY);
                }
            }
        }
    }

    private static final class CcFreezeBehavior extends AbilityBehavior {
        @Override
        void cast(Unit u, AbilityType type, int radius, double power,
                List<Unit> enemies, List<Unit> allies, List<Unit> all, CombatEngine e) {
            Unit main = e.findTarget(u, enemies);
            if (main == null)
                return;
            int cx = main.gridX, cy = main.gridY;
            for (Unit t : enemies) {
                if (!t.isAlive)
                    continue;
                if (AStarPathfinder.manhattan(t.gridX, t.gridY, cx, cy) <= radius) {
                    double dmg = e.calcMagicDmg(power, t, u);
                    e.dealRawDamage(t, dmg);
                    t.frozenDuration = Math.max(t.frozenDuration, 2.0);
                    e.emit(EventType.FREEZE, u, t, dmg, t.gridX, t.gridY);
                }
            }
        }
    }

    private static final class HealBehavior extends AbilityBehavior {
        @Override
        void cast(Unit u, AbilityType type, int radius, double power,
                List<Unit> enemies, List<Unit> allies, List<Unit> all, CombatEngine e) {
            Unit lowestAlly = null;
            double lowestPct = Double.MAX_VALUE;
            for (Unit a : allies) {
                if (!a.isAlive)
                    continue;
                double pct = a.hp / a.maxHp;
                if (pct < lowestPct) {
                    lowestPct = pct;
                    lowestAlly = a;
                }
            }
            if (lowestAlly != null) {
                lowestAlly.hp = Math.min(lowestAlly.hp + power, lowestAlly.maxHp);
                e.emit(EventType.HEAL, u, lowestAlly, power, lowestAlly.gridX, lowestAlly.gridY);
            }
        }
    }

    private static final class BuffSelfBehavior extends AbilityBehavior {
        @Override
        void cast(Unit u, AbilityType type, int radius, double power,
                List<Unit> enemies, List<Unit> allies, List<Unit> all, CombatEngine e) {
            u.attackCooldown = 0;
            u.buffDuration = 4.0;
            u.attackSpeed = u.template.baseAttackSpeed * 2.2;
        }
    }

    private static final class BuffTeamBehavior extends AbilityBehavior {
        @Override
        void cast(Unit u, AbilityType type, int radius, double power,
                List<Unit> enemies, List<Unit> allies, List<Unit> all, CombatEngine e) {
            for (Unit a : allies) {
                if (!a.isAlive)
                    continue;
                a.armor += 40;
                a.ap *= 1.30;
                a.buffDuration = Math.max(a.buffDuration, 5.0);
            }
        }
    }

    private static final class DebuffBehavior extends AbilityBehavior {
        @Override
        void cast(Unit u, AbilityType type, int radius, double power,
                List<Unit> enemies, List<Unit> allies, List<Unit> all, CombatEngine e) {
            Unit target = e.findTarget(u, enemies);
            if (target == null)
                return;
            double dmg = e.calcPhysDmg(power, target, u);
            e.dealRawDamage(target, dmg);
            target.debuffArmorReduction += target.armor * 0.30;
            target.debuffDuration = 3.0;
            int[] jumps = { target.gridX + 1, target.gridY, target.gridX - 1, target.gridY };
            for (int i = 0; i < 4; i += 2) {
                if (e.grid.isInBounds(jumps[i], jumps[i + 1]) && e.grid.isWalkable(jumps[i], jumps[i + 1])) {
                    u.gridX = jumps[i];
                    u.gridY = jumps[i + 1];
                    break;
                }
            }
            e.emit(EventType.DAMAGE_DEALT, u, target, dmg, target.gridX, target.gridY);
        }
    }

    private static final class SingleHitBehavior extends AbilityBehavior {
        @Override
        void cast(Unit u, AbilityType type, int radius, double power,
                List<Unit> enemies, List<Unit> allies, List<Unit> all, CombatEngine e) {
            Unit target = e.findTarget(u, enemies);
            if (target == null)
                return;
            double dmg = type == AbilityType.SINGLE_MAGIC
                    ? e.calcMagicDmg(power, target, u)
                    : e.calcPhysDmg(power, target, u);
            e.dealRawDamage(target, dmg);
            u.isInvisible = true;
            u.frozenDuration = 1.5;
            e.emit(EventType.DAMAGE_DEALT, u, target, dmg, target.gridX, target.gridY);
        }
    }

    private static final class PierceBehavior extends AbilityBehavior {
        @Override
        void cast(Unit u, AbilityType type, int radius, double power,
                List<Unit> enemies, List<Unit> allies, List<Unit> all, CombatEngine e) {
            int hits = 0;
            for (Unit t : enemies) {
                if (!t.isAlive || hits >= 3)
                    continue;
                if (AStarPathfinder.manhattan(u.gridX, u.gridY, t.gridX, t.gridY) <= u.attackRange) {
                    double dmg = e.calcPhysDmg(power, t, u);
                    e.dealRawDamage(t, dmg);
                    e.emit(EventType.DAMAGE_DEALT, u, t, dmg, t.gridX, t.gridY);
                    hits++;
                }
            }
            if (u.template.id.equals("sniper") && hits == 0) {
                Unit st = e.findTarget(u, enemies);
                if (st != null) {
                    double dmg = e.calcPhysDmg(u.ap * 2.2, st, u);
                    e.dealRawDamage(st, dmg);
                    for (Unit t : enemies) {
                        if (t == st || !t.isAlive)
                            continue;
                        if (AStarPathfinder.manhattan(st.gridX, st.gridY, t.gridX, t.gridY) == 1)
                            e.dealRawDamage(t, e.calcPhysDmg(u.ap, t, u));
                    }
                    e.emit(EventType.DAMAGE_DEALT, u, st, dmg, st.gridX, st.gridY);
                }
            }
        }
    }

    private static final Map<AbilityType, AbilityBehavior> ABILITY_DISPATCH;
    static {
        Map<AbilityType, AbilityBehavior> m = new EnumMap<>(AbilityType.class);
        AbilityBehavior aoe = new AoeBehavior();
        m.put(AbilityType.AOE_MAGIC, aoe);
        m.put(AbilityType.AOE_PHYSICAL, aoe);
        m.put(AbilityType.CC_STUN, new CcStunBehavior());
        m.put(AbilityType.CC_FREEZE, new CcFreezeBehavior());
        m.put(AbilityType.HEAL, new HealBehavior());
        m.put(AbilityType.BUFF_SELF, new BuffSelfBehavior());
        m.put(AbilityType.BUFF_TEAM, new BuffTeamBehavior());
        m.put(AbilityType.DEBUFF, new DebuffBehavior());
        AbilityBehavior single = new SingleHitBehavior();
        m.put(AbilityType.SINGLE_PHYSICAL, single);
        m.put(AbilityType.SINGLE_MAGIC, single);
        m.put(AbilityType.PIERCE, new PierceBehavior());
        ABILITY_DISPATCH = Collections.unmodifiableMap(m);
    }
}
