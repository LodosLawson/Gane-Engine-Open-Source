package renderEngine;

import org.lwjgl.util.vector.Vector3f;

import entityRenderers.EntityRenderer;
import environmentMapRenderer.EnviroMapRenderer;
import scene.Scene;
import shinyRenderer.ShinyRenderer;
import skybox.classic.SkyboxRenderer;
import textures.Texture;
import utils.DisplayManager;
import water.tile.WaterFrameBuffers;
import water.tile.WaterRenderer;


/**
 * Oyun motorunun temel render motoru sÃ„Â±nÃ„Â±fÃ„Â±.
 * Pencere yÃƒÂ¶netimini (DisplayManager) ve alt render sistemlerini (MasterRenderer) 
 * birleÃ…Å¸tirerek ana oyun dÃƒÂ¶ngÃƒÂ¼sÃƒÂ¼nÃƒÂ¼n gÃƒÂ¶rsel tarafÃ„Â±nÃ„Â± ÃƒÂ§alÃ„Â±Ã…Å¸tÃ„Â±rÃ„Â±r.
 */
public class RenderEngine {

	// Ekran/Pencere yÃƒÂ¶neticisi
	private DisplayManager display;
	// TÃƒÂ¼m alt render iÃ…Å¸lemlerini orkestre eden ana render yÃƒÂ¶neticisi
	private MasterRenderer renderer;

	/**
	 * Yeni bir RenderEngine oluÃ…Å¸turur. DoÃ„Å¸rudan dÃ„Â±Ã…Å¸arÃ„Â±dan ÃƒÂ§aÃ„Å¸rÃ„Â±lmamalÃ„Â±dÃ„Â±r, init() metodu kullanÃ„Â±lmalÃ„Â±dÃ„Â±r.
	 * 
	 * @param display Ekran yÃƒÂ¶neticisi
	 * @param renderer Ana render yÃƒÂ¶neticisi
	 */
	private RenderEngine(DisplayManager display, MasterRenderer renderer) {
		this.display = display;
		this.renderer = renderer;
	}

	/**
	 * Her karenin (frame) sonunda ekranÃ„Â± gÃƒÂ¼nceller ve OpenGL buffer'larÃ„Â±nÃ„Â± takas (swap) eder.
	 */
	public void update() {
		display.update();
	}

	/** @return KullanÃ„Â±lan ekran yÃƒÂ¶neticisini dÃƒÂ¶ndÃƒÂ¼rÃƒÂ¼r */
	public DisplayManager getDisplayManager() {
		return display;
	}

	/**
	 * Verilen sahneyi (Scene) ekrana ÃƒÂ§izer.
	 *
	 * @param scene Ãƒâ€¡izilecek olan sahne verisi (modeller, Ã„Â±Ã…Å¸Ã„Â±klar, kamera vs.)
	 * @param delta Bu karenin geÃƒÂ§en sÃƒÂ¼resi (saniye) Ã¢â‚¬â€ su etkileÃ…Å¸im sistemi iÃƒÂ§in gerekli
	 */
	public void renderScene(Scene scene, float delta) {
		renderer.renderScene(scene, delta);
	}
	
	/** @return Ana render yÃƒÂ¶neticisini dÃƒÂ¶ndÃƒÂ¼rÃƒÂ¼r */
	public MasterRenderer getMasterRenderer() {
		return renderer;
	}
	
	/**
	 * Dinamik yansÃ„Â±malar iÃƒÂ§in ÃƒÂ§evresel kÃƒÂ¼p haritasÃ„Â±nÃ„Â± (Environment Map) ÃƒÂ§izer.
	 * 
	 * @param enviroMap Ãƒâ€¡izimin kaydedileceÃ„Å¸i doku (Texture)
	 * @param scene Ãƒâ€¡izilecek sahne
	 * @param center KÃƒÂ¼p haritasÃ„Â± kamerasÃ„Â±nÃ„Â±n yerleÃ…Å¸tirileceÃ„Å¸i merkez nokta
	 */
	public void renderEnvironmentMap(Texture enviroMap, Scene scene, Vector3f center){
		EnviroMapRenderer.renderEnvironmentMap(enviroMap, scene, center, renderer);
	}

	/**
	 * Motor kapatÃ„Â±lÃ„Â±rken tÃƒÂ¼m donanÃ„Â±m belleklerini temizler ve ekranÃ„Â± kapatÃ„Â±r.
	 */
	public void close() {
		renderer.cleanUp();
		display.closeDisplay();
	}

	/**
	 * Render motorunu yapÃ„Â±landÃ„Â±rÃ„Â±r, OpenGL baÃ„Å¸lamÃ„Â±nÃ„Â± yaratÃ„Â±r ve alt renderer'larÃ„Â± (su, gÃƒÂ¶kyÃƒÂ¼zÃƒÂ¼, nesneler) baÃ…Å¸latÃ„Â±r.
	 * Uygulama baÃ…Å¸larken sadece 1 kez ÃƒÂ§aÃ„Å¸rÃ„Â±lmalÃ„Â±dÃ„Â±r.
	 * 
	 * @return BaÃ…Å¸latÃ„Â±lmÃ„Â±Ã…Å¸ ve kullanÃ„Â±ma hazÃ„Â±r RenderEngine objesi
	 */
	public static RenderEngine init() {
		DisplayManager display = DisplayManager.createDisplay();
		EntityRenderer basicRenderer = new EntityRenderer();
		WaterFrameBuffers waterFbos = new WaterFrameBuffers();
		SkyboxRenderer skyRenderer = new SkyboxRenderer();
		skybox.atmosphere.AtmosphereRenderer atmosphereRenderer = new skybox.atmosphere.AtmosphereRenderer();
		water.ocean.OceanRenderer oceanRenderer = new water.ocean.OceanRenderer(waterFbos);
		
		ShinyRenderer shinyRenderer = new ShinyRenderer();
		
		MasterRenderer rendererInstance = new MasterRenderer(basicRenderer, skyRenderer, atmosphereRenderer, oceanRenderer, waterFbos,
				shinyRenderer);
		
		return new RenderEngine(display, rendererInstance);
	}

}


