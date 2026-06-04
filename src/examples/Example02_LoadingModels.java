package examples;

import org.lwjgl.opengl.Display;

import gane.AppSettings;
import renderEngine.RenderEngine;
import scene.Scene;
import scene.GameObject;
import utils.NativeLibraryLoader;
import utils.MyFile;
import textures.Texture;
import sunRenderer.Sun;
import guiRendering.OpenglYaziCizimi;
import extra.Camera;

/**
 * Gane Engine Egitimi 02: 3D Model Yukleme ve Isiklandirma
 * 
 * Bu ornek, sahneye bir Gunes (Isik) eklemeyi ve fiziksel bir 
 * 3D objeyi (.glb veya .obj) nasil yukleyip pozisyonlandiracaginizi gosterir.
 */
public class Example02_LoadingModels {

	public static void main(String[] args) {
		NativeLibraryLoader.loadNativeLibraries();
		AppSettings.setup(1280, 720, false, "Gane Engine - 02 Loading Models", null);
		RenderEngine renderEngine = RenderEngine.init();

		Camera camera = new Camera();
		camera.setMode(Camera.CameraMode.FREE);
		camera.getPosition().set(0, 5, 20);
		Scene scene = new Scene(camera);

		// 1. Sahneye Isik (Gunes) Ekleme
		// Isik olmadan 3D modeller karanlik veya simsiyah gorunur.
		Texture sunTex = Texture.newTexture(new MyFile("res/sun.png")).create();
		Sun sun = new Sun(sunTex, 200.0f);
		scene.setSun(sun);
		
		// Gunes isiginin gelis yonunu ve gucunu ayarlayalim
		scene.setLightDirection(new org.lwjgl.util.vector.Vector3f(0.5f, -1.0f, 0.5f));
		scene.setLightBrightness(1.2f);
		scene.setAmbientLight(0.3f); // Golgede kalan yerlerin aydinlik seviyesi

		// 2. 3D Model Yukleme
		// "res/" klasoru altindaki .glb veya .obj model dosyamizi GameObject olarak yukluyoruz
		GameObject ship = new GameObject("res/DEFAULT_VEC_SHIP/fishing_boat_v.glb");
		
		// 3. Obje Ayarlari (Pozisyon, Rotasyon, Buyukluk)
		ship.getPosition().set(0, 0, 0);       // XYZ koordinatlari (Merkeze koy)
		ship.setScale(1.5f);                   // Objenin 1.5 kati buyutulmesi
		ship.getRotation().set(0, -90f, 0);    // Y ekseninde (Saga-Sola donus) 90 derece dondurme
		
		// 4. Objeyi Sahneye Dahil Etme
		scene.addEntity(ship);

		OpenglYaziCizimi uiText = new OpenglYaziCizimi();
		uiText.init();

		long lastTime = System.nanoTime();

		while (!Display.isCloseRequested()) {
			long currentTime = System.nanoTime();
			float delta = (currentTime - lastTime) / 1000000000.0f;
			lastTime = currentTime;

			camera.move();
			
			// A) Objeyi animasyonlu bir sekilde kendi etrafinda dondurme
			float currentYaw = ship.getRotation().y;
			ship.getRotation().y = currentYaw + (20f * delta); // Saniyede 20 derece donsun

			// B) Objenin is mantigini (update) calistir
			ship.update(delta);

			// C) Sahneyi Ciz
			renderEngine.renderScene(scene, delta);

			// D) UI Cizimleri
			uiText.beginUI();
			uiText.drawText("Gane Engine - Egitim #2", 20, 20, java.awt.Color.WHITE);
			uiText.drawText("Model basariyla yuklendi ve isiklandirma yapildi!", 20, 50, java.awt.Color.GREEN);
			uiText.drawText("Kamera Kontrolu: WASD", 20, 90, java.awt.Color.CYAN);
			uiText.endUI();

			renderEngine.update();
		}

		uiText.cleanup();
		scene.delete();
		renderEngine.close();
	}

}
