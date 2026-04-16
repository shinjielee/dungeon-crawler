package ui;

import engine.CombatEngine;
import engine.CombatEngine.CombatEvent;
import game.GameConstants;
import game.GameState;
import game.Phase;
import game.SoundManager;
import model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class GamePanel extends GameStatePanel {
    private final Consumer<Unit> onUnitSelected;

    // Drag state
    private Unit draggingUnit = null;
    private int dragSourceX = -1, dragSourceY = -1;
    private boolean dragFromBench = false;
    private int mouseX, mouseY;

    // Particles
    private final List<Particle> particles = new ArrayList<>();

    // Combat
    private CombatEngine combatEngine;
    private javax.swing.Timer combatTimer;
    private javax.swing.Timer animTimer;

    // Preview of attack range
    private Unit hoveredUnit = null;
    private java.awt.Point hoveredCell = null;

    public GamePanel(GameState gs, Consumer<Unit> onUnitSelected) {
        super(gs);
        this.onUnitSelected = onUnitSelected;
        setBackground(new Color(10, 10, 20));
        setPreferredSize(new Dimension(
                GameConstants.GRID_COLS * GameConstants.CELL_SIZE + 2,
                GameConstants.GRID_ROWS * GameConstants.CELL_SIZE + 2));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMousePressed(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handleMouseReleased(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClicked(e);
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
                repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                handleMouseMoved(e);
            }
        });

        // Animation timer at ~50fps
        animTimer = new javax.swing.Timer(20, evt -> {
            updateAnimations();
            repaint();
        });
        animTimer.start();
    }

    // ── Public methods ─────────────────────────────────────────────────────────

    public void startCombat() {
        List<Unit> playerUnits = gs.combatPlayerUnits;
        List<Unit> enemyUnits = gs.currentEnemies;

        combatEngine = new CombatEngine(gs.grid, playerUnits, enemyUnits);
        combatEngine.setEventListener(this::handleCombatEvent);

        SoundManager.combatStart();
        combatTimer = new javax.swing.Timer(GameConstants.COMBAT_TICK_MS, evt -> {
            if (gs.phase != Phase.COMBAT) {
                combatTimer.stop();
                return;
            }
            boolean ongoing = combatEngine.tick();
            if (!ongoing) {
                combatTimer.stop();
                boolean win = combatEngine.isPlayerWin();
                gs.endCombat(win);
                if (win)
                    SoundManager.victory();
                else
                    SoundManager.defeat();
            }
        });
        combatTimer.start();
    }

    public void stopCombat() {
        if (combatTimer != null)
            combatTimer.stop();
    }

    public void addParticle(Particle p) {
        synchronized (particles) {
            particles.add(p);
        }
    }

    // ── Painting ───────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawGrid(g2);
        // Highlight available cells if bench unit is selected
        if (gs.selectedBenchUnit != null)
            drawDeployHighlight(g2);
        drawEnemyUnits(g2);
        drawPlayerUnits(g2);
        drawParticles(g2);
        if (draggingUnit != null)
            drawDraggingUnit(g2);
        if (gs.phase == Phase.DEPLOY && hoveredUnit != null)
            drawAttackRange(g2, hoveredUnit);
        drawPhaseOverlay(g2);
    }

    private void drawGrid(Graphics2D g2) {
        int cs = GameConstants.CELL_SIZE;
        for (int y = 0; y < GameConstants.GRID_ROWS; y++) {
            for (int x = 0; x < GameConstants.GRID_COLS; x++) {
                int px = x * cs, py = y * cs;
                TileType tile = gs.grid.getTile(x, y);

                // Zone background
                Color bg;
                if (y >= GameConstants.PLAYER_ROW_START) {
                    bg = tile == TileType.OBSTACLE ? new Color(40, 30, 20) : new Color(15, 25, 45);
                } else {
                    bg = tile == TileType.OBSTACLE ? new Color(40, 20, 20) : new Color(25, 12, 12);
                }
                if (tile == TileType.LAVA)
                    bg = new Color(60, 20, 10);
                g2.setColor(bg);
                g2.fillRect(px, py, cs, cs);

                // Lava glow
                if (tile == TileType.LAVA) {
                    long t = System.currentTimeMillis();
                    float alpha = 0.3f + 0.2f * (float) Math.sin(t / 400.0 + x + y);
                    g2.setColor(new Color(1f, 0.3f, 0f, alpha));
                    g2.fillRect(px, py, cs, cs);
                    g2.setFont(new Font("Monospaced", Font.PLAIN, 18));
                    g2.setColor(new Color(255, 100, 0, 180));
                    g2.drawString("🌋", px + cs / 2 - 9, py + cs / 2 + 6);
                }

                // Obstacle
                if (tile == TileType.OBSTACLE) {
                    g2.setColor(new Color(80, 60, 40));
                    g2.fillRoundRect(px + 4, py + 4, cs - 8, cs - 8, 10, 10);
                    g2.setColor(new Color(100, 80, 55));
                    g2.drawRoundRect(px + 4, py + 4, cs - 8, cs - 8, 10, 10);
                    g2.setFont(new Font("Monospaced", Font.PLAIN, 20));
                    g2.setColor(new Color(150, 130, 100));
                    g2.drawString("⬛", px + cs / 2 - 10, py + cs / 2 + 7);
                }

                // Grid lines
                g2.setColor(y >= GameConstants.PLAYER_ROW_START ? new Color(30, 50, 90) : new Color(70, 25, 25));
                g2.drawRect(px, py, cs, cs);
            }
        }

        // Zone separator
        int sepY = GameConstants.PLAYER_ROW_START * cs;
        g2.setColor(new Color(100, 80, 180));
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(0, sepY, GameConstants.GRID_COLS * cs, sepY);
        g2.setStroke(new BasicStroke(1));

        // Zone labels
        g2.setFont(new Font("Noto Sans CJK TC", Font.BOLD, 11));
        g2.setColor(new Color(150, 80, 80));
        g2.drawString("◀ 敵方區域 ▶", 5, 14);
        g2.setColor(new Color(80, 120, 200));
        g2.drawString("◀ 玩家區域 ▶", 5, sepY + 14);
    }

    private void drawEnemyUnits(Graphics2D g2) {
        List<Unit> units = gs.phase == Phase.COMBAT ? gs.currentEnemies : gs.currentEnemies;
        for (Unit u : units) {
            if (!u.isAlive)
                continue;
            drawUnit(g2, u, false);
        }
    }

    private void drawPlayerUnits(Graphics2D g2) {
        List<Unit> units;
        if (gs.phase == Phase.COMBAT) {
            units = gs.combatPlayerUnits;
        } else {
            units = gs.getDeployedList();
        }
        for (Unit u : units) {
            if (u == draggingUnit)
                continue;
            if (!u.isAlive || (gs.phase != Phase.COMBAT && u.gridX < 0))
                continue;
            drawUnit(g2, u, true);
        }
    }

    private void drawUnit(Graphics2D g2, Unit u, boolean isPlayer) {
        int cs = GameConstants.CELL_SIZE;

        // Calculate draw position (use animated pos for combat)
        int px, py;
        if (gs.phase == Phase.COMBAT) {
            // Smooth animation
            px = (int) u.drawX;
            py = (int) u.drawY;
        } else {
            px = u.gridX * cs;
            py = u.gridY * cs;
        }

        int padding = 4;
        int size = cs - padding * 2;

        // Damage flash
        boolean flashing = u.lastDamageFlash > 0;

        // Circle background
        Color baseColor = flashing ? new Color(255, 100, 100) : u.template.color;
        if (u.isInvisible)
            baseColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 100);
        if (u.stunDuration > 0) {
            baseColor = new Color(200, 200, 100);
        } else if (u.frozenDuration > 0) {
            baseColor = new Color(100, 180, 255);
        }

        g2.setColor(baseColor.darker());
        g2.fillOval(px + padding, py + padding, size, size);
        g2.setColor(baseColor);
        g2.fillOval(px + padding + 2, py + padding + 2, size - 4, size - 4);

        // Star glow for 2/3 star
        if (u.starLevel >= 2) {
            Color glowColor = u.starLevel == 3 ? new Color(255, 200, 50, 80) : new Color(100, 200, 255, 60);
            g2.setColor(glowColor);
            g2.setStroke(new BasicStroke(3));
            g2.drawOval(px + padding - 1, py + padding - 1, size + 2, size + 2);
            g2.setStroke(new BasicStroke(1));
        }

        // Border
        g2.setColor(isPlayer ? new Color(60, 120, 200) : new Color(200, 60, 60));
        g2.drawOval(px + padding, py + padding, size, size);

        // Symbol
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Noto Sans CJK TC", Font.BOLD, 16));
        FontMetrics fm = g2.getFontMetrics();
        String sym = u.template.symbol;
        g2.drawString(sym, px + cs / 2 - fm.stringWidth(sym) / 2, py + cs / 2 + fm.getAscent() / 2 - 3);

        // Stars below symbol
        if (u.starLevel > 1) {
            g2.setFont(new Font("Noto Sans CJK TC", Font.BOLD, 9));
            g2.setColor(new Color(255, 220, 50));
            String st = u.getStarString();
            fm = g2.getFontMetrics();
            g2.drawString(st, px + cs / 2 - fm.stringWidth(st) / 2, py + cs - padding - 14);
        }

        // Status icons
        if (u.stunDuration > 0) {
            g2.setFont(new Font("Monospaced", Font.PLAIN, 14));
            g2.drawString("💫", px + 2, py + 14);
        } else if (u.frozenDuration > 0) {
            g2.setFont(new Font("Monospaced", Font.PLAIN, 14));
            g2.drawString("❄", px + 2, py + 14);
        }
        if (u.isCasting) {
            g2.setFont(new Font("Monospaced", Font.PLAIN, 14));
            g2.drawString("✨", px + cs - 18, py + 14);
        }

        // HP bar
        int barY = py + cs - padding - 10;
        int barW = cs - padding * 2;
        g2.setColor(new Color(30, 10, 10));
        g2.fillRoundRect(px + padding, barY, barW, 7, 3, 3);
        double hpPct = u.getHpPercent();
        Color hpColor = hpPct > 0.6 ? new Color(50, 200, 50)
                : hpPct > 0.3 ? new Color(220, 180, 30) : new Color(220, 50, 50);
        g2.setColor(hpColor);
        g2.fillRoundRect(px + padding, barY, (int) (barW * hpPct), 7, 3, 3);
        g2.setColor(new Color(80, 80, 80));
        g2.drawRoundRect(px + padding, barY, barW, 7, 3, 3);

        // Mana bar
        int manaBarY = barY - 9;
        g2.setColor(new Color(10, 10, 40));
        g2.fillRoundRect(px + padding, manaBarY, barW, 6, 3, 3);
        g2.setColor(new Color(50, 100, 255));
        g2.fillRoundRect(px + padding, manaBarY, (int) (barW * u.getManaPercent()), 6, 3, 3);

        // Item indicator
        if (u.equippedItem != null) {
            g2.setFont(new Font("Noto Sans CJK TC", Font.BOLD, 11));
            g2.setColor(u.equippedItem.color);
            g2.drawString(u.equippedItem.symbol, px + padding, py + padding + 12);
        }

        // Attack animation flash
        if (u.isAttacking) {
            g2.setColor(new Color(255, 255, 100, 100));
            g2.fillOval(px + padding, py + padding, size, size);
        }
    }

    private void drawDraggingUnit(Graphics2D g2) {
        if (draggingUnit == null)
            return;
        int cs = GameConstants.CELL_SIZE;
        int size = cs - 8;

        g2.setColor(draggingUnit.template.color.darker().darker());
        g2.fillOval(mouseX - size / 2, mouseY - size / 2, size, size);
        g2.setColor(draggingUnit.template.color);
        g2.fillOval(mouseX - size / 2 + 3, mouseY - size / 2 + 3, size - 6, size - 6);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Noto Sans CJK TC", Font.BOLD, 16));
        FontMetrics fm = g2.getFontMetrics();
        String sym = draggingUnit.template.symbol;
        g2.drawString(sym, mouseX - fm.stringWidth(sym) / 2, mouseY + fm.getAscent() / 2 - 3);

        // Highlight target cell
        int gx = mouseX / cs, gy = mouseY / cs;
        if (gs.grid.isInPlayerZone(gx, gy) && gs.grid.isWalkable(gx, gy)) {
            g2.setColor(new Color(100, 200, 100, 70));
            g2.fillRect(gx * cs, gy * cs, cs, cs);
            g2.setColor(new Color(100, 255, 100, 150));
            g2.drawRect(gx * cs, gy * cs, cs, cs);
        }
    }

    private void drawDeployHighlight(Graphics2D g2) {
        int cs = GameConstants.CELL_SIZE;
        for (int y = GameConstants.PLAYER_ROW_START; y < GameConstants.GRID_ROWS; y++) {
            for (int x = 0; x < GameConstants.GRID_COLS; x++) {
                if (!gs.grid.isWalkable(x, y))
                    continue;
                int idx = y * 8 + x;
                boolean empty = gs.deployedUnits[idx] == null;
                Color highlight = empty ? new Color(80, 160, 80, 80) : new Color(160, 120, 30, 60);
                g2.setColor(highlight);
                g2.fillRect(x * cs, y * cs, cs, cs);
                g2.setColor(empty ? new Color(100, 220, 100, 150) : new Color(200, 160, 50, 120));
                g2.drawRect(x * cs, y * cs, cs, cs);
            }
        }
        // Show selected bench unit icon at mouse position
        if (hoveredCell != null && gs.grid.isInPlayerZone(hoveredCell.x, hoveredCell.y)) {
            Unit u = gs.selectedBenchUnit;
            int px = hoveredCell.x * cs + cs / 2, py = hoveredCell.y * cs + cs / 2;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            g2.setColor(u.template.color);
            g2.fillOval(px - 20, py - 20, 40, 40);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }
    }

    private void drawAttackRange(Graphics2D g2, Unit u) {
        if (u.gridX < 0)
            return;
        int cs = GameConstants.CELL_SIZE;
        int range = u.attackRange;
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                int dist = Math.abs(dx) + Math.abs(dy);
                if (dist > range || dist == 0)
                    continue;
                int nx = u.gridX + dx, ny = u.gridY + dy;
                if (!gs.grid.isInBounds(nx, ny))
                    continue;
                g2.setColor(new Color(255, 255, 100, 40));
                g2.fillRect(nx * cs, ny * cs, cs, cs);
                g2.setColor(new Color(255, 255, 100, 80));
                g2.drawRect(nx * cs, ny * cs, cs, cs);
            }
        }
    }

    private void drawParticles(Graphics2D g2) {
        synchronized (particles) {
            for (Particle p : particles) {
                if (p.text != null) {
                    g2.setFont(new Font("Noto Sans CJK TC", Font.BOLD, 13));
                    g2.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(),
                            (int) (p.alpha * 255)));
                    g2.drawString(p.text, (int) p.x, (int) p.y);
                } else {
                    g2.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(),
                            (int) (p.alpha * 255)));
                    int s = (int) p.size;
                    g2.fillOval((int) p.x - s / 2, (int) p.y - s / 2, s, s);
                }
            }
        }
    }

    private void drawPhaseOverlay(Graphics2D g2) {
        int w = getWidth(), h = getHeight();
        if (gs.phase == Phase.RESOLUTION) {
            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRect(0, 0, w, h);

            String msg1 = gs.lastCombatWin ? "⚔ 勝利！" : "💀 失敗！";
            String msg2 = gs.lastResultMessage;
            String msg3 = "點擊繼續…";

            g2.setFont(new Font("Noto Sans CJK TC", Font.BOLD, 36));
            FontMetrics fm = g2.getFontMetrics();
            Color msgColor = gs.lastCombatWin ? new Color(100, 255, 100) : new Color(255, 100, 100);
            g2.setColor(msgColor);
            g2.drawString(msg1, (w - fm.stringWidth(msg1)) / 2, h / 2 - 30);

            g2.setFont(new Font("Noto Sans CJK TC", Font.BOLD, 18));
            fm = g2.getFontMetrics();
            g2.setColor(Color.WHITE);
            g2.drawString(msg2, (w - fm.stringWidth(msg2)) / 2, h / 2 + 10);

            g2.setFont(new Font("Noto Sans CJK TC", Font.PLAIN, 14));
            fm = g2.getFontMetrics();
            long t = System.currentTimeMillis();
            float alpha = 0.5f + 0.5f * (float) Math.sin(t / 600.0);
            g2.setColor(new Color(1f, 1f, 1f, alpha));
            g2.drawString(msg3, (w - fm.stringWidth(msg3)) / 2, h / 2 + 50);
        }

        if (gs.phase == Phase.DEPLOY) {
            // Show hint
            g2.setFont(new Font("Noto Sans CJK TC", Font.BOLD, 12));
            g2.setColor(new Color(180, 180, 255, 200));
            g2.drawString("拖曳單位至藍色區域佈署 | 右鍵查看詳情", 5, getHeight() - 5);
        }
    }

    // ── Animation ──────────────────────────────────────────────────────────────

    private void updateAnimations() {
        int cs = GameConstants.CELL_SIZE;
        float speed = 8.0f;

        // Smooth unit movement
        if (gs.phase == Phase.COMBAT) {
            for (Unit u : gs.combatPlayerUnits) {
                if (!u.isAlive)
                    continue;
                double targetPx = u.gridX * cs + cs / 2.0 - cs / 2.0;
                double targetPy = u.gridY * cs + cs / 2.0 - cs / 2.0;
                u.drawX += (targetPx - u.drawX) * 0.18;
                u.drawY += (targetPy - u.drawY) * 0.18;
            }
            for (Unit e : gs.currentEnemies) {
                if (!e.isAlive)
                    continue;
                double targetPx = e.gridX * cs;
                double targetPy = e.gridY * cs;
                e.drawX += (targetPx - e.drawX) * 0.18;
                e.drawY += (targetPy - e.drawY) * 0.18;
            }
        }

        // Animate attack/cast timers
        for (Unit u : gs.combatPlayerUnits) {
            if (u.attackAnimTimer > 0)
                u.attackAnimTimer -= 0.033;
            if (u.attackAnimTimer <= 0)
                u.isAttacking = false;
            if (u.castAnimTimer > 0)
                u.castAnimTimer -= 0.033;
            if (u.castAnimTimer <= 0)
                u.isCasting = false;
        }
        for (Unit e : gs.currentEnemies) {
            if (e.attackAnimTimer > 0)
                e.attackAnimTimer -= 0.033;
            if (e.attackAnimTimer <= 0)
                e.isAttacking = false;
            if (e.castAnimTimer > 0)
                e.castAnimTimer -= 0.033;
            if (e.castAnimTimer <= 0)
                e.isCasting = false;
        }

        // Update particles
        synchronized (particles) {
            particles.removeIf(Particle::isDead);
            particles.forEach(Particle::update);
        }
    }

    // ── Mouse handlers ─────────────────────────────────────────────────────────

    private void handleMousePressed(MouseEvent e) {
        if (gs.phase != Phase.SHOP && gs.phase != Phase.DEPLOY)
            return;
        int cs = GameConstants.CELL_SIZE;
        int gx = e.getX() / cs, gy = e.getY() / cs;
        if (!gs.grid.isInBounds(gx, gy))
            return;
        if (!SwingUtilities.isLeftMouseButton(e))
            return;

        // Pick deployed unit
        if (gs.phase == Phase.DEPLOY || gs.phase == Phase.SHOP) {
            int idx = gy * 8 + gx;
            Unit u = gs.deployedUnits[idx];
            if (u != null && gs.grid.isInPlayerZone(gx, gy)) {
                draggingUnit = u;
                dragSourceX = gx;
                dragSourceY = gy;
                dragFromBench = false;
                mouseX = e.getX();
                mouseY = e.getY();
            }
        }
    }

    private void handleMouseReleased(MouseEvent e) {
        if (draggingUnit == null)
            return;
        int cs = GameConstants.CELL_SIZE;
        int gx = e.getX() / cs, gy = e.getY() / cs;

        if (gs.grid.isInBounds(gx, gy) && gs.grid.isWalkable(gx, gy) && gs.grid.isInPlayerZone(gx, gy)) {
            if (gx != dragSourceX || gy != dragSourceY) {
                if (dragFromBench) {
                    // Deploying count limit
                    if (!isAlreadyDeployed(draggingUnit) && gs.getDeployedCount() >= gs.getMaxFieldUnits()) {
                        JOptionPane.showMessageDialog(this,
                                "已達到 Lv." + gs.playerLevel + " 出戰上限 " + gs.getMaxFieldUnits() + " 個單位！",
                                "無法佈署", JOptionPane.WARNING_MESSAGE);
                    } else {
                        gs.deployUnit(draggingUnit, gx, gy);
                        SoundManager.deploy();
                    }
                } else {
                    gs.moveDeployedUnit(draggingUnit, gx, gy);
                }
            }
        } else if (!gs.grid.isInPlayerZone(gx, gy) || !gs.grid.isInBounds(gx, gy)) {
            // Dragged outside player zone = recall to bench
            gs.recallUnit(draggingUnit);
        }

        draggingUnit = null;
        dragFromBench = false;
        repaint();
    }

    private boolean isAlreadyDeployed(Unit u) {
        for (Unit d : gs.deployedUnits)
            if (d == u)
                return true;
        return false;
    }

    private void handleMouseClicked(MouseEvent e) {
        int cs = GameConstants.CELL_SIZE;
        int gx = e.getX() / cs, gy = e.getY() / cs;
        if (!gs.grid.isInBounds(gx, gy))
            return;

        if (gs.phase == Phase.RESOLUTION) {
            gs.continueToShop();
            stopCombat();
            return;
        }

        // Deploy selected bench unit on left-click in player zone
        if (SwingUtilities.isLeftMouseButton(e) && gs.selectedBenchUnit != null
                && gs.grid.isInPlayerZone(gx, gy) && gs.grid.isWalkable(gx, gy)) {
            if (gs.getDeployedCount() < gs.getMaxFieldUnits() || gs.deployedUnits[gy * 8 + gx] != null) {
                gs.deployUnit(gs.selectedBenchUnit, gx, gy);
                gs.selectedBenchUnit = null;
                SoundManager.deploy();
                return;
            } else {
                javax.swing.JOptionPane.showMessageDialog(this,
                        "已達出戰上限！請升級或先召回其他單位。", "無法佈署",
                        javax.swing.JOptionPane.WARNING_MESSAGE);
                gs.selectedBenchUnit = null;
                return;
            }
        }

        // Right-click for unit info
        if (SwingUtilities.isRightMouseButton(e)) {
            int idx = gy * 8 + gx;
            Unit u = gs.deployedUnits[idx];
            if (u == null) {
                // Check enemies
                for (Unit en : gs.currentEnemies) {
                    if (en.gridX == gx && en.gridY == gy) {
                        u = en;
                        break;
                    }
                }
            }
            if (u != null) {
                onUnitSelected.accept(u);
                // Show context menu for deploy/shop phase
                if (gs.phase == Phase.SHOP || gs.phase == Phase.DEPLOY) {
                    showUnitContextMenu(u, e.getX(), e.getY());
                }
            }
        }
    }

    private void handleMouseMoved(MouseEvent e) {
        int cs = GameConstants.CELL_SIZE;
        int gx = e.getX() / cs, gy = e.getY() / cs;
        hoveredCell = gs.grid.isInBounds(gx, gy) ? new java.awt.Point(gx, gy) : null;
        hoveredUnit = null;
        if (gs.grid.isInBounds(gx, gy)) {
            int idx = gy * 8 + gx;
            hoveredUnit = gs.deployedUnits[idx];
            if (hoveredUnit == null) {
                for (Unit en : gs.currentEnemies) {
                    if (en.gridX == gx && en.gridY == gy) {
                        hoveredUnit = en;
                        break;
                    }
                }
            }
        }
        repaint();
    }

    private void showUnitContextMenu(Unit u, int rx, int ry) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem infoItem = new JMenuItem("📋 查看詳情: " + u);
        infoItem.addActionListener(ev -> onUnitSelected.accept(u));
        menu.add(infoItem);

        if (u.isPlayerUnit) {
            JMenuItem sellItem = new JMenuItem("💰 賣出 +" + u.getSellValue() + " 金");
            sellItem.addActionListener(ev -> {
                gs.sellUnit(u);
            });
            menu.add(sellItem);

            // Equip item sub-menu
            if (!gs.storedItems.isEmpty()) {
                JMenu equipMenu = new JMenu("🎒 裝備物品");
                for (Item it : gs.storedItems) {
                    JMenuItem itItem = new JMenuItem(it.symbol + " " + it.name + " - " + it.description);
                    itItem.addActionListener(ev -> gs.equipItem(u, it));
                    equipMenu.add(itItem);
                }
                menu.add(equipMenu);
            }

            if (isAlreadyDeployed(u)) {
                JMenuItem recallItem = new JMenuItem("⬅ 召回至備用席");
                recallItem.addActionListener(ev -> gs.recallUnit(u));
                menu.add(recallItem);
            }
        }

        menu.show(this, rx, ry);
    }

    // ── Combat events → Particles ──────────────────────────────────────────────

    private void handleCombatEvent(CombatEvent event) {
        SwingUtilities.invokeLater(() -> {
            int cs = GameConstants.CELL_SIZE;
            int px = event.targetX * cs + cs / 2;
            int py = event.targetY * cs + cs / 2;

            switch (event.type) {
                case ATTACK -> {
                    spawnHitParticles(px, py, event.value, false);
                    SoundManager.hit();
                }
                case DAMAGE_DEALT -> {
                    spawnHitParticles(px, py, event.value, event.source != null && event.source.template.isMagicDamage);
                    SoundManager.hit();
                }
                case ABILITY_CAST -> {
                    spawnCastParticles(px, py, event.source);
                    SoundManager.cast();
                }
                case HEAL -> {
                    spawnHealParticles(px, py, (int) event.value);
                    SoundManager.heal();
                }
                case UNIT_DIED -> {
                    spawnDeathParticles(px, py);
                    SoundManager.death();
                }
                case UNIT_REVIVE -> {
                    spawnReviveParticles(px, py);
                    SoundManager.cast();
                }
                case STUN -> {
                    spawnStatusParticle(px, py, "💫 眩暈", new Color(255, 255, 100));
                    SoundManager.stun();
                }
                case FREEZE -> {
                    spawnStatusParticle(px, py, "❄ 冰凍", new Color(100, 200, 255));
                    SoundManager.stun();
                }
                default -> {
                }
            }
        });
    }

    private void spawnHitParticles(int px, int py, double damage, boolean magic) {
        Color c = magic ? new Color(180, 100, 255) : new Color(255, 180, 50);
        Random rng = new Random();
        for (int i = 0; i < 5; i++) {
            double vx = (rng.nextDouble() - 0.5) * 5;
            double vy = -(rng.nextDouble() * 3 + 1);
            synchronized (particles) {
                particles.add(new Particle(px + rng.nextInt(20) - 10, py, vx, vy, c, 8 + rng.nextInt(6), 18));
            }
        }
        if (damage > 0) {
            String dmgStr = "-" + (int) damage;
            Color dmgColor = magic ? new Color(200, 100, 255) : new Color(255, 100, 50);
            synchronized (particles) {
                particles.add(Particle.text(px - 15, py - 10, dmgStr, dmgColor, 30));
            }
        }
    }

    private void spawnCastParticles(int px, int py, Unit caster) {
        Color c = caster != null ? caster.template.color : new Color(200, 100, 255);
        Random rng = new Random();
        for (int i = 0; i < 10; i++) {
            double angle = rng.nextDouble() * Math.PI * 2;
            double speed = rng.nextDouble() * 4 + 1;
            synchronized (particles) {
                particles.add(new Particle(px, py, Math.cos(angle) * speed, Math.sin(angle) * speed,
                        c, 10 + rng.nextInt(8), 25));
            }
        }
        synchronized (particles) {
            particles.add(Particle.text(px - 20, py - 20, "✨ 施法！", new Color(255, 220, 100), 35));
        }
    }

    private void spawnHealParticles(int px, int py, int amount) {
        synchronized (particles) {
            particles.add(Particle.text(px - 15, py - 10, "+" + amount + " HP", new Color(50, 255, 100), 35));
            for (int i = 0; i < 6; i++) {
                Random rng = new Random();
                particles.add(new Particle(px + rng.nextInt(30) - 15, py + rng.nextInt(30) - 15,
                        (rng.nextDouble() - 0.5) * 2, -rng.nextDouble() * 2 - 0.5,
                        new Color(50, 200, 100), 8, 25));
            }
        }
    }

    private void spawnDeathParticles(int px, int py) {
        Random rng = new Random();
        for (int i = 0; i < 12; i++) {
            double vx = (rng.nextDouble() - 0.5) * 7;
            double vy = -(rng.nextDouble() * 5 + 1);
            synchronized (particles) {
                particles.add(new Particle(px, py, vx, vy, new Color(200, 50, 50), 10 + rng.nextInt(8), 30));
            }
        }
        synchronized (particles) {
            particles.add(Particle.text(px - 20, py - 25, "💀 陣亡", new Color(255, 80, 80), 45));
        }
    }

    private void spawnReviveParticles(int px, int py) {
        synchronized (particles) {
            particles.add(Particle.text(px - 20, py - 25, "✦ 復活！", new Color(255, 255, 100), 45));
        }
        Random rng = new Random();
        for (int i = 0; i < 10; i++) {
            double angle = rng.nextDouble() * Math.PI * 2;
            double speed = rng.nextDouble() * 3 + 1;
            synchronized (particles) {
                particles.add(new Particle(px, py, Math.cos(angle) * speed, Math.sin(angle) * speed,
                        new Color(255, 255, 100), 12, 30));
            }
        }
    }

    private void spawnStatusParticle(int px, int py, String text, Color color) {
        synchronized (particles) {
            particles.add(Particle.text(px - 25, py - 20, text, color, 40));
        }
    }

    // ── Drag from bench ────────────────────────────────────────────────────────

    public void startDragFromBench(Unit unit, int screenX, int screenY) {
        draggingUnit = unit;
        dragFromBench = true;
        dragSourceX = -1;
        dragSourceY = -1;
        Point p = SwingUtilities.convertPoint((JComponent) SwingUtilities.getRoot(this), screenX, screenY, this);
        mouseX = p.x;
        mouseY = p.y;
    }
}
