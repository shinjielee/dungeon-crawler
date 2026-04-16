package ui;

import game.GameConstants;
import game.GameState;
import game.Phase;
import game.SoundManager;
import model.Item;
import model.Unit;
import model.UnitTemplate;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.function.Consumer;

public class ShopPanel extends GameStatePanel {
    private final Consumer<Unit> onUnitSelected;
    private final Runnable onRepaintAll;

    // Drag-for-equip state
    private Item draggingItem = null;

    public ShopPanel(GameState gs, Consumer<Unit> onUnitSelected, Runnable onRepaintAll) {
        super(gs);
        this.onUnitSelected = onUnitSelected;
        this.onRepaintAll = onRepaintAll;

        setPreferredSize(new Dimension(0, 200));
        setBackground(new Color(15, 15, 30));
        setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, new Color(60, 60, 120)));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleClick(e);
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (gs.phase == Phase.TITLE || gs.phase == Phase.VICTORY || gs.phase == Phase.DEFEAT)
            return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        int shopW = w - 420; // reserve right for bench + actions
        int slotW = shopW / GameConstants.SHOP_SIZE;
        int slotH = h - 56;
        int shopY = 32;

        // Title row
        g2.setFont(new Font("Noto Sans CJK TC", Font.BOLD, 13));
        g2.setColor(new Color(180, 180, 255));
        g2.drawString("🏪 商店", 10, 22);

        // Lock indicator
        if (gs.shopLocked) {
            g2.setColor(new Color(255, 200, 50));
            g2.drawString("[已鎖定]", 75, 22);
        }

        // Draw shop slots
        for (int i = 0; i < GameConstants.SHOP_SIZE; i++) {
            int sx = 10 + i * (slotW + 4);
            UnitTemplate ut = (i < gs.shopUnits.size()) ? gs.shopUnits.get(i) : null;
            drawShopSlot(g2, sx, shopY, slotW - 4, slotH, ut, i);
        }

        // Action buttons
        int btnX = shopW + 15;
        drawActionButton(g2, btnX, shopY, 100, 38, "🔄 刷新(" + GameConstants.REFRESH_COST + "金)",
                new Color(40, 80, 140));
        drawActionButton(g2, btnX, shopY + 44, 100, 38, gs.shopLocked ? "🔓 解鎖" : "🔒 鎖定(1金)", new Color(100, 70, 20));
        if (gs.playerLevel < 9)
            drawActionButton(g2, btnX, shopY + 88, 100, 38, "⬆ 升級(" + GameConstants.XP_COST + "金)",
                    new Color(30, 100, 60));

        // Bench area
        int benchX = btnX + 115;
        drawBench(g2, benchX, 0, w - benchX - 5, h);
    }

    private void drawShopSlot(Graphics2D g2, int x, int y, int w, int h, UnitTemplate ut, int index) {
        // Background
        Color bg = ut != null ? new Color(20, 20, 40) : new Color(15, 15, 25);
        g2.setColor(bg);
        g2.fillRoundRect(x, y, w, h, 10, 10);

        if (ut == null) {
            g2.setColor(new Color(40, 40, 60));
            g2.drawRoundRect(x, y, w, h, 10, 10);
            g2.setColor(new Color(60, 60, 90));
            g2.setFont(new Font("Noto Sans CJK TC", Font.PLAIN, 12));
            g2.drawString("已購買", x + w / 2 - 20, y + h / 2 + 5);
            return;
        }

        // Border (rarity color)
        g2.setColor(rarityColor(ut.rarity));
        g2.drawRoundRect(x, y, w, h, 10, 10);

        // Unit circle
        int cx = x + w / 2, cy = y + 36;
        int radius = 26;
        g2.setColor(ut.color.darker());
        g2.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
        g2.setColor(ut.color);
        g2.fillOval(cx - radius + 3, cy - radius + 3, radius * 2 - 6, radius * 2 - 6);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Noto Sans CJK TC", Font.BOLD, 16));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(ut.symbol, cx - fm.stringWidth(ut.symbol) / 2, cy + fm.getAscent() / 2 - 2);

        // Name
        g2.setFont(new Font("Noto Sans CJK TC", Font.BOLD, 12));
        g2.setColor(Color.WHITE);
        fm = g2.getFontMetrics();
        g2.drawString(ut.name, cx - fm.stringWidth(ut.name) / 2, y + 76);

        // Tags
        g2.setFont(new Font("Noto Sans CJK TC", Font.PLAIN, 10));
        g2.setColor(new Color(150, 200, 255));
        StringBuilder tags = new StringBuilder();
        for (int i = 0; i < ut.tags.length; i++) {
            if (i > 0)
                tags.append(" ");
            tags.append(ut.tags[i].displayName);
        }
        String tagStr = tags.toString();
        fm = g2.getFontMetrics();
        g2.drawString(tagStr, cx - fm.stringWidth(tagStr) / 2, y + 90);

        // Cost
        int cost = GameConstants.UNIT_COST[ut.rarity - 1];
        g2.setFont(new Font("Noto Sans CJK TC", Font.BOLD, 13));
        Color costColor = gs.playerGold >= cost ? new Color(255, 215, 0) : new Color(200, 80, 80);
        g2.setColor(costColor);
        String costStr = "💰 " + cost;
        fm = g2.getFontMetrics();
        g2.drawString(costStr, cx - fm.stringWidth(costStr) / 2, y + h - 8);

        // Rarity stars
        g2.setFont(new Font("Noto Sans CJK TC", Font.PLAIN, 10));
        g2.setColor(rarityColor(ut.rarity));
        String rarStr = "★".repeat(ut.rarity);
        fm = g2.getFontMetrics();
        g2.drawString(rarStr, cx - fm.stringWidth(rarStr) / 2, y + 105);
    }

    private void drawActionButton(Graphics2D g2, int x, int y, int w, int h, String label, Color color) {
        g2.setColor(color);
        g2.fillRoundRect(x, y, w, h, 8, 8);
        g2.setColor(color.brighter());
        g2.drawRoundRect(x, y, w, h, 8, 8);
        g2.setFont(new Font("Noto Sans CJK TC", Font.BOLD, 11));
        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        // Wrap into two lines if needed
        if (fm.stringWidth(label) > w - 6) {
            String[] parts = label.split("\\(");
            g2.drawString(parts[0].trim(), x + (w - fm.stringWidth(parts[0].trim())) / 2, y + 15);
            if (parts.length > 1) {
                String second = "(" + parts[1];
                g2.drawString(second, x + (w - fm.stringWidth(second)) / 2, y + 28);
            }
        } else {
            g2.drawString(label, x + (w - fm.stringWidth(label)) / 2, y + h / 2 + fm.getAscent() / 2 - 2);
        }
    }

    private void drawBench(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(new Color(18, 18, 35));
        g2.fillRect(x, y, w, h);
        g2.setColor(new Color(50, 50, 90));
        g2.drawLine(x, y, x, y + h);

        g2.setFont(new Font("Noto Sans CJK TC", Font.BOLD, 12));
        g2.setColor(new Color(150, 150, 200));
        g2.drawString("📦 備用席 (" + gs.bench.size() + "/" + GameConstants.MAX_BENCH + ")", x + 6, 18);

        int slotSize = 54;
        int cols = Math.max(1, w / (slotSize + 4));
        for (int i = 0; i < gs.bench.size(); i++) {
            int bx = x + 4 + (i % cols) * (slotSize + 4);
            int by = 24 + (i / cols) * (slotSize + 4);
            Unit u = gs.bench.get(i);
            // Highlight if selected for deployment
            if (u == gs.selectedBenchUnit) {
                g2.setColor(new Color(255, 220, 50, 120));
                g2.fillRoundRect(bx - 3, by - 3, slotSize + 6, slotSize + 6, 10, 10);
            }
            drawMiniUnit(g2, bx, by, slotSize, u);
        }
    }

    private void drawMiniUnit(Graphics2D g2, int x, int y, int size, Unit u) {
        g2.setColor(new Color(20, 20, 40));
        g2.fillRoundRect(x, y, size, size, 8, 8);
        g2.setColor(rarityColor(u.template.rarity));
        g2.drawRoundRect(x, y, size, size, 8, 8);

        int cx = x + size / 2, cy = y + size / 2 - 2;
        int r = size / 2 - 5;
        g2.setColor(u.template.color);
        g2.fillOval(cx - r, cy - r, r * 2, r * 2);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Noto Sans CJK TC", Font.BOLD, 13));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(u.template.symbol, cx - fm.stringWidth(u.template.symbol) / 2, cy + fm.getAscent() / 2 - 1);

        // Star label
        g2.setFont(new Font("Noto Sans CJK TC", Font.BOLD, 9));
        g2.setColor(new Color(255, 220, 50));
        g2.drawString(u.getStarString(), x + 2, y + size - 4);

        // Item indicator
        if (u.equippedItem != null) {
            g2.setFont(new Font("Monospaced", Font.BOLD, 10));
            g2.setColor(u.equippedItem.color);
            g2.drawString(u.equippedItem.symbol, x + size - 13, y + size - 4);
        }
    }

    private void handleClick(MouseEvent e) {
        if (gs.phase != Phase.SHOP && gs.phase != Phase.DEPLOY)
            return;

        int w = getWidth(), h = getHeight();
        int shopW = w - 420;
        int slotW = shopW / GameConstants.SHOP_SIZE;
        int slotH = h - 56;
        int btnX = shopW + 15;

        // Check shop slots
        for (int i = 0; i < GameConstants.SHOP_SIZE; i++) {
            int sx = 10 + i * (slotW + 4);
            if (e.getX() >= sx && e.getX() <= sx + slotW - 4 && e.getY() >= 32 && e.getY() <= 32 + slotH) {
                if (gs.phase == Phase.SHOP) {
                    if (gs.buyUnit(i))
                        SoundManager.buy();
                }
                return;
            }
        }

        // Check action buttons
        if (e.getX() >= btnX && e.getX() <= btnX + 100) {
            if (e.getY() >= 32 && e.getY() <= 70 && gs.phase == Phase.SHOP) {
                if (gs.playerGold >= GameConstants.REFRESH_COST) {
                    gs.playerGold -= GameConstants.REFRESH_COST;
                    gs.refreshShop();
                    SoundManager.buttonClick();
                }
            } else if (e.getY() >= 76 && e.getY() <= 114) {
                gs.lockShop();
            } else if (e.getY() >= 120 && e.getY() <= 158 && gs.phase == Phase.SHOP) {
                gs.buyXp();
            }
        }

        // Check bench slots (right-click for info, left-click to select)
        int benchX = btnX + 115;
        int slotSize = 54;
        int cols = Math.max(1, (w - benchX - 5) / (slotSize + 4));
        for (int i = 0; i < gs.bench.size(); i++) {
            int bx = benchX + 4 + (i % cols) * (slotSize + 4);
            int by = 24 + (i / cols) * (slotSize + 4);
            if (e.getX() >= bx && e.getX() <= bx + slotSize && e.getY() >= by && e.getY() <= by + slotSize) {
                Unit u = gs.bench.get(i);
                if (SwingUtilities.isRightMouseButton(e)) {
                    onUnitSelected.accept(u);
                } else if (e.getClickCount() == 2) {
                    // Double-click = sell
                    int confirm = JOptionPane.showConfirmDialog(this,
                            "賣出 " + u + " 獲得 " + u.getSellValue() + " 金幣？",
                            "賣出單位", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        gs.sellUnit(u);
                        gs.selectedBenchUnit = null;
                        SoundManager.sell();
                    }
                } else {
                    // Single click: select for deployment (toggle)
                    gs.selectedBenchUnit = (gs.selectedBenchUnit == u) ? null : u;
                    if (gs.selectedBenchUnit != null) {
                        onUnitSelected.accept(u);
                        JOptionPane.showMessageDialog(this,
                                "已選擇【" + u + "】，請在藍色戰場區點擊格子佈署。\n再次點擊同一單位可取消選擇。",
                                "佈署提示", JOptionPane.INFORMATION_MESSAGE);
                    }
                    gs.notifyListeners();
                }
                return;
            }
        }

        onRepaintAll.run();
    }

    private Color rarityColor(int rarity) {
        return switch (rarity) {
            case 1 -> new Color(140, 140, 140);
            case 2 -> new Color(50, 180, 50);
            case 3 -> new Color(50, 80, 200);
            case 4 -> new Color(150, 50, 200);
            case 5 -> new Color(255, 140, 20);
            default -> Color.WHITE;
        };
    }
}
