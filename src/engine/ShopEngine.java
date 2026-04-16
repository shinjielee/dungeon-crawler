package engine;

import game.GameConstants;
import model.*;
import java.util.*;

public class ShopEngine {
    private final Random rng = new Random();
    private final List<UnitTemplate> pool;

    public ShopEngine() {
        pool = new ArrayList<>(DataManager.get().getAllUnits());
    }

    /** Generate a shop of SHOP_SIZE units for the given player level */
    public List<UnitTemplate> generateShop(int playerLevel, boolean locked, List<UnitTemplate> current) {
        if (locked && current != null)
            return current;

        List<UnitTemplate> result = new ArrayList<>();
        int level = Math.min(playerLevel, GameConstants.RARITY_PROB.length);
        int[] probs = GameConstants.RARITY_PROB[level - 1];

        for (int i = 0; i < GameConstants.SHOP_SIZE; i++) {
            int rarity = pickRarity(probs);
            UnitTemplate t = pickByRarity(rarity);
            if (t != null)
                result.add(t);
        }
        return result;
    }

    private int pickRarity(int[] probs) {
        int roll = rng.nextInt(100);
        int cumulative = 0;
        for (int i = 0; i < probs.length; i++) {
            cumulative += probs[i];
            if (roll < cumulative)
                return i + 1;
        }
        return 1;
    }

    private UnitTemplate pickByRarity(int rarity) {
        List<UnitTemplate> available = DataManager.get().getByRarity(rarity);
        if (available.isEmpty())
            return pickByRarity(Math.max(1, rarity - 1));
        return available.get(rng.nextInt(available.size()));
    }

    /** Generate enemy team for the given floor */
    public List<Unit> generateEnemyTeam(int floor) {
        List<Unit> enemies = new ArrayList<>();
        int count = Math.min(2 + floor / 3, 8);
        int baseRarity = Math.min(1 + floor / 10, 5);
        int starLevel = floor >= 40 ? 3 : floor >= 20 ? 2 : 1;

        // Pick random templates
        List<UnitTemplate> allTemplates = DataManager.get().getAllUnits();
        Collections.shuffle(allTemplates, rng);

        for (int i = 0; i < count; i++) {
            UnitTemplate t = allTemplates.get(i % allTemplates.size());
            // Scale rarity to floor
            int r = Math.min(baseRarity + rng.nextInt(2), 5);
            List<UnitTemplate> candidates = DataManager.get().getByRarity(r);
            if (candidates.isEmpty())
                candidates = allTemplates;
            UnitTemplate chosen = candidates.get(rng.nextInt(candidates.size()));

            Unit enemy = new Unit(chosen, starLevel, false);
            // Scale HP and AP with floor
            double floorMult = 1.0 + floor * 0.08;
            enemy.maxHp *= floorMult;
            enemy.hp = enemy.maxHp;
            enemy.ap *= floorMult;

            // Place in enemy area (rows 0-3)
            enemy.gridX = i % GameConstants.GRID_COLS;
            enemy.gridY = i / GameConstants.GRID_COLS; // 0, 1, 2, 3
            enemies.add(enemy);
        }

        // Boss floor: super enemy
        if (floor % 10 == 0) {
            Unit boss = new Unit(DataManager.get().getByRarity(Math.min(floor / 10 + 1, 5))
                    .get(rng.nextInt(Math.max(1, DataManager.get().getByRarity(Math.min(floor / 10 + 1, 5)).size()))),
                    3, false);
            boss.maxHp *= (1.0 + floor * 0.15);
            boss.hp = boss.maxHp;
            boss.ap *= (1.0 + floor * 0.12);
            boss.gridX = 3;
            boss.gridY = 0;
            enemies.add(boss);
        }
        return enemies;
    }

    /** Calculate end-of-round gold */
    public int calcIncome(int gold, int winStreak, int lossStreak, int humanBonus) {
        int income = GameConstants.BASE_INCOME;
        income += Math.min(GameConstants.MAX_INTEREST, gold / GameConstants.INTEREST_EVERY);
        if (winStreak >= 6)
            income += 3;
        else if (winStreak >= 4)
            income += 2;
        else if (winStreak >= 2)
            income += 1;
        if (lossStreak >= 6)
            income += 3;
        else if (lossStreak >= 4)
            income += 2;
        else if (lossStreak >= 2)
            income += 1;
        income += humanBonus;
        return income;
    }

    /** Calculate player HP damage after a loss */
    public int calcPlayerDamage(List<Unit> survivingEnemies) {
        if (survivingEnemies.isEmpty())
            return 0;
        int dmg = 2; // base loss damage
        for (Unit e : survivingEnemies) {
            if (e.isAlive)
                dmg += e.starLevel;
        }
        return Math.max(2, dmg);
    }
}
