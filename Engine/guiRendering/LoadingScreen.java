package guiRendering;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import java.awt.Color;

/**
 * Oyun yüklenirken (Asset'ler, texture'lar, modeller vb. okunurken)
 * ekranda gösterilen Yükleme Ekranı (Loading Screen) altyapısı.
 * Geliştirici tarafından çağrılarak anlık yükleme yüzdesi ve metni güncellenebilir.
 */
public class LoadingScreen {

    private OpenglYaziCizimi renderer;
    private UITheme theme;

    public LoadingScreen() {
        this(UITheme.dark()); // Varsayılan tema
    }

    public LoadingScreen(UITheme theme) {
        this.renderer = new OpenglYaziCizimi();
        this.renderer.init();
        this.theme = theme;
    }

    /**
     * Yükleme ekranının temasını dinamik olarak değiştirir.
     */
    public void setTheme(UITheme theme) {
        this.theme = theme;
    }

    /**
     * Yükleme ekranını render eder. Ekrana anlık olarak yansıması için
     * arka planda Display.update() çağırır.
     * 
     * @param progress 0-100 arası yüzde
     * @param statusText Yüklenen içeriği anlatan metin (Örn: "Modeller Yükleniyor...")
     */
    public void render(int progress, String statusText) {
        // Ekranı tam olarak temizle (Z-Buffer ve Renk buffer)
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        renderer.beginUI();

        int screenW = Display.getWidth();
        int screenH = Display.getHeight();

        // 1. Arka Plan
        renderer.drawPanel(0, 0, screenW, screenH, theme.getPanelBackground());

        // 2. Başlık Metni
        String mainTitle = "YUKLENIYOR...";
        renderer.drawText(mainTitle, screenW / 2 - 60, screenH / 2 - 80, theme.getTitleTextColor());

        // 3. Durum Metni (Ne yükleniyor?)
        renderer.drawText(statusText, screenW / 2 - 100, screenH / 2 - 20, theme.getMessageTextColor());

        // 4. İlerleme Çubuğu Çerçevesi (Border)
        int barWidth = 400;
        int barHeight = 30;
        int barX = (screenW - barWidth) / 2;
        int barY = screenH / 2 + 20;

        renderer.drawPanel(barX - 2, barY - 2, barWidth + 4, barHeight + 4, theme.getPanelBorder());
        
        // 5. İlerleme Çubuğu İçi (Arka plan - boş kısım)
        renderer.drawPanel(barX, barY, barWidth, barHeight, theme.getButtonNormal());

        // 6. İlerleme Çubuğu Dolu Kısım (Progress)
        // İlerleme en fazla %100 olabilir
        progress = Math.min(100, Math.max(0, progress));
        int fillWidth = (int) ((progress / 100f) * barWidth);
        if (fillWidth > 0) {
            renderer.drawPanel(barX, barY, fillWidth, barHeight, theme.getButtonHovered());
        }

        // 7. Yüzde Metni
        renderer.drawText("%" + progress, barX + barWidth / 2 - 15, barY + 8, theme.getButtonLabelColor());

        renderer.endUI();

        // Ekrana yansıt (Eğer aktif sahne yoksa direkt bunu bas)
        Display.update();
    }
}
