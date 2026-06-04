package examples;

import org.lwjgl.opengl.Display;
import org.lwjgl.util.vector.Vector3f;

import gane.AppSettings;
import renderEngine.RenderEngine;
import scene.Scene;
import utils.NativeLibraryLoader;
import skybox.atmosphere.AtmosphereSky;
import terrain.flat.FlatTerrain;
import textures.Texture;
import sunRenderer.Sun;
import utils.MyFile;
import water.tile.WaterTile;
import environment.DayNightManager;
import guiRendering.OpenglYaziCizimi;
import extra.Camera;

/**
 * Gane Engine Egitimi 04: Su Yuzeyi (Okyanus) ve Gece/Gunduz Dongusu
 * 
 * Bu ornek, sinirsiz bir okyanus yuzeyi eklemeyi ve oyun dunyasina
 * dinamik bir gece-gunduz gecisi (DayNightManager) entegre etmeyi gosterir.
 */
public class Example04_WaterAndPhysics {

	public static void main(String[] args) {
		NativeLibraryLoader.loadNativeLibraries();
		AppSettings.setup(1280, 720, false, "Gane Engine - 04 Water & Time", null);
		RenderEngine renderEngine = RenderEngine.init();

		Camera camera = new Camera();
		camera.setMode(Camera.CameraMode.FREE);
		camera.getPosition().set(0, 20, 0); 
		Scene scene = new Scene(camera);

		Texture sunTex = Texture.newTexture(new MyFile("res/sun.png")).create();
		Sun sun = new Sun(sunTex, 200.0f);
		scene.setSun(sun);

		// 1. Dinamik Gece/Gunduz Sistemi
		// Baslangic saati 8.0f (Sabah 8), Hiz carpani: 600.0f (Hizlandirilmis zaman)
		DayNightManager dayNight = new DayNightManager(scene, 8.0f, 600.0f);

		AtmosphereSky atmoSky = new AtmosphereSky();
		atmoSky.setCloudsEnabled(false);
		scene.setSky(atmoSky);

		FlatTerrain terrain = new FlatTerrain(2000, 2000);
		terrain.setInfinite(true);
		terrain.setHighQuality(true);
		// Adaciklar olusturmak icin yuksekligi dusuk tuttuk
		terrain.generateProceduralTerrainV2(30f, 0.4f, 4, 150f, 12345L);
		scene.addTerrain(terrain);

		// 2. Okyanus (Su Yuzeyi) Ekleme
		// MerkezX=0, MerkezZ=0, Yukseklik=15.0f, Boyut=20000f (Sonsuz hissi)
		WaterTile ocean = new WaterTile(0, 0, 15.0f, 20000f);
		scene.addWater(ocean);

		// Su yuzeyiyle ufuk cizgisini kaynastirmak icin Fog ayari
		scene.setFogColor(new Vector3f(0.7f, 0.75f, 0.8f)); 
		scene.setFogStart(200.0f);
		scene.setFogDensity(1.5f);

		OpenglYaziCizimi uiText = new OpenglYaziCizimi();
		uiText.init();

		long lastTime = System.nanoTime();

		while (!Display.isCloseRequested()) {
			long currentTime = System.nanoTime();
			float delta = (currentTime - lastTime) / 1000000000.0f;
			lastTime = currentTime;

			camera.move();
			
			// A) Gece/Gunduz dongusunu guncelle (Gunes hareket eder ve isik rengi degisir)
			dayNight.update(delta);

			// B) Sahneyi Ciz (Render)
			renderEngine.renderScene(scene, delta);

			// C) UI Cizimi
			uiText.beginUI();
			uiText.drawText("Gane Engine - Egitim #4", 20, 20, java.awt.Color.WHITE);
			uiText.drawText("Okyanus eklendi. Zaman hizla akiyor (Gece-Gunduz dongusu aktif).", 20, 50, java.awt.Color.GREEN);
			uiText.drawText("Saat: " + String.format("%.1f", dayNight.getTimeOfDay()).replace(",", "."), 20, 90, java.awt.Color.YELLOW);
			uiText.endUI();

			renderEngine.update();
		}

		uiText.cleanup();
		scene.delete();
		renderEngine.close();
	}

}
