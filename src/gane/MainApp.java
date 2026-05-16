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
import utils.NativeLibraryLoader;

public class MainApp {

	private static Scene activeScene;
	private static boolean running = false;
	
	public static void setActiveScene(Scene scene) {
		activeScene = scene;
	}

	public static void main(String[] args) {
		// Set LWJGL native library path before loading any LWJGL classes
		NativeLibraryLoader.loadNativeLibraries();
		
		// Steam API'yi başlat
		SteamManager.init();
		
		running = true;

		// 1. Engine başlat (Display + renderer'lar)
		RenderEngine renderEngine = RenderEngine.init();
		
		// Fizik motorunu başlat
		PhysicsEngine physicsEngine = new PhysicsEngine();

		// 2. Basit sahne kur: sadece kamera, beyaz arkaplan ve hiçbir obje yok.
		Camera camera = new Camera();
		activeScene = new Scene(camera, null, false);
		activeScene.setLightDirection(WorldSettings.LIGHT_DIR);

		// 3. UI yöneticisi başlat ve buton ekle
		UIManager uiManager = new UIManager();
		uiManager.addButton(60, 110, 200, 48, "Hello World!",
				() -> uiManager.showMessage("Hello World!"));

		// 4. Arkaplanda dönen renkli 3D küp
		RotatingCube cube = new RotatingCube(0f, 0f, -5f);

		// 3. Ana oyun döngüsü
		while (!Display.isCloseRequested() && running) {

			camera.move();

			float delta = renderEngine.getDisplayManager().getFrameTime();
			
			// Fizik motorunu çalıştır
			physicsEngine.update(activeScene, delta);
			
			// Steam arka plan bildirimleri ve P2P paketlerini dinle
			SteamManager.update();
			
			for (var entity : activeScene.getAllEntities()) {
				entity.update(delta);
			}

			// UI giriş durumunu güncelle (fare hover / tıklama)
			uiManager.update();
			
			// Küpü güncelle
			cube.update(delta);

			// 3D sahnesi çiz
			renderEngine.renderScene(activeScene);

			// Küpü perspective projeksiyon ile sahneden sonra çiz
			GL11.glMatrixMode(GL11.GL_PROJECTION);
			GL11.glLoadIdentity();
			float fov  = (float) Math.toRadians(70);
			float near = 0.1f;
			float far  = 1000f;
			float aspect = (float) Display.getWidth() / Display.getHeight();
			float yScale = 1f / (float) Math.tan(fov / 2f);
			float xScale = yScale / aspect;
			float frustumLen = far - near;
			java.nio.FloatBuffer projBuf = org.lwjgl.BufferUtils.createFloatBuffer(16);
			projBuf.put(new float[]{
				xScale, 0, 0, 0,
				0, yScale, 0, 0,
				0, 0, -((far + near) / frustumLen), -1,
				0, 0, -((2 * near * far) / frustumLen), 0
			}).flip();
			GL11.glLoadMatrix(projBuf);
			GL11.glMatrixMode(GL11.GL_MODELVIEW);
			GL11.glLoadIdentity();
			cube.render();

			// UI'yı 3D üstüne çiz (en son çalışır)
			uiManager.render();
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

}
