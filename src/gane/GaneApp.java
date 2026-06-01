package gane;

import org.lwjgl.opengl.Display;
import org.lwjgl.util.vector.Vector3f;

import extra.Camera;
import gane.objects.Player;
import guiRendering.UIManager;
import physics.PhysicsEngine;
import renderEngine.RenderEngine;
import scene.Scene;
import terrain.flat.FlatTerrain;
import utils.NativeLibraryLoader;

/**
 * â•”â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•—
 * â•‘              GANE ENGINE â€” ANA UYGULAMA TEMEL SINIFI                   â•‘
 * â• â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•£
 * â•‘  Bu sÄ±nÄ±fÄ± extend ederek kendi oyununu oluÅŸturabilirsin.               â•‘
 * â•‘  Engine'in tÃ¼m yaÅŸam dÃ¶ngÃ¼sÃ¼ (lifecycle) bu sÄ±nÄ±f tarafÄ±ndan          â•‘
 * â•‘  yÃ¶netilir. Sen sadece hook metodlarÄ±nÄ± doldurman yeterli.             â•‘
 * â• â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•£
 * â•‘                                                                          â•‘
 * â•‘  KULLANIM â€” Kendi oyununu bu ÅŸekilde yaz:                              â•‘
 * â•‘                                                                          â•‘
 * â•‘  public class BenimOyunum extends GaneApp {                            â•‘
 * â•‘                                                                          â•‘
 * â•‘    public static void main(String[] args) {                            â•‘
 * â•‘      new BenimOyunum().start();                                        â•‘
 * â•‘    }                                                                    â•‘
 * â•‘                                                                          â•‘
 * â•‘    @Override                                                            â•‘
 * â•‘    protected void onInit() {                                           â•‘
 * â•‘      // Terrain, Ä±ÅŸÄ±k, entity kur                                      â•‘
 * â•‘      FlatTerrain arazi = new FlatTerrain(1000, 1000);                 â•‘
 * â•‘      arazi.generateProceduralTerrainV2(80f, 0.4f, 4, 250f, 12345L);  â•‘
 * â•‘      getScene().addTerrain(arazi);                                     â•‘
 * â•‘                                                                          â•‘
 * â•‘      getScene().setSky(new skybox.atmosphere.AtmosphereSky());                    â•‘
 * â•‘      getScene().addWater(new water.tile.WaterTile(0, -10, 5f, 1000f));     â•‘
 * â•‘    }                                                                    â•‘
 * â•‘                                                                          â•‘
 * â•‘    @Override                                                            â•‘
 * â•‘    protected void onUpdate(float delta) {                              â•‘
 * â•‘      // Her kare Ã§aÄŸrÄ±lÄ±r (oyun mantÄ±ÄŸÄ± buraya)                        â•‘
 * â•‘    }                                                                    â•‘
 * â•‘                                                                          â•‘
 * â•‘    @Override                                                            â•‘
 * â•‘    protected void onCleanup() {                                        â•‘
 * â•‘      // KapatÄ±lÄ±rken Ã§aÄŸrÄ±lÄ±r (opsiyonel)                              â•‘
 * â•‘    }                                                                    â•‘
 * â•‘  }                                                                      â•‘
 * â•‘                                                                          â•‘
 * â• â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•£
 * â•‘  GELIÅžMIÅž KULLANIM:                                                     â•‘
 * â•‘                                                                          â•‘
 * â•‘  Kendi terrain'ini extend edip kullanabilirsin:                        â•‘
 * â•‘                                                                          â•‘
 * â•‘  public class KarliArazi extends FlatTerrain {                         â•‘
 * â•‘    public KarliArazi(float w, float d) { super(w, d); }               â•‘
 * â•‘    @Override                                                            â•‘
 * â•‘    protected FlatTerrainShader createShader() {                       â•‘
 * â•‘      return new FlatTerrainShader("kar.vert", "kar.frag");            â•‘
 * â•‘    }                                                                    â•‘
 * â•‘  }                                                                      â•‘
 * â•‘                                                                          â•‘
 * â•‘  Ve bunu onInit iÃ§inde:                                                â•‘
 * â•‘    getScene().addTerrain(new KarliArazi(1000, 1000));                 â•‘
 * â•‘                                                                          â•‘
 * â•šâ•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
 */
public abstract class GaneApp {

    // ---------------------------------------------------------------
    // ENGINE BÄ°LEÅžENLERÄ°
    // ---------------------------------------------------------------

    private Scene         scene;
    private Camera        camera;
    private Player        player;
    private RenderEngine  renderEngine;
    private PhysicsEngine physicsEngine;
    private UIManager     uiManager;
    private boolean       running = false;

    // ---------------------------------------------------------------
    // YAÅžAM DÃ–NGÃœSÃœ â€” Engine tarafÄ±ndan Ã§aÄŸrÄ±lÄ±r
    // ---------------------------------------------------------------

    /**
     * Engine'i baÅŸlatÄ±r ve oyun dÃ¶ngÃ¼sÃ¼nÃ¼ Ã§alÄ±ÅŸtÄ±rÄ±r.
     * Bu metodu main() iÃ§inden Ã§aÄŸÄ±r:
     *
     * <pre>
     * public static void main(String[] args) {
     *     new BenimOyunum().start();
     * }
     * </pre>
     */
    public final void start() {
        // 1. Native kÃ¼tÃ¼phaneleri yÃ¼kle (LWJGL)
        NativeLibraryLoader.loadNativeLibraries();

        // 2. Pencere ayarlarÄ±nÄ± kur
        AppSettings.setup(1920, 1080, false, getWindowTitle(),
                          "res/WoodFloor004_4K-PNG_Color.png");

        running = true;

        // 3. Render motorunu baÅŸlat
        renderEngine = RenderEngine.init();

        // 4. Fizik motorunu baÅŸlat
        physicsEngine = new PhysicsEngine();

        // 5. Kamera ve oyuncu oluÅŸtur
        camera = new Camera();
        camera.setMode(Camera.CameraMode.FIRST_PERSON);

        player = new Player(camera);
        player.setPosition(new Vector3f(0, 10, 0));
        player.setScale(1f);
        player.setRotation(new Vector3f(0, 0, 0));

        // 6. Sahne oluÅŸtur
        scene = new Scene(camera, null, true);
        player.setScene(scene);
        scene.addEntity(player);
        camera.setTarget(player);
        scene.setFrustumCullingEnabled(true);

        // 7. UI yÃ¶neticisi baÅŸlat
        uiManager = new UIManager(guiRendering.UITheme.neon());

        // 8. GeliÅŸtirici hook'u â€” sahneyi burada kur
        onInit();

        // 9. Ana oyun dÃ¶ngÃ¼sÃ¼
        while (!Display.isCloseRequested() && running) {
            float delta = renderEngine.getDisplayManager().getFrameTime();

            // Fizik gÃ¼ncelle
            physicsEngine.update(scene, delta);
            particles.ParticleManager.getInstance().update(scene, delta);

            // Kamera hareketi
            camera.move();

            // GeliÅŸtirici gÃ¼ncellemesi
            onUpdate(delta);

            // Sahneyi Ã§iz
            renderEngine.renderScene(scene, delta);

            // UI Ã§iz
            uiManager.render(renderEngine.getMasterRenderer().getShadowMapTexture());

            // Frame gÃ¼ncelle
            renderEngine.update();
        }

        // 10. Temizlik
        onCleanup();
        scene.delete();
        uiManager.cleanup();
        renderEngine.close();
        running = false;
    }

    // ---------------------------------------------------------------
    // GELÄ°ÅžTÄ°RÄ°CÄ° HOOK'LARI â€” Alt sÄ±nÄ±flar bunlarÄ± override eder
    // ---------------------------------------------------------------

    /**
     * Engine baÅŸladÄ±ktan sonra, oyun dÃ¶ngÃ¼sÃ¼ baÅŸlamadan Ã¶nce bir kez Ã§aÄŸrÄ±lÄ±r.
     * <p>
     * Bu metod iÃ§inde:
     * <ul>
     *   <li>Terrain oluÅŸtur ve sahneye ekle</li>
     *   <li>GÃ¶kyÃ¼zÃ¼ (Sky) ayarla</li>
     *   <li>Su (Ocean/Water) ekle</li>
     *   <li>Entity'leri yÃ¼kle</li>
     *   <li>IÅŸÄ±klandÄ±rmayÄ± ayarla</li>
     *   <li>UI butonlarÄ± ekle</li>
     * </ul>
     *
     * <pre>
     * {@literal @}Override
     * protected void onInit() {
     *     FlatTerrain arazi = new FlatTerrain(1000, 1000);
     *     arazi.generateProceduralTerrainV2(80f, 0.4f, 4, 250f, 12345L);
     *     getScene().addTerrain(arazi);
     *
     *     getScene().setSky(new skybox.atmosphere.AtmosphereSky());
     *     getScene().setLightDirection(new Vector3f(-0.5f, -0.8f, -0.3f));
     * }
     * </pre>
     */
    protected abstract void onInit();

    /**
     * Her kare (frame) Ã§aÄŸrÄ±lÄ±r.
     * <p>
     * Oyun mantÄ±ÄŸÄ±nÄ±, input iÅŸlemeyi, animasyonlarÄ± buraya yaz.
     *
     * @param delta Son kare ile bu kare arasÄ±ndaki sÃ¼re (saniye). Kare hÄ±zÄ±ndan
     *              baÄŸÄ±msÄ±z hareket iÃ§in bu deÄŸerle Ã§arp: <br>
     *              {@code pozisyon += hÄ±z * delta;}
     */
    protected void onUpdate(float delta) {
        // Alt sÄ±nÄ±flar override edebilir (zorunlu deÄŸil)
    }

    /**
     * Oyun kapatÄ±lÄ±rken Ã§aÄŸrÄ±lÄ±r.
     * Ek kaynaklarÄ± (ses dosyalarÄ±, custom OpenGL objeleri vb.) burada temizle.
     * Terrain, entity ve shader temizliÄŸi engine tarafÄ±ndan otomatik yapÄ±lÄ±r.
     */
    protected void onCleanup() {
        // Alt sÄ±nÄ±flar override edebilir (zorunlu deÄŸil)
    }

    /**
     * Pencere baÅŸlÄ±ÄŸÄ±nÄ± dÃ¶ndÃ¼rÃ¼r. Override edebilirsin.
     *
     * @return Pencere baÅŸlÄ±k metni
     */
    protected String getWindowTitle() {
        return "Gane Engine";
    }

    // ---------------------------------------------------------------
    // GETTER'LAR â€” onInit ve onUpdate iÃ§inde kullanÄ±lÄ±r
    // ---------------------------------------------------------------

    /**
     * Aktif sahneyi dÃ¶ndÃ¼rÃ¼r.
     * Terrain, entity ve Ä±ÅŸÄ±k eklemek iÃ§in kullan.
     *
     * @return Aktif {@link Scene} nesnesi
     */
    public Scene getScene() { return scene; }

    /**
     * Aktif kamerayÄ± dÃ¶ndÃ¼rÃ¼r.
     * Kamera modunu ve pozisyonunu deÄŸiÅŸtirmek iÃ§in kullan.
     *
     * @return {@link Camera} nesnesi
     */
    public Camera getCamera() { return camera; }

    /**
     * Oyuncu nesnesini dÃ¶ndÃ¼rÃ¼r.
     *
     * @return {@link Player} nesnesi
     */
    public Player getPlayer() { return player; }

    /**
     * Render motorunu dÃ¶ndÃ¼rÃ¼r.
     * GÃ¶lge mesafesi ve okyanus render ayarlarÄ± iÃ§in kullan.
     *
     * @return {@link RenderEngine} nesnesi
     */
    public RenderEngine getRenderEngine() { return renderEngine; }

    /**
     * Fizik motorunu dÃ¶ndÃ¼rÃ¼r.
     *
     * @return {@link PhysicsEngine} nesnesi
     */
    public PhysicsEngine getPhysicsEngine() { return physicsEngine; }

    /**
     * UI yÃ¶neticisini dÃ¶ndÃ¼rÃ¼r.
     * Buton ve mesaj eklemek iÃ§in kullan.
     *
     * @return {@link UIManager} nesnesi
     */
    public UIManager getUIManager() { return uiManager; }

    /**
     * Oyun dÃ¶ngÃ¼sÃ¼nÃ¼ durdurur ve pencereyi kapatÄ±r.
     */
    public void stop() { running = false; }
}

