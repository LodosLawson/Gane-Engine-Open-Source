package guiRendering;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

/**
 * Ekrana yazı (text) ve basit arayüz elemanları (panel, çizgi) çizdirmek için kullanılan sınıftır.
 * Java'nın AWT kütüphanesiyle yazıyı bir resme dönüştürür ve OpenGL ile ekrana çizer.
 */
public class OpenglYaziCizimi {

    /**
     * OpenGL tarafında oluşturulan yazının kaplama (texture) ID'si.
     */
    private int textureId;
    
    /**
     * Çizilecek yazının piksel cinsinden genişliği.
     */
    private int textWidth;
    
    /**
     * Çizilecek yazının piksel cinsinden yüksekliği.
     */
    private int textHeight;
    
    /**
     * OpenGL için oluşturulan 2'nin kuvveti şeklindeki kaplamanın genişliği.
     */
    private int textureWidth;
    
    /**
     * OpenGL için oluşturulan 2'nin kuvveti şeklindeki kaplamanın yüksekliği.
     */
    private int textureHeight;
    
    /**
     * Ekrana çizdirilecek varsayılan metin.
     */
    private String text = "Gane Engine - Matrix Modu";
    
    /**
     * Yazının çizileceği yazı tipi (font) nesnesi.
     */
    private Font font;

    /**
     * Sınıfı kullanıma hazırlar. Yazı tipini oluşturur ve ilk kaplamayı hazırlar.
     */
    public void init() {
        font = new Font("Times New Roman", Font.BOLD, 24);
        createTexture();
    }

    /**
     * Ekrana çizilecek metni günceller ve eğer değiştiyse yeni bir texture (kaplama) oluşturur.
     * @param newText Yeni çizilecek metin.
     */
    public void setText(String newText) {
        if (newText == null || newText.equals(this.text)) { return; } // <--- ÇALIŞMIYOR! (Kullanıcı notu bırakılmış)
        this.text = newText;
        createTexture();
    }

    /**
     * Arayüz elemanlarını ve yazıları ekrana çizmek için kullanılan örnek bir metot.
     * Neden: Kullanıcıya bilgi göstermek veya oyun menülerini çizmek içindir.
     */
    public void render() {
        beginUI();

        drawPanel(50, 100, 500, 100, new Color(255, 255, 255, 220));
        drawText(text, 70, 140, Color.BLACK);
        drawSeparator(70, 168, 420, new Color(0, 0, 0, 120));
        drawPanel(50, 100, 500, 100, new Color(255, 255, 255, 220));
        drawText(text, 70, 140, Color.BLACK);

        endUI();
    }

    /**
     * Belirtilen koordinatlara, belirtilen metni çizer.
     * @param text Çizilecek metin.
     * @param x X koordinatı (ekranın sol üst köşesinden itibaren).
     * @param y Y koordinatı (ekranın sol üst köşesinden itibaren).
     * @param color Yazı rengi.
     */
    public void drawText(String text, int x, int y, Color color) {
        setText(text); // <--- HER KAREDE ÇALIŞIYOR! (Kullanıcı notu bırakılmış)

        if (text == null || text.isEmpty()) {
            return;
        }

        setText(text);
        if (textWidth == 0 || textHeight == 0) {
            return;
        }

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
        bindTextTexture();

        // Yazının, doku üzerindeki oranlarını (UV koordinatlarını) hesapla.
        float u = (float) textWidth / textureWidth;
        float v = (float) textHeight / textureHeight;

        // OpenGL'e renk bilgisini geçir.
        GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f,
                color.getAlpha() / 255f);

        // Dikdörtgen çizimi (QUADS) ile yazının kaplamasını ekrana yapıştır.
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0);
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(u, 0);
        GL11.glVertex2f(x + textWidth, y);
        GL11.glTexCoord2f(u, v);
        GL11.glVertex2f(x + textWidth, y + textHeight);
        GL11.glTexCoord2f(0, v);
        GL11.glVertex2f(x, y + textHeight);
        GL11.glEnd();

        // Rengi sıfırla (beyaz yap) ve kaplamayı serbest bırak.
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    /**
     * Düz renkli bir dikdörtgen (panel) çizer.
     * @param x X konumu.
     * @param y Y konumu.
     * @param width Genişlik.
     * @param height Yükseklik.
     * @param color Panelin rengi (saydamlık içerebilir).
     */
    public void drawPanel(int x, int y, int width, int height, Color color) {
        GL11.glDisable(GL11.GL_TEXTURE_2D); // Kaplamayı kapat, sadece renk çizeceğiz.
        GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f,
                color.getAlpha() / 255f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + width, y);
        GL11.glVertex2f(x + width, y + height);
        GL11.glVertex2f(x, y + height);
        GL11.glEnd();
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    /**
     * Düz bir çizgi (ayırıcı/separator) çizer.
     * @param x Başlangıç X konumu.
     * @param y Başlangıç Y konumu.
     * @param width Çizginin uzunluğu.
     * @param color Çizginin rengi.
     */
    public void drawSeparator(int x, int y, int width, Color color) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f,
                color.getAlpha() / 255f);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + width, y);
        GL11.glEnd();
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    /**
     * Arayüz (UI) çizimlerine başlamadan önce OpenGL ortamını 2D çizime hazırlar.
     * Neden: 3D derinlik testini kapatıp saydamlık ayarlarını açmamız gerekir.
     */
    public void beginUI() {
        // 3D shader programını durdur (EntityShader aktif kalırsa glColor4f çalışmaz)
        org.lwjgl.opengl.GL20.glUseProgram(0);

        // 3D'den kalan state'leri kapat
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_LIGHTING);

        // Saydamlık ve alpha testini aç
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glEnable(GL11.GL_TEXTURE_2D);

        // Projeksiyon matrisini 2D ortografik moda al
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        // (0,0) = sol-üst, (width, height) = sağ-alt
        GL11.glOrtho(0, Display.getWidth(), Display.getHeight(), 0, -1, 1);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        // Renk sıfırla (shader kalmışsa beyaz = çizim görünür)
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    /**
     * Arayüz (UI) çizimleri bittikten sonra OpenGL ortamını tekrar 3D çizim yapmaya uygun hale getirir.
     */
    public void endUI() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    /**
     * Çizilecek yazının OpenGL kaplamasını aktif hale getirir.
     */
    private void bindTextTexture() {
        if (textureId == 0) {
            return;
        }

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
    }

    /**
     * Java'nın dahili kütüphanelerini kullanarak yazıyı saydam bir resim (BufferedImage) üzerine çizer
     * ve bu resmi OpenGL için kullanılabilecek bir kaplamaya (Texture) çevirir.
     */
    private void createTexture() {
        // Metnin ne kadar piksel yer kaplayacağını ölçmek için geçici bir resim oluştur.
        BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = temp.createGraphics();
        g.setFont(font);
        FontMetrics metrics = g.getFontMetrics(font);
        textWidth = Math.max(1, metrics.stringWidth(text));
        textHeight = Math.max(1, metrics.getHeight());
        g.dispose();

        // Eski sistemlerde performans ve uyumluluk için boyutları 2'nin katları yap (Örn: 128x64).
        textureWidth = nextPowerOfTwo(textWidth);
        textureHeight = nextPowerOfTwo(textHeight);

        // Asıl resmi oluştur ve arka planı tamamen saydam yap.
        BufferedImage image = new BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setFont(font);
        g2.setColor(new Color(0, 0, 0, 0));
        g2.fillRect(0, 0, textureWidth, textureHeight);
        g2.setColor(Color.WHITE); // Yazıyı beyaz çiz, render sırasında renk vereceğiz.
        g2.drawString(text, 0, metrics.getAscent());
        g2.dispose();

        // Resmi OpenGL'in okuyabileceği bir ByteBuffer formatına dönüştür.
        ByteBuffer buffer = createByteBuffer(image);

        // Varsa eski kaplamayı sil.
        if (textureId != 0) {
            GL11.glDeleteTextures(textureId);
        }

        // OpenGL tarafında yeni kaplamayı oluştur ve ayarlarını yap.
        textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, textureWidth, textureHeight, 0, GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE, buffer);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    /**
     * Java'nın BufferedImage formatındaki resmini OpenGL'in anlayabileceği byte dizisine dönüştürür.
     * @param image Dönüştürülecek resim nesnesi.
     * @return İçerisinde piksel RGBA bilgilerini tutan ByteBuffer.
     */
    private ByteBuffer createByteBuffer(BufferedImage image) {
        int[] pixels = new int[textureWidth * textureHeight];
        image.getRGB(0, 0, textureWidth, textureHeight, pixels, 0, textureWidth);

        ByteBuffer buffer = BufferUtils.createByteBuffer(textureWidth * textureHeight * 4);
        for (int y = 0; y < textureHeight; y++) {
            for (int x = 0; x < textureWidth; x++) {
                int pixel = pixels[y * textureWidth + x];
                // Java'nın ARGB sistemini OpenGL'in RGBA sistemine çeviriyoruz.
                buffer.put((byte) ((pixel >> 16) & 0xFF)); // Red (Kırmızı)
                buffer.put((byte) ((pixel >> 8) & 0xFF));  // Green (Yeşil)
                buffer.put((byte) (pixel & 0xFF));         // Blue (Mavi)
                buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha (Saydamlık)
            }
        }
        buffer.flip();
        return buffer;
    }

    /**
     * Verilen değerden büyük veya eşit olan, en yakın 2'nin kuvvetini bulur (örn: 100 girilirse 128 döner).
     * @param value Hesaplama yapılacak sayı.
     * @return 2'nin kuvveti olan en yakın üst değer.
     */
    private int nextPowerOfTwo(int value) {
        int result = 1;
        while (result < value) {
            result <<= 1;
        }
        return result;
    }

    /**
     * Oyun veya program kapatılırken OpenGL hafızasındaki yazının kaplamasını (texture) siler.
     * Neden: Bellek sızıntılarını (memory leak) önlemek için gereklidir.
     */
    public void cleanup() {
        if (textureId != 0) {
            GL11.glDeleteTextures(textureId);
            textureId = 0;
        }
    }
}

