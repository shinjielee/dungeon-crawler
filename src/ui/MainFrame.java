package ui;

import engine.SaveManager;
import game.GameConstants;
import game.GameState;
import game.Phase;
import model.Unit;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MainFrame extends JFrame {
    private final GameState gs;
    private GamePanel gamePanel;
    private ShopPanel shopPanel;
    private SynergyPanel synergyPanel;
    private InfoPanel infoPanel;
    private HUDPanel hudPanel;
    private JPanel titlePanel;
    private JPanel centerWrapper;
    private CardLayout cardLayout;

    private javax.swing.Timer repaintTimer;

    public MainFrame() {
        this.gs = GameState.get();

        setTitle("地牢獵人 - Dungeon Crawler Auto-Battler");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(false);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (gs.phase != Phase.TITLE && gs.phase != Phase.VICTORY && gs.phase != Phase.DEFEAT) {
                    int choice = JOptionPane.showConfirmDialog(MainFrame.this,
                            "是否儲存遊戲後離開？", "離開遊戲",
                            JOptionPane.YES_NO_CANCEL_OPTION);
                    if (choice == JOptionPane.CANCEL_OPTION)
                        return;
                    if (choice == JOptionPane.YES_OPTION)
                        SaveManager.save(gs);
                }
                dispose();
                System.exit(0);
            }
        });

        buildUI();
        showTitleScreen();

        gs.addStateListener(this::onStateChanged);

        pack();
        setLocationRelativeTo(null);

        // Global repaint timer
        repaintTimer = new javax.swing.Timer(33, e -> repaintAll());
        repaintTimer.start();
    }

    private void buildUI() {
        setLayout(new BorderLayout());

        // ── Main game layout ──────────────────────────────────────────────────
        JPanel gameLayout = new JPanel(new BorderLayout());
        gameLayout.setBackground(new Color(10, 10, 20));

        hudPanel = new HUDPanel(gs);
        gamePanel = new GamePanel(gs, unit -> {
            infoPanel.setSelectedUnit(unit);
        });
        synergyPanel = new SynergyPanel(gs);
        infoPanel = new InfoPanel(gs);
        shopPanel = new ShopPanel(gs, unit -> infoPanel.setSelectedUnit(unit), this::repaintAll);

        gameLayout.add(hudPanel, BorderLayout.NORTH);

        JPanel centerArea = new JPanel(new BorderLayout());
        centerArea.add(synergyPanel, BorderLayout.WEST);
        centerArea.add(gamePanel, BorderLayout.CENTER);
        centerArea.add(infoPanel, BorderLayout.EAST);
        gameLayout.add(centerArea, BorderLayout.CENTER);
        gameLayout.add(shopPanel, BorderLayout.SOUTH);

        // ── Title screen ──────────────────────────────────────────────────────
        titlePanel = buildTitlePanel();

        // CardLayout to switch between title and game
        cardLayout = new CardLayout();
        centerWrapper = new JPanel(cardLayout);
        centerWrapper.add(titlePanel, "TITLE");
        centerWrapper.add(gameLayout, "GAME");

        add(centerWrapper, BorderLayout.CENTER);

        // ── Phase buttons ──────────────────────────────────────────────────────
        JPanel phaseButtons = buildPhaseButtonPanel();
        add(phaseButtons, BorderLayout.SOUTH);
    }

    private JPanel buildTitlePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(8, 8, 18));
        panel.setPreferredSize(new Dimension(
                GameConstants.GRID_COLS * GameConstants.CELL_SIZE + 175 + 195,
                GameConstants.GRID_ROWS * GameConstants.CELL_SIZE + 52 + 200));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 30, 0);

        // Title text drawn as custom panel
        JPanel titleText = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // Background gradient
                GradientPaint grad = new GradientPaint(0, 0, new Color(8, 8, 18), getWidth(), getHeight(),
                        new Color(20, 10, 40));
                g2.setPaint(grad);
                g2.fillRect(0, 0, getWidth(), getHeight());

                int cx = getWidth() / 2;
                int cy = getHeight() / 2;

                // Dungeon icon
                g2.setFont(new Font("Monospaced", Font.PLAIN, 60));
                g2.setColor(new Color(200, 100, 50));
                FontMetrics fm = g2.getFontMetrics();
                String icon = "⚔";
                g2.drawString(icon, cx - fm.stringWidth(icon) / 2, cy - 80);

                // Main title
                g2.setFont(new Font("Noto Sans CJK TC", Font.BOLD, 42));
                g2.setColor(new Color(255, 200, 50));
                fm = g2.getFontMetrics();
                String t1 = "地牢獵人";
                g2.drawString(t1, cx - fm.stringWidth(t1) / 2, cy - 10);

                // Subtitle
                g2.setFont(new Font("Noto Sans CJK TC", Font.PLAIN, 18));
                g2.setColor(new Color(150, 150, 255));
                fm = g2.getFontMetrics();
                String t2 = "Dungeon Crawler: Auto-Battler Engine";
                g2.drawString(t2, cx - fm.stringWidth(t2) / 2, cy + 25);

                // Description
                g2.setFont(new Font("Noto Sans CJK TC", Font.PLAIN, 14));
                g2.setColor(new Color(120, 180, 120));
                String[] desc = {
                        "◆ 招募英雄，組建最強陣容",
                        "◆ 利用羈絆效果提升戰力",
                        "◆ 自動戰鬥，策略決勝",
                        "◆ 挑戰第 50 層終極 Boss"
                };
                for (int i = 0; i < desc.length; i++) {
                    fm = g2.getFontMetrics();
                    g2.drawString(desc[i], cx - fm.stringWidth(desc[i]) / 2, cy + 65 + i * 22);
                }
            }
        };
        titleText.setOpaque(false);
        titleText.setPreferredSize(new Dimension(700, 380));

        gbc.fill = GridBagConstraints.BOTH;
        panel.add(titleText, gbc);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        btnPanel.setOpaque(false);

        JButton newGameBtn = makeButton("⚔ 新遊戲", new Color(50, 120, 50));
        newGameBtn.addActionListener(e -> startNewGame());

        JButton loadGameBtn = makeButton("📂 讀取存檔", new Color(50, 80, 140));
        loadGameBtn.setEnabled(SaveManager.hasSaveFile());
        loadGameBtn.addActionListener(e -> loadGame());

        JButton quitBtn = makeButton("✕ 離開", new Color(100, 40, 40));
        quitBtn.addActionListener(e -> System.exit(0));

        btnPanel.add(newGameBtn);
        btnPanel.add(loadGameBtn);
        btnPanel.add(quitBtn);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.fill = GridBagConstraints.NONE;
        panel.add(btnPanel, gbc);

        return panel;
    }

    private JPanel buildPhaseButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 6));
        panel.setBackground(new Color(12, 12, 25));
        panel.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, new Color(50, 50, 100)));

        JButton deployBtn = makeButton("⚔ 進入佈署", new Color(40, 80, 140));
        deployBtn.addActionListener(e -> {
            if (gs.phase == Phase.SHOP) {
                if (gs.getDeployedCount() == 0 && gs.bench.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "請先購買單位！", "提示", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                gs.enterDeploy();
            }
        });

        JButton startCombatBtn = makeButton("▶ 開始戰鬥", new Color(140, 50, 30));
        startCombatBtn.addActionListener(e -> {
            if (gs.phase == Phase.DEPLOY) {
                if (gs.getDeployedCount() == 0) {
                    JOptionPane.showMessageDialog(this, "請至少佈署1個單位！", "提示", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                gs.startCombat();
                gamePanel.startCombat();
            }
        });

        JButton backToShopBtn = makeButton("◀ 返回商店", new Color(50, 70, 100));
        backToShopBtn.addActionListener(e -> {
            if (gs.phase == Phase.DEPLOY) {
                gs.phase = Phase.SHOP;
                gs.notifyListeners();
            }
        });

        JButton saveBtn = makeButton("💾 儲存", new Color(40, 60, 80));
        saveBtn.addActionListener(e -> {
            SaveManager.save(gs);
            JOptionPane.showMessageDialog(this, "遊戲已儲存！", "儲存成功", JOptionPane.INFORMATION_MESSAGE);
        });

        JButton menuBtn = makeButton("🏠 主選單", new Color(60, 40, 80));
        menuBtn.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(this,
                    "回到主選單？（未儲存的進度將消失）", "確認", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                gamePanel.stopCombat();
                showTitleScreen();
            }
        });

        panel.add(deployBtn);
        panel.add(startCombatBtn);
        panel.add(backToShopBtn);
        panel.add(saveBtn);
        panel.add(menuBtn);

        JButton muteBtn = makeButton("🔊 音效", new Color(40, 80, 60));
        muteBtn.addActionListener(e -> {
            game.SoundManager.setMuted(!game.SoundManager.isMuted());
            muteBtn.setText(game.SoundManager.isMuted() ? "🔇 靜音" : "🔊 音效");
        });
        panel.add(muteBtn);

        return panel;
    }

    private JButton makeButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Noto Sans CJK TC", Font.BOLD, 13));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(130, 36));
        btn.addMouseListener(new MouseAdapter() {
            final Color orig = bg;

            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(orig.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(orig);
            }
        });
        return btn;
    }

    // ── State transitions ──────────────────────────────────────────────────────

    private void showTitleScreen() {
        cardLayout.show(centerWrapper, "TITLE");
        gs.phase = Phase.TITLE;
    }

    private void startNewGame() {
        gs.startNewGame();
        cardLayout.show(centerWrapper, "GAME");
        pack();
        setLocationRelativeTo(null);
    }

    private void loadGame() {
        GameState.reset();
        if (SaveManager.load(GameState.get())) {
            cardLayout.show(centerWrapper, "GAME");
            pack();
            setLocationRelativeTo(null);
            JOptionPane.showMessageDialog(this, "存檔載入成功！", "讀取存檔", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "找不到存檔！", "錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onStateChanged() {
        SwingUtilities.invokeLater(() -> {
            // Victory screen
            if (gs.phase == Phase.VICTORY) {
                showEndDialog(true);
            }
            // Defeat screen
            if (gs.phase == Phase.DEFEAT) {
                showEndDialog(false);
            }
            repaintAll();
        });
    }

    private void showEndDialog(boolean victory) {
        gamePanel.stopCombat();
        String title = victory ? "🏆 恭喜通關！" : "💀 遊戲結束";
        String msg;
        if (victory) {
            msg = "恭喜！你擊敗了第 " + GameConstants.MAX_FLOOR + " 層的最終 Boss！\n成功完成地牢挑戰！";
        } else {
            msg = "你的血量歸零，地牢之旅結束了。\n最終抵達：第 " + gs.floor + " 層";
        }
        SaveManager.deleteSave();

        int choice = JOptionPane.showOptionDialog(this, msg, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
                new String[] { "🔄 再來一局", "🏠 回主選單" }, "再來一局");

        if (choice == JOptionPane.YES_OPTION) {
            GameState.reset();
            startNewGame();
        } else {
            GameState.reset();
            showTitleScreen();
        }
    }

    private void repaintAll() {
        hudPanel.repaint();
        synergyPanel.repaint();
        infoPanel.repaint();
        shopPanel.repaint();
        // gamePanel is repainted by its own animation timer
    }
}
