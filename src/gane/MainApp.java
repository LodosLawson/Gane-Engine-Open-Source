package gane;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import extra.Camera;
import guiRendering.RotatingCube;
import guiRendering.UIManager;
import physics.PhysicsEngine;
import renderEngine.RenderEngine;
import scene.Scene;
import steam.SteamManager;
import scene.Entity;
import scene.GameObject;
import scene.KeyframeAnimationComponent;
import scene.Skin;
import textures.Texture;
import loaders.ModelLoader;
import scene.Model;
import utils.NativeLibraryLoader;
import utils.MyFile;
import utils.MousePicker;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Vector3f;
import scene.GameObject;

public class MainApp {

	private static Scene activeScene;
	private static boolean running = false;
	private static boolean paused = false;

	public static void setActiveScene(Scene scene) {
		activeScene = scene;
	}

	public static void main(String[] args) {
		// Set LWJGL native library path before loading any LWJGL classes
		NativeLibraryLoader.loadNativeLibraries();

		// 1. Oyun / Pencere Ayarlarını Yapılandır (Özel Pencere Başlığı, Çözünürlük ve
		// İkon)
		AppSettings.setup(1280, 720, false, "Gane Engine 3D Scene", "res/WoodFloor004_4K-PNG_Color.png");

		// Steam API'yi başlat
		SteamManager.init();

		running = true;

		// 1. Engine başlat (Display + renderer'lar)
		RenderEngine renderEngine = RenderEngine.init();

		// --- YÜKLEME EKRANI (LOADING SCREEN) BAŞLAT ---
		// Kullanıcı istediği temayı verebilir (Örn: UITheme.neon())
		guiRendering.LoadingScreen loadingScreen = new guiRendering.LoadingScreen(guiRendering.UITheme.neon());

		loadingScreen.render(10, "Motor Baslatiliyor...");
		sleepSimulate(400); // Geliştiricinin asset'leri yüklediği zamanı simüle eder

		// Fizik motorunu başlat
		PhysicsEngine physicsEngine = new PhysicsEngine();

		loadingScreen.render(40, "Fizik Motoru Yuklendi...");
		sleepSimulate(400);

		// 2. Basit sahne kur: sadece kamera, beyaz arkaplan ve hiçbir obje yok.
		Camera camera = new Camera();

		activeScene = new Scene(camera, null, true);
		activeScene.getWater().clear();

		water.WaterTile waterr = new water.WaterTile(0f, 0f, -2.0f, 100f);
		activeScene.getWater().add(waterr);

		// Su yüzeyi ayarları
		waterr.setWaveSpeed(0.001f); // Dalga akış hızı
		waterr.setWaveStrength(0.015f); // Dalga bükülme (distortion) gücü
		waterr.setWaveAmplitude(0.15f); // Vertex dalga yüksekliği (3D dalga efekti)
		waterr.setWaveFrequency(0.3f); // Dalga frekansı (sıklık)

		// Renk ve saydamlık
		waterr.setWaterColor(0.0f, 0.25f, 0.4f); // Koyu okyanus mavisi
		waterr.setTransparency(0.55f); // Yarı saydam
		waterr.setColorMixFactor(0.15f); // Su renginin görünürlüğü

		// Derinlik efekti
		waterr.setDepth(10.0f);
		waterr.setDepthDarkness(0.7f);

		// Sualtı kamera efekti
		waterr.setUnderwaterFogDensity(0.06f);
		waterr.setUnderwaterFogColor(0.0f, 0.12f, 0.25f);

		// Yansıma kalitesi
		waterr.setFresnelPower(0.6f);
		waterr.setShineDamper(30.0f);
		waterr.setReflectivity(0.9f);
		activeScene.setLightDirection(new org.lwjgl.util.vector.Vector3f(-1f, 0f, 0f));
		activeScene.setLightBrightness(1f);
		activeScene.setAmbientLight(0.2f);

		MousePicker picker = new MousePicker(camera, camera.getProjectionMatrix());
		boolean mouseWasDown = false;

		loadingScreen.render(70, "Sahne Olusturuldu...");
		sleepSimulate(400);

		// 3. UI yöneticisi başlat ve buton ekle
		UIManager uiManager = new UIManager(guiRendering.UITheme.neon());
		uiManager.addButton(60, 110, 200, 48, "Hello World!",
				() -> uiManager.showMessage("Hello World!"));

		gane.Menu.escMneu pauseMenu = new gane.Menu.escMneu();

		loadingScreen.render(100, "Tamamlandi! Oyun Basliyor...");
		sleepSimulate(400);

		// 4. Arkaplanda dönen renkli 3D küp
		// RotatingCube cube = new RotatingCube(0f, 0f, -5f);

		// Test Model Ekleme
		ModelLoader modelLoader = new ModelLoader();
		Model model = modelLoader.loadModel(new MyFile("res/Başlıksız.obj"));

		// 1. Ana renk kaplamasını (Wood Floor Color) yükle ve filtreleri uygula
		Texture texture = Texture
				.newTexture(new MyFile("res/WoodFloor004_4K-PNG_Color.png"))
				.anisotropic()
				.create();

		// 2. Klasörden çıkan pürüzlülük (Roughness) haritasını yükle
		Texture extraTexture = Texture
				.newTexture(new MyFile("res/WoodFloor004_4K-PNG_Roughness.png"))
				.anisotropic()
				.create();

		// 3. Materyali (Skin) oluştur ve objeyi sahneye yerleştir (Eski Mimari /
		// Manuel)
		Skin skin = new Skin(texture, extraTexture);
		skin.setTransparent(true);
		Entity testentity = new Entity(model, skin);
		testentity.getPosition().set(5f, 0f, -5f);
		activeScene.addEntity(testentity);

		// ----------------------------------------------------
		// YENİ MODERN MİMARİ (GameObject - Bileşen Tabanlı)
		// ----------------------------------------------------
		// Model, texture, animasyon ve pozisyon gibi tüm ayarlar
		// AhsapZemin sınıfının kendi içinde tanımlandı.
		// MainApp sadece objeyi çağırır ve sahneye ekler!
		gane.objects.AhsapZemin yeniZemin = new gane.objects.AhsapZemin();
		activeScene.addEntity(yeniZemin);

		// Su alanının tam ortasında (0, 0) ve su seviyesinin yarı altında (-2.2f) duran
		gane.objects.AhsapZemin suIciZemin = new gane.objects.AhsapZemin();
		suIciZemin.getPosition().set(-8.2f, -3.42f, 0f); // Objenin model yüksekliğine göre Y=-3.42f değeri objeyi su seviyesinde (-2.0f) tam yarı yarıya batırır

		// Su üstünde yüzen hissi vermek için animatörü özelleştiriyoruz
		scene.AnimatorComponent suAnimator = suIciZemin.getComponent(scene.AnimatorComponent.class);
		if (suAnimator != null) {
			suAnimator.setBounceEffect(0.5f, 0.15f); // Yavaşça ve hafifçe su üstünde batıp çıkma hareketi yapar
			suAnimator.setContinuousRotation(0f, 15f, 0f); // Yavaşça kendi ekseninde döner
		}
		activeScene.addEntity(suIciZemin);

		// 1. Güneş Işığı (Directional Light) - Bütün sahneyi aynı yönden aydınlatır
		// Zaten Scene içinde tanımlı ama yönünü değiştirebilirsin. (x, y, z)
		activeScene.setLightDirection(new org.lwjgl.util.vector.Vector3f(0.5f, -1f, 0.5f));

		// Çıkıntılı objeni tanımla
		GameObject cikintiliObje = new GameObject("res/animt/Başlıksızz0001.obj",
				"res/WoodFloor004_4K-PNG_Color.png");
		// Animasyon bileşenini ekle
		KeyframeAnimationComponent animasyon = new KeyframeAnimationComponent();
		// 1. kareden 60. kareye kadar olan OBJ dosyalarını otomatik yükle
		animasyon.loadFrames("res/animt/Başlıksızz", 1, 100, ".obj");
		animasyon.setFPS(60f); // Saniyede 24 kare hızla oynat
		animasyon.setLoop(true); // Sürekli tekrar etsin
		cikintiliObje.addComponent(animasyon);
		activeScene.addEntity(cikintiliObje);

		// 2. Nokta/Spot Işığı (Point Light) - Sadece yakınındaki objeleri aydınlatır
		// Parametreler: Pozisyon(x,y,z), Renk(R,G,B), Azalma/Zayıflama(Sabit, Doğrusal,
		// Kare)
		// Azalma (Attenuation) değerini artırırsan ışığın menzili kısalır.
		scene.Light pointLight = new scene.Light(
				new org.lwjgl.util.vector.Vector3f(5f, 2f, -3f), // Işığın konumu (Modelin azıcık önünde/üstünde)
				new org.lwjgl.util.vector.Vector3f(1.0f, 0.2f, 0.2f), // Işığın rengi (Kırmızımsı)
				new org.lwjgl.util.vector.Vector3f(1.0f, 0.05f, 0.01f) // Işığın menzili (Uzaklaştıkça sönme miktarı)
		);
		// activeScene.setPointLight(pointLight);

		// 3. Ana oyun döngüsü
		while (!Display.isCloseRequested() && running) {

			// Güncelleme (update) döngüsünün içi:
			while (Keyboard.next()) {
				if (Keyboard.getEventKeyState()) { // Tuşa basıldığında true döner
					if (Keyboard.getEventKey() == Keyboard.KEY_ESCAPE) {
						paused = !paused;
						pauseMenu.setActive(paused);
					}

					// --- KAMERA MODU DEĞİŞTİRME KISAYOLLARI ---
					if (Keyboard.getEventKey() == Keyboard.KEY_1) {
						camera.setMode(extra.Camera.CameraMode.EDITOR);
						uiManager.showMessage("Kamera Modu: EDITOR");
					}
					if (Keyboard.getEventKey() == Keyboard.KEY_2) {
						camera.setMode(extra.Camera.CameraMode.FREE);
						uiManager.showMessage("Kamera Modu: FREE (Serbest Ucus)");
					}
					if (Keyboard.getEventKey() == Keyboard.KEY_3) {
						camera.setMode(extra.Camera.CameraMode.THIRD_PERSON);
						uiManager.showMessage("Kamera Modu: THIRD PERSON");
					}

					// --- ANİMASYON TETİKLEME KISAYOLU (SPACE) ---
					if (Keyboard.getEventKey() == Keyboard.KEY_SPACE) {
						for (var entity : activeScene.getAllEntities()) {
							if (entity instanceof scene.GameObject) {
								scene.GameObject go = (scene.GameObject) entity;
								scene.AnimatorComponent anim = go.getComponent(scene.AnimatorComponent.class);
								if (anim != null) {
									anim.triggerAction();
									uiManager.showMessage("Animasyon Tetiklendi! (Spin & Bounce)");
								}
							}
						}
					}
				}
			}

			float delta = renderEngine.getDisplayManager().getFrameTime();

			if (!paused) {
				camera.move();
				picker.update();

				// Sadece Editör modundayken (ve Shift'e basılı DEĞİLKEN) obje seçimi yap
				boolean isMouseDown = Mouse.isButtonDown(0);
				if (camera.getMode() == extra.Camera.CameraMode.EDITOR &&
						!(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))) {

					if (isMouseDown && !mouseWasDown) {
						for (var entity : activeScene.getAllEntities()) {
							// 3.0f yarıçaplı bir küre çarpışma testi
							if (MousePicker.intersects(camera.getPosition(), picker.getCurrentRay(),
									entity.getPosition(), 3.0f)) {
								uiManager.showMessage("Secilen Obje: " + entity.getClass().getSimpleName());
								break; // İlk bulduğunu seç ve çık
							}
						}
					}
				}
				mouseWasDown = isMouseDown;

				// Fizik motorunu çalıştır
				physicsEngine.update(activeScene, delta);

				// Objelerin ve komponentlerin kendi update mantığını çalıştır
				for (var entity : activeScene.getAllEntities()) {
					entity.update(delta);
				}

				// Ana oyun UI giriş durumunu güncelle (fare hover / tıklama)
				uiManager.update();
			} else {
				// Oyun duraklatıldığında sadece duraklatma menüsü girdileri güncellenir
				pauseMenu.update();
			}

			// Steam arka plan bildirimleri ve P2P paketlerini dinle
			SteamManager.update();

			// Küpü güncelle
			// cube.update(delta);

			// 3D sahnesi çiz (Arkaplanda statik 3D görüntü kalmaya devam etsin)
			renderEngine.renderScene(activeScene);

			// Küpü perspective projeksiyon ile sahneden sonra çiz
			GL11.glMatrixMode(GL11.GL_PROJECTION);
			GL11.glLoadIdentity();
			float fov = (float) Math.toRadians(70);
			float near = 0.1f;
			float far = 1000f;
			float aspect = (float) Display.getWidth() / Display.getHeight();
			float yScale = 1f / (float) Math.tan(fov / 2f);
			float xScale = yScale / aspect;
			float frustumLen = far - near;
			java.nio.FloatBuffer projBuf = org.lwjgl.BufferUtils.createFloatBuffer(16);
			projBuf.put(new float[] {
					xScale, 0, 0, 0,
					0, yScale, 0, 0,
					0, 0, -((far + near) / frustumLen), -1,
					0, 0, -((2 * near * far) / frustumLen), 0
			}).flip();
			GL11.glLoadMatrix(projBuf);
			GL11.glMatrixMode(GL11.GL_MODELVIEW);
			GL11.glLoadIdentity();
			// cube.render();

			// UI'yı 3D üstüne çiz (en son çalışır)
			if (!paused) {
				uiManager.render();
			}

			// Duraklatma menüsünü çiz
			pauseMenu.render();

			renderEngine.update();
		}

		activeScene.delete();
		uiManager.cleanup();
		renderEngine.close();
		SteamManager.shutdown();
		running = false;
	}

	public static Scene getActiveScene() {
		return activeScene;
	}

	public static void stop() {
		running = false;
	}

	public static boolean isPaused() {
		return paused;
	}

	public static void setPaused(boolean p) {
		paused = p;
	}

	private static void sleepSimulate(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
		}
	}
}
