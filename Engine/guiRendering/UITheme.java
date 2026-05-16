package guiRendering;

import java.awt.Color;

/**
 * UI bileşenlerinin görsel stilini (renk, boyut, font vb.) tanımlayan şablon sınıfı.
 * 
 * Kullanıcı doğrudan varsayılan şablonu kullanabilir ya da
 * ayarları değiştirerek kendi temasını oluşturabilir.
 * 
 * Yerleşik Şablon Seçenekleri:
 *   UITheme.dark()    — Koyu lacivert, mavi tonları (varsayılan)
 *   UITheme.light()   — Açık, beyaz/gri tonlar
 *   UITheme.neon()    — Siyah zemin üstü parlak yeşil/cyan neon
 *
 * Kullanım Örneği:
 *   UITheme theme = UITheme.dark();           // hazır şablonu al
 *   theme.setPanelBackground(Color.RED);      // istediğin rengi değiştir
 *   UIManager ui = new UIManager(theme);      // şablonu uygula
 */
public class UITheme {

    // ─── Pencere / Panel renkleri ─────────────────────────────────
    /** Pencere arka plan rengi */
    private Color panelBackground;
    /** Pencere kenar çizgisi (border) rengi */
    private Color panelBorder;
    /** Başlık çubuğu arka plan rengi */
    private Color titleBarBackground;
    /** Başlık çubuğu alt çizgisi rengi */
    private Color titleBarLine;

    // ─── Buton renkleri ───────────────────────────────────────────
    /** Fare üzerinde değilken buton arka plan rengi */
    private Color buttonNormal;
    /** Fare üzerindeyken buton arka plan rengi (Hover efekti) */
    private Color buttonHovered;
    /** Buton kenarlık rengi */
    private Color buttonBorder;
    /** Buton etiket (metin) rengi */
    private Color buttonLabelColor;

    // ─── Metin renkleri ───────────────────────────────────────────
    /** Başlık metni rengi */
    private Color titleTextColor;
    /** Mesaj/bilgi metni rengi */
    private Color messageTextColor;

    // ─── Boyutlar ─────────────────────────────────────────────────
    /** Başlık çubuğunun piksel cinsinden yüksekliği */
    private int titleBarHeight;

    // ─── Private constructor — sadece factory metotlarından erişilir ───
    private UITheme() {}

    // ══════════════════════════════════════════════════════════════
    //  HAZIR ŞABLONLAR (Factory Methods)
    // ══════════════════════════════════════════════════════════════

    /**
     * Koyu lacivert/mavi tonlardan oluşan varsayılan (default) şablonu döndürür.
     * Hiçbir parametre vermeden kullanmak için uygundur.
     *
     * @return Koyu tema şablonu
     */
    public static UITheme dark() {
        UITheme t = new UITheme();
        t.panelBackground    = new Color(18, 22, 45);
        t.panelBorder        = new Color(60, 80, 160);
        t.titleBarBackground = new Color(30, 40, 100);
        t.titleBarLine       = new Color(80, 100, 200);
        t.buttonNormal       = new Color(50, 50, 80, 210);
        t.buttonHovered      = new Color(80, 80, 140, 230);
        t.buttonBorder       = new Color(120, 120, 200, 255);
        t.buttonLabelColor   = new Color(230, 230, 255, 255);
        t.titleTextColor     = new Color(180, 200, 255);
        t.messageTextColor   = new Color(80, 255, 120);
        t.titleBarHeight     = 36;
        return t;
    }

    /**
     * Açık renk (beyaz/gri) temalı şablonu döndürür.
     * Gündüz / açık ekran tercihlerinde kullanılabilir.
     *
     * @return Açık tema şablonu
     */
    public static UITheme light() {
        UITheme t = new UITheme();
        t.panelBackground    = new Color(240, 240, 245);
        t.panelBorder        = new Color(160, 160, 200);
        t.titleBarBackground = new Color(200, 210, 240);
        t.titleBarLine       = new Color(120, 130, 180);
        t.buttonNormal       = new Color(200, 210, 235, 230);
        t.buttonHovered      = new Color(170, 185, 220, 250);
        t.buttonBorder       = new Color(100, 120, 180, 255);
        t.buttonLabelColor   = new Color(30, 30, 80, 255);
        t.titleTextColor     = new Color(40, 50, 110);
        t.messageTextColor   = new Color(20, 120, 50);
        t.titleBarHeight     = 36;
        return t;
    }

    /**
     * Siyah zemin üstü parlak yeşil/cyan renklerde Neon temalı şablonu döndürür.
     * Retro / siber estetik tercihler için uygundur.
     *
     * @return Neon tema şablonu
     */
    public static UITheme neon() {
        UITheme t = new UITheme();
        t.panelBackground    = new Color(5, 8, 15);
        t.panelBorder        = new Color(0, 220, 180);
        t.titleBarBackground = new Color(0, 30, 25);
        t.titleBarLine       = new Color(0, 220, 180);
        t.buttonNormal       = new Color(0, 40, 35, 210);
        t.buttonHovered      = new Color(0, 80, 65, 240);
        t.buttonBorder       = new Color(0, 220, 180, 255);
        t.buttonLabelColor   = new Color(0, 255, 200, 255);
        t.titleTextColor     = new Color(0, 255, 200);
        t.messageTextColor   = new Color(0, 255, 160);
        t.titleBarHeight     = 36;
        return t;
    }

    // ══════════════════════════════════════════════════════════════
    //  GETTER / SETTER — Bireysel renkleri özelleştirmek için
    // ══════════════════════════════════════════════════════════════

    /** @return Pencere arka plan rengini döndürür */
    public Color getPanelBackground()        { return panelBackground; }
    /** Pencere arka plan rengini atar */
    public UITheme setPanelBackground(Color c)   { panelBackground = c;    return this; }

    /** @return Pencere kenarlık rengini döndürür */
    public Color getPanelBorder()            { return panelBorder; }
    /** Pencere kenarlık rengini atar */
    public UITheme setPanelBorder(Color c)       { panelBorder = c;        return this; }

    /** @return Başlık çubuğu arka plan rengini döndürür */
    public Color getTitleBarBackground()     { return titleBarBackground; }
    /** Başlık çubuğu arka plan rengini atar */
    public UITheme setTitleBarBackground(Color c){ titleBarBackground = c; return this; }

    /** @return Başlık çubuğu alt çizgi rengini döndürür */
    public Color getTitleBarLine()           { return titleBarLine; }
    /** Başlık çubuğu alt çizgi rengini atar */
    public UITheme setTitleBarLine(Color c)      { titleBarLine = c;       return this; }

    /** @return Butonun normal (hover dışı) arka plan rengini döndürür */
    public Color getButtonNormal()           { return buttonNormal; }
    /** Butonun normal arka plan rengini atar */
    public UITheme setButtonNormal(Color c)      { buttonNormal = c;       return this; }

    /** @return Butonun fare üzerindeyken arka plan rengini döndürür */
    public Color getButtonHovered()          { return buttonHovered; }
    /** Butonun hover arka plan rengini atar */
    public UITheme setButtonHovered(Color c)     { buttonHovered = c;      return this; }

    /** @return Buton kenarlık rengini döndürür */
    public Color getButtonBorder()           { return buttonBorder; }
    /** Buton kenarlık rengini atar */
    public UITheme setButtonBorder(Color c)      { buttonBorder = c;       return this; }

    /** @return Buton etiketi metin rengini döndürür */
    public Color getButtonLabelColor()       { return buttonLabelColor; }
    /** Buton etiketi metin rengini atar */
    public UITheme setButtonLabelColor(Color c)  { buttonLabelColor = c;   return this; }

    /** @return Başlık metni rengini döndürür */
    public Color getTitleTextColor()         { return titleTextColor; }
    /** Başlık metni rengini atar */
    public UITheme setTitleTextColor(Color c)    { titleTextColor = c;     return this; }

    /** @return Mesaj metin rengini döndürür */
    public Color getMessageTextColor()       { return messageTextColor; }
    /** Mesaj metin rengini atar */
    public UITheme setMessageTextColor(Color c)  { messageTextColor = c;   return this; }

    /** @return Başlık çubuğunun piksel yüksekliğini döndürür */
    public int getTitleBarHeight()           { return titleBarHeight; }
    /** Başlık çubuğunun yüksekliğini atar */
    public UITheme setTitleBarHeight(int h)      { titleBarHeight = h;     return this; }
}
