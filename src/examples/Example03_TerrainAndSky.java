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
import guiRendering.OpenglYaziCizimi;
import extra.Camera;

/**
 * Gane Engine Egitimi 03: Arazi (Terrain) ve Gokyuzu (Sky)
 * 
 * Bu ornekte, oyun dunyasinin temel zeminini (Terrain), gercekci atmosferi
 * (Sky) ve uzak cizim hatalarini gizleyen Sis (Fog) ozelliklerinin nasil
 * kurulacagi gosterilmektedir.
 */
public class Example03_TerrainAndSky {

	public static void main(String[] args) {
		NativeLibraryLoader.loadNativeLibraries();
		AppSettings.setup(1280, 720, false, "Gane Engine - 03 Terrain and Sky", null);
		RenderEngine renderEngine = RenderEngine.init();

		Camera camera = new Camera();
		camera.setMode(Camera.CameraMode.FREE);
		camera.getPosition().set(0, 50, 0); // Zemin uzerinde biraz yuksekte baslat
		Scene scene = new Scene(camera);

		// 1. Isik ve Gunes ayarlari
		Texture sunTex = Texture.newTexture(new MyFile("res/sun.png")).create();
		Sun sun = new Sun(sunTex, 200.0f);
		scene.setSun(sun);
		scene.setLightDirection(new Vector3f(0.5f, -1.0f, 0.5f));
		scene.setLightBrightness(1.4f);
		scene.setAmbientLight(0.3f);

		// 2. Gokyuzu (AtmosphereSky) Kurulumu
		// Gercekci bir gokyuzu rengi ve ufuk (horizon) gecisi saglar.
		AtmosphereSky atmoSky = new AtmosphereSky();
		atmoSky.setCloudsEnabled(false); // Bulutlari istege bagli acip kapatabilirsiniz
		scene.setSky(atmoSky);

		// 3. Sonsuz Arazi (Infinite FlatTerrain) Olusturma
		// Arazi boyutu (2000x2000) ve kesisim detaylari ayarlanir.
		FlatTerrain terrain = new FlatTerrain(2000, 2000);
		terrain.setInfinite(true);    // Kamerayla beraber sinirsiz uzamasi
		terrain.setHighQuality(true); // Daha sik (smooth) bir grid agi
		
		// Rastgele yukseklik (Procedural) haritasi olusturulur.
		// generateProceduralTerrainV2 parametreleri:
		// (maxHeight, roughness, octaves, amplitude, seed)
		terrain.generateProceduralTerrainV2(60f, 0.4f, 4, 150f, 12345L);
		scene.addTerrain(terrain);

		// 4. Sis (Fog) ve Ufuk (Horizon) Kaynastirmasi
		// Arazinin sonsuzda kesilme (clipping) kismini gizlemek icin
		// gokyuzu ile ayni renkte bir sis (fog) tabakasi eklernir.
		scene.setFogColor(new Vector3f(0.7f, 0.75f, 0.8f)); 
		scene.setFogStart(50.0f);   // Kameradan 50 birim ileride baslasin
		scene.setFogDensity(1.5f);  // Yogunluk

		OpenglYaziCizimi uiText = new OpenglYaziCizimi();
		uiText.init();

		long lastTime = System.nanoTime();

		while (!Display.isCloseRequested()) {
			long currentTime = System.nanoTime();
			float delta = (currentTime - lastTime) / 1000000000.0f;
			lastTime = currentTime;

			camera.move();
			
			// Kamera arazi altina dusmesin diye kucuk bir carpisma engelleme (Collision):
			// Kameranin (X, Z) kordinatlarindaki terrain yuksekligini okuruz.
			float terrainHeight = terrain.getHeightAt(camera.getPosition().x, camera.getPosition().z);
			if (camera.getPosition().y < terrainHeight + 2.0f) {
				camera.getPosition().y = terrainHeight + 2.0f;
			}

			renderEngine.renderScene(scene, delta);

			uiText.beginUI();
			uiText.drawText("Gane Engine - Egitim #3", 20, 20, java.awt.Color.WHITE);
			uiText.drawText("Arazi (Terrain) ve Atmosfer basariyla olusturuldu!", 20, 50, java.awt.Color.GREEN);
			uiText.drawText("Kamera ile dolasin (WASD). Yer altina gecemezsiniz.", 20, 90, java.awt.Color.CYAN);
			uiText.endUI();

			renderEngine.update();
		}

		uiText.cleanup();
		scene.delete();
		renderEngine.close();
	}

}
