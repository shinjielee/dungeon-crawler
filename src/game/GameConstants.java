package game;

public class GameConstants {
    public static final int GRID_COLS = 8;
    public static final int GRID_ROWS = 8;
    public static final int CELL_SIZE = 65;
    public static final int PLAYER_ROW_START = 4; // rows 4-7 are player area
    public static final int MAX_FLOOR = 50;
    public static final int INITIAL_PLAYER_HP = 20;
    public static final int BASE_INCOME = 5;
    public static final int MAX_INTEREST = 5;
    public static final int INTEREST_EVERY = 10;
    public static final int COMBAT_TICK_MS = 280;
    public static final int SHOP_SIZE = 5;
    public static final int REFRESH_COST = 2;
    public static final int XP_COST = 4;
    public static final int MAX_BENCH = 8;
    public static final int STAR2_MULTIPLIER = 2; // HP x2, AP x1.8
    public static final int STAR3_MULTIPLIER = 3; // HP x4, AP x3.2

    // XP needed to reach levels 2..9 (index 0 => level 2)
    public static final int[] XP_TO_LEVEL = { 2, 2, 6, 10, 20, 36, 56, 80 };

    // Max units on field per player level (index = level-1)
    // Easier: start at 3 and cap at 8 by level 6
    public static final int[] MAX_FIELD_UNITS = { 3, 4, 5, 6, 7, 8, 8, 8, 8 };

    // Rarity probability at each player level [level-1][rarity-1]
    public static final int[][] RARITY_PROB = {
            /* lv1 */ { 100, 0, 0, 0, 0 },
            /* lv2 */ { 100, 0, 0, 0, 0 },
            /* lv3 */ { 75, 25, 0, 0, 0 },
            /* lv4 */ { 55, 30, 15, 0, 0 },
            /* lv5 */ { 45, 33, 20, 2, 0 },
            /* lv6 */ { 35, 35, 25, 5, 0 },
            /* lv7 */ { 22, 40, 30, 7, 1 },
            /* lv8 */ { 15, 35, 35, 12, 3 },
            /* lv9 */ { 10, 25, 35, 22, 8 },
    };

    // Unit cost by rarity
    public static final int[] UNIT_COST = { 1, 2, 3, 4, 5 };

    // Sell value = cost * starLevel / 2 (rounded down, min 1)
}
