package gane;

import org.lwjgl.opengl.Display;

import extra.Camera;
import renderEngine.RenderEngine;
import scene.Scene;

public class MainApp {

	private static Scene activeScene;
	private static boolean running = false;
	
	public static void setActiveScene(Scene scene) {
		activeScene = scene;
	}

	public static void main(String[] args) {
		running = true;

		// 1. Engine başlat (Display + renderer'lar)
		RenderEngine renderEngine = RenderEngine.init();

		// 2. Basit sahne kur: sadece kamera, beyaz arkaplan ve hiçbir obje yok.
		Camera camera = new Camera();
		activeScene = new Scene(camera, null, false);
		activeScene.setLightDirection(WorldSettings.LIGHT_DIR);

		// 3. Ana oyun döngüsü
		while (!Display.isCloseRequested() && running) {

			camera.move();

			float delta = renderEngine.getDisplayManager().getFrameTime();
			for (var entity : activeScene.getAllEntities()) {
				entity.update(delta);
			}

			renderEngine.renderScene(activeScene);
			renderEngine.update();
		}

		activeScene.delete();
		renderEngine.close();
		running = false;
	}

	public static Scene getActiveScene() {
		return activeScene;
	}
	
	public static void stop() {
		running = false;
	}

}
