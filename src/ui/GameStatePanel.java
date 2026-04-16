package ui;

import game.GameState;

import javax.swing.JPanel;

/**
 * Abstract base panel for all game UI panels.
 *
 * <p>
 * Centralises the shared {@code GameState} reference and the CJK font-family
 * constant so every subclass can use them without repeating boilerplate.
 * To add a new panel, extend this class and implement {@link #paintComponent}.
 */
public abstract class GameStatePanel extends JPanel {

    /** Font family used consistently across all game panels. */
    protected static final String FONT_CJK = "Noto Sans CJK TC";

    protected final GameState gs;

    protected GameStatePanel(GameState gs) {
        this.gs = gs;
    }
}
