package terrain;

import scene.Scene;
import utils.ICamera;

/**
 * Tüm terrain (arazi) tipleri için ortak arayüz.
 * FlatTerrain, VoxelTerrain gibi tüm terrain türleri bu arayüzü uygular.
 *
 * Kullanım:
 * terrain.FlatTerrain ft = new terrain.FlatTerrain(512, 512);
 * activeScene.addTerrain(ft);
 */
public interface ITerrain {

    /**
     * Terrain'i ekrana çizer. Her kare çağrılır.
     * 
     * @param camera Aktif kamera
     * @param scene  Sahne (ışık bilgisi vb. için)
     */
    void render(ICamera camera, Scene scene, org.lwjgl.util.vector.Matrix4f toShadowSpace, int shadowMapTexId);

    /**
     * Verilen (x, z) dünya koordinatlarında terrain'in yüzey yüksekliğini döndürür.
     * Fizik motoru ve kamera için kullanılır.
     * 
     * @param x Dünya X koordinatı
     * @param z Dünya Z koordinatı
     * @return Y yüksekliği
     */
    float getHeightAt(float x, float z);

    void setClipPlane(org.lwjgl.util.vector.Vector4f plane);

    float getWidth();

    float getDepth();

    void cleanUp();

    int getVaoId();

    int getIndexVboId();

    int getIndexCount();
}
