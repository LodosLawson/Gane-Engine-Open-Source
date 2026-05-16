package guiRendering;

import java.awt.Color;

/**
 * OpenGL üzerinde çizilen, tıklanabilir bir 2D buton bileşeni.
 * 
 * Nasıl Kullanılır:
 *   UIButton btn = new UIButton(100, 200, 200, 50, "Tıkla");
 *   btn.setOnClick(() -> System.out.println("Hello World"));
 *   // Oyun döngüsünde:
 *   btn.update(mouseX, mouseY, mousePressed);
 *   btn.render(ui);
 */
public class UIButton {

    /** Butonun ekrandaki X konumu (piksel) */
    private int x, y;
    /** Butonun piksel cinsinden genişliği ve yüksekliği */
    private int width, height;
    /** Buton üzerinde gösterilecek metin etiketi */
    private String label;

    /** Fare butonun üzerindeyken true */
    private boolean hovered = false;
    /** Butonun tıklanıp tıklanmadığını takip eder (tekrar tetiklenmeyi önler) */
    private boolean wasPressed = false;

    /** Buton tıklandığında çalışacak aksiyonu tutan fonksiyon arayüzü */
    private Runnable onClick;

    // --- Renk sabitleri ---
    private static final Color COLOR_NORMAL   = new Color(50, 50, 80, 210);
    private static final Color COLOR_HOVERED  = new Color(80, 80, 140, 230);
    private static final Color COLOR_BORDER   = new Color(120, 120, 200, 255);
    private static final Color COLOR_LABEL    = new Color(230, 230, 255, 255);

    /**
     * Yeni bir UIButton oluşturur.
     *
     * @param x      Sol kenarın X koordinatı
     * @param y      Üst kenarın Y koordinatı
     * @param width  Butonun genişliği (piksel)
     * @param height Butonun yüksekliği (piksel)
     * @param label  Buton üzerindeki yazı
     */
    public UIButton(int x, int y, int width, int height, String label) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.label = label;
    }

    /**
     * Buton tıklandığında çalışacak eylemi (Action) tanımlar.
     *
     * @param onClick Çalıştırılacak lambda veya Runnable
     */
    public void setOnClick(Runnable onClick) {
        this.onClick = onClick;
    }

    /**
     * Fare konumunu ve tıklama durumunu işler.
     * Her karede (frame) oyun döngüsünden çağrılmalıdır.
     *
     * @param mouseX     Farenin ekrandaki X koordinatı
     * @param mouseY     Farenin ekrandaki Y koordinatı
     * @param mouseDown  Sol fare tuşu basılıysa true
     */
    public void update(int mouseX, int mouseY, boolean mouseDown) {
        // Fare butonun üzerinde mi?
        hovered = mouseX >= x && mouseX <= x + width
               && mouseY >= y && mouseY <= y + height;

        if (hovered && mouseDown && !wasPressed) {
            // İlk kez basıldı: onClick tetikle
            if (onClick != null) {
                onClick.run();
            }
            wasPressed = true;
        } else if (!mouseDown) {
            // Fare bırakıldı: tekrar tıklanmaya hazır
            wasPressed = false;
        }
    }

    /**
     * Butonu ekrana çizer. OpenGL'in 2D modunun (beginUI/endUI) içinde çağrılmalıdır.
     *
     * @param ui Yazı ve panel çizimine erişim sağlayan UI renderer
     */
    public void render(OpenglYaziCizimi ui) {
        Color bg     = hovered ? COLOR_HOVERED : COLOR_NORMAL;
        int borderW  = 2;

        // 1. Dış kenarlık (border): biraz büyük, farklı renkte bir dikdörtgen
        ui.drawPanel(x - borderW, y - borderW,
                     width + borderW * 2, height + borderW * 2,
                     COLOR_BORDER);

        // 2. Buton arka planı
        ui.drawPanel(x, y, width, height, bg);

        // 3. Etiket metni: ortalanmış (yaklaşık)
        int textOffsetX = width / 2 - label.length() * 7;
        int textOffsetY = height / 2 - 6;
        ui.drawText(label, x + textOffsetX, y + textOffsetY, COLOR_LABEL);
    }
}
