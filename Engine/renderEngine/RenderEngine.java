package renderEngine;

import org.lwjgl.util.vector.Vector3f;

import entityRenderers.EntityRenderer;
import environmentMapRenderer.EnviroMapRenderer;
import scene.Scene;
import shinyRenderer.ShinyRenderer;
import skybox.SkyboxRenderer;
import textures.Texture;
import utils.DisplayManager;
import water.WaterFrameBuffers;
import water.WaterRenderer;

/**
 * Oyun motorunun temel render motoru sınıfı.
 * Pencere yönetimini (DisplayManager) ve alt render sistemlerini (MasterRenderer) 
 * birleştirerek ana oyun döngüsünün görsel tarafını çalıştırır.
 */
public class RenderEngine {

	// Ekran/Pencere yöneticisi
	private DisplayManager display;
	// Tüm alt render işlemlerini orkestre eden ana render yöneticisi
	private MasterRenderer renderer;

	/**
	 * Yeni bir RenderEngine oluşturur. Doğrudan dışarıdan çağrılmamalıdır, init() metodu kullanılmalıdır.
	 * 
	 * @param display Ekran yöneticisi
	 * @param renderer Ana render yöneticisi
	 */
	private RenderEngine(DisplayManager display, MasterRenderer renderer) {
		this.display = display;
		this.renderer = renderer;
	}

	/**
	 * Her karenin (frame) sonunda ekranı günceller ve OpenGL buffer'larını takas (swap) eder.
	 */
	public void update() {
		display.update();
	}

	/** @return Kullanılan ekran yöneticisini döndürür */
	public DisplayManager getDisplayManager() {
		return display;
	}

	/**
	 * Verilen sahneyi (Scene) ekrana çizer.
	 * 
	 * @param scene Çizilecek olan sahne verisi (modeller, ışıklar, kamera vs.)
	 */
	public void renderScene(Scene scene) {
		renderer.renderScene(scene);
	}
	
	/**
	 * Dinamik yansımalar için çevresel küp haritasını (Environment Map) çizer.
	 * 
	 * @param enviroMap Çizimin kaydedileceği doku (Texture)
	 * @param scene Çizilecek sahne
	 * @param center Küp haritası kamerasının yerleştirileceği merkez nokta
	 */
	public void renderEnvironmentMap(Texture enviroMap, Scene scene, Vector3f center){
		EnviroMapRenderer.renderEnvironmentMap(enviroMap, scene, center, renderer);
	}

	/**
	 * Motor kapatılırken tüm donanım belleklerini temizler ve ekranı kapatır.
	 */
	public void close() {
		renderer.cleanUp();
		display.closeDisplay();
	}

	/**
	 * Render motorunu yapılandırır, OpenGL bağlamını yaratır ve alt renderer'ları (su, gökyüzü, nesneler) başlatır.
	 * Uygulama başlarken sadece 1 kez çağrılmalıdır.
	 * 
	 * @return Başlatılmış ve kullanıma hazır RenderEngine objesi
	 */
	public static RenderEngine init() {
		DisplayManager display = DisplayManager.createDisplay();
		EntityRenderer basicRenderer = new EntityRenderer();
		WaterFrameBuffers waterFbos = new WaterFrameBuffers();
		SkyboxRenderer skyRenderer = new SkyboxRenderer();
		WaterRenderer waterRenderer = new WaterRenderer(waterFbos);
		ShinyRenderer shinyRenderer = new ShinyRenderer();
		
		MasterRenderer renderer = new MasterRenderer(basicRenderer, skyRenderer, waterRenderer, waterFbos,
				shinyRenderer);
		
		return new RenderEngine(display, renderer);
	}

}
