package guiRendering;

import java.awt.Color;

/**
 * OpenGL üzerinde çizilen, tıklanabilir ve temalı (UITheme) 2D buton bileşeni.
 *
 * Butonun renk ve görünümü tamamen bağlı olduğu UITheme'den okunur.
 * Tema değiştirilirse buton otomatik olarak yeni temayı yansıtır.
 *
 * Kullanım:
 *   UIButton btn = new UIButton(100, 200, 200, 50, "Tamam", myTheme);
 *   btn.setOnClick(() -> System.out.println("Tıklandı!"));
 *   // Döngüde:
 *   btn.update(mouseX, mouseY, mouseDown);
 *   btn.render(ui);
 */
public class UIButton {

    /** Butonun ekrandaki sol-üst köşe koordinatları (piksel) */
    private int x, y;
    /** Butonun genişliği ve yüksekliği (piksel) */
    private int width, height;
    /** Buton üzerindeki yazı etiketi */
    private String label;

    /** Fare butonun üzerindeyken true */
    private boolean hovered = false;
    /** Tıklama tekrarını önlemek için önceki tuş durumu */
    private boolean wasPressed = false;

    /** Tıklama eylemi (Lambda veya Runnable) */
    private Runnable onClick;

    /**
     * Bu butonun görünümünü belirleyen şablon.
     * Tema değiştirilebilir (setTheme).
     */
    private UITheme theme;

    /**
     * Yeni bir UIButton oluşturur.
     *
     * @param x      Sol kenar X koordinatı (piksel)
     * @param y      Üst kenar Y koordinatı (piksel)
     * @param width  Genişlik (piksel)
     * @param height Yükseklik (piksel)
     * @param label  Buton üzerinde gösterilecek metin
     * @param theme  Kullanılacak görsel tema (null ise dark() varsayılan alınır)
     */
    public UIButton(int x, int y, int width, int height, String label, UITheme theme) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.label = label;
        this.theme = (theme != null) ? theme : UITheme.dark();
    }

    /**
     * Tıklama gerçekleştiğinde çalıştırılacak eylemi atar.
     *
     * @param onClick Lambda veya Runnable
     */
    public void setOnClick(Runnable onClick) {
        this.onClick = onClick;
    }

    /**
     * Fare konumunu ve tıklama durumunu işler.
     * Oyun döngüsünde her karede render öncesinde çağrılmalıdır.
     *
     * @param mouseX    Farenin ekrandaki X koordinatı
     * @param mouseY    Farenin ekrandaki Y koordinatı (yukarıdan aşağı)
     * @param mouseDown Sol fare tuşu basılıysa true
     */
    public void update(int mouseX, int mouseY, boolean mouseDown) {
        hovered = mouseX >= x && mouseX <= x + width
               && mouseY >= y && mouseY <= y + height;

        if (hovered && mouseDown && !wasPressed) {
            if (onClick != null) onClick.run();
            wasPressed = true;
        } else if (!mouseDown) {
            wasPressed = false;
        }
    }

    /**
     * Butonu ekrana çizer. UIManager'ın beginUI() ile açtığı 2D blok içinde çağrılmalıdır.
     *
     * @param ui Çizim için kullanılacak OpenGL yazı motoru
     */
    public void render(OpenglYaziCizimi ui) {
        Color bg     = hovered ? theme.getButtonHovered() : theme.getButtonNormal();
        Color border = theme.getButtonBorder();
        Color label  = theme.getButtonLabelColor();
        int bw = 2; // Border genişliği (piksel)

        // 1. Dış kenarlık
        ui.drawPanel(x - bw, y - bw, width + bw * 2, height + bw * 2, border);
        // 2. Arka plan
        ui.drawPanel(x, y, width, height, bg);
        // 3. Merkeze yakın etiket
        int tx = x + width / 2 - this.label.length() * 7;
        int ty = y + height / 2 - 6;
        ui.drawText(this.label, tx, ty, label);
    }

    /** Butonun bağlı olduğu temayı değiştirir */
    public void setTheme(UITheme theme) {
        this.theme = theme;
    }
}
