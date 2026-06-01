package gane;

import org.lwjgl.opengl.Display;
import renderEngine.RenderEngine;
import utils.NativeLibraryLoader;

/**
 * Gane Engine Ana Çalıştırıcı Sınıfı (Barebones).
 * Sıfırdan tekrar inşa edilmek üzere temizlenmiştir.
 */
public class MainApp {

	public static boolean playMode = true;
	private static boolean paused = false;
	private static boolean running = false;
	private static RenderEngine renderEngine;

	public static void setPaused(boolean p) {
		paused = p;
	}

	public static boolean isPaused() {
		return paused;
	}

	public static void stop() {
		running = false;
	}

	public static void main(String[] args) {
		// Native LWJGL kütüphanelerini yükle
		NativeLibraryLoader.loadNativeLibraries();

		// Oyun pencere ayarlarını başlat
		AppSettings.setup(1980, 1080, false, "Gane Engine 3D Scene", null);

		running = true;

		// Render motorunu başlat
		renderEngine = RenderEngine.init();

		// 1. Kamera Oluştur
		extra.Camera camera = new extra.Camera();
		camera.setMode(extra.Camera.CameraMode.FREE);
		camera.getPosition().set(64, 40, 64);
		camera.setPitch(20);

		// 2. Sahne (Scene) Oluştur
		scene.Scene scene = new scene.Scene(camera);

		// 3. Atmosfer
		skybox.atmosphere.AtmosphereSky atmoSky = new skybox.atmosphere.AtmosphereSky();
		scene.setSky(atmoSky);

		// 4. Gece/Gündüz Sistemi (Güneş yönetimi)
		// Başlangıç saati: 12.0f (Öğlen), Çarpan: 600.0f (Hızlı geçiş, önceden 100.0f
		// idi)
		environment.DayNightManager dayNight = new environment.DayNightManager(scene, 12.0f, 600.0f);

		// 5. Arazi (Infinite FlatTerrain)
		// Optimizasyon: FPS dususunu (fill rate) engellemek icin terrain boyutu 2000'e cekildi.
		// Takilmayi (snapping stutter) cozmek icin HighQuality (512 grid) acildi.
		terrain.flat.FlatTerrain terrain = new terrain.flat.FlatTerrain(2000, 2000); 
		terrain.setInfinite(true);
		terrain.setHighQuality(true); // gridCount = 512 yapar, vspX kuculur ve takilmalar gecer!
		terrain.generateProceduralTerrainV2(80f, 0.4f, 4, 250f, 12345L);
		scene.addTerrain(terrain);
		
		// Rendering sinirini gizlemek icin sis (fog) ayarlarini yapilandir:
		scene.setFogStart(800f);
		scene.setFogDensity(1.1f);

		// 6. Okyanus (Infinite Water)
		// We set a very large size so the ocean appears infinite
		water.tile.WaterTile ocean = new water.tile.WaterTile(0, 0, 5.0f, 20000f);
		scene.addWater(ocean);

		// GLB Test Object (Bird)
		scene.GameObject bird = new scene.GameObject("res/DEFAULT_BIRD/DEF_BIRD.glb", "res/DEFAULT_BIRD/texture_0.png");
		bird.getPosition().set(64, 45, 50); // Kuşu kameranın önüne koy
		bird.setScale(5.0f); // Kuşu 5 kat büyüt ki gözüksün
		scene.addEntity(bird);

		// 7. Atmosfer bulutları aktiftir (AtmosphereSky varsayılan olarak cloudsEnabled
		// = true)
		atmoSky.setCloudsEnabled(true);

		// 8. Ağaçlar ve Çimenler (Flora) otomatik olarak FloraManager tarafından
		// üretilecektir.
		// Eski statik üretim kodları kaldırıldı.

		// 9. UI ve FPS çizimi için OpenglYaziCizimi
		guiRendering.OpenglYaziCizimi uiText = new guiRendering.OpenglYaziCizimi();
		uiText.init();

		// 10. Güneş (Sun) Dokusu ve Objesi
		textures.Texture sunTex = textures.Texture.newTexture(new utils.MyFile("res/sun.png")).create();
		sunRenderer.Sun sun = new sunRenderer.Sun(sunTex, 200.0f); // Uzaklık 1000'e çıktığı için boyut orantılı
																	// büyütüldü
		scene.setSun(sun);

		long lastTime = System.nanoTime();

		int frames = 0;
		long timer = System.currentTimeMillis();
		int currentFps = 0;

		// POV için Dummy Player (Görünmez Merkez Noktası)
		scene.Entity dummyPlayer = new scene.Entity(null, null);
		dummyPlayer.getPosition().set(camera.getPosition());
		boolean vKeyPressed = false;
		String currentModeStr = "FREE";
		
		// Arazi/Okyanus durum degiskenleri
		boolean oKeyPressed = false;
		boolean isTerrainHighQuality = true;
		float terrainTexScale = 256f;
		float oceanTexScale = 0.6f;

		// Ana oyun döngüsü
		while (running && !Display.isCloseRequested()) {
			long currentTime = System.nanoTime();
			float delta = (currentTime - lastTime) / 1000000000.0f;
			lastTime = currentTime;

			// FPS hesaplama (saniyede bir güncellenir)
			frames++;
			if (System.currentTimeMillis() - timer > 1000) {
				timer += 1000;
				currentFps = frames;
				frames = 0;
			}

			// FOV (Görüş Açısı) Kontrolü
			if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_ADD)
					|| org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_EQUALS)) {
				camera.setFOV(camera.getFOV() - 10f * delta); // Zoom in
			}
			if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_SUBTRACT)
					|| org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_MINUS)) {
				camera.setFOV(camera.getFOV() + 10f * delta); // Zoom out
			}

			// POV Değiştirme (V Tuşu)
			if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_V)) {
				if (!vKeyPressed) {
					vKeyPressed = true;
					if (camera.getMode() == extra.Camera.CameraMode.FREE) {
						camera.setMode(extra.Camera.CameraMode.FIRST_PERSON);
						camera.setTarget(dummyPlayer);
						currentModeStr = "FIRST PERSON";
					} else if (camera.getMode() == extra.Camera.CameraMode.FIRST_PERSON) {
						camera.setMode(extra.Camera.CameraMode.RPG_THIRD_PERSON);
						currentModeStr = "THIRD PERSON";
					} else {
						camera.setMode(extra.Camera.CameraMode.FREE);
						camera.setTarget(null);
						currentModeStr = "FREE";
					}
				}
			} else {
				vKeyPressed = false;
			}

			// Dummy Player'ı hareket ettir (Sadece POV modlarında çalışır)
			if (camera.getMode() != extra.Camera.CameraMode.FREE) {
				float moveSpeed = 10.0f * delta;
				if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_W)) {
					dummyPlayer.getPosition().x += moveSpeed * Math.sin(Math.toRadians(camera.getYaw()));
					dummyPlayer.getPosition().z -= moveSpeed * Math.cos(Math.toRadians(camera.getYaw()));
				}
				if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_S)) {
					dummyPlayer.getPosition().x -= moveSpeed * Math.sin(Math.toRadians(camera.getYaw()));
					dummyPlayer.getPosition().z += moveSpeed * Math.cos(Math.toRadians(camera.getYaw()));
				}
				if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_A)) {
					dummyPlayer.getPosition().x -= moveSpeed * Math.cos(Math.toRadians(camera.getYaw()));
					dummyPlayer.getPosition().z -= moveSpeed * Math.sin(Math.toRadians(camera.getYaw()));
				}
				if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_D)) {
					dummyPlayer.getPosition().x += moveSpeed * Math.cos(Math.toRadians(camera.getYaw()));
					dummyPlayer.getPosition().z += moveSpeed * Math.sin(Math.toRadians(camera.getYaw()));
				}
				// Oyuncuyu yere yapıştır (Arazi Yüksekliğine eşitle)
				float h = terrain.getHeightAt(dummyPlayer.getPosition().x, dummyPlayer.getPosition().z);
				dummyPlayer.getPosition().y = h;
			} else {
				// FREE moddayken dummy'i kamerayla aynı yere taşı ki First Person'a geçerken
				// aniden uzağa ışınlanmasın
				dummyPlayer.getPosition().set(camera.getPosition().x, camera.getPosition().y, camera.getPosition().z);
			}

			// --- ARAZİ VE OKYANUS KONTROLLERİ ---
			// Arazi Poligon Sayısı (O Tuşu - Toggle)
			if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_O)) {
				if (!oKeyPressed) {
					oKeyPressed = true;
					isTerrainHighQuality = !isTerrainHighQuality;
					terrain.setHighQuality(isTerrainHighQuality);
				}
			} else {
				oKeyPressed = false;
			}
			
			// Arazi Doku Boyutu (K: Küçült, L: Büyüt)
			if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_K)) {
				terrainTexScale = Math.max(10f, terrainTexScale - 100f * delta);
				terrain.setTextureScale(terrainTexScale);
			}
			if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_L)) {
				terrainTexScale = Math.min(2000f, terrainTexScale + 100f * delta);
				terrain.setTextureScale(terrainTexScale);
			}
			
			// Okyanus Doku Boyutu (N: Küçült, M: Büyüt)
			if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_N)) {
				oceanTexScale = Math.max(0.01f, oceanTexScale - 0.5f * delta);
				ocean.setTextureScale(oceanTexScale);
			}
			if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_M)) {
				oceanTexScale = Math.min(10.0f, oceanTexScale + 0.5f * delta);
				ocean.setTextureScale(oceanTexScale);
			}

			// Kamerayı hareket ettir
			camera.move();

			// Gece gündüz döngüsünü güncelle
			dayNight.update(delta);

			// Sahneyi çiz
			renderEngine.renderScene(scene, delta);

			// Ekranı güncellemeden hemen önce UI (Yazı) çiz
			uiText.beginUI();
			uiText.drawText("FPS: " + currentFps + " | FOV: " + (int) camera.getFOV() + " | POV: " + currentModeStr, 20,
					20, java.awt.Color.WHITE);
			uiText.drawText("POV Modunu Degistirmek icin V tusuna basin.", 20, 50, java.awt.Color.LIGHT_GRAY);
			
			// Yeni kontrolleri ekrana yazdır:
			uiText.drawText("Arazi Kalitesi [O]: " + (isTerrainHighQuality ? "YUKSEK (512x512)" : "DUSUK (256x256)"), 20, 80, java.awt.Color.YELLOW);
			uiText.drawText("Arazi Doku Boyutu [K / L]: " + (int)terrainTexScale, 20, 110, java.awt.Color.YELLOW);
			uiText.drawText("Okyanus Doku Boyutu [N / M]: " + String.format("%.2f", oceanTexScale), 20, 140, java.awt.Color.CYAN);
			
			uiText.endUI();

			// Ekranı güncelle
			renderEngine.update();
		}

		// Temizle ve kapat
		uiText.cleanup();
		scene.delete();
		renderEngine.close();
	}
}
