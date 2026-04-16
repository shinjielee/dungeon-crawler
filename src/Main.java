import ui.MainFrame;
import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        // Set system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        // CJK font fallback
        Font cjkFont = new Font("Noto Sans CJK TC", Font.PLAIN, 13);
        if (!cjkFont.getFamily().equals("Noto Sans CJK TC")) {
            // Try fallback fonts
            String[] fallbacks = { "微軟正黑體", "Microsoft JhengHei", "SimHei", "Arial Unicode MS" };
            for (String fb : fallbacks) {
                Font f = new Font(fb, Font.PLAIN, 13);
                if (f.getFamily().equals(fb)) {
                    cjkFont = f;
                    break;
                }
            }
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
