package gane;

import org.lwjgl.opengl.Display;
import utils.NativeLibraryLoader;

import extra.Camera;
import renderEngine.RenderEngine;
import scene.Entity;
import scene.Scene;
import physics.PhysicsEngine;
import skybox.Skybox;
import steam.SteamManager;
import textures.Texture;

public class Engine {

	private RenderEngine renderEngine;
	private Scene scene;
	private Camera camera;
	private PhysicsEngine physicsEngine;
	private boolean running;

	public Engine() {
		NativeLibraryLoader.loadNativeLibraries();
		SteamManager.init(); // Steam API başlatılıyor
		init();
	}

	private void init() {
		renderEngine = RenderEngine.init();
		physicsEngine = new PhysicsEngine();
		camera = new Camera();

		Texture emptyCubeMap = Texture.newEmptyCubeMap(256);
		Skybox skybox = new Skybox(emptyCubeMap, 100f);

		scene = new Scene(camera, skybox);
		scene.setLightDirection(WorldSettings.LIGHT_DIR);
	}

	public void run() {
		running = true;
		while (!Display.isCloseRequested() && running) {
			camera.move();
			float delta = renderEngine.getDisplayManager().getFrameTime();
			
			// Fiziği güncelle
			physicsEngine.update(scene, delta);
			
			// Steam arka plan callback'lerini işle
			SteamManager.update();
			
			for (Entity entity : scene.getAllEntities()) {
				entity.update(delta);
			}
			renderEngine.renderScene(scene);
			renderEngine.update();
		}
		cleanup();
	}

	public void stop() {
		running = false;
	}

	public void cleanup() {
		scene.delete();
		renderEngine.close();
		SteamManager.shutdown(); // Kapanışta Steam'i kapat
		running = false;
	}

	public static void main(String[] args) {
		new Engine().run();
	}
}
