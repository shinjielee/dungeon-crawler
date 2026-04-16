package game;

import engine.*;
import model.*;
import java.util.*;

/**
 * Central game state – singleton coordinating all game data.
 */
public class GameState {
    private static GameState instance;

    // ── Round / progress ──────────────────────────────────────────────────────
    public Phase phase = Phase.TITLE;
    public int floor = 1;
    public int playerHp = GameConstants.INITIAL_PLAYER_HP;
    public int maxPlayerHp = GameConstants.INITIAL_PLAYER_HP;
    public int playerGold = 3;
    public int playerLevel = 1;
    public int playerXp = 0;
    public int winStreak = 0;
    public int lossStreak = 0;
    public boolean shopLocked = false;

    // ── Units & field ─────────────────────────────────────────────────────────
    /** Bench: units not on field (max 8) */
    public List<Unit> bench = new ArrayList<>();
    /** Deployed: 8×8 grid, index = y*8 + x */
    public Unit[] deployedUnits = new Unit[64];
    /** Current shop offering */
    public List<UnitTemplate> shopUnits = new ArrayList<>();
    /** Items held by player (not equipped) */
    public List<Item> storedItems = new ArrayList<>();
    /** Enemy team generated for current floor */
    public List<Unit> currentEnemies = new ArrayList<>();
    /** Combat-time copies of player units */
    public List<Unit> combatPlayerUnits = new ArrayList<>();

    // ── UI selection state ────────────────────────────────────────────────────
    public Unit selectedBenchUnit = null; // unit picked from bench, waiting to be placed

    // ── Last combat result ────────────────────────────────────────────────────
    public boolean lastCombatWin = false;
    public int lastGoldEarned = 0;
    public int lastHpLost = 0;
    public String lastResultMessage = "";

    // ── Engines ───────────────────────────────────────────────────────────────
    public final Grid grid = new Grid(GameConstants.GRID_COLS, GameConstants.GRID_ROWS);
    public final SynergyEngine synergyEngine = new SynergyEngine();
    public final ShopEngine shopEngine = new ShopEngine();

    // ── Listeners ─────────────────────────────────────────────────────────────
    private final List<Runnable> stateListeners = new ArrayList<>();

    private GameState() {
    }

    public static GameState get() {
        if (instance == null)
            instance = new GameState();
        return instance;
    }

    public static void reset() {
        instance = new GameState();
    }

    // ── Listeners ─────────────────────────────────────────────────────────────
    public void addStateListener(Runnable r) {
        stateListeners.add(r);
    }

    public void notifyListeners() {
        stateListeners.forEach(Runnable::run);
    }

    // ── Phase transitions ─────────────────────────────────────────────────────
    public void startNewGame() {
        GameLogger.phase("=== NEW GAME STARTED ===");
        phase = Phase.SHOP;
        floor = 1;
        playerHp = GameConstants.INITIAL_PLAYER_HP;
        maxPlayerHp = GameConstants.INITIAL_PLAYER_HP;
        playerGold = 3;
        playerLevel = 1;
        playerXp = 0;
        winStreak = 0;
        lossStreak = 0;
        shopLocked = false;
        bench.clear();
        Arrays.fill(deployedUnits, null);
        storedItems.clear();
        grid.generateTerrain(floor);
        refreshShop();
        notifyListeners();
    }

    public void refreshShop() {
        GameLogger.shop("Shop refreshed (level=" + playerLevel + " locked=" + shopLocked + ")");
        shopUnits = shopEngine.generateShop(playerLevel, shopLocked, shopLocked ? shopUnits : null);
        shopLocked = false;
        notifyListeners();
    }

    public void enterDeploy() {
        GameLogger.phase("=== DEPLOY phase, floor=" + floor + " deployed=" + getDeployedCount() + " ===");
        phase = Phase.DEPLOY;
        // Generate enemy team
        currentEnemies = shopEngine.generateEnemyTeam(floor);
        for (Unit e : currentEnemies) {
            e.resetForCombat();
            e.drawX = e.gridX * GameConstants.CELL_SIZE;
            e.drawY = e.gridY * GameConstants.CELL_SIZE;
        }
        synergyEngine.updateSynergies(getDeployedList());
        notifyListeners();
    }

    public void startCombat() {
        GameLogger.phase("=== COMBAT START, floor=" + floor + " playerUnits=" + getDeployedCount() + " enemies="
                + currentEnemies.size() + " ===");
        phase = Phase.COMBAT;
        // Prepare combat copies (we fight with in-place units)
        combatPlayerUnits = new ArrayList<>(getDeployedList());
        for (Unit u : combatPlayerUnits) {
            u.resetForCombat();
            u.drawX = u.gridX * GameConstants.CELL_SIZE;
            u.drawY = u.gridY * GameConstants.CELL_SIZE;
        }
        // Apply synergy buffs
        synergyEngine.applySynergyBuffs(combatPlayerUnits);
        notifyListeners();
    }

    public void endCombat(boolean playerWon) {
        GameLogger.phase("=== COMBAT END, won=" + playerWon + " hp=" + playerHp + " floor=" + floor + " ===");
        lastCombatWin = playerWon;
        if (playerWon) {
            winStreak++;
            lossStreak = 0;
        } else {
            lossStreak++;
            winStreak = 0;
            List<Unit> survivors = new ArrayList<>();
            for (Unit e : currentEnemies)
                if (e.isAlive)
                    survivors.add(e);
            lastHpLost = shopEngine.calcPlayerDamage(survivors);
            playerHp = Math.max(0, playerHp - lastHpLost);
        }

        // Update deployed-unit positions from their combat-final coordinates.
        // Use two-pass (collect then apply) to avoid overwriting units that
        // crossed each other's starting cells during combat.
        Map<Integer, Unit> newPositions = new LinkedHashMap<>();
        for (int i = 0; i < deployedUnits.length; i++) {
            Unit u = deployedUnits[i];
            if (u == null)
                continue;
            Unit combat = combatPlayerUnits.stream().filter(c -> c.uid == u.uid).findFirst().orElse(null);
            if (combat != null) {
                u.gridX = combat.gridX;
                u.gridY = combat.gridY;
                int newIdx = u.gridY * 8 + u.gridX;
                if (newIdx >= 0 && newIdx < 64)
                    newPositions.put(newIdx, u); // last writer wins on collision
            }
        }
        Arrays.fill(deployedUnits, null);
        for (Map.Entry<Integer, Unit> e : newPositions.entrySet())
            deployedUnits[e.getKey()] = e.getValue();

        // Give boss drop item on boss floors
        if (floor % 10 == 0) {
            List<Item> allItems = DataManager.get().getAllItems();
            Item drop = allItems.get(new Random().nextInt(allItems.size()));
            storedItems.add(drop);
        }

        // Calculate gold income
        int humanBonus = synergyEngine.getHumanGoldBonus();
        lastGoldEarned = shopEngine.calcIncome(playerGold, winStreak, lossStreak, humanBonus);
        playerGold += lastGoldEarned;

        if (playerWon) {
            floor++;
            lastResultMessage = "勝利！獎勵 " + lastGoldEarned + " 金幣";
        } else {
            lastResultMessage = "失敗！損失 " + lastHpLost + " HP，獲得 " + lastGoldEarned + " 金幣";
        }

        phase = Phase.RESOLUTION;

        if (floor > GameConstants.MAX_FLOOR) {
            phase = Phase.VICTORY;
        } else if (playerHp <= 0) {
            phase = Phase.DEFEAT;
        }
        notifyListeners();
    }

    public void continueToShop() {
        GameLogger.phase("Continue to SHOP, floor=" + floor + " gold=" + playerGold + " hp=" + playerHp);
        phase = Phase.SHOP;
        selectedBenchUnit = null;
        grid.generateTerrain(floor);
        refreshShop();
        // Auto-merge stars
        checkAndMergeStars();
        notifyListeners();
    }

    // ── Shop actions ──────────────────────────────────────────────────────────
    public boolean buyUnit(int shopIndex) {
        if (shopIndex < 0 || shopIndex >= shopUnits.size())
            return false;
        UnitTemplate t = shopUnits.get(shopIndex);
        if (t == null)
            return false;
        int cost = GameConstants.UNIT_COST[t.rarity - 1];
        if (playerGold < cost)
            return false;
        if (bench.size() >= GameConstants.MAX_BENCH)
            return false;

        playerGold -= cost;
        GameLogger.shop("BUY: " + t.name + " cost=" + cost + " goldLeft=" + playerGold);
        Unit newUnit = new Unit(t, 1, true);
        bench.add(newUnit);

        // Remove from shop
        shopUnits.set(shopIndex, null);

        checkAndMergeStars();
        notifyListeners();
        return true;
    }

    public boolean sellUnit(Unit unit) {
        int value = unit.getSellValue();
        GameLogger.shop("SELL: " + unit.template.name + " value=" + value + " goldAfter=" + (playerGold + value));
        if (unit.equippedItem != null) {
            storedItems.add(unit.equippedItem);
            unit.equippedItem = null;
        }
        boolean removed = bench.remove(unit);
        if (!removed) {
            // Check deployed
            for (int i = 0; i < deployedUnits.length; i++) {
                if (deployedUnits[i] == unit) {
                    deployedUnits[i] = null;
                    removed = true;
                    break;
                }
            }
        }
        if (removed) {
            playerGold += value;
            synergyEngine.updateSynergies(getDeployedList());
            notifyListeners();
        }
        return removed;
    }

    public boolean buyXp() {
        if (playerGold < GameConstants.XP_COST)
            return false;
        if (playerLevel >= 9)
            return false;
        playerGold -= GameConstants.XP_COST;
        addXp(GameConstants.XP_COST);
        notifyListeners();
        return true;
    }

    private void addXp(int amount) {
        if (playerLevel >= 9)
            return;
        playerXp += amount;
        int needed = GameConstants.XP_TO_LEVEL[playerLevel - 1];
        if (playerXp >= needed) {
            playerXp -= needed;
            playerLevel = Math.min(9, playerLevel + 1);
        }
    }

    public void lockShop() {
        shopLocked = !shopLocked;
        notifyListeners();
    }

    // ── Unit movement ─────────────────────────────────────────────────────────
    /** Move unit from bench to field */
    public boolean deployUnit(Unit unit, int gx, int gy) {
        if (!grid.isInPlayerZone(gx, gy))
            return false;
        if (!grid.isWalkable(gx, gy))
            return false;
        int idx = gy * 8 + gx;
        if (deployedUnits[idx] != null && deployedUnits[idx] != unit) {
            // Swap
            Unit existing = deployedUnits[idx];
            deployedUnits[idx] = null;
            bench.add(existing);
            existing.gridX = -1;
            existing.gridY = -1;
        }
        bench.remove(unit);
        deployedUnits[idx] = unit;
        unit.gridX = gx;
        unit.gridY = gy;
        unit.drawX = gx * GameConstants.CELL_SIZE;
        unit.drawY = gy * GameConstants.CELL_SIZE;
        GameLogger.info("DEPLOY: " + unit.template.name + " -> (" + gx + "," + gy + ")");
        synergyEngine.updateSynergies(getDeployedList());
        notifyListeners();
        return true;
    }

    /** Move deployed unit to another cell */
    public boolean moveDeployedUnit(Unit unit, int newGx, int newGy) {
        if (!grid.isInPlayerZone(newGx, newGy))
            return false;
        if (!grid.isWalkable(newGx, newGy))
            return false;
        // Remove from old position
        for (int i = 0; i < deployedUnits.length; i++) {
            if (deployedUnits[i] == unit) {
                deployedUnits[i] = null;
                break;
            }
        }
        int newIdx = newGy * 8 + newGx;
        if (deployedUnits[newIdx] != null) {
            Unit other = deployedUnits[newIdx];
            // Put other unit at the source position
            int oldIdx = unit.gridY * 8 + unit.gridX;
            deployedUnits[oldIdx] = other;
            other.gridX = unit.gridX;
            other.gridY = unit.gridY;
        }
        deployedUnits[newIdx] = unit;
        unit.gridX = newGx;
        unit.gridY = newGy;
        synergyEngine.updateSynergies(getDeployedList());
        notifyListeners();
        return true;
    }

    /** Return deployed unit to bench */
    public void recallUnit(Unit unit) {
        for (int i = 0; i < deployedUnits.length; i++) {
            if (deployedUnits[i] == unit) {
                deployedUnits[i] = null;
                break;
            }
        }
        unit.gridX = -1;
        unit.gridY = -1;
        bench.add(unit);
        synergyEngine.updateSynergies(getDeployedList());
        notifyListeners();
    }

    // ── Item management ───────────────────────────────────────────────────────
    public void equipItem(Unit unit, Item item) {
        if (unit.equippedItem != null)
            storedItems.add(unit.equippedItem);
        storedItems.remove(item);
        unit.equippedItem = item;
        notifyListeners();
    }

    // ── Star merging ──────────────────────────────────────────────────────────
    public void checkAndMergeStars() {
        boolean merged = true;
        while (merged) {
            merged = false;
            // Collect all units (bench + deployed)
            List<Unit> allUnits = new ArrayList<>(bench);
            for (Unit u : deployedUnits)
                if (u != null)
                    allUnits.add(u);

            // Group by (templateId, starLevel)
            Map<String, List<Unit>> groups = new LinkedHashMap<>();
            for (Unit u : allUnits) {
                String key = u.template.id + ":" + u.starLevel;
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(u);
            }

            for (Map.Entry<String, List<Unit>> e : groups.entrySet()) {
                List<Unit> group = e.getValue();
                if (group.size() >= 3) {
                    int star = group.get(0).starLevel;
                    if (star >= 3)
                        continue; // already max star
                    // Merge the first 3
                    Unit survivor = group.get(0);
                    for (int i = 1; i < 3; i++) {
                        Unit dead = group.get(i);
                        bench.remove(dead);
                        for (int j = 0; j < deployedUnits.length; j++) {
                            if (deployedUnits[j] == dead) {
                                deployedUnits[j] = null;
                                break;
                            }
                        }
                        // Transfer items
                        if (dead.equippedItem != null)
                            storedItems.add(dead.equippedItem);
                    }
                    survivor.starLevel++;
                    survivor.applyBaseStats();
                    if (survivor.equippedItem != null)
                        survivor.applyItem();
                    GameLogger.info("STAR MERGE: " + survivor.template.name + " -> star" + survivor.starLevel);
                    game.SoundManager.starMerge();
                    merged = true;
                    break;
                }
            }
        }
        notifyListeners();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    public List<Unit> getDeployedList() {
        List<Unit> list = new ArrayList<>();
        for (Unit u : deployedUnits)
            if (u != null)
                list.add(u);
        return list;
    }

    public int getMaxFieldUnits() {
        return GameConstants.MAX_FIELD_UNITS[Math.min(playerLevel - 1, GameConstants.MAX_FIELD_UNITS.length - 1)];
    }

    public int getDeployedCount() {
        int c = 0;
        for (Unit u : deployedUnits)
            if (u != null)
                c++;
        return c;
    }

    public int getXpForNextLevel() {
        if (playerLevel >= 9)
            return 0;
        return GameConstants.XP_TO_LEVEL[playerLevel - 1];
    }
}
