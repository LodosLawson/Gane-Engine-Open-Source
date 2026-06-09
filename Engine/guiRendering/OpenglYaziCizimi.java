package guiRendering;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

/**
 * Ekrana yazı (text) ve basit arayüz elemanları (panel, çizgi) çizdirmek için kullanılan sınıftır.
 * Java'nın AWT kütüphanesiyle yazıyı bir resme dönüştürür ve OpenGL ile ekrana çizer.
 * Performans için VBO tabanlı BatchRenderer ve doku önbelleği (cache) kullanır.
 */
public class OpenglYaziCizimi {

    private Font font;
    private BatchRenderer batchRenderer;

    // Metin dokularını önbelleğe almak için (Performans iyileştirmesi)
    private static class TextCacheEntry {
        int textureId;
        int width;
        int height;
        int texWidth;
        int texHeight;
        long lastUsed;
    }
    
    private final Map<String, TextCacheEntry> textCache = new HashMap<>();

    /**
     * Sınıfı kullanıma hazırlar. Yazı tipini oluşturur ve BatchRenderer'ı başlatır.
     */
    public void init() {
        font = new Font("Times New Roman", Font.BOLD, 24);
        batchRenderer = new BatchRenderer();
    }

    /**
     * Örnek çizim testi.
     */
    public void render() {
        beginUI();

        drawPanel(50, 100, 500, 100, new Color(255, 255, 255, 220));
        drawText("Gane Engine - Matrix Modu", 70, 140, Color.BLACK);
        drawSeparator(70, 168, 420, new Color(0, 0, 0, 120));

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
        if (text == null || text.isEmpty()) {
            return;
        }

        TextCacheEntry entry = getTextTexture(text);
        if (entry == null || entry.textureId == 0) {
            return;
        }

        // Yazının, doku üzerindeki oranlarını (UV koordinatlarını) hesapla.
        float u = (float) entry.width / entry.texWidth;
        float v = (float) entry.height / entry.texHeight;

        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;

        // BatchRenderer'ı kullan
        if (batchRenderer.currentTextureId != entry.textureId || batchRenderer.quadCount >= BatchRenderer.MAX_QUADS) {
            batchRenderer.flush();
            batchRenderer.currentTextureId = entry.textureId;
        }

        batchRenderer.drawQuad(x, y, entry.width, entry.height, 0f, 0f, u, v, r, g, b, a);
    }

    /**
     * Düz renkli bir dikdörtgen (panel) çizer.
     * @param x X konumu.
     * @param y Y konumu.
     * @param width Genişlik.
     * @param height Yükseklik.
     * @param color Panelin rengi.
     */
    public void drawPanel(int x, int y, int width, int height, Color color) {
        batchRenderer.drawQuad(x, y, width, height, color, 0);
    }

    /**
     * Ekrana belirtilen koordinat ve boyutta bir 2D kaplama (texture) çizer.
     */
    public void drawTexture(int textureId, int x, int y, int width, int height) {
        batchRenderer.drawQuad(x, y, width, height, Color.WHITE, textureId);
    }

    /**
     * Düz bir çizgi (ayırıcı/separator) çizer.
     * Çizgiyi 1px yüksekliğinde bir panel olarak çizerek Batching'i bozmuyoruz.
     * @param x Başlangıç X konumu.
     * @param y Başlangıç Y konumu.
     * @param width Çizginin uzunluğu.
     * @param color Çizginin rengi.
     */
    public void drawSeparator(int x, int y, int width, Color color) {
        batchRenderer.drawQuad(x, y, width, 1, color, 0);
    }

    /**
     * Arayüz (UI) çizimlerine başlamadan önce OpenGL ortamını 2D çizime hazırlar.
     */
    public void beginUI() {
        // Eski kullanılmayan metin dokularını temizle
        cleanTextCache();

        // 3D shader programını durdur (EntityShader aktif kalırsa glColor4f çalışmaz)
        org.lwjgl.opengl.GL20.glUseProgram(0);

        // 3D'den kalan state'leri kapat
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_LIGHTING);

        // Saydamlık ve alpha testini aç
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Her zaman varsayılan doku birimini aktif et
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
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

        // BatchRenderer'ı başlat
        batchRenderer.begin();
    }

    /**
     * Arayüz (UI) çizimleri bittikten sonra OpenGL ortamını tekrar 3D çizim yapmaya uygun hale getirir.
     */
    public void endUI() {
        // Çizimleri GPU'ya gönder ve batchRenderer'ı kapat
        batchRenderer.end();

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
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
     * Metin için dokuyu getirir veya önbellekte yoksa oluşturur.
     */
    private TextCacheEntry getTextTexture(String textStr) {
        TextCacheEntry entry = textCache.get(textStr);
        long now = System.currentTimeMillis();
        if (entry != null) {
            entry.lastUsed = now;
            return entry;
        }

        entry = new TextCacheEntry();
        
        // Metnin ne kadar piksel yer kaplayacağını ölçmek için geçici bir resim oluştur.
        BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = temp.createGraphics();
        g.setFont(font);
        FontMetrics metrics = g.getFontMetrics(font);
        int w = Math.max(1, metrics.stringWidth(textStr));
        int h = Math.max(1, metrics.getHeight());
        g.dispose();

        int texWidth = nextPowerOfTwo(w);
        int texHeight = nextPowerOfTwo(h);

        // Asıl resmi oluştur ve arka planı tamamen saydam yap.
        BufferedImage image = new BufferedImage(texWidth, texHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setFont(font);
        g2.setColor(new Color(0, 0, 0, 0));
        g2.fillRect(0, 0, texWidth, texHeight);
        g2.setColor(Color.WHITE); // Yazıyı beyaz çiz, render sırasında renk vereceğiz.
        g2.drawString(textStr, 0, metrics.getAscent());
        g2.dispose();

        // Resmi OpenGL'in okuyabileceği bir ByteBuffer formatına dönüştür.
        ByteBuffer buffer = createByteBuffer(image, texWidth, texHeight);

        // OpenGL tarafında yeni kaplamayı oluştur ve ayarlarını yap.
        int id = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, texWidth, texHeight, 0, GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE, buffer);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        entry.textureId = id;
        entry.width = w;
        entry.height = h;
        entry.texWidth = texWidth;
        entry.texHeight = texHeight;
        entry.lastUsed = now;

        textCache.put(textStr, entry);
        return entry;
    }

    /**
     * Önbellekten son 2 saniyedir kullanılmayan dokuları siler.
     * Bu sayede dinamik yazılardan (FPS vb.) kaynaklı bellek sızıntıları önlenir.
     */
    private void cleanTextCache() {
        long now = System.currentTimeMillis();
        java.util.Iterator<Map.Entry<String, TextCacheEntry>> it = textCache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, TextCacheEntry> entry = it.next();
            if (now - entry.getValue().lastUsed > 2000) {
                GL11.glDeleteTextures(entry.getValue().textureId);
                it.remove();
            }
        }
    }

    private ByteBuffer createByteBuffer(BufferedImage image, int texWidth, int texHeight) {
        int[] pixels = new int[texWidth * texHeight];
        image.getRGB(0, 0, texWidth, texHeight, pixels, 0, texWidth);

        ByteBuffer buffer = BufferUtils.createByteBuffer(texWidth * texHeight * 4);
        for (int y = 0; y < texHeight; y++) {
            for (int x = 0; x < texWidth; x++) {
                int pixel = pixels[y * texWidth + x];
                buffer.put((byte) ((pixel >> 16) & 0xFF)); // Red
                buffer.put((byte) ((pixel >> 8) & 0xFF));  // Green
                buffer.put((byte) (pixel & 0xFF));         // Blue
                buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
            }
        }
        buffer.flip();
        return buffer;
    }

    private int nextPowerOfTwo(int value) {
        int result = 1;
        while (result < value) {
            result <<= 1;
        }
        return result;
    }

    public void cleanup() {
        if (batchRenderer != null) {
            batchRenderer.cleanUp();
        }
        for (TextCacheEntry entry : textCache.values()) {
            if (entry.textureId != 0) {
                GL11.glDeleteTextures(entry.textureId);
            }
        }
        textCache.clear();
    }
}
