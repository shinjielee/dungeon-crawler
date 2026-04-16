package model;

public enum TileType {
    NORMAL, // 一般地板（可通行）
    OBSTACLE, // 障礙物（不可通行）
    LAVA, // 岩漿（可通行但持續扣血）
    PLAYER, // 玩家部署區域標記
    ENEMY // 敵方區域標記
}
