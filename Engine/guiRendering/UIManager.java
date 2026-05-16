package guiRendering;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/**
 * Oyun motorunun 2D arayüz yöneticisi.
 *
 * Tüm UI bileşenlerini (pencere, buton, mesaj) tek bir yerden yönetir.
 * Görünüm tamamen UITheme üzerinden kontrol edilir — varsayılan olarak
 * koyu (dark) tema ile başlar, kullanıcı istediği temayı atayabilir veya
 * özelleştirebilir.
 *
 * Kullanım:
 * // Varsayılan tema ile (dark):
 * UIManager ui = new UIManager();
 *
 * // Hazır bir tema ile:
 * UIManager ui = new UIManager(UITheme.neon());
 *
 * // Özelleştirilmiş tema:
 * UITheme myTheme = UITheme.dark().setPanelBackground(new Color(10, 0, 20));
 * UIManager ui = new UIManager(myTheme);
 *
 * // Buton ekle:
 * ui.addButton(60, 110, 200, 48, "Tamam", () -> ui.showMessage("Merhaba!"));
 *
 * // Oyun döngüsünde:
 * ui.update();
 * ui.render();
 */
public class UIManager {

    /** OpenGL yazı ve panel çizim altyapısı */
    private OpenglYaziCizimi renderer;

    /** Görsel şablon — tüm renkler ve boyutlar buradan okunur */
    private UITheme theme;

    /** Kayıtlı tüm butonlar */
    private List<UIButton> buttons = new ArrayList<>();

    /** Geçici bilgi mesajı (null ise görünmez) */
    private String infoMessage = null;

    /** Mesajın ekranda kalma süresi (ms) */
    private static final long MESSAGE_DURATION_MS = 4000;
    private long infoMessageTime = -1;

    /** Pencere pozisyonu ve boyutu */
    private int winX = 40, winY = 40, winW = 320, winH = 200;

    // surukleme durumu
    private boolean dragging = false;
    private int dragoffsetX, dragoffsetY;

    /**
     * Varsayılan (dark) tema ile UIManager oluşturur.
     * Display başlatıldıktan SONRA çağrılmalıdır.
     */
    public UIManager() {
        this(UITheme.dark());
    }

    /**
     * Belirtilen tema ile UIManager oluşturur.
     *
     * @param theme Kullanılacak görsel şablon
     */
    public UIManager(UITheme theme) {
        this.theme = (theme != null) ? theme : UITheme.dark();
        this.renderer = new OpenglYaziCizimi();
        this.renderer.init();
    }

    // ─── Buton Yönetimi ──────────────────────────────────────────────────

    /**
     * Mevcut tema ile yeni bir buton ekler.
     *
     * @param x       Butonun sol üst X koordinatı
     * @param y       Butonun sol üst Y koordinatı
     * @param width   Genişlik (piksel)
     * @param height  Yükseklik (piksel)
     * @param label   Buton üzerindeki metin
     * @param onClick Tıklanınca çalışacak eylem
     */
    public void addButton(int x, int y, int width, int height, String label, Runnable onClick) {
        UIButton btn = new UIButton(x, y, width, height, label, theme);
        btn.setOnClick(onClick);
        buttons.add(btn);
    }

    // ─── Mesaj ──────────────────────────────────────────────────────────

    /**
     * Ekranın ortasında geçici bir bilgi mesajı gösterir.
     * {@code MESSAGE_DURATION_MS} ms sonra otomatik kaybolur.
     *
     * @param message Gösterilecek metin
     */
    public void showMessage(String message) {
        this.infoMessage = message;
        this.infoMessageTime = System.currentTimeMillis();
    }

    // ─── Tema ────────────────────────────────────────────────────────────

    /**
     * Aktif temayı değiştirir. Tüm butonlar anında yeni temayı kullanır.
     *
     * @param newTheme Yeni UITheme şablonu
     */
    public void setTheme(UITheme newTheme) {
        this.theme = newTheme;
        for (UIButton btn : buttons) {
            btn.setTheme(newTheme);
        }
    }

    /** @return Aktif UITheme şablonunu döndürür */
    public UITheme getTheme() {
        return theme;
    }

    // ─── Pencere Konumu ──────────────────────────────────────────────────

    /**
     * Pencere çerçevesinin konumunu ve boyutunu ayarlar.
     *
     * @param x Pencerenin sol kenar X koordinatı
     * @param y Pencerenin üst kenar Y koordinatı
     * @param w Pencere genişliği (piksel)
     * @param h Pencere yüksekliği (piksel)
     */
    public void setWindowBounds(int x, int y, int w, int h) {
        this.winX = x;
        this.winY = y;
        this.winW = w;
        this.winH = h;
    }

    // ─── Güncelleme ve Çizim ─────────────────────────────────────────────

    /**
     * Fare giriş verilerini okur ve tüm butonları günceller.
     * Oyun döngüsünde renderdan ÖNCE çağrılmalıdır.
     */
    public void update() {
        int mouseX = Mouse.getX();
        int mouseY = Display.getHeight() - Mouse.getY(); // Y'yi ters çevir
        boolean down = Mouse.isButtonDown(0);

        // --- pencere surukleme
        // Baslik Cubugu alani winX, winY > winX+winW, winY + titleBarHeight
        boolean inTitleBar = mouseX >= winX && mouseX <= winX + winW
                && mouseY >= winY && mouseY <= winY + theme.getTitleBarHeight();
        
        if (inTitleBar && down && !dragging) {
            dragging = true;
            dragoffsetX = mouseX - winX; // Farenin pencere içindeki offset'i
            dragoffsetY = mouseY - winY;
        }
        
        if (!down) {
            dragging = false;
        }
        
        if (dragging) {
            winX = mouseX - dragoffsetX; // Pencere konumunu güncelle
            winY = mouseY - dragoffsetY;
        }

        for (UIButton btn : buttons) {
            btn.update(mouseX, mouseY, down, winX, winY);
        }

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
        renderer.beginUI();

        // 1. Pencere dış kenarlığı
        renderer.drawPanel(winX - 2, winY - 2, winW + 4, winH + 4,
                theme.getPanelBorder());

        // 2. Pencere arka planı
        renderer.drawPanel(winX, winY, winW, winH,
                theme.getPanelBackground());

        // 3. Başlık çubuğu
        int tbH = theme.getTitleBarHeight();
        renderer.drawPanel(winX, winY, winW, tbH,
                theme.getTitleBarBackground());

        // 4. Başlık alt çizgisi
        renderer.drawPanel(winX, winY + tbH, winW, 2,
                theme.getTitleBarLine());

        // 5. Başlık metni
        renderer.drawText("Gane Engine", winX + 12, winY + 10,
                theme.getTitleTextColor());

        // 6. Butonlar
        for (UIButton btn : buttons) {
            btn.render(renderer, winX, winY);
        }

        // 7. Bilgi mesajı
        if (infoMessage != null) {
            int charW = 14;
            int msgW = infoMessage.length() * charW;
            int msgX = Display.getWidth() / 2 - msgW / 2;
            int msgY = Display.getHeight() / 2 - 40;
            renderer.drawPanel(msgX - 20, msgY - 12, msgW + 40, 52,
                    theme.getPanelBackground());
            renderer.drawPanel(msgX - 22, msgY - 14, msgW + 44, 56,
                    theme.getPanelBorder());
            renderer.drawPanel(msgX - 20, msgY - 12, msgW + 40, 52,
                    theme.getPanelBackground());
            renderer.drawText(infoMessage, msgX, msgY,
                    theme.getMessageTextColor());
        }

        renderer.endUI();
    }

    /**
     * OpenGL kaynaklarını serbest bırakır. Uygulama kapatılırken çağrılmalıdır.
     */
    public void cleanup() {
        renderer.cleanup();
    }
}
