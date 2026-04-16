package engine;

import engine.Grid;
import java.util.*;

public class AStarPathfinder {

    public record Point(int x, int y) {
    }

    private static class Node implements Comparable<Node> {
        int x, y, g, h;
        Node parent;

        Node(int x, int y, int g, int h, Node parent) {
            this.x = x;
            this.y = y;
            this.g = g;
            this.h = h;
            this.parent = parent;
        }

        int f() {
            return g + h;
        }

        @Override
        public int compareTo(Node o) {
            return Integer.compare(f(), o.f());
        }
    }

    /**
     * Find the next step toward the target.
     * blockedCells: set of (x,y) that are occupied by units (except target).
     * Returns the next Point, or null if no path.
     */
    public static Point nextStep(Grid grid, int startX, int startY, int goalX, int goalY,
            Set<Long> blockedCells) {
        if (startX == goalX && startY == goalY)
            return null;

        PriorityQueue<Node> open = new PriorityQueue<>();
        Map<Long, Node> best = new HashMap<>();

        Node start = new Node(startX, startY, 0, manhattan(startX, startY, goalX, goalY), null);
        open.add(start);
        best.put(key(startX, startY), start);

        int[][] dirs = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };
        int iterations = 0;

        while (!open.isEmpty() && iterations++ < 200) {
            Node cur = open.poll();
            if (cur.x == goalX && cur.y == goalY) {
                // Backtrack to find first step
                Node node = cur;
                while (node.parent != null && !(node.parent.x == startX && node.parent.y == startY)) {
                    node = node.parent;
                }
                return new Point(node.x, node.y);
            }
            for (int[] d : dirs) {
                int nx = cur.x + d[0], ny = cur.y + d[1];
                if (!grid.isInBounds(nx, ny))
                    continue;
                if (!grid.isWalkable(nx, ny))
                    continue;
                // Allow moving through the goal even if "blocked" by a unit there
                if (!(nx == goalX && ny == goalY) && blockedCells.contains(key(nx, ny)))
                    continue;
                int ng = cur.g + 1;
                long nk = key(nx, ny);
                if (!best.containsKey(nk) || best.get(nk).g > ng) {
                    Node next = new Node(nx, ny, ng, manhattan(nx, ny, goalX, goalY), cur);
                    best.put(nk, next);
                    open.add(next);
                }
            }
        }
        // No path found: try moving to the adjacent cell nearest to goal
        return nearestStep(grid, startX, startY, goalX, goalY, blockedCells);
    }

    private static Point nearestStep(Grid grid, int sx, int sy, int gx, int gy,
            Set<Long> blocked) {
        int[][] dirs = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };
        Point best = null;
        int bestDist = Integer.MAX_VALUE;
        for (int[] d : dirs) {
            int nx = sx + d[0], ny = sy + d[1];
            if (!grid.isInBounds(nx, ny) || !grid.isWalkable(nx, ny))
                continue;
            if (blocked.contains(key(nx, ny)))
                continue;
            int dist = manhattan(nx, ny, gx, gy);
            if (dist < bestDist) {
                bestDist = dist;
                best = new Point(nx, ny);
            }
        }
        return best;
    }

    public static int manhattan(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    public static long key(int x, int y) {
        return (long) x << 16 | (y & 0xFFFF);
    }
}
