package ui;

import game.GameState;
import model.Unit;
import model.Item;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class InfoPanel extends GameStatePanel {
    private Unit selectedUnit;

    public InfoPanel(GameState gs) {
        super(gs);
        setPreferredSize(new Dimension(195, 0));
        setBackground(new Color(12, 12, 25));
        setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, new Color(50, 50, 100)));
    }

    public void setSelectedUnit(Unit unit) {
        this.selectedUnit = unit;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        g2.setFont(new Font("Noto Sans CJK TC", Font.BOLD, 14));
        g2.setColor(new Color(180, 180, 255));
        g2.drawString("🔍 單位詳情", 10, 22);
        g2.setColor(new Color(50, 50, 100));
        g2.drawLine(5, 28, w - 5, 28);

        if (selectedUnit == null) {
            g2.setFont(new Font("Noto Sans CJK TC", Font.PLAIN, 12));
            g2.setColor(new Color(100, 100, 130));
            g2.drawString("右鍵點擊單位", 15, 55);
            g2.drawString("查看詳細資訊", 15, 72);
            drawStoredItems(g2, w, 120);
            return;
        }

        Unit u = selectedUnit;
        int y = 40;
        int pad = 10;
        int lw = w - pad * 2;

        // Unit color circle
        g2.setColor(u.template.color);
        g2.fillOval(pad, y, 36, 36);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Noto Sans CJK TC", Font.BOLD, 14));
        FontMetrics fm = g2.getFontMetrics();
        String sym = u.template.symbol;
        g2.drawString(sym, pad + 18 - fm.stringWidth(sym) / 2, y + 24);

        // Name & stars
        g2.setFont(new Font("Noto Sans CJK TC", Font.BOLD, 15));
        g2.setColor(starColor(u.starLevel));
        g2.drawString(u.template.name, pad + 42, y + 14);
        g2.setFont(new Font("Noto Sans CJK TC", Font.BOLD, 12));
        g2.setColor(new Color(255, 220, 50));
        g2.drawString(u.getStarString(), pad + 42, y + 30);

        y += 44;

        // Tags
        g2.setFont(new Font("Noto Sans CJK TC", Font.PLAIN, 12));
        g2.setColor(new Color(150, 220, 255));
        g2.drawString(u.getTagString(), pad, y);
        y += 18;

        // Rarity
        g2.setColor(rarityColor(u.template.rarity));
        String rarStr = "★".repeat(u.template.rarity) + " 稀有度";
        g2.drawString(rarStr, pad, y);
        y += 20;

        g2.setColor(new Color(50, 50, 100));
        g2.drawLine(pad, y, w - pad, y);
        y += 10;

        // Stats
        g2.setFont(new Font("Noto Sans CJK TC", Font.PLAIN, 12));
        drawStat(g2, pad, y, lw, "❤ HP", (int) u.hp + " / " + (int) u.maxHp, new Color(100, 255, 100));
        y += 17;
        drawStat(g2, pad, y, lw, "⚔ 攻擊", (int) u.ap + "（" + (u.template.isMagicDamage ? "魔法" : "物理") + "）",
                new Color(255, 200, 100));
        y += 17;
        drawStat(g2, pad, y, lw, "🛡 護甲", String.valueOf((int) u.armor), new Color(150, 200, 255));
        y += 17;
        drawStat(g2, pad, y, lw, "✨ 魔抗", String.valueOf((int) u.magicResist), new Color(200, 150, 255));
        y += 17;
        drawStat(g2, pad, y, lw, "⚡ 攻速", String.format("%.1f /s", u.attackSpeed), new Color(255, 255, 150));
        y += 17;
        drawStat(g2, pad, y, lw, "📏 射程", u.attackRange == 1 ? "近戰" : u.attackRange + " 格", new Color(200, 200, 200));
        y += 17;
        drawStat(g2, pad, y, lw, "🎯 暴擊", (int) (u.critChance * 100) + "%", new Color(255, 150, 100));
        y += 17;
        drawStat(g2, pad, y, lw, "💨 閃避", (int) (u.dodgeChance * 100) + "%", new Color(150, 255, 200));
        y += 17;

        // DPS
        double dps = u.ap * u.attackSpeed * (1 + u.critChance);
        drawStat(g2, pad, y, lw, "📊 DPS", String.format("%.0f", dps), new Color(255, 100, 100));
        y += 17;

        g2.setColor(new Color(50, 50, 100));
        g2.drawLine(pad, y, w - pad, y);
        y += 10;

        // Ability
        g2.setFont(new Font("Noto Sans CJK TC", Font.BOLD, 12));
        g2.setColor(new Color(180, 100, 255));
        g2.drawString("★ " + u.template.abilityName, pad, y);
        y += 15;

        g2.setFont(new Font("Noto Sans CJK TC", Font.PLAIN, 11));
        g2.setColor(new Color(180, 180, 210));
        y = drawWrapped(g2, u.template.abilityDesc, pad, y, lw);
        y += 5;

        drawStat(g2, pad, y, lw, "法力需求", u.template.manaRequired + "/100", new Color(100, 180, 255));
        y += 17;

        // Equipped item
        if (u.equippedItem != null) {
            g2.setColor(new Color(50, 50, 100));
            g2.drawLine(pad, y, w - pad, y);
            y += 10;
            g2.setFont(new Font("Noto Sans CJK TC", Font.BOLD, 12));
            g2.setColor(u.equippedItem.color);
            g2.drawString("裝備: " + u.equippedItem.name, pad, y);
            y += 15;
            g2.setFont(new Font("Noto Sans CJK TC", Font.PLAIN, 11));
            g2.setColor(new Color(200, 200, 170));
            g2.drawString(u.equippedItem.description, pad, y);
            y += 15;
        }

        // Stored items section
        drawStoredItems(g2, w, Math.max(y + 10, getHeight() - 120));
    }

    private void drawStoredItems(Graphics2D g2, int w, int y) {
        if (gs.storedItems.isEmpty())
            return;
        int pad = 10;
        g2.setColor(new Color(50, 50, 100));
        g2.drawLine(pad, y, w - pad, y);
        y += 10;
        g2.setFont(new Font("Noto Sans CJK TC", Font.BOLD, 12));
        g2.setColor(new Color(200, 180, 100));
        g2.drawString("🎒 庫存裝備", pad, y);
        y += 18;
        for (Item it : gs.storedItems) {
            g2.setFont(new Font("Noto Sans CJK TC", Font.PLAIN, 11));
            g2.setColor(it.color);
            g2.drawString("  " + it.symbol + " " + it.name, pad, y);
            y += 15;
        }
    }

    private void drawStat(Graphics2D g2, int x, int y, int w, String label, String value, Color valueColor) {
        g2.setColor(new Color(150, 150, 180));
        g2.drawString(label, x, y);
        g2.setColor(valueColor);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(value, x + w - fm.stringWidth(value), y);
    }

    private int drawWrapped(Graphics2D g2, String text, int x, int y, int maxW) {
        if (text == null)
            return y;
        FontMetrics fm = g2.getFontMetrics();
        String[] words = text.split("");
        StringBuilder line = new StringBuilder();
        for (String ch : words) {
            if (fm.stringWidth(line + ch) > maxW) {
                g2.drawString(line.toString(), x, y);
                y += fm.getHeight();
                line = new StringBuilder();
            }
            line.append(ch);
        }
        if (!line.isEmpty()) {
            g2.drawString(line.toString(), x, y);
            y += fm.getHeight();
        }
        return y;
    }

    private Color starColor(int star) {
        return switch (star) {
            case 1 -> new Color(200, 200, 200);
            case 2 -> new Color(100, 200, 255);
            case 3 -> new Color(255, 180, 50);
            default -> Color.WHITE;
        };
    }

    private Color rarityColor(int rarity) {
        return switch (rarity) {
            case 1 -> new Color(150, 150, 150);
            case 2 -> new Color(50, 200, 50);
            case 3 -> new Color(50, 100, 220);
            case 4 -> new Color(150, 50, 220);
            case 5 -> new Color(255, 150, 30);
            default -> Color.WHITE;
        };
    }
}
