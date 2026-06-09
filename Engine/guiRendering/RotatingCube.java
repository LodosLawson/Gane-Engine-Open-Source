package guiRendering;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

/**
 * Fixed-function OpenGL kullanılarak çizilen, sürekli dönen renkli 3D küp.
 * Herhangi bir external kütüphane veya GLSL shader gerektirmez.
 * 
 * Nasıl Kullanılır:
 *   RotatingCube cube = new RotatingCube(0, 0, -6);
 *   // Oyun döngüsünde:
 *   cube.update(delta);
 *   cube.render();
 */
public class RotatingCube {

    /** Küpün 3D uzaydaki konumu */
    private Vector3f position;

    /** Mevcut Y ekseni dönüş açısı (derece) */
    private float rotationY = 0f;

    /** Mevcut X ekseni dönüş açısı (derece) */
    private float rotationX = 0f;

    /** Saniyede kaç derece döneceği (Y ekseni) */
    private float rotationSpeedY = 60f;

    /** Saniyede kaç derece döneceği (X ekseni) */
    private float rotationSpeedX = 25f;

    /** Küpün yarı kenar uzunluğu */
    private float size = 1.0f;

    /**
     * Belirtilen konumda yeni bir dönen küp oluşturur.
     *
     * @param x 3D uzay X konumu
     * @param y 3D uzay Y konumu
     * @param z 3D uzay Z konumu (negatif değer = kameradan uzak)
     */
    public RotatingCube(float x, float y, float z) {
        this.position = new Vector3f(x, y, z);
    }

    /**
     * Dönüş açısını günceller. Oyun döngüsünde her karede çağrılmalıdır.
     *
     * @param delta Son kareden bu yana geçen süre (saniye)
     */
    public void update(float delta) {
        rotationY = (rotationY + rotationSpeedY * delta) % 360f;
        rotationX = (rotationX + rotationSpeedX * delta) % 360f;
    }

    /**
     * Küpü ekrana çizer. 3D sahne çizimi sırasında (renderScene öncesinde değil, sonrasında değil —
     * renderScene IÇINDE) çağrılması için ayrı bir OpenGL matris bloğu kullanılır.
     * UIManager.render() çağrısından ÖNCE, renderScene() çağrısından SONRA çalıştırılmalıdır.
     */
    public void render() {
        // Shader'ı kapat (fixed-function pipeline kullanacağız)
        org.lwjgl.opengl.GL20.glUseProgram(0);

        // 3D state'i ayarla
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);

        // Mevcut dönüşüm matrisini kaydet
        GL11.glPushMatrix();

        // Konuma taşı
        GL11.glTranslatef(position.x, position.y, position.z);

        // Döndür
        GL11.glRotatef(rotationY, 0f, 1f, 0f);
        GL11.glRotatef(rotationX, 1f, 0f, 0f);

        float s = size;

        // Her yüz farklı renkte çizilir (6 yüz = 6 renk)
        GL11.glBegin(GL11.GL_QUADS);

        // ÖN YÜZ — kırmızı
        GL11.glColor3f(0.9f, 0.25f, 0.25f);
        GL11.glVertex3f(-s, -s,  s);
        GL11.glVertex3f( s, -s,  s);
        GL11.glVertex3f( s,  s,  s);
        GL11.glVertex3f(-s,  s,  s);

        // ARKA YÜZ — mavi
        GL11.glColor3f(0.25f, 0.4f, 0.9f);
        GL11.glVertex3f(-s, -s, -s);
        GL11.glVertex3f(-s,  s, -s);
        GL11.glVertex3f( s,  s, -s);
        GL11.glVertex3f( s, -s, -s);

        // SOL YÜZ — yeşil
        GL11.glColor3f(0.25f, 0.85f, 0.35f);
        GL11.glVertex3f(-s, -s, -s);
        GL11.glVertex3f(-s, -s,  s);
        GL11.glVertex3f(-s,  s,  s);
        GL11.glVertex3f(-s,  s, -s);

        // SAĞ YÜZ — sarı
        GL11.glColor3f(0.95f, 0.85f, 0.1f);
        GL11.glVertex3f( s, -s, -s);
        GL11.glVertex3f( s,  s, -s);
        GL11.glVertex3f( s,  s,  s);
        GL11.glVertex3f( s, -s,  s);

        // ÜST YÜZ — beyaz
        GL11.glColor3f(0.95f, 0.95f, 0.95f);
        GL11.glVertex3f(-s,  s, -s);
        GL11.glVertex3f(-s,  s,  s);
        GL11.glVertex3f( s,  s,  s);
        GL11.glVertex3f( s,  s, -s);

        // ALT YÜZ — mor
        GL11.glColor3f(0.7f, 0.25f, 0.9f);
        GL11.glVertex3f(-s, -s, -s);
        GL11.glVertex3f( s, -s, -s);
        GL11.glVertex3f( s, -s,  s);
        GL11.glVertex3f(-s, -s,  s);

        GL11.glEnd();

        // Rengi beyaza sıfırla
        GL11.glColor3f(1f, 1f, 1f);

        // Matris bloğunu geri al
        GL11.glPopMatrix();
    }
}
