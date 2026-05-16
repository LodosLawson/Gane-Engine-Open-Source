package guiRendering;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/**
 * Oyun motorunun 2D arayüz yöneticisi.
 * 
 * Ekrana bir pencere (panel), içine butonlar ve tıklama sonucu
 * oluşan metin çıktısı render eder.
 * 
 * Kullanım:
 *   UIManager uiManager = new UIManager();
 *   uiManager.addButton(x, y, w, h, "Etiket", () -> uiManager.showMessage("Hello World!"));
 *   // Oyun döngüsünde:
 *   uiManager.update();
 *   uiManager.render();
 */
public class UIManager {

    /** OpenGL yazı ve panel çizim motoru */
    private OpenglYaziCizimi ui;

    /** Ekranda aktif olan tüm butonlar */
    private List<UIButton> buttons = new ArrayList<>();

    /**
     * Ekranda gösterilecek mesaj metni. null ise gösterilmez.
     */
    private String infoMessage = null;

    /** Mesajın ekranda kalma süresi (milisaniye) */
    private static final long MESSAGE_DURATION_MS = 4000;
    private long infoMessageTime = -1;

    /**
     * UIManager'ı başlatır.
     * OpenGL Display başlatıldıktan SONRA çağrılmalıdır.
     */
    public UIManager() {
        ui = new OpenglYaziCizimi();
        ui.init();
    }

    /**
     * Yöneticiye yeni bir buton ekler.
     *
     * @param x       Sol kenar X
     * @param y       Üst kenar Y
     * @param width   Genişlik (piksel)
     * @param height  Yükseklik (piksel)
     * @param label   Buton etiketi
     * @param onClick Tıklama eylemi
     */
    public void addButton(int x, int y, int width, int height, String label, Runnable onClick) {
        UIButton btn = new UIButton(x, y, width, height, label);
        btn.setOnClick(onClick);
        buttons.add(btn);
    }

    /**
     * Ekranda geçici mesaj gösterir. {@code MESSAGE_DURATION_MS} ms sonra kaybolur.
     *
     * @param message Gösterilecek metin
     */
    public void showMessage(String message) {
        this.infoMessage = message;
        this.infoMessageTime = System.currentTimeMillis();
    }

    /**
     * Fare giriş durumunu okuyup tüm butonları günceller.
     * Her karede renderdan ÖNCE çağrılmalıdır.
     */
    public void update() {
        int mouseX = Mouse.getX();
        // LWJGL Mouse Y ekseni aşağıdan başlar, biz yukarıdan istiyoruz
        int mouseY = Display.getHeight() - Mouse.getY();
        boolean mouseDown = Mouse.isButtonDown(0);

        for (UIButton btn : buttons) {
            btn.update(mouseX, mouseY, mouseDown);
        }

        // Süresi dolan mesajı temizle
        if (infoMessage != null
                && System.currentTimeMillis() - infoMessageTime > MESSAGE_DURATION_MS) {
            infoMessage = null;
        }
    }

    /**
     * Tüm UI bileşenlerini ekrana çizer.
     * 3D sahne renderından SONRA çağrılmalıdır.
     */
    public void render() {
        ui.beginUI();

        int screenW = Display.getWidth();
        int screenH = Display.getHeight();

        // --- Pencere boyutları ---
        int winW = 320;
        int winH = 200;
        int winX = 40;
        int winY = 40;

        // 1. Pencere dış kenarlığı (koyu mavi)
        ui.drawPanel(winX - 2, winY - 2, winW + 4, winH + 4,
                new Color(60, 80, 160));

        // 2. Pencere arka planı (koyu lacivert, tam opak)
        ui.drawPanel(winX, winY, winW, winH,
                new Color(18, 22, 45));

        // 3. Başlık çubuğu (daha koyu şerit)
        ui.drawPanel(winX, winY, winW, 36,
                new Color(30, 40, 100));

        // 4. Başlık metni
        ui.drawText("Gane Engine  |  UI Demo", winX + 12, winY + 10,
                new Color(180, 200, 255));

        // 5. İnce ayırıcı çizgi (başlık altında)
        ui.drawPanel(winX, winY + 36, winW, 2,
                new Color(80, 100, 200));

        // 6. Butonları çiz
        for (UIButton btn : buttons) {
            btn.render(ui);
        }

        // 7. Ekrana yazılan mesaj varsa ortada göster
        if (infoMessage != null) {
            int charW  = 14;
            int msgW   = infoMessage.length() * charW;
            int msgX   = screenW / 2 - msgW / 2;
            int msgY   = screenH / 2 - 40;

            // Mesaj kutusu arka planı
            ui.drawPanel(msgX - 24, msgY - 16, msgW + 48, 60,
                    new Color(10, 60, 20));
            // Mesaj kutusu kenarlığı
            ui.drawPanel(msgX - 26, msgY - 18, msgW + 52, 64,
                    new Color(50, 200, 80));
            ui.drawPanel(msgX - 24, msgY - 16, msgW + 48, 60,
                    new Color(10, 60, 20));
            // Mesaj metni
            ui.drawText(infoMessage, msgX, msgY, new Color(80, 255, 120));
        }

        ui.endUI();
    }

    /**
     * OpenGL kaynaklarını serbest bırakır. Uygulama kapanırken çağrılmalıdır.
     */
    public void cleanup() {
        ui.cleanup();
    }
}
