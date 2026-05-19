package gane.Menu;

import guiRendering.UIManager;
import guiRendering.UITheme;
import org.lwjgl.opengl.Display;
import gane.MainApp;

public class escMneu {
    private UIManager uiManager;
    private boolean active = false;

    public escMneu() {
        this.uiManager = new UIManager(guiRendering.UITheme.neon());
        int width = 1000, height = 1000;
        recenter(width, height);

        // Butonlar pencereye göre RELATİF konumlanır
        uiManager.addButton(50, 60, 200, 40, "DEVAM ET", () -> {
            this.active = false;
            MainApp.setPaused(false);
        });
        uiManager.addButton(50, 120, 200, 40, "OYUNDAN CIK", () -> {
            MainApp.stop();
        });
        uiManager.addButton(50, 180, 200, 40, "aa", () -> {
        });
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        if (active)
            recenter(400, 400);
    }

    private void recenter(int width, int height) {
        int x = (Display.getWidth() - width) / 2;
        int y = (Display.getHeight() - height) / 2;
        uiManager.setWindowBounds(x, y, width, height);
    }

    public void update() {
        if (active)
            uiManager.update();
    }

    public void render() {
        if (active)
            uiManager.render();
    }
}
