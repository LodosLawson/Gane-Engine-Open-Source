package terrain.flat;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import shaders.ShaderProgram;
import shaders.UniformFloat;
import shaders.UniformMatrix;
import shaders.UniformSampler;
import shaders.UniformVec3;
import shaders.UniformVec4;
import utils.MyFile;

/**
 * â•”â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•—
 * â•‘          GANE ENGINE â€” FLAT TERRAIN SHADER YÃ–NETÄ°CÄ°SÄ°              â•‘
 * â• â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•£
 * â•‘  FlatTerrain iÃ§in GLSL shader programÄ±nÄ± yÃ¶netir.                  â•‘
 * â•‘  ShaderProgram temel sÄ±nÄ±fÄ±nÄ± geniÅŸleterek uniform deÄŸiÅŸken        â•‘
 * â•‘  yÃ¼kleme ve shader derleme iÅŸlemlerini Ã¼stlenir.                   â•‘
 * â• â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•£
 * â•‘  GENÄ°ÅžLETME (INHERITANCE) KULLANIMI:                               â•‘
 * â•‘                                                                      â•‘
 * â•‘  Kendi shader'Ä±nÄ±zÄ± kullanmak iÃ§in FlatTerrainShader'Ä± extend edin â•‘
 * â•‘  ve sadece dosya yollarÄ±nÄ± override edin:                           â•‘
 * â•‘                                                                      â•‘
 * â•‘  public class KarliTerrainShader extends FlatTerrainShader {        â•‘
 * â•‘    @Override                                                         â•‘
 * â•‘    protected String getVertexShaderPath() {                         â•‘
 * â•‘      return "terrain/flat/terrain_flat.vert"; // ya da kendi yolun â•‘
 * â•‘    }                                                                 â•‘
 * â•‘    @Override                                                         â•‘
 * â•‘    protected String getFragmentShaderPath() {                       â•‘
 * â•‘      return "benim_oyunum/kar_terrain.frag";                        â•‘
 * â•‘    }                                                                 â•‘
 * â•‘    @Override                                                         â•‘
 * â•‘    protected void loadExtraUniforms() {                             â•‘
 * â•‘      // Ekstra uniform deÄŸiÅŸkenlerini burada yÃ¼kleyin               â•‘
 * â•‘    }                                                                 â•‘
 * â•‘  }                                                                   â•‘
 * â•šâ•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
 */
public class FlatTerrainShader extends ShaderProgram {

    // ---------------------------------------------------------------
    // SHADER DOSYA YOLLARI â€” Alt sÄ±nÄ±flar bu metotlarÄ± override edebilir
    // ---------------------------------------------------------------

    /**
     * Vertex shader dosyasÄ±nÄ±n yolunu dÃ¶ndÃ¼rÃ¼r.
     * <p>
     * Alt sÄ±nÄ±flar bu metodu override ederek kendi vertex shader'larÄ±nÄ± kullanabilir.
     * Yol, projenin kÃ¶k dizininden (Engine/ kaynak klasÃ¶rÃ¼nden) verilmelidir.
     *
     * @return Vertex shader dosyasÄ±nÄ±n yolu (Ã¶rn: "terrain/flat/terrain_flat.vert")
     */
    protected String getVertexShaderPath() {
        return "terrain/flat/terrain_flat.vert";
    }

    /**
     * Fragment shader dosyasÄ±nÄ±n yolunu dÃ¶ndÃ¼rÃ¼r.
     * <p>
     * Alt sÄ±nÄ±flar bu metodu override ederek kendi fragment shader'larÄ±nÄ± kullanabilir.
     * Yol, projenin kÃ¶k dizininden (Engine/ kaynak klasÃ¶rÃ¼nden) verilmelidir.
     *
     * @return Fragment shader dosyasÄ±nÄ±n yolu (Ã¶rn: "terrain/flat/terrain_flat.frag")
     */
    protected String getFragmentShaderPath() {
        return "terrain/flat/terrain_flat.frag";
    }

    // ---------------------------------------------------------------
    // UNIFORM DEÄžÄ°ÅžKENLER
    // ---------------------------------------------------------------

    // Matrisler
    private UniformMatrix uModel        = new UniformMatrix("uModel");
    private UniformMatrix uView         = new UniformMatrix("uView");
    private UniformMatrix uProjection   = new UniformMatrix("uProjection");

    // IÅŸÄ±k
    private UniformVec3   uLightDir     = new UniformVec3("uLightDir");
    private UniformVec3   uLightColor   = new UniformVec3("uLightColor");
    private UniformFloat  uAmbient      = new UniformFloat("uAmbient");

    // Doku
    private UniformFloat  uTextureScale = new UniformFloat("uTextureScale");
    private UniformSampler uGrassTex   = new UniformSampler("uGrassTex");
    private UniformSampler uDirtTex    = new UniformSampler("uDirtTex");
    private UniformSampler uDirt2Tex   = new UniformSampler("uDirt2Tex");
    private UniformSampler uSandTex    = new UniformSampler("uSandTex");

    // Normal haritalarÄ±
    private UniformSampler uGrassNormal = new UniformSampler("uGrassNormal");
    private UniformSampler uDirtNormal  = new UniformSampler("uDirtNormal");
    private UniformSampler uDirt2Normal = new UniformSampler("uDirt2Normal");
    private UniformSampler uSandNormal  = new UniformSampler("uSandNormal");

    // KÄ±rpma dÃ¼zlemi (Su refleksiyonu iÃ§in)
    private UniformVec4   uClipPlane   = new UniformVec4("uClipPlane");

    // Sonsuz/ProsedÃ¼rel terrain parametreleri
    private UniformFloat  uInfinite     = new UniformFloat("uInfinite");
    private UniformFloat  uMaxHeight    = new UniformFloat("uMaxHeight");
    private UniformFloat  uRoughness    = new UniformFloat("uRoughness");
    private shaders.UniformInt uOctaves = new shaders.UniformInt("uOctaves");
    private UniformFloat  uScale        = new UniformFloat("uScale");
    private UniformFloat  uOffsetX      = new UniformFloat("uOffsetX");
    private UniformFloat  uOffsetZ      = new UniformFloat("uOffsetZ");
    private UniformFloat  uBaseHeight   = new UniformFloat("uBaseHeight");

    // Gezegen modu
    private UniformFloat  uIsPlanetary  = new UniformFloat("uIsPlanetary");
    private UniformVec3   uPlanetCenter = new UniformVec3("uPlanetCenter");

    // Sis ve kamera
    private UniformVec3  uFogColor    = new UniformVec3("uFogColor");
    private UniformFloat uFogDensity  = new UniformFloat("uFogDensity");
    private UniformFloat uFogStart    = new UniformFloat("uFogStart");
    private UniformVec3  uCameraPos   = new UniformVec3("uCameraPos");

    // Bulut gÃ¶lgeleri
    private UniformFloat uTime = new UniformFloat("uTime");
    private shaders.UniformVec2  uWindDir = new shaders.UniformVec2("uWindDir");
    private UniformFloat uCloudShadowEnabled = new UniformFloat("uCloudShadowEnabled");
    private UniformVec4[] uCloudShadowPos = new UniformVec4[32];
    private UniformFloat[] uCloudShadowAlpha = new UniformFloat[32];
    private shaders.UniformInt uNumCloudShadows = new shaders.UniformInt("uNumCloudShadows");

    // GÃ¶lge haritasÄ±
    private UniformMatrix toShadowMapSpace = new UniformMatrix("toShadowMapSpace");
    private UniformSampler shadowMap = new UniformSampler("shadowMap");

    // Nokta Ä±ÅŸÄ±klar (maksimum 4 adet)
    public UniformVec3[] pointLightPos = new UniformVec3[4];
    public UniformVec3[] pointLightColor = new UniformVec3[4];
    public UniformVec3[] pointLightAttenuation = new UniformVec3[4];

    // ---------------------------------------------------------------
    // CONSTRUCTOR
    // ---------------------------------------------------------------

    /**
     * FlatTerrainShader'Ä± baÅŸlatÄ±r.
     * Vertex ve fragment shader dosyalarÄ± {@link #getVertexShaderPath()} ve
     * {@link #getFragmentShaderPath()} metotlarÄ±ndan okunur.
     * Alt sÄ±nÄ±flar bu metotlarÄ± override ederek kendi shader dosyalarÄ±nÄ± kullanabilir.
     */
    public FlatTerrainShader() {
        super(new MyFile(getStaticVertexPath()), new MyFile(getStaticFragmentPath()),
              "inPosition", "inNormal", "inUV");
        initUniforms();
    }

    /**
     * Alt sÄ±nÄ±flarÄ±n Ã¶zel shader yollarÄ±yla constructor Ã§aÄŸÄ±rmasÄ± iÃ§in kullanÄ±lÄ±r.
     *
     * @param vertexPath   Vertex shader dosyasÄ±nÄ±n yolu
     * @param fragmentPath Fragment shader dosyasÄ±nÄ±n yolu
     */
    protected FlatTerrainShader(String vertexPath, String fragmentPath) {
        super(new MyFile(vertexPath), new MyFile(fragmentPath),
              "inPosition", "inNormal", "inUV");
        initUniforms();
    }

    // Java'da constructor'dan override edilebilen metot Ã§aÄŸrÄ±lamadÄ±ÄŸÄ± iÃ§in
    // static helper kullanÄ±yoruz. Alt sÄ±nÄ±flar kendi constructor'larÄ±nda
    // super(vertexPath, fragmentPath) Ã§aÄŸÄ±rmalÄ±.
    private static String getStaticVertexPath() {
        return "terrain/flat/terrain_flat.vert";
    }
    private static String getStaticFragmentPath() {
        return "terrain/flat/terrain_flat.frag";
    }

    private void initUniforms() {
        for (int i = 0; i < 32; i++) {
            uCloudShadowPos[i] = new UniformVec4("uCloudShadowPos[" + i + "]");
            uCloudShadowAlpha[i] = new UniformFloat("uCloudShadowAlpha[" + i + "]");
        }
        for (int i = 0; i < 4; i++) {
            pointLightPos[i] = new UniformVec3("pointLightPos[" + i + "]");
            pointLightColor[i] = new UniformVec3("pointLightColor[" + i + "]");
            pointLightAttenuation[i] = new UniformVec3("pointLightAttenuation[" + i + "]");
        }

        java.util.List<shaders.Uniform> list = new java.util.ArrayList<>();
        list.add(uModel); list.add(uView); list.add(uProjection);
        list.add(uLightDir); list.add(uLightColor); list.add(uAmbient);
        list.add(uTextureScale);
        list.add(uGrassTex); list.add(uDirtTex); list.add(uDirt2Tex); list.add(uSandTex);
        list.add(uClipPlane);
        list.add(uGrassNormal); list.add(uDirtNormal); list.add(uDirt2Normal); list.add(uSandNormal);
        list.add(uInfinite); list.add(uMaxHeight); list.add(uRoughness);
        list.add(uOctaves); list.add(uScale); list.add(uOffsetX); list.add(uOffsetZ); list.add(uBaseHeight);
        list.add(uIsPlanetary); list.add(uPlanetCenter);
        list.add(uFogColor); list.add(uFogDensity); list.add(uFogStart); list.add(uCameraPos);
        list.add(uTime); list.add(uWindDir); list.add(uCloudShadowEnabled);
        for (int i = 0; i < 32; i++) { list.add(uCloudShadowPos[i]); list.add(uCloudShadowAlpha[i]); }
        list.add(uNumCloudShadows);
        list.add(toShadowMapSpace); list.add(shadowMap);
        for (int i = 0; i < 4; i++) {
            list.add(pointLightPos[i]); list.add(pointLightColor[i]); list.add(pointLightAttenuation[i]);
        }

        storeAllUniformLocations(list.toArray(new shaders.Uniform[0]));

        start();
        uGrassTex.loadTexUnit(0);
        uDirtTex.loadTexUnit(1);
        uDirt2Tex.loadTexUnit(2);
        uGrassNormal.loadTexUnit(3);
        uDirtNormal.loadTexUnit(4);
        uDirt2Normal.loadTexUnit(5);
        shadowMap.loadTexUnit(6);
        uSandTex.loadTexUnit(7);
        uSandNormal.loadTexUnit(8);

        // Alt sÄ±nÄ±flarÄ±n ekstra uniform'larÄ±nÄ± yÃ¼klemesi iÃ§in hook
        loadExtraUniforms();
        stop();
    }

    // ---------------------------------------------------------------
    // GENÄ°ÅžLETME HOOK'U â€” Alt sÄ±nÄ±flar ekstra uniform yÃ¼klemek iÃ§in override edebilir
    // ---------------------------------------------------------------

    /**
     * Alt sÄ±nÄ±flara Ã¶zel uniform deÄŸiÅŸkenlerini yÃ¼klemeleri iÃ§in hook metot.
     * <p>
     * VarsayÄ±lan implementasyonu boÅŸtur. Alt sÄ±nÄ±flar ekstra uniform
     * deÄŸiÅŸkenleri eklemek isterlerse bu metodu override edebilirler:
     *
     * <pre>
     * {@literal @}Override
     * protected void loadExtraUniforms() {
     *     mySnowUniform.loadFloat(1.0f);
     * }
     * </pre>
     */
    protected void loadExtraUniforms() {
        // Alt sÄ±nÄ±flar burayÄ± dolduracak
    }

    // ---------------------------------------------------------------
    // UNIFORM YÃœKLEME METODlarÄ±
    // ---------------------------------------------------------------

    public void loadClipPlane(org.lwjgl.util.vector.Vector4f plane) {
        uClipPlane.loadVec4(plane);
    }

    public void loadFogParams(org.lwjgl.util.vector.Vector3f color, float density, float start) {
        uFogColor.loadVec3(color);
        uFogDensity.loadFloat(density);
        uFogStart.loadFloat(start);
    }

    public void loadCameraPos(org.lwjgl.util.vector.Vector3f pos) {
        uCameraPos.loadVec3(pos);
    }

    public void loadToShadowSpaceMatrix(Matrix4f matrix) {
        toShadowMapSpace.loadMatrix(matrix);
    }

    public void loadPlanetaryParams(boolean isPlanetary, org.lwjgl.util.vector.Vector3f center) {
        uIsPlanetary.loadFloat(isPlanetary ? 1.0f : 0.0f);
        if (center != null) uPlanetCenter.loadVec3(center);
    }

    public void loadCloudShadowData(float time, org.lwjgl.util.vector.Vector2f windDir,
                                    boolean enabled, java.util.List<skybox.atmosphere.CloudCluster> clusters) {
        uTime.loadFloat(time);
        uWindDir.loadVec2(windDir);
        uCloudShadowEnabled.loadFloat(enabled ? 1.0f : 0.0f);

        int count = clusters != null ? Math.min(clusters.size(), 32) : 0;
        uNumCloudShadows.loadInt(count);
        for (int i = 0; i < count; i++) {
            skybox.atmosphere.CloudCluster c = clusters.get(i);
            float r = c.currentScale * 650.0f;
            uCloudShadowPos[i].loadVec4(new org.lwjgl.util.vector.Vector4f(c.position.x, c.position.y, c.position.z, r));
            uCloudShadowAlpha[i].loadFloat(c.alpha);
        }
        for (int i = count; i < 32; i++) {
            uCloudShadowPos[i].loadVec4(new org.lwjgl.util.vector.Vector4f(0, 0, 0, 0));
            uCloudShadowAlpha[i].loadFloat(0.0f);
        }
    }

    public void loadPointLights(java.util.List<scene.Light> pointLights) {
        for (int i = 0; i < 4; i++) {
            if (pointLights != null && i < pointLights.size() && pointLights.get(i) != null) {
                this.pointLightPos[i].loadVec3(pointLights.get(i).getPosition());
                this.pointLightColor[i].loadVec3(pointLights.get(i).getColor());
                this.pointLightAttenuation[i].loadVec3(pointLights.get(i).getAttenuation());
            } else {
                this.pointLightColor[i].loadVec3(new Vector3f(0, 0, 0));
                this.pointLightAttenuation[i].loadVec3(new Vector3f(1, 0, 0));
            }
        }
    }

    public void loadInfiniteParams(boolean infinite, float maxHeight, float roughness,
                                   int octaves, float scale, float offsetX, float offsetZ, float baseHeight) {
        uInfinite.loadFloat(infinite ? 1.0f : 0.0f);
        uMaxHeight.loadFloat(maxHeight);
        uRoughness.loadFloat(roughness);
        uOctaves.loadInt(octaves);
        uScale.loadFloat(scale);
        uOffsetX.loadFloat(offsetX);
        uOffsetZ.loadFloat(offsetZ);
        uBaseHeight.loadFloat(baseHeight);
    }

    public void loadMatrices(Matrix4f model, Matrix4f view, Matrix4f proj) {
        uModel.loadMatrix(model);
        uView.loadMatrix(view);
        uProjection.loadMatrix(proj);
    }

    public void loadLight(Vector3f dir, Vector3f color, float ambient) {
        uLightDir.loadVec3(dir);
        uLightColor.loadVec3(color);
        uAmbient.loadFloat(ambient);
    }

    public void loadTextureScale(float scale) {
        uTextureScale.loadFloat(scale);
    }
}

