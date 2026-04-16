package ui;

import game.GameConstants;
import game.GameState;
import game.Phase;
import model.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class HUDPanel extends GameStatePanel {

    public HUDPanel(GameState gs) {
        super(gs);
        setPreferredSize(new Dimension(0, 52));
        setBackground(new Color(15, 15, 30));
        setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(60, 60, 100)));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        int pad = 12;
        int x = pad;

        // ── Floor & Phase ─────────────────────────────────────────────────────
        String phaseLabel = switch (gs.phase) {
            case SHOP -> "🏪 商店";
            case DEPLOY -> "⚔️ 佈署";
            case COMBAT -> "💥 戰鬥";
            case RESOLUTION -> "📋 結算";
            default -> gs.phase.name();
        };
        g2.setFont(new Font("Noto Sans CJK TC", Font.BOLD, 15));
        drawLabel(g2, x, h / 2, "第 " + gs.floor + " 層  " + phaseLabel, new Color(200, 200, 255));
        x += 240;

        // ── Player HP ─────────────────────────────────────────────────────────
        drawHpBar(g2, x, 10, 130, 32, gs.playerHp, gs.maxPlayerHp);
        x += 145;

        // ── Gold ──────────────────────────────────────────────────────────────
        drawLabel(g2, x, h / 2, "💰 " + gs.playerGold, new Color(255, 215, 0));
        x += 90;

        // ── Level & XP ────────────────────────────────────────────────────────
        int lvl = gs.playerLevel;
        if (lvl < 9) {
            String xpStr = "Lv." + lvl + "  " + gs.playerXp + "/" + gs.getXpForNextLevel() + " XP";
            drawLabel(g2, x, h / 2, xpStr, new Color(100, 220, 255));
        } else {
            drawLabel(g2, x, h / 2, "Lv. MAX", new Color(255, 200, 50));
        }
        x += 170;

        // ── Units on field ────────────────────────────────────────────────────
        int deployed = gs.getDeployedCount();
        int maxDeploy = gs.getMaxFieldUnits();
        Color unitColor = deployed >= maxDeploy ? new Color(255, 150, 50) : new Color(150, 255, 150);
        drawLabel(g2, x, h / 2, "出戰: " + deployed + "/" + maxDeploy, unitColor);
        x += 130;

        // ── Streak ───────────────────────────────────────────────────────────
        if (gs.winStreak >= 2) {
            drawLabel(g2, x, h / 2, "🔥 連勝" + gs.winStreak, new Color(255, 150, 50));
        } else if (gs.lossStreak >= 2) {
            drawLabel(g2, x, h / 2, "💀 連敗" + gs.lossStreak, new Color(180, 100, 100));
        }
    }

    private void drawLabel(Graphics2D g2, int x, int cy, String text, Color color) {
        g2.setColor(color);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, x, cy + fm.getAscent() / 2 - 1);
    }

    private void drawHpBar(Graphics2D g2, int x, int y, int bw, int bh, int hp, int maxHp) {
        g2.setColor(new Color(50, 20, 20));
        g2.fillRoundRect(x, y, bw, bh, 6, 6);

        double pct = (double) hp / maxHp;
        Color barColor = pct > 0.6 ? new Color(60, 200, 60)
                : pct > 0.3 ? new Color(230, 180, 30) : new Color(220, 50, 50);
        g2.setColor(barColor);
        g2.fillRoundRect(x + 1, y + 1, (int) ((bw - 2) * pct), bh - 2, 5, 5);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Noto Sans CJK TC", Font.BOLD, 12));
        String txt = "❤ " + hp + "/" + maxHp;
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(txt, x + (bw - fm.stringWidth(txt)) / 2, y + bh / 2 + fm.getAscent() / 2 - 1);

        g2.setColor(new Color(100, 100, 130));
        g2.drawRoundRect(x, y, bw, bh, 6, 6);
    }
}
