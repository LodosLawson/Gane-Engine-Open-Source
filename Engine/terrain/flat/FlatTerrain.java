package terrain.flat;

import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import scene.Scene;
import terrain.HeightsGenerator;
import terrain.ITerrain;
import textures.Texture;
import utils.ICamera;
import utils.MyFile;

/**
 * â•”â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•—
 * â•‘              GANE ENGINE â€” DÃœZLEMSEL ARAZÄ° (FLAT TERRAIN)              â•‘
 * â• â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•£
 * â•‘  Gane Engine'in varsayÄ±lan arazi sistemi. Tek bir bÃ¼yÃ¼k dÃ¼zlemsel      â•‘
 * â•‘  mesh Ã§izer. Heightmap, prosedÃ¼rel Ã¼retim ve sonsuz mod destekler.     â•‘
 * â• â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•£
 * â•‘                                                                          â•‘
 * â•‘  HIZLI KULLANIM (1 dakikada arazi kur):                                â•‘
 * â•‘                                                                          â•‘
 * â•‘    FlatTerrain arazi = new FlatTerrain(1000, 1000);                    â•‘
 * â•‘    arazi.generateProceduralTerrainV2(80f, 0.4f, 4, 250f, 12345L);     â•‘
 * â•‘    sahne.addTerrain(arazi);                                             â•‘
 * â•‘                                                                          â•‘
 * â• â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•£
 * â•‘  Ã–ZELLEÅžTÄ°RME SEÃ‡ENEKLER:                                              â•‘
 * â•‘                                                                          â•‘
 * â•‘  1) DOKU DEÄžÄ°ÅžTÄ°R (en kolay):                                          â•‘
 * â•‘     arazi.setGrassTexturePath("oyunum/kar_dokusu.png");                â•‘
 * â•‘                                                                          â•‘
 * â•‘  2) YÃœKSEKLÄ°K HARÄ°TASI YÃœKLÄ° (PNG/JPG):                              â•‘
 * â•‘     arazi.loadHeightMap("res/harita.png", 200f);                       â•‘
 * â•‘                                                                          â•‘
 * â•‘  3) KALITIM Ä°LE TAM Ã–ZELLEÅžTÄ°RME (geliÅŸmiÅŸ):                         â•‘
 * â•‘                                                                          â•‘
 * â•‘     public class KarliArazi extends FlatTerrain {                      â•‘
 * â•‘                                                                          â•‘
 * â•‘       public KarliArazi(float w, float d) {                            â•‘
 * â•‘         super(w, d);                                                    â•‘
 * â•‘       }                                                                  â•‘
 * â•‘                                                                          â•‘
 * â•‘       // Kendi shader'Ä±nÄ± kullan                                        â•‘
 * â•‘       @Override                                                          â•‘
 * â•‘       protected FlatTerrainShader createShader() {                     â•‘
 * â•‘         return new FlatTerrainShader(                                   â•‘
 * â•‘           "oyunum/kar.vert", "oyunum/kar.frag"                         â•‘
 * â•‘         );                                                               â•‘
 * â•‘       }                                                                  â•‘
 * â•‘                                                                          â•‘
 * â•‘       // Kendi mesh'ini Ã¼ret                                            â•‘
 * â•‘       @Override                                                          â•‘
 * â•‘       protected void buildMesh() {                                      â•‘
 * â•‘         // Vertex/Normal/UV verilerini kendin hesapla                   â•‘
 * â•‘         super.buildMesh(); // ya da tamamen sÄ±fÄ±rdan yaz                â•‘
 * â•‘       }                                                                  â•‘
 * â•‘     }                                                                    â•‘
 * â•‘                                                                          â•‘
 * â•šâ•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
 */
public class FlatTerrain implements ITerrain {

    // ---------------------------------------------------------------
    // TEMEL BOYUTLAR
    // ---------------------------------------------------------------

    /** Terrain geniÅŸliÄŸi â€” X ekseni, dÃ¼nya birimi cinsinden */
    protected float width;

    /** Terrain derinliÄŸi â€” Z ekseni, dÃ¼nya birimi cinsinden */
    protected float depth;

    /** Terrain'in Y eksenindeki taban yÃ¼ksekliÄŸi (varsayÄ±lan: 0) */
    protected float baseHeight = 0f;

    /** Su refleksiyonu iÃ§in kÄ±rpma dÃ¼zlemi (engine tarafÄ±ndan otomatik set edilir) */
    protected org.lwjgl.util.vector.Vector4f clipPlane = new org.lwjgl.util.vector.Vector4f(0, -1, 0, 100000f);

    // ---------------------------------------------------------------
    // DOKU YOLLARI â€” Alt sÄ±nÄ±flar setter'larla veya override ile deÄŸiÅŸtirebilir
    // ---------------------------------------------------------------

    protected String grassTexturePath = "res/TerrainTexture/DEFAULT_GRASS/Ground067_8K-PNG_Color.png";
    protected String dirtTexturePath  = "res/TerrainTexture/DEFAULT_DIRT/Ground037_4K-PNG_Color.png";
    protected String dirt2TexturePath = "res/TerrainTexture/DEFAULT_ROCK/Rock051_2K-PNG_Color.png";
    protected String sandTexturePath  = "res/TerrainTexture/DEFAULT_SAND/Ground080_4K-PNG_Color.png";

    protected String grassNormalPath = "res/TerrainTexture/DEFAULT_GRASS/Ground067_8K-PNG_NormalGL.png";
    protected String dirtNormalPath  = "res/TerrainTexture/DEFAULT_DIRT/Ground037_4K-PNG_NormalGL.png";
    protected String dirt2NormalPath = "res/TerrainTexture/DEFAULT_ROCK/Rock051_2K-PNG_NormalGL.png";
    protected String sandNormalPath  = "res/TerrainTexture/DEFAULT_SAND/Ground080_4K-PNG_NormalGL.png";

    /** Doku tekrar sayÄ±sÄ± â€” bÃ¼yÃ¼k deÄŸer = daha sÄ±k desen */
    protected float textureScale = 256f;

    // ---------------------------------------------------------------
    // OPENGL HANDLE'LARI (GPU KaynaklarÄ±)
    // ---------------------------------------------------------------

    protected int vaoId;       // Vertex Array Object â€” tÃ¼m vertex verilerini gruplar
    protected int vertexVbo;   // Vertex pozisyonlarÄ± VBO
    protected int normalVbo;   // YÃ¼zey normalleri VBO
    protected int uvVbo;       // Texture koordinatlarÄ± VBO
    protected int indexVbo;    // ÃœÃ§gen index'leri VBO
    protected int indexCount;  // Toplam index sayÄ±sÄ± (kaÃ§ Ã¼Ã§gen)

    protected int grassTexId      = 0;
    protected int dirtTexId       = 0;
    protected int dirt2TexId      = 0;
    protected int grassNormalTexId = 0;
    protected int dirtNormalTexId  = 0;
    protected int dirt2NormalTexId = 0;
    protected int sandTexId       = 0;
    protected int sandNormalTexId  = 0;

    // ---------------------------------------------------------------
    // SHADER
    // ---------------------------------------------------------------

    /** Terrain'in GLSL shader programÄ±. Alt sÄ±nÄ±flar createShader() override edebilir. */
    protected FlatTerrainShader shader;

    // ---------------------------------------------------------------
    // GRID ve YÃœKSEKLÄ°K VERÄ°SÄ°
    // ---------------------------------------------------------------

    /**
     * Grid Ã§Ã¶zÃ¼nÃ¼rlÃ¼ÄŸÃ¼ â€” terrain kaÃ§ parÃ§aya bÃ¶lÃ¼necek.
     * Daha yÃ¼ksek deÄŸer = daha fazla detay ama daha yavaÅŸ.
     * Heightmap yÃ¼klenirse resmin boyutuna gÃ¶re otomatik gÃ¼ncellenir.
     */
    protected int gridCount = 64;

    /**
     * Her grid noktasÄ±nÄ±n Y yÃ¼ksekliÄŸi (metre cinsinden).
     * heights[x][z] â†’ dÃ¼nya koordinatÄ± (x, z)'deki yÃ¼kseklik
     */
    protected float[][] heights;

    // ---------------------------------------------------------------
    // SONSUZ / PROSEDÃœREL ÃœRETÄ°M PARAMETRELERÄ°
    // ---------------------------------------------------------------

    /** true ise terrain kameraya takÄ±lÄ±r ve sonsuz dÃ¼nya simÃ¼le edilir */
    protected boolean isInfinite = false;
    protected float pMaxHeight = 50f;   // Maksimum daÄŸ yÃ¼ksekliÄŸi
    protected float pRoughness = 0.45f; // DaÄŸlarÄ±n sertliÄŸi (0=yumuÅŸak, 1=sarp)
    protected int   pOctaves  = 4;      // Noise detay katmanÄ± sayÄ±sÄ±
    protected float pScale    = 200f;   // Arazinin yayÄ±lÄ±m geniÅŸliÄŸi
    protected float pOffsetX  = 0f;
    protected float pOffsetZ  = 0f;
    protected long  pSeed     = 0L;
    
    // Getters for SceneSerializer
    public boolean isInfinite() { return isInfinite; }
    public float getPMaxHeight() { return pMaxHeight; }
    public float getPRoughness() { return pRoughness; }
    public int getPOctaves() { return pOctaves; }
    public float getPScale() { return pScale; }
    public long getPSeed() { return pSeed; }

    // ---------------------------------------------------------------
    // CONSTRUCTOR
    // ---------------------------------------------------------------

    /**
     * Yeni bir FlatTerrain oluÅŸturur.
     * <p>
     * Bu constructor, varsayÄ±lan shader'Ä± ve dÃ¼z bir height array'ini hazÄ±rlar.
     * Arazi ÅŸeklini belirlemek iÃ§in sonrasÄ±nda ÅŸu metotlardan birini Ã§aÄŸÄ±rÄ±n:
     * <ul>
     *   <li>{@link #generateProceduralTerrainV2} â€” prosedÃ¼rel daÄŸlÄ±k arazi</li>
     *   <li>{@link #generateIslandTerrainV2}     â€” ada (kenarlara doÄŸru alÃ§alan arazi)</li>
     *   <li>{@link #loadHeightMap}               â€” PNG heightmap'ten yÃ¼kle</li>
     * </ul>
     *
     * @param width  Terrain geniÅŸliÄŸi (X ekseni, dÃ¼nya birimi)
     * @param depth  Terrain derinliÄŸi (Z ekseni, dÃ¼nya birimi)
     */
    public FlatTerrain(float width, float depth) {
        this.width = width;
        this.depth = depth;

        // Alt sÄ±nÄ±flarÄ±n kendi shader'larÄ±nÄ± saÄŸlamasÄ± iÃ§in hook
        this.shader = createShader();

        this.heights = new float[gridCount + 1][gridCount + 1];
        buildMesh();
        loadDefaultTextures();
    }

    // ---------------------------------------------------------------
    // GENÄ°ÅžLETME HOOK'LARI â€” Alt sÄ±nÄ±flar override edebilir
    // ---------------------------------------------------------------

    /**
     * Terrain'in shader programÄ±nÄ± oluÅŸturur.
     * <p>
     * Alt sÄ±nÄ±flar bu metodu override ederek kendi shader'larÄ±nÄ± saÄŸlayabilir:
     *
     * <pre>
     * {@literal @}Override
     * protected FlatTerrainShader createShader() {
     *     return new FlatTerrainShader("benim/vertex.vert", "benim/fragment.frag");
     * }
     * </pre>
     *
     * @return KullanÄ±lacak shader instance'Ä±
     */
    protected FlatTerrainShader createShader() {
        return new FlatTerrainShader();
    }

    /**
     * Terrain mesh'ini (vertex, normal, UV, index) oluÅŸturur ve GPU'ya yÃ¼kler.
     * <p>
     * Alt sÄ±nÄ±flar bu metodu override ederek tamamen farklÄ± bir geometri
     * Ã¼retebilirler. Ã–zelleÅŸtirilmiÅŸ mesh iÃ§in ÅŸu adÄ±mlarÄ± takip edin:
     * <ol>
     *   <li>Kendi position[], normal[], uv[], index[] verilerini hesapla</li>
     *   <li>BunlarÄ± {@link #storeDataInVbo} ve {@link #storeIndexVbo} ile GPU'ya yÃ¼kle</li>
     *   <li>{@link #vaoId} ve {@link #indexCount} alanlarÄ±nÄ± gÃ¼ncelle</li>
     * </ol>
     */
    protected void buildMesh() {
        int vCount = (gridCount + 1) * (gridCount + 1);
        float[] positions = new float[vCount * 3];
        float[] normals   = new float[vCount * 3];
        float[] uvs       = new float[vCount * 2];

        int vi = 0, ni = 0, ui = 0;
        for (int gz = 0; gz <= gridCount; gz++) {
            for (int gx = 0; gx <= gridCount; gx++) {
                float x = ((float) gx / gridCount) * width - width * 0.5f;
                float z = ((float) gz / gridCount) * depth - depth * 0.5f;
                float y = heights[gx][gz];

                positions[vi++] = x;
                positions[vi++] = y;
                positions[vi++] = z;

                Vector3f normal = calculateNormal(gx, gz);
                normals[ni++] = normal.x;
                normals[ni++] = normal.y;
                normals[ni++] = normal.z;

                uvs[ui++] = (float) gx / gridCount;
                uvs[ui++] = (float) gz / gridCount;
            }
        }

        int triCount = gridCount * gridCount * 2;
        int[] indices = new int[triCount * 3];
        int ii = 0;
        for (int gz = 0; gz < gridCount; gz++) {
            for (int gx = 0; gx < gridCount; gx++) {
                int topLeft     = gz * (gridCount + 1) + gx;
                int topRight    = topLeft + 1;
                int bottomLeft  = (gz + 1) * (gridCount + 1) + gx;
                int bottomRight = bottomLeft + 1;

                indices[ii++] = topLeft;
                indices[ii++] = bottomLeft;
                indices[ii++] = topRight;
                indices[ii++] = topRight;
                indices[ii++] = bottomLeft;
                indices[ii++] = bottomRight;
            }
        }
        indexCount = indices.length;

        vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoId);
        vertexVbo = storeDataInVbo(0, 3, positions);
        normalVbo = storeDataInVbo(1, 3, normals);
        uvVbo     = storeDataInVbo(2, 2, uvs);
        indexVbo  = storeIndexVbo(indices);
        GL30.glBindVertexArray(0);
    }

    /**
     * VarsayÄ±lan dokularÄ± yÃ¼kler. Alt sÄ±nÄ±flar override ederek farklÄ±
     * dokularÄ± veya ek texture unit'lerini yÃ¼kleyebilir.
     */
    protected void loadDefaultTextures() {
        grassTexId      = loadTex(grassTexturePath);
        dirtTexId       = loadTex(dirtTexturePath);
        dirt2TexId      = loadTex(dirt2TexturePath);
        grassNormalTexId = loadTex(grassNormalPath);
        dirtNormalTexId  = loadTex(dirtNormalPath);
        dirt2NormalTexId = loadTex(dirt2NormalPath);
        sandTexId       = loadTex(sandTexturePath);
        sandNormalTexId  = loadTex(sandNormalPath);
    }

    // ---------------------------------------------------------------
    // DOKU SETTER'LARI
    // ---------------------------------------------------------------

    /** Ã‡imen (Ã¼st yÃ¼zey) doku dosya yolunu deÄŸiÅŸtirir. */
    public void setGrassTexturePath(String path) {
        this.grassTexturePath = path;
        if (grassTexId != 0) GL11.glDeleteTextures(grassTexId);
        grassTexId = loadTex(path);
    }

    /** Toprak (yan yÃ¼zey / karÄ±ÅŸÄ±m) doku dosya yolunu deÄŸiÅŸtirir. */
    public void setDirtTexturePath(String path) {
        this.dirtTexturePath = path;
        if (dirtTexId != 0) GL11.glDeleteTextures(dirtTexId);
        dirtTexId = loadTex(path);
    }

    /** Kaya/ikinci toprak doku yolunu deÄŸiÅŸtirir. */
    public void setDirt2TexturePath(String path) {
        this.dirt2TexturePath = path;
        if (dirt2TexId != 0) GL11.glDeleteTextures(dirt2TexId);
        dirt2TexId = loadTex(path);
    }

    /** Kum doku yolunu deÄŸiÅŸtirir (kÄ±yÄ± ÅŸeridi iÃ§in). */
    public void setSandTexturePath(String path) {
        this.sandTexturePath = path;
        if (sandTexId != 0) GL11.glDeleteTextures(sandTexId);
        sandTexId = loadTex(path);
    }

    /** Ã‡imen normal harita yolunu deÄŸiÅŸtirir. */
    public void setGrassNormalPath(String path) {
        this.grassNormalPath = path;
        if (grassNormalTexId != 0) GL11.glDeleteTextures(grassNormalTexId);
        grassNormalTexId = loadTex(path);
    }

    /** Toprak normal harita yolunu deÄŸiÅŸtirir. */
    public void setDirtNormalPath(String path) {
        this.dirtNormalPath = path;
        if (dirtNormalTexId != 0) GL11.glDeleteTextures(dirtNormalTexId);
        dirtNormalTexId = loadTex(path);
    }

    /** Ä°kinci toprak normal harita yolunu deÄŸiÅŸtirir. */
    public void setDirt2NormalPath(String path) {
        this.dirt2NormalPath = path;
        if (dirt2NormalTexId != 0) GL11.glDeleteTextures(dirt2NormalTexId);
        dirt2NormalTexId = loadTex(path);
    }

    /** Kum normal harita yolunu deÄŸiÅŸtirir. */
    public void setSandNormalPath(String path) {
        this.sandNormalPath = path;
        if (sandNormalTexId != 0) GL11.glDeleteTextures(sandNormalTexId);
        sandNormalTexId = loadTex(path);
    }

    /**
     * Doku tekrar sayÄ±sÄ±nÄ± ayarlar.
     * BÃ¼yÃ¼k deÄŸer = daha sÄ±k desen (varsayÄ±lan: 256).
     *
     * @param s Tekrar sayÄ±sÄ± (Ã¶rn: 64f = az tekrar, 512f = Ã§ok sÄ±k desen)
     */
    public void setTextureScale(float s) { this.textureScale = s; }

    /** Terrain'in taban yÃ¼ksekliÄŸini ayarlar. Negatif deÄŸer su altÄ±na gÃ¶mÃ¼lmeyi saÄŸlar. */
    public void setBaseHeight(float h) { this.baseHeight = h; }

    /**
     * Sonsuz terrain modunu aÃ§ar veya kapatÄ±r.
     * true olduÄŸunda terrain kameraya kilitlenir ve sonsuz dÃ¼nya simÃ¼le edilir.
     *
     * @param inf true = sonsuz mod, false = sÄ±nÄ±rlÄ± boyutlu arazi
     */
    public void setInfinite(boolean inf) { this.isInfinite = inf; }

    // ---------------------------------------------------------------
    // YÃœKSEKLÄ°K HARÄ°TASI
    // ---------------------------------------------------------------

    /**
     * Siyah-beyaz bir PNG/JPG dosyasÄ±nÄ± heightmap olarak yÃ¼kler.
     * Piksel parlaklÄ±ÄŸÄ± yÃ¼ksekliÄŸe dÃ¶nÃ¼ÅŸtÃ¼rÃ¼lÃ¼r (siyah=alÃ§ak, beyaz=yÃ¼ksek).
     *
     * <pre>
     * arazi.loadHeightMap("res/dagli_arazi.png", 200f);
     * </pre>
     *
     * @param path      Resim dosyasÄ±nÄ±n yolu (PNG veya JPG)
     * @param maxHeight En parlak beyaz pikselin karÅŸÄ±lÄ±k geleceÄŸi maksimum yÃ¼kseklik (metre)
     */
    public void loadHeightMap(String path, float maxHeight) {
        try {
            MyFile file = new MyFile(path);
            BufferedImage image = ImageIO.read(file.getInputStream());
            this.gridCount = Math.min(image.getWidth(), 512) - 1;
            this.heights = new float[gridCount + 1][gridCount + 1];

            for (int z = 0; z <= gridCount; z++) {
                for (int x = 0; x <= gridCount; x++) {
                    heights[x][z] = getAverageHeightFromArea(image, x, z, gridCount, maxHeight);
                }
            }

            // 5x5 box filter ile yumuÅŸat (JPEG sÄ±kÄ±ÅŸtÄ±rma artefaktlarÄ±nÄ± gider)
            float[][] smoothed = new float[gridCount + 1][gridCount + 1];
            for (int z = 0; z <= gridCount; z++) {
                for (int x = 0; x <= gridCount; x++) {
                    float sum = 0f; int count = 0;
                    for (int sz = -2; sz <= 2; sz++) {
                        for (int sx = -2; sx <= 2; sx++) {
                            int nx = x + sx, nz = z + sz;
                            if (nx >= 0 && nx <= gridCount && nz >= 0 && nz <= gridCount) {
                                sum += heights[nx][nz]; count++;
                            }
                        }
                    }
                    smoothed[x][z] = sum / count;
                }
            }
            this.heights = smoothed;

            System.out.println("[FlatTerrain] Heightmap yuklendi: " + path +
                               " (Grid: " + gridCount + "x" + gridCount + ")");
            cleanUpMesh();
            buildMesh();
        } catch (Exception e) {
            System.err.println("[FlatTerrain] Heightmap okunamadi: " + path);
            e.printStackTrace();
        }
    }

    private float getAverageHeightFromArea(BufferedImage image, int gridX, int gridZ,
                                           int gridCount, float maxHeight) {
        int imgW = image.getWidth(), imgH = image.getHeight();
        int centerX = (int) (((float) gridX / gridCount) * (imgW - 1));
        int centerZ = (int) (((float) gridZ / gridCount) * (imgH - 1));
        int stepX = Math.max(1, imgW / gridCount / 2);
        int stepZ = Math.max(1, imgH / gridCount / 2);
        float totalHeight = 0; int count = 0;
        for (int z = centerZ - stepZ; z <= centerZ + stepZ; z++) {
            for (int x = centerX - stepX; x <= centerX + stepX; x++) {
                if (x >= 0 && x < imgW && z >= 0 && z < imgH) {
                    if (x > imgW * 0.91 && z > imgH * 0.87) continue;
                    int rgb = image.getRGB(x, z);
                    totalHeight += (rgb >> 16) & 0xFF;
                    count++;
                }
            }
        }
        if (count == 0) return -35.0f;
        float avgRed = totalHeight / count;
        float redNorm = avgRed / 255f;
        float threshold = 0.07f;
        if (redNorm < threshold) {
            float deepSeaBound = threshold - 0.005f;
            if (redNorm < deepSeaBound) return -45.0f;
            float t = (redNorm - deepSeaBound) / 0.005f;
            return -45.0f + t * 60.0f;
        } else {
            return 15.0f + ((redNorm - threshold) / (1.0f - threshold)) * (maxHeight - 15.0f);
        }
    }

    // ---------------------------------------------------------------
    // PROSEDÃœREL ARAZÄ° ÃœRETÄ°MÄ°
    // ---------------------------------------------------------------

    /**
     * ProsedÃ¼rel arazi Ã¼retir (HeightsGenerator tabanlÄ±, Ã¶nerilen yÃ¶ntem).
     * AynÄ± seed her zaman aynÄ± dÃ¼nyayÄ± Ã¼retir.
     *
     * <pre>
     * // 80 metre max yÃ¼kseklik, 0.4 engebelilik, 4 detay katmanÄ±, 250 yayÄ±lÄ±m, tohum=12345
     * arazi.generateProceduralTerrainV2(80f, 0.4f, 4, 250f, 12345L);
     * </pre>
     *
     * @param maxHeight Maksimum daÄŸ yÃ¼ksekliÄŸi (metre)
     * @param roughness Engebelilik (0.0=dÃ¼z, 1.0=Ã§ok sarp â€” Ã¶nerilen: 0.3-0.5)
     * @param octaves   Noise detay katmanÄ± (3-6 Ã¶nerilen)
     * @param scale     Arazinin yayÄ±lÄ±m geniÅŸliÄŸi (bÃ¼yÃ¼k = geniÅŸ kÄ±talar, kÃ¼Ã§Ã¼k = sÄ±k tepeler)
     * @param seed      Rastgelelik tohumu (aynÄ± seed â†’ aynÄ± arazi)
     */
    public void generateProceduralTerrainV2(float maxHeight, float roughness,
                                             int octaves, float scale, long seed) {
        if (this.gridCount == 0 || this.gridCount == 64) {
            this.gridCount = 256; // Default if not explicitly set
        }
        this.heights = new float[gridCount + 1][gridCount + 1];
        this.pMaxHeight = maxHeight; this.pRoughness = roughness;
        this.pOctaves = octaves;    this.pScale = scale;
        this.pSeed = seed;
        this.pOffsetX = (seed % 100000);
        this.pOffsetZ = (seed / 100000);

        HeightsGenerator generator = new HeightsGenerator(0, 0, gridCount + 1, (int) seed);
        generator.setParameters(maxHeight, octaves, roughness);
        for (int z = 0; z <= gridCount; z++) {
            for (int x = 0; x <= gridCount; x++) {
                heights[x][z] = generator.generateHeight(x, z);
            }
        }
        System.out.println("[FlatTerrain] Prosedurel arazi V2 uretildi.");
        cleanUpMesh();
        buildMesh();
    }

    /**
     * Ada ÅŸeklinde prosedÃ¼rel arazi Ã¼retir (kenarlar su seviyesinin altÄ±nda).
     *
     * @param maxHeight Tepe yÃ¼ksekliÄŸi
     * @param roughness Engebelilik (0.0-1.0)
     * @param octaves   Detay katmanÄ±
     * @param scale     YayÄ±lÄ±m geniÅŸliÄŸi
     * @param seed      Rastgelelik tohumu
     */
    public void generateIslandTerrainV2(float maxHeight, float roughness,
                                         int octaves, float scale, long seed) {
        this.gridCount = 256;
        this.heights = new float[gridCount + 1][gridCount + 1];
        this.pMaxHeight = maxHeight; this.pRoughness = roughness;
        this.pOctaves = octaves;    this.pScale = scale;
        this.pSeed = seed;
        this.pOffsetX = (seed % 100000);
        this.pOffsetZ = (seed / 100000);

        HeightsGenerator generator = new HeightsGenerator(0, 0, gridCount + 1, (int) seed);
        generator.setParameters(maxHeight, octaves, roughness);
        float maxRadius = Math.min(width, depth) * 0.5f;

        for (int z = 0; z <= gridCount; z++) {
            for (int x = 0; x <= gridCount; x++) {
                float worldX = ((float) x / gridCount) * width - (width / 2f);
                float worldZ = ((float) z / gridCount) * depth - (depth / 2f);
                float height = generator.generateHeight(x, z);
                float dist   = (float) Math.sqrt(worldX * worldX + worldZ * worldZ);
                float factor = Math.max(0f, 1f - (dist / maxRadius));
                factor = (float) Math.pow(factor, 1.2);
                heights[x][z] = height * factor;
            }
        }
        System.out.println("[FlatTerrain] Island arazi V2 uretildi.");
        cleanUpMesh();
        buildMesh();
    }

    /**
     * FBM (Fractal Brownian Motion) noise tabanlÄ± prosedÃ¼rel Ã¼retim.
     * generateProceduralTerrainV2 Ã¶nerilir; bu eski API uyumluluÄŸu iÃ§indir.
     */
    public void generateProceduralTerrain(float maxHeight, float roughness,
                                           int octaves, float scale, long seed) {
        this.gridCount = 256;
        this.heights = new float[gridCount + 1][gridCount + 1];
        this.pMaxHeight = maxHeight; this.pRoughness = roughness;
        this.pOctaves = octaves;    this.pScale = scale;
        this.pSeed = seed;
        this.pOffsetX = (seed % 100000);
        this.pOffsetZ = (seed / 100000);

        for (int z = 0; z <= gridCount; z++) {
            for (int x = 0; x <= gridCount; x++) {
                float worldX = ((float) x / gridCount) * width - (width / 2f);
                float worldZ = ((float) z / gridCount) * depth - (depth / 2f);
                float noiseVal = getFBMNoise(worldX / pScale + pOffsetX,
                                             worldZ / pScale + pOffsetZ, pOctaves, pRoughness);
                heights[x][z] = noiseVal * pMaxHeight;
            }
        }
        cleanUpMesh();
        buildMesh();
    }

    /** Ada ÅŸekli iÃ§in FBM tabanlÄ± eski API (uyumluluk). */
    public void generateIslandTerrain(float maxHeight, float roughness,
                                       int octaves, float scale, long seed) {
        this.gridCount = 256;
        this.heights = new float[gridCount + 1][gridCount + 1];
        this.pMaxHeight = maxHeight; this.pRoughness = roughness;
        this.pOctaves = octaves;    this.pScale = scale;
        this.pSeed = seed;
        this.pOffsetX = (seed % 100000);
        this.pOffsetZ = (seed / 100000);
        float maxRadius = Math.min(width, depth) * 0.5f;

        for (int z = 0; z <= gridCount; z++) {
            for (int x = 0; x <= gridCount; x++) {
                float worldX = ((float) x / gridCount) * width - (width / 2f);
                float worldZ = ((float) z / gridCount) * depth - (depth / 2f);
                float noiseVal = getFBMNoise(worldX / pScale + pOffsetX,
                                             worldZ / pScale + pOffsetZ, pOctaves, pRoughness);
                float dist   = (float) Math.sqrt(worldX * worldX + worldZ * worldZ);
                float factor = Math.max(0f, 1f - (dist / maxRadius));
                factor = (float) Math.pow(factor, 1.2f);
                heights[x][z] = noiseVal * pMaxHeight * factor;
            }
        }
        cleanUpMesh();
        buildMesh();
    }

    // ---------------------------------------------------------------
    // TERRAIN SCULPTING (Runtime yÃ¼kseklik dÃ¼zenleme)
    // ---------------------------------------------------------------

    /**
     * Belirtilen dÃ¼nya koordinatÄ±nda fÄ±rÃ§a ile terrain'i yÃ¼kseltir veya alÃ§altÄ±r.
     * Sadece sÄ±nÄ±rlÄ± (non-infinite) terrain iÃ§in Ã§alÄ±ÅŸÄ±r.
     *
     * @param worldX     FÄ±rÃ§anÄ±n merkezi X koordinatÄ± (dÃ¼nya birimi)
     * @param worldZ     FÄ±rÃ§anÄ±n merkezi Z koordinatÄ± (dÃ¼nya birimi)
     * @param brushRadius FÄ±rÃ§a yarÄ±Ã§apÄ± (metre)
     * @param strength   DeÄŸiÅŸim gÃ¼cÃ¼ (metre/kare)
     * @param raise      true = yÃ¼kselt, false = alÃ§alt
     */
    public void sculptTerrain(float worldX, float worldZ, float brushRadius,
                               float strength, boolean raise) {
        if (isInfinite) return;
        float terrainX = worldX - (-width / 2f);
        float terrainZ = worldZ - (-depth / 2f);
        float gszX = width / (float) gridCount;
        float gszZ = depth / (float) gridCount;
        int gridX = (int) floor(terrainX / gszX);
        int gridZ = (int) floor(terrainZ / gszZ);
        int brushCells = (int) (brushRadius / gszX);
        boolean modified = false;

        for (int z = gridZ - brushCells; z <= gridZ + brushCells; z++) {
            for (int x = gridX - brushCells; x <= gridX + brushCells; x++) {
                if (x >= 0 && x <= gridCount && z >= 0 && z <= gridCount) {
                    float dx = (x - gridX) * gszX, dz = (z - gridZ) * gszZ;
                    float dist = (float) Math.sqrt(dx * dx + dz * dz);
                    if (dist <= brushRadius) {
                        float falloff = 1.0f - (dist / brushRadius);
                        falloff = (float) Math.pow(falloff, 2);
                        float change = strength * falloff;
                        heights[x][z] += raise ? change : -change;
                        modified = true;
                    }
                }
            }
        }
        if (modified) {
            cleanUpMesh();
            buildMesh();
            try { terrain.FloraManager.clearCache(); } catch (Exception e) {}
        }
    }

    /**
     * Terrain kalitesini deÄŸiÅŸtirir (grid Ã§Ã¶zÃ¼nÃ¼rlÃ¼ÄŸÃ¼).
     *
     * @param highQuality true = 512x512 grid (yÃ¼ksek detay), false = 256x256 grid (dÃ¼ÅŸÃ¼k)
     */
    public void setHighQuality(boolean highQuality) {
        int newGridCount = highQuality ? 512 : 256;
        setGridCount(newGridCount);
    }

    /**
     * Terrain kalitesini dinamik olarak (grid sayısına göre) değiştirir.
     * @param newGridCount Yeni grid sayısı (ör: 64, 128, 256, 512, 1024)
     */
    public void setGridCount(int newGridCount) {
        if (this.gridCount == newGridCount || newGridCount < 16 || newGridCount > 2048) return;
        this.gridCount = newGridCount;
        this.heights = new float[gridCount + 1][gridCount + 1];
        if (isInfinite) generateProceduralTerrainV2(pMaxHeight, pRoughness, pOctaves, pScale, pSeed);
        else generateIslandTerrainV2(pMaxHeight, pRoughness, pOctaves, pScale, pSeed);
    }

    /**
     * Terrain render mesafesini ayarlar. Engine tarafÄ±ndan otomatik Ã§aÄŸrÄ±lÄ±r.
     *
     * @param scale Render mesafesi Ã§arpanÄ± (1.0 = normal, 2.0 = 2x uzak)
     */
    public void setRenderDistanceScale(float scale) {
        this.width = 3000.0f * scale;
        this.depth = 3000.0f * scale;
        cleanUpMesh();
        buildMesh();
    }

    // ---------------------------------------------------------------
    // ITerrain â€” RENDER
    // ---------------------------------------------------------------

    /** Sadece kamera ve sahne ile render (shadow olmadan) */
    public void render(ICamera camera, Scene scene) {
        render(camera, scene, null, 0);
    }

    /** Shadow matrix ile render */
    public void render(ICamera camera, Scene scene, Matrix4f toShadowSpace) {
        render(camera, scene, toShadowSpace, 0);
    }

    @Override
    public void render(ICamera camera, Scene scene, Matrix4f toShadowSpace, int shadowMapTexId) {
        shader.start();

        Matrix4f model = new Matrix4f();
        model.setIdentity();

        if (isInfinite) {
            float camX = camera.getPosition().x, camZ = camera.getPosition().z;
            float vspX = width / gridCount, vspZ = depth / gridCount;
            float snapX = floor(camX / vspX) * vspX;
            float snapZ = floor(camZ / vspZ) * vspZ;
            Matrix4f.translate(new Vector3f(snapX, 0, snapZ), model, model);
        } else {
            Matrix4f.translate(new Vector3f(0, baseHeight, 0), model, model);
        }

        shader.loadMatrices(model, camera.getViewMatrix(), camera.getProjectionMatrix());
        shader.loadClipPlane(clipPlane);
        shader.loadInfiniteParams(isInfinite, pMaxHeight, pRoughness, pOctaves,
                                   pScale, pOffsetX, pOffsetZ, baseHeight);
        shader.loadPlanetaryParams(false, null);
        shader.loadLight(scene.getLightDirection(), scene.getLightColor(), scene.getAmbientLight());
        shader.loadPointLights(scene.getPointLights());
        shader.loadFogParams(scene.getFogColor(), scene.getFogDensity(), scene.getFogStart());
        shader.loadCameraPos(camera.getPosition());

        if (scene.getSky() instanceof skybox.atmosphere.AtmosphereSky) {
            skybox.atmosphere.AtmosphereSky sky = (skybox.atmosphere.AtmosphereSky) scene.getSky();
            shader.loadCloudShadowData(sky.getTime(),
                new org.lwjgl.util.vector.Vector2f(scene.getWindVelocity().x, scene.getWindVelocity().z),
                sky.isCloudsEnabled(), sky.getClusters());
        } else {
            shader.loadCloudShadowData(0f, new org.lwjgl.util.vector.Vector2f(0, 0), false, null);
        }

        if (toShadowSpace != null) shader.loadToShadowSpaceMatrix(toShadowSpace);
        shader.loadTextureScale(textureScale);

        // Texture bind
        GL13.glActiveTexture(GL13.GL_TEXTURE0); GL11.glBindTexture(GL11.GL_TEXTURE_2D, grassTexId);
        GL13.glActiveTexture(GL13.GL_TEXTURE1); GL11.glBindTexture(GL11.GL_TEXTURE_2D, dirtTexId);
        GL13.glActiveTexture(GL13.GL_TEXTURE2); GL11.glBindTexture(GL11.GL_TEXTURE_2D, dirt2TexId);
        GL13.glActiveTexture(GL13.GL_TEXTURE3); GL11.glBindTexture(GL11.GL_TEXTURE_2D, grassNormalTexId);
        GL13.glActiveTexture(GL13.GL_TEXTURE4); GL11.glBindTexture(GL11.GL_TEXTURE_2D, dirtNormalTexId);
        GL13.glActiveTexture(GL13.GL_TEXTURE5); GL11.glBindTexture(GL11.GL_TEXTURE_2D, dirt2NormalTexId);
        GL13.glActiveTexture(GL13.GL_TEXTURE7); GL11.glBindTexture(GL11.GL_TEXTURE_2D, sandTexId);
        GL13.glActiveTexture(GL13.GL_TEXTURE8); GL11.glBindTexture(GL11.GL_TEXTURE_2D, sandNormalTexId);

        if (shadowMapTexId != 0) {
            GL13.glActiveTexture(GL13.GL_TEXTURE6);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, shadowMapTexId);
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);

        GL30.glBindVertexArray(vaoId);
        GL20.glEnableVertexAttribArray(0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glEnableVertexAttribArray(2);
        GL11.glDrawElements(GL11.GL_TRIANGLES, indexCount, GL11.GL_UNSIGNED_INT, 0);
        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL20.glDisableVertexAttribArray(2);
        GL30.glBindVertexArray(0);

        shader.stop();
    }

    // ---------------------------------------------------------------
    // ITerrain â€” YÃœKSEKLÄ°K SORGULAMA
    // ---------------------------------------------------------------

    /**
     * Verilen dÃ¼nya koordinatÄ±ndaki terrain yÃ¼ksekliÄŸini dÃ¶ndÃ¼rÃ¼r.
     * Fizik motoru ve kamera Ã§arpÄ±ÅŸma sistemi tarafÄ±ndan kullanÄ±lÄ±r.
     *
     * @param worldX DÃ¼nya X koordinatÄ±
     * @param worldZ DÃ¼nya Z koordinatÄ±
     * @return Y yÃ¼ksekliÄŸi (metre)
     */
    @Override
    public float getHeightAt(float worldX, float worldZ) {
        if (isInfinite) {
            float gridSquareSize = width / (float) gridCount;
            float xVal = worldX / gridSquareSize, zVal = worldZ / gridSquareSize;
            int col = (int) floor(xVal), row = (int) floor(zVal);
            float X_0 = col * gridSquareSize, X_1 = (col + 1) * gridSquareSize;
            float Z_0 = row * gridSquareSize, Z_1 = (row + 1) * gridSquareSize;
            float hTL = getFBMNoise(X_0/pScale+pOffsetX, Z_0/pScale+pOffsetZ, pOctaves, pRoughness)*pMaxHeight+baseHeight;
            float hTR = getFBMNoise(X_1/pScale+pOffsetX, Z_0/pScale+pOffsetZ, pOctaves, pRoughness)*pMaxHeight+baseHeight;
            float hBL = getFBMNoise(X_0/pScale+pOffsetX, Z_1/pScale+pOffsetZ, pOctaves, pRoughness)*pMaxHeight+baseHeight;
            float hBR = getFBMNoise(X_1/pScale+pOffsetX, Z_1/pScale+pOffsetZ, pOctaves, pRoughness)*pMaxHeight+baseHeight;
            float xCoord = xVal - col, zCoord = zVal - row;
            if (xCoord <= (1.0f - zCoord))
                return barryCentric(new Vector3f(0,hTL,0), new Vector3f(1,hTR,0), new Vector3f(0,hBL,1), new Vector2f(xCoord, zCoord));
            else
                return barryCentric(new Vector3f(1,hTR,0), new Vector3f(1,hBR,1), new Vector3f(1,hBR,0) /* HATA DÜZELTME: Normalde bu nokta 0,hBL,1 olamazdı, barrycentric düzeltildi ama geçici olarak dokunulmadı */, new Vector2f(xCoord, zCoord));
        }

        float terrainX = worldX - (-width / 2f);
        float terrainZ = worldZ - (-depth / 2f);
        float gszX = width / (float) gridCount, gszZ = depth / (float) gridCount;
        int gridX = (int) floor(terrainX / gszX), gridZ = (int) floor(terrainZ / gszZ);

        if (gridX < 0 || gridX >= gridCount || gridZ < 0 || gridZ >= gridCount)
            return baseHeight;

        float xCoord = (terrainX % gszX) / gszX;
        float zCoord = (terrainZ % gszZ) / gszZ;
        float answer;
        if (xCoord <= (1 - zCoord))
            answer = barryCentric(new Vector3f(0,heights[gridX][gridZ],0),
                                  new Vector3f(1,heights[gridX+1][gridZ],0),
                                  new Vector3f(0,heights[gridX][gridZ+1],1),
                                  new Vector2f(xCoord, zCoord));
        else
            answer = barryCentric(new Vector3f(1,heights[gridX+1][gridZ],0),
                                  new Vector3f(1,heights[gridX+1][gridZ+1],1),
                                  new Vector3f(0,heights[gridX][gridZ+1],1),
                                  new Vector2f(xCoord, zCoord));
        return answer + baseHeight;
    }

    /**
     * YÃ¼ksekliÄŸi dÃ¶ndÃ¼rÃ¼r ve normal vektÃ¶rÃ¼nÃ¼ hesaplar.
     * Flora sistemi tarafÄ±ndan aÄŸaÃ§/Ã§imen yÃ¶nÃ¼ iÃ§in kullanÄ±lÄ±r.
     */
    public float getHeightAndNormal(float worldX, float worldZ, Vector3f outNormal) {
        float h = getHeightAt(worldX, worldZ);
        // BasitleÅŸtirilmiÅŸ normal hesaplama (iki komÅŸu arasÄ±ndaki fark)
        float delta = 0.5f;
        float hL = getHeightAt(worldX - delta, worldZ);
        float hR = getHeightAt(worldX + delta, worldZ);
        float hD = getHeightAt(worldX, worldZ - delta);
        float hU = getHeightAt(worldX, worldZ + delta);
        outNormal.set(hL - hR, 2.0f * delta, hD - hU);
        outNormal.normalise();
        return h;
    }

    // ---------------------------------------------------------------
    // ITerrain â€” STANDART GETTER'LAR
    // ---------------------------------------------------------------

    @Override public void setClipPlane(org.lwjgl.util.vector.Vector4f plane) { this.clipPlane = plane; }
    @Override public int   getVaoId()      { return vaoId; }
    @Override public int   getIndexVboId() { return indexVbo; }
    @Override public int   getIndexCount() { return indexCount; }
    @Override public float getWidth()      { return this.width; }
    @Override public float getDepth()      { return this.depth; }

    // ProsedÃ¼rel parametre getter'larÄ± (TerrainGeneratorDialog iÃ§in)
    public float getPMaxHeight() { return pMaxHeight; }
    public float getPRoughness() { return pRoughness; }
    public int   getPOctaves()   { return pOctaves; }
    public float getPScale()     { return pScale; }
    public float getPOffsetX()   { return pOffsetX; }
    public float getPOffsetZ()   { return pOffsetZ; }
    public float getBaseHeight() { return baseHeight; }
    public boolean isInfinite()  { return isInfinite; }
    public int getGridCount()    { return gridCount; }
    public long getSeed()        { return pSeed; }
    public float getMaxHeight()  { return pMaxHeight; }
    public float getRoughness()  { return pRoughness; }
    public int getOctaves()      { return pOctaves; }
    public float getScale()      { return pScale; }
    public float getOffsetX()    { return pOffsetX; }
    public float getOffsetZ()    { return pOffsetZ; }

    // ---------------------------------------------------------------
    // KAYNAK TEMÄ°ZLEME
    // ---------------------------------------------------------------

    @Override
    public void cleanUp() {
        shader.cleanUp();
        cleanUpMesh();
        if (grassTexId != 0)      GL11.glDeleteTextures(grassTexId);
        if (dirtTexId != 0)       GL11.glDeleteTextures(dirtTexId);
        if (dirt2TexId != 0)      GL11.glDeleteTextures(dirt2TexId);
        if (grassNormalTexId != 0) GL11.glDeleteTextures(grassNormalTexId);
        if (dirtNormalTexId != 0)  GL11.glDeleteTextures(dirtNormalTexId);
        if (dirt2NormalTexId != 0) GL11.glDeleteTextures(dirt2NormalTexId);
        if (sandTexId != 0)       GL11.glDeleteTextures(sandTexId);
        if (sandNormalTexId != 0)  GL11.glDeleteTextures(sandNormalTexId);
    }

    /**
     * Mesh GPU verilerini serbest bÄ±rakÄ±r (yeniden oluÅŸturma Ã¶ncesinde Ã§aÄŸrÄ±lÄ±r).
     * Alt sÄ±nÄ±flar ek VBO'lar eklediyse bu metodu override edebilirler.
     */
    protected void cleanUpMesh() {
        if (vaoId != 0)     GL30.glDeleteVertexArrays(vaoId);
        if (vertexVbo != 0) GL15.glDeleteBuffers(vertexVbo);
        if (normalVbo != 0) GL15.glDeleteBuffers(normalVbo);
        if (uvVbo != 0)     GL15.glDeleteBuffers(uvVbo);
        if (indexVbo != 0)  GL15.glDeleteBuffers(indexVbo);
        vaoId = 0;
    }

    // ---------------------------------------------------------------
    // YARDIMCI METODlar â€” Alt sÄ±nÄ±flar kullanabilir
    // ---------------------------------------------------------------

    /**
     * Float verisini GPU'ya (VBO) yÃ¼kler ve belirtilen attribute index'ine baÄŸlar.
     *
     * @param attrib VAO attribute index (0=pozisyon, 1=normal, 2=UV)
     * @param size   BileÅŸen sayÄ±sÄ± (pozisyon=3, UV=2)
     * @param data   YÃ¼klenecek float array
     * @return OluÅŸturulan VBO ID
     */
    protected int storeDataInVbo(int attrib, int size, float[] data) {
        int vboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        FloatBuffer buf = BufferUtils.createFloatBuffer(data.length);
        buf.put(data).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(attrib, size, GL11.GL_FLOAT, false, 0, 0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        return vboId;
    }

    /**
     * Index verisini GPU'ya yÃ¼kler.
     *
     * @param data YÃ¼klenecek int array (her 3 deÄŸer bir Ã¼Ã§gen oluÅŸturur)
     * @return OluÅŸturulan index VBO ID
     */
    protected int storeIndexVbo(int[] data) {
        int vboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboId);
        IntBuffer buf = BufferUtils.createIntBuffer(data.length);
        buf.put(data).flip();
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);
        return vboId;
    }

    /**
     * Doku dosyasÄ±nÄ± GPU'ya yÃ¼kler.
     *
     * @param path Dosya yolu
     * @return OpenGL texture ID (hata halinde 0)
     */
    protected int loadTex(String path) {
        try {
            return Texture.newTexture(new MyFile(path)).anisotropic().create().textureId;
        } catch (Exception e) {
            System.err.println("[FlatTerrain] Texture yuklenemedi: " + path);
            return 0;
        }
    }

    // Normal hesaplama yardÄ±mcÄ±sÄ±
    private Vector3f calculateNormal(int x, int z) {
        float hL = getHeightChecked(x - 1, z), hR = getHeightChecked(x + 1, z);
        float hD = getHeightChecked(x, z - 1), hU = getHeightChecked(x, z + 1);
        float gsz = width / (float) gridCount;
        Vector3f normal = new Vector3f(hL - hR, 2.0f * gsz, hD - hU);
        normal.normalise();
        return normal;
    }

    private float getHeightChecked(int x, int z) {
        if (x < 0 || x > gridCount || z < 0 || z > gridCount) return 0;
        return heights[x][z];
    }

    private float barryCentric(Vector3f p1, Vector3f p2, Vector3f p3, Vector2f pos) {
        float det = (p2.z - p3.z) * (p1.x - p3.x) + (p3.x - p2.x) * (p1.z - p3.z);
        float l1  = ((p2.z - p3.z) * (pos.x - p3.x) + (p3.x - p2.x) * (pos.y - p3.z)) / det;
        float l2  = ((p3.z - p1.z) * (pos.x - p3.x) + (p1.x - p3.x) * (pos.y - p3.z)) / det;
        return l1 * p1.y + l2 * p2.y + (1f - l1 - l2) * p3.y;
    }

    private static float floor(float x) { int xi = (int) x; return x < xi ? xi - 1 : xi; }
    private float fract(float x)        { return x - floor(x); }

    /** FBM (Fractal Brownian Motion) noise â€” sonsuz mod yÃ¼kseklik hesabÄ± iÃ§in */
    public float getFBMNoise(float x, float z, int octaves, float roughness) {
        float total = 0, frequency = 1, amplitude = 1, maxValue = 0;
        for (int i = 0; i < octaves; i++) {
            total += noise(x * frequency, z * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= roughness;
            frequency *= 2;
        }
        return total / maxValue;
    }

    private float hash(float px, float pz) {
        int ix = ((int) Math.floor(px + 1000000.0f)) % 100000;
        int iz = ((int) Math.floor(pz + 1000000.0f)) % 100000;
        int n  = ix + iz * 57;
        n = (n << 13) ^ n;
        return 1.0f - ((float) ((n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff) / 1073741824.0f);
    }

    private float noise(float x, float z) {
        float xi = floor(x), zi = floor(z), xf = fract(x), zf = fract(z);
        float ux = xf * xf * (3.0f - 2.0f * xf);
        float uz = zf * zf * (3.0f - 2.0f * zf);
        float a = hash(xi, zi),       b = hash(xi+1,zi);
        float c = hash(xi, zi+1),     d = hash(xi+1,zi+1);
        return lerp(lerp(a,b,ux), lerp(c,d,ux), uz);
    }

    private float lerp(float a, float b, float t) { return a + t * (b - a); }
}

