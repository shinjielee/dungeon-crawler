package ui;

import engine.DataManager;
import game.GameState;
import model.Synergy;
import model.Tag;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class SynergyPanel extends GameStatePanel {

    public SynergyPanel(GameState gs) {
        super(gs);
        setPreferredSize(new Dimension(175, 0));
        setBackground(new Color(12, 12, 25));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 2, new Color(50, 50, 100)));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        // Title
        g2.setFont(new Font("Noto Sans CJK TC", Font.BOLD, 14));
        g2.setColor(new Color(180, 180, 255));
        g2.drawString("★ 羈絆效果", 10, 22);

        g2.setColor(new Color(50, 50, 100));
        g2.drawLine(5, 28, w - 5, 28);

        List<Synergy> synergies = gs.synergyEngine.getAllSynergies();
        int y = 38;
        for (Synergy s : synergies) {
            if (s.currentCount == 0 && !s.isActive())
                continue;
            drawSynergy(g2, s, 6, y, w - 12);
            y += 56;
        }

        if (synergies.stream().noneMatch(s -> s.currentCount > 0)) {
            g2.setFont(new Font("Noto Sans CJK TC", Font.PLAIN, 12));
            g2.setColor(new Color(100, 100, 130));
            g2.drawString("（尚無羈絆）", 15, 55);
        }
    }

    private void drawSynergy(Graphics2D g2, Synergy s, int x, int y, int w) {
        boolean active = s.isActive();
        Color headerColor = active ? new Color(255, 210, 50) : new Color(120, 120, 160);
        Color bgColor = active ? new Color(30, 25, 5) : new Color(20, 20, 35);

        g2.setColor(bgColor);
        g2.fillRoundRect(x, y, w, 50, 8, 8);

        // Border
        g2.setColor(active ? new Color(200, 160, 30) : new Color(50, 50, 80));
        g2.drawRoundRect(x, y, w, 50, 8, 8);

        // Tag name
        g2.setFont(new Font("Noto Sans CJK TC", Font.BOLD, 12));
        g2.setColor(headerColor);
        g2.drawString(s.name, x + 6, y + 16);

        // Count and threshold pips
        drawThresholdPips(g2, s, x + 6, y + 26, w - 12);

        // Description of active level
        if (active) {
            g2.setFont(new Font("Noto Sans CJK TC", Font.PLAIN, 10));
            g2.setColor(new Color(200, 200, 150));
            drawWrapped(g2, s.getActiveDescription(), x + 6, y + 40, w - 12);
        }
    }

    private void drawThresholdPips(Graphics2D g2, Synergy s, int x, int y, int w) {
        int count = s.currentCount;
        int dotSize = 12;
        int gap = 3;
        int cx = x;
        for (int i = 0; i < s.thresholds.length; i++) {
            int thresh = s.thresholds[i];
            boolean reached = count >= thresh;
            g2.setColor(reached ? new Color(255, 200, 50) : new Color(60, 60, 100));
            g2.fillRoundRect(cx, y - 10, dotSize + 2, dotSize, 4, 4);
            g2.setFont(new Font("Monospaced", Font.BOLD, 9));
            g2.setColor(reached ? Color.BLACK : new Color(120, 120, 160));
            g2.drawString(String.valueOf(thresh), cx + 2, y);
            cx += dotSize + gap + 2;
        }
        // Current count label
        g2.setFont(new Font("Noto Sans CJK TC", Font.BOLD, 11));
        g2.setColor(count > 0 ? new Color(255, 255, 200) : new Color(100, 100, 130));
        g2.drawString(" " + count, cx + 2, y);
    }

    private void drawWrapped(Graphics2D g2, String text, int x, int y, int maxW) {
        if (text == null || text.isEmpty())
            return;
        // Simple single-line truncation
        FontMetrics fm = g2.getFontMetrics();
        if (fm.stringWidth(text) <= maxW) {
            g2.drawString(text, x, y);
        } else {
            // Truncate
            String truncated = text;
            while (fm.stringWidth(truncated + "…") > maxW && truncated.length() > 0) {
                truncated = truncated.substring(0, truncated.length() - 1);
            }
            g2.drawString(truncated + "…", x, y);
        }
    }
}
