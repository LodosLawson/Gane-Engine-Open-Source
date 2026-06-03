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
		camera.setMode(extra.Camera.CameraMode.RPG_THIRD_PERSON);
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
		// Optimizasyon: FPS dususunu (fill rate) engellemek icin terrain boyutu 2000'e
		// cekildi.
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

		// Kuş dönüş (rotation) offsetlerini live (canlı) değiştirmek için değişkenler
		float currentBirdYawOffset = 270.0f; // 0.0f yaparsanız kuşlar ters/yan uçar. Blender modelleri için 180 olması
												// gerekir!
		float currentBirdPitchOffset = -90.0f;
		float currentBirdRollOffset = 0.0f; // Sıfırlandı, yan yatmayı önlemek için.

		// GLB Test Object (Bird)
		scene.GameObject bird = new scene.GameObject("res/DEFAULT_BIRD/DEF_BIRD.glb", "res/DEFAULT_BIRD/texture_0.png");
		bird.getPosition().set(0, 0, 0); // Kuşu kameranın önüne koy
		bird.setScale(0.5f); // Kuşu daha görünür olması için 2.5 kata çıkardık
		default_controls.BirdPlayerController birdController = new default_controls.BirdPlayerController(camera, terrain);
		birdController.setModelYawOffset(currentBirdYawOffset);
		birdController.setModelPitchOffset(currentBirdPitchOffset);
		birdController.setModelRollOffset(currentBirdRollOffset);
		bird.addComponent(birdController);
		scene.addEntity(bird);

		// Kameranın başlangıçta kuşa odaklanmasını sağla
		camera.setTarget(bird);

		// Yapay Zekalı Diğer Kuşları Ekle (AI Birds)
		java.util.Random rand = new java.util.Random();
		for (int i = 0; i < 40; i++) {
			scene.GameObject aiBird = new scene.GameObject("res/DEFAULT_BIRD/DEF_BIRD.glb",
					"res/DEFAULT_BIRD/texture_0.png");
			// Geniş bir alana (2000x2000) değil, oyuncunun 200 birim yakınına dağıt
			float startX = 64.0f + (rand.nextFloat() * 400f - 200f);
			float startZ = 64.0f + (rand.nextFloat() * 400f - 200f);
			float startY = terrain.getHeightAt(startX, startZ) + 30f + rand.nextFloat() * 60f;

			aiBird.getPosition().set(startX, startY, startZ);
			aiBird.setScale(2.0f + rand.nextFloat() * 1.5f); // Kuş boyutları 2.0 ile 3.5 arası rastgele olsun

			default_controls.BirdAIController aiController = new default_controls.BirdAIController(terrain);
			aiController.setModelYawOffset(currentBirdYawOffset);
			aiController.setModelPitchOffset(currentBirdPitchOffset);
			aiController.setModelRollOffset(currentBirdRollOffset);
			aiBird.addComponent(aiController);

			scene.addEntity(aiBird);
		}

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

		// 11. Gemi (Ship) Ekleme
		// Yeni mimari ile direkt .glb yüklüyoruz.
		// Artık multi-mesh desteklendiği için fishing_boat_v.glb eksiksiz olarak 40
		// parça halinde render edilecektir.
		scene.GameObject ship = new scene.GameObject("res/DEFAULT_VEC_SHIP/fishing_boat_v.glb");
		ship.getPosition().set(0, 5.0f, 30); // Gemiyi tam gözümüzün önüne, kuşun 30 birim uzağına koyalım
		ship.getRotation().set(-90, 0, 0); // Geminin burnu suya batık olmaması için düzeltildi
		ship.setScale(1.0f); // Boyutu sıfırladık
		ship.setCullingRadius(50000.0f);

		default_controls.ShipController shipController = new default_controls.ShipController(camera, terrain);
		ship.addComponent(shipController);
		ship.getPosition().set(-15, 0, 0);
		scene.addEntity(ship);

		// 12. Balık Ekleme (Koi Fish) - Tekil Balık (Oyuncu Kontrollü)
		scene.GameObject fish = new scene.GameObject("res/DEFAULT_FISH/koi_fish.glb");
		fish.getPosition().set(-10, 2.0f, 0); // Geminin hemen yanına
		fish.setScale(0.5f); // Balık boyutunu ayarlayalım
		fish.setCullingRadius(5000.0f);
		default_controls.FishPlayerController fishPlayerController = new default_controls.FishPlayerController(camera);
		fish.addComponent(fishPlayerController);
		scene.addEntity(fish);

		// Sürü Halinde Balıklar (School of Fishes)
		for (int i = 0; i < 30; i++) {
			scene.GameObject aiFish = new scene.GameObject("res/DEFAULT_FISH/koi_fish.glb");
			// Geminin etrafına (100 birim yarıçaplı alana) rastgele dağıt
			float fStartX = ship.getPosition().x + (rand.nextFloat() * 200f - 100f);
			float fStartZ = ship.getPosition().z + (rand.nextFloat() * 200f - 100f);
			
			aiFish.getPosition().set(fStartX, 2.0f, fStartZ); 
			aiFish.setScale(0.3f + rand.nextFloat() * 0.4f); // 0.3 - 0.7 arası rastgele boyut
			aiFish.setCullingRadius(5000.0f);
			
			default_controls.FishController aiFishController = new default_controls.FishController();
			aiFish.addComponent(aiFishController);
			
			scene.addEntity(aiFish);
		}

		// Kameranın başlangıçta gemiye odaklanmasını sağla
		camera.setTarget(ship);

		long lastTime = System.nanoTime();

		int frames = 0;
		long timer = System.currentTimeMillis();
		int currentFps = 0;

		// POV için Player (Kuşu kullanıyoruz)
		scene.Entity player = bird;
		boolean vKeyPressed = false;
		String currentModeStr = "THIRD PERSON";

		// Arazi/Okyanus durum degiskenleri
		boolean oKeyPressed = false;
		boolean isTerrainHighQuality = true;
		int terrainGridCount = 512;
		boolean leftBracketPressed = false;
		boolean rightBracketPressed = false;
		float terrainTexScale = 256f;
		float oceanTexScale = 0.6f;

		boolean f1Pressed = false, f2Pressed = false, f3Pressed = false, f4Pressed = false, f5Pressed = false,
				f6Pressed = false;

		// Zaman kontrolleri
		boolean pKeyPressed = false;
		boolean xKeyPressed = false;
		float savedTimeMultiplier = dayNight.getTimeMultiplier();
		boolean timePaused = false;

		// Ana oyun döngüsü
		while (running && !Display.isCloseRequested()) {
			long currentTime = System.nanoTime();
			float delta = (currentTime - lastTime) / 1000000000.0f;
			lastTime = currentTime;

			// Su Mesh Wireframe (F3) - Bas-Çek kontrolü (Debounce)
			if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_F3)) {
				if (!f3Pressed) {
					water.ocean.OceanRenderer.renderWireframe = !water.ocean.OceanRenderer.renderWireframe;
					f3Pressed = true;
				}
			} else {
				f3Pressed = false;
			}

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
						camera.setTarget(player);
						currentModeStr = "FIRST PERSON (KUS)";
					} else if (camera.getMode() == extra.Camera.CameraMode.FIRST_PERSON) {
						camera.setMode(extra.Camera.CameraMode.RPG_THIRD_PERSON);
						camera.setTarget(player);
						currentModeStr = "THIRD PERSON (KUS)";
					} else if (camera.getMode() == extra.Camera.CameraMode.RPG_THIRD_PERSON
							&& camera.getTarget() == player) {
						camera.setMode(extra.Camera.CameraMode.RPG_THIRD_PERSON);
						camera.setTarget(ship);
						currentModeStr = "THIRD PERSON (GEMI)";
					} else if (camera.getMode() == extra.Camera.CameraMode.RPG_THIRD_PERSON
							&& camera.getTarget() == ship) {
						camera.setMode(extra.Camera.CameraMode.RPG_THIRD_PERSON);
						camera.setTarget(fish);
						currentModeStr = "THIRD PERSON (BALIK)";
					} else {
						camera.setMode(extra.Camera.CameraMode.FREE);
						camera.setTarget(null);
						currentModeStr = "FREE";
					}
				}
			} else {
				vKeyPressed = false;
			}

			// Tüm nesneleri güncelle (Bileşenleri ve animasyonları çalıştırır)
			for (int i = 0; i < scene.getAllEntities().size(); i++) {
				scene.getAllEntities().get(i).update(delta);
			}

			// --- ARAZİ VE OKYANUS KONTROLLERİ ---
			// Arazi Poligon Sayısı (O Tuşu - Toggle)
			if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_O)) {
				if (!oKeyPressed) {
					oKeyPressed = true;
					isTerrainHighQuality = !isTerrainHighQuality;
					terrainGridCount = isTerrainHighQuality ? 512 : 256;
					terrain.setGridCount(terrainGridCount);
				}
			} else {
				oKeyPressed = false;
			}

			// Arazi Poligon/Mesh Çözünürlüğünü Manuel Artırma/Azaltma ([ ve ] Tuşları)
			if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LBRACKET)) {
				if (!leftBracketPressed) {
					leftBracketPressed = true;
					terrainGridCount = Math.max(64, terrainGridCount / 2);
					terrain.setGridCount(terrainGridCount);
				}
			} else {
				leftBracketPressed = false;
			}

			if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RBRACKET)) {
				if (!rightBracketPressed) {
					rightBracketPressed = true;
					terrainGridCount = Math.min(2048, terrainGridCount * 2);
					terrain.setGridCount(terrainGridCount);
				}
			} else {
				rightBracketPressed = false;
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

			// --- ZAMAN / GÜNEŞ KONTROLLERİ ---
			// Render Modu Cycle (X tuşu)
			if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_X)) {
				if (!xKeyPressed) {
					xKeyPressed = true;
					postProcessing.PostProcessing.RenderMode[] modes = postProcessing.PostProcessing.RenderMode.values();
					int nextOrdinal = (postProcessing.PostProcessing.currentRenderMode.ordinal() + 1) % modes.length;
					postProcessing.PostProcessing.currentRenderMode = modes[nextOrdinal];
				}
			} else {
				xKeyPressed = false;
			}

			// Durdur / Devam Et (P tuşu)
			if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_P)) {
				if (!pKeyPressed) {
					pKeyPressed = true;
					timePaused = !timePaused;
					if (timePaused) {
						savedTimeMultiplier = dayNight.getTimeMultiplier();
						dayNight.setTimeMultiplier(0);
					} else {
						dayNight.setTimeMultiplier(savedTimeMultiplier);
					}
				}
			} else {
				pKeyPressed = false;
			}

			// Hızlandır / Yavaşlat (Yukarı / Aşağı Ok Tuşları)
			if (!timePaused) {
				if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_UP)) {
					dayNight.setTimeMultiplier(dayNight.getTimeMultiplier() + 500f * delta);
				}
				if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_DOWN)) {
					dayNight.setTimeMultiplier(Math.max(1.0f, dayNight.getTimeMultiplier() - 500f * delta));
				}
			}

			// Saati manuel değiştir (Sol / Sağ Ok Tuşları)
			if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RIGHT)) {
				float newTime = dayNight.getTimeOfDay() + 4.0f * delta;
				if (newTime >= 24.0f)
					newTime -= 24.0f;
				dayNight.setTimeOfDay(newTime);
			}
			if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LEFT)) {
				float newTime = dayNight.getTimeOfDay() - 4.0f * delta;
				if (newTime < 0.0f)
					newTime += 24.0f;
				dayNight.setTimeOfDay(newTime);
			}

			// Güneş Yörünge Rotasyonu (U / I tuşları)
			if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_U)) {
				dayNight.setSunOrbitYaw(dayNight.getSunOrbitYaw() - 30.0f * delta);
			}
			if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_I)) {
				dayNight.setSunOrbitYaw(dayNight.getSunOrbitYaw() + 30.0f * delta);
			}

			// Kus Offset Ayarlari (F1-F6)
			if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_F1)) {
				if (!f1Pressed) {
					currentBirdPitchOffset += 10.0f;
					f1Pressed = true;
					System.out.println("Yeni Kus Offsetleri -> Pitch: " + currentBirdPitchOffset + ", Yaw: "
							+ currentBirdYawOffset + ", Roll: " + currentBirdRollOffset);
				}
			} else {
				f1Pressed = false;
			}

			if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_F2)) {
				if (!f2Pressed) {
					currentBirdPitchOffset -= 10.0f;
					f2Pressed = true;
					System.out.println("Yeni Kus Offsetleri -> Pitch: " + currentBirdPitchOffset + ", Yaw: "
							+ currentBirdYawOffset + ", Roll: " + currentBirdRollOffset);
				}
			} else {
				f2Pressed = false;
			}

			if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_F3)) {
				if (!f3Pressed) {
					currentBirdYawOffset += 10.0f;
					f3Pressed = true;
					System.out.println("Yeni Kus Offsetleri -> Pitch: " + currentBirdPitchOffset + ", Yaw: "
							+ currentBirdYawOffset + ", Roll: " + currentBirdRollOffset);
				}
			} else {
				f3Pressed = false;
			}

			if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_F4)) {
				if (!f4Pressed) {
					currentBirdYawOffset -= 10.0f;
					f4Pressed = true;
					System.out.println("Yeni Kus Offsetleri -> Pitch: " + currentBirdPitchOffset + ", Yaw: "
							+ currentBirdYawOffset + ", Roll: " + currentBirdRollOffset);
				}
			} else {
				f4Pressed = false;
			}

			if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_F5)) {
				if (!f5Pressed) {
					currentBirdRollOffset += 10.0f;
					f5Pressed = true;
					System.out.println("Yeni Kus Offsetleri -> Pitch: " + currentBirdPitchOffset + ", Yaw: "
							+ currentBirdYawOffset + ", Roll: " + currentBirdRollOffset);
				}
			} else {
				f5Pressed = false;
			}

			if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_F6)) {
				if (!f6Pressed) {
					currentBirdRollOffset -= 10.0f;
					f6Pressed = true;
					System.out.println("Yeni Kus Offsetleri -> Pitch: " + currentBirdPitchOffset + ", Yaw: "
							+ currentBirdYawOffset + ", Roll: " + currentBirdRollOffset);
				}
			} else {
				f6Pressed = false;
			}

			// Apply new offsets to player bird
			birdController.setModelPitchOffset(currentBirdPitchOffset);
			birdController.setModelYawOffset(currentBirdYawOffset);
			birdController.setModelRollOffset(currentBirdRollOffset);

			// Apply new offsets to AI birds
			for (scene.Entity ent : scene.getAllEntities()) {
				if (ent instanceof scene.GameObject) {
					default_controls.BirdAIController aiC = ((scene.GameObject) ent).getComponent(default_controls.BirdAIController.class);
					if (aiC != null) {
						aiC.setModelPitchOffset(currentBirdPitchOffset);
						aiC.setModelYawOffset(currentBirdYawOffset);
						aiC.setModelRollOffset(currentBirdRollOffset);
					}
				}
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
			uiText.drawText("POV Degistir: [V] | Render Modu [X]: " + postProcessing.PostProcessing.currentRenderMode.name(), 20, 50, java.awt.Color.LIGHT_GRAY);

			// Yeni kontrolleri ekrana yazdır:
			uiText.drawText("Arazi Kalitesi/Poligon (Grid) [O, [, ] ]: " + terrainGridCount + "x" + terrainGridCount,
					20, 80, java.awt.Color.YELLOW);
			uiText.drawText(
					"Kus Offset (Canli Test) -> Pitch(F1/F2): " + currentBirdPitchOffset + " | Yaw(F3/F4): "
							+ currentBirdYawOffset + " | Roll(F5/F6): " + currentBirdRollOffset,
					20, 110, java.awt.Color.ORANGE);
			uiText.drawText("Okyanus Doku Boyutu [N / M]: " + String.format("%.2f", oceanTexScale), 20, 140,
					java.awt.Color.CYAN);

			// Gemi Scale ayarlama
			if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_8)) {
				ship.setScale(ship.getScale() + 5.0f * delta * ship.getScale());
			}
			if (org.lwjgl.input.Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_2)) {
				ship.setScale(Math.max(0.0001f, ship.getScale() - 5.0f * delta * ship.getScale()));
			}

			// Zaman bilgileri UI
			String timeStr = String.format("%.1f", dayNight.getTimeOfDay());
			String speedStr = timePaused ? "DURDURULDU" : String.format("%.0fx", dayNight.getTimeMultiplier());
			uiText.drawText(
					"Saat [Sol/Sag]: " + timeStr + " | Zaman Hizi [Yukari/Asagi]: " + speedStr + " | Durdur [P]", 20,
					170, java.awt.Color.GREEN);

			String orbitStr = String.format("%.0f", dayNight.getSunOrbitYaw());
			uiText.drawText("Gunes Rotasyonu [U/I]: " + orbitStr + " derece", 20, 200, java.awt.Color.ORANGE);

			uiText.drawText("Gemi Scale [8 / 2]: " + String.format("%.4f", ship.getScale()) + " | Yukseklik [J / K]",
					20, 230, java.awt.Color.MAGENTA);

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
