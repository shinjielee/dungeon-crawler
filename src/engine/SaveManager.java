package engine;

import game.GameState;
import model.Unit;
import model.UnitTemplate;
import model.Item;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SaveManager {
    private static final String SAVE_FILE = "dungeon_save.txt";

    public static void save(GameState gs) {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(SAVE_FILE), StandardCharsets.UTF_8))) {
            pw.println("floor=" + gs.floor);
            pw.println("hp=" + gs.playerHp);
            pw.println("gold=" + gs.playerGold);
            pw.println("level=" + gs.playerLevel);
            pw.println("xp=" + gs.playerXp);
            pw.println("winStreak=" + gs.winStreak);
            pw.println("lossStreak=" + gs.lossStreak);

            // Save bench units
            pw.println("bench_count=" + gs.bench.size());
            for (Unit u : gs.bench) {
                pw.println("bu=" + u.template.id + "," + u.starLevel
                        + "," + (u.equippedItem == null ? "none" : u.equippedItem.id));
            }

            // Save deployed units
            int deployed = 0;
            for (Unit u : gs.deployedUnits)
                if (u != null)
                    deployed++;
            pw.println("deployed_count=" + deployed);
            for (int i = 0; i < gs.deployedUnits.length; i++) {
                Unit u = gs.deployedUnits[i];
                if (u != null) {
                    int x = i % 8, y = i / 8;
                    pw.println("du=" + u.template.id + "," + u.starLevel
                            + "," + x + "," + y
                            + "," + (u.equippedItem == null ? "none" : u.equippedItem.id));
                }
            }

            // Save items in storage
            pw.println("item_count=" + gs.storedItems.size());
            for (Item it : gs.storedItems) {
                pw.println("item=" + it.id);
            }
        } catch (IOException e) {
            System.err.println("Save failed: " + e.getMessage());
        }
    }

    public static boolean load(GameState gs) {
        File f = new File(SAVE_FILE);
        if (!f.exists())
            return false;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(f), StandardCharsets.UTF_8))) {
            String line;
            DataManager dm = DataManager.get();
            while ((line = br.readLine()) != null) {
                String[] kv = line.split("=", 2);
                if (kv.length < 2)
                    continue;
                String key = kv[0], val = kv[1];
                switch (key) {
                    case "floor" -> gs.floor = Integer.parseInt(val);
                    case "hp" -> gs.playerHp = Integer.parseInt(val);
                    case "gold" -> gs.playerGold = Integer.parseInt(val);
                    case "level" -> gs.playerLevel = Integer.parseInt(val);
                    case "xp" -> gs.playerXp = Integer.parseInt(val);
                    case "winStreak" -> gs.winStreak = Integer.parseInt(val);
                    case "lossStreak" -> gs.lossStreak = Integer.parseInt(val);
                    case "bu" -> {
                        String[] p = val.split(",");
                        UnitTemplate ut = dm.getById(p[0]);
                        if (ut != null) {
                            Unit u = new Unit(ut, Integer.parseInt(p[1]), true);
                            if (!p[2].equals("none"))
                                u.equippedItem = findItem(p[2]);
                            gs.bench.add(u);
                        }
                    }
                    case "du" -> {
                        String[] p = val.split(",");
                        UnitTemplate ut = dm.getById(p[0]);
                        if (ut != null) {
                            Unit u = new Unit(ut, Integer.parseInt(p[1]), true);
                            int x = Integer.parseInt(p[2]), y = Integer.parseInt(p[3]);
                            if (!p[4].equals("none"))
                                u.equippedItem = findItem(p[4]);
                            u.gridX = x;
                            u.gridY = y;
                            gs.deployedUnits[y * 8 + x] = u;
                        }
                    }
                    case "item" -> {
                        Item it = findItem(val);
                        if (it != null)
                            gs.storedItems.add(it);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            System.err.println("Load failed: " + e.getMessage());
            return false;
        }
    }

    private static Item findItem(String id) {
        return DataManager.get().getAllItems().stream()
                .filter(it -> it.id.equals(id)).findFirst().orElse(null);
    }

    public static boolean hasSaveFile() {
        return new File(SAVE_FILE).exists();
    }

    public static void deleteSave() {
        new File(SAVE_FILE).delete();
    }
}
