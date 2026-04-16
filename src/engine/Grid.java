package engine;

import game.GameConstants;
import model.TileType;

public class Grid {
    public final int cols;
    public final int rows;
    private TileType[][] tiles;

    public Grid(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;
        tiles = new TileType[cols][rows];
        initNormal();
    }

    private void initNormal() {
        for (int x = 0; x < cols; x++)
            for (int y = 0; y < rows; y++)
                tiles[x][y] = TileType.NORMAL;
        markZones();
    }

    private void markZones() {
        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                if (y >= GameConstants.PLAYER_ROW_START)
                    tiles[x][y] = TileType.PLAYER;
                else
                    tiles[x][y] = TileType.ENEMY;
            }
        }
    }

    /** Generate random terrain obstacles for higher floors */
    public void generateTerrain(int floor) {
        initNormal();
        if (floor < 5)
            return; // no obstacles in early floors

        java.util.Random rng = new java.util.Random(floor * 31337L);
        int numObstacles = Math.min(4, floor / 8);
        int numLava = Math.min(2, floor / 15);

        for (int i = 0; i < numObstacles; i++) {
            int x = rng.nextInt(cols);
            int y = 1 + rng.nextInt(6); // rows 1-6
            if (isCenterColumn(x, y))
                continue;
            tiles[x][y] = TileType.OBSTACLE;
        }
        for (int i = 0; i < numLava; i++) {
            int x = rng.nextInt(cols);
            int y = 2 + rng.nextInt(4); // rows 2-5
            if (tiles[x][y] == TileType.OBSTACLE)
                continue;
            tiles[x][y] = TileType.LAVA;
        }
        // Re-mark zones after terrain (zone = PLAYER/ENEMY for context, but blocked by
        // OBSTACLE/LAVA)
    }

    private boolean isCenterColumn(int x, int y) {
        return x == 3 || x == 4;
    }

    public TileType getTile(int x, int y) {
        if (x < 0 || x >= cols || y < 0 || y >= rows)
            return TileType.OBSTACLE;
        return tiles[x][y];
    }

    public boolean isWalkable(int x, int y) {
        TileType t = getTile(x, y);
        return t != TileType.OBSTACLE;
    }

    public boolean isInPlayerZone(int x, int y) {
        return y >= GameConstants.PLAYER_ROW_START;
    }

    public boolean isInBounds(int x, int y) {
        return x >= 0 && x < cols && y >= 0 && y < rows;
    }

    public boolean isLava(int x, int y) {
        return getTile(x, y) == TileType.LAVA;
    }
}
