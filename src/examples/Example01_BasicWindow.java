package examples;

import org.lwjgl.opengl.Display;

import gane.AppSettings;
import renderEngine.RenderEngine;
import scene.Scene;
import utils.NativeLibraryLoader;
import guiRendering.OpenglYaziCizimi;
import extra.Camera;

/**
 * Gane Engine Egitimi 01: Temel Pencere ve Oyun Dongusu
 * 
 * Bu ornek, Gane Engine kullanilarak nasil bos bir oyun penceresi
 * acilacagini, kameranin nasil tanimlanacagini ve ana oyun dongusunun 
 * (Game Loop) nasil kurulacagini gosterir.
 */
public class Example01_BasicWindow {

	public static void main(String[] args) {
		// 1. Sisteme ozel LWJGL native kutuphanelerini (DLL, SO) yukle
		NativeLibraryLoader.loadNativeLibraries();

		// 2. Oyun penceresini baslat (Genislik, Yukseklik, Tam Ekran, Baslik)
		AppSettings.setup(1280, 720, false, "Gane Engine - 01 Basic Window", null);

		// 3. Render motorunu (OpenGL) baslat
		RenderEngine renderEngine = RenderEngine.init();

		// 4. Kamerayi olustur
		Camera camera = new Camera();
		camera.setMode(Camera.CameraMode.FREE); // WASD ve Fare ile serbest dolasma
		camera.getPosition().set(0, 5, 20);

		// 5. Bos bir sahne olustur
		Scene scene = new Scene(camera);

		// 6. UI (Yazi/Arayuz) sistemini baslat
		OpenglYaziCizimi uiText = new OpenglYaziCizimi();
		uiText.init();

		long lastTime = System.nanoTime();

		// --- ANA OYUN DONGUSU (Game Loop) ---
		while (!Display.isCloseRequested()) {
			long currentTime = System.nanoTime();
			float delta = (currentTime - lastTime) / 1000000000.0f; // Saniye cinsinden gecen sure
			lastTime = currentTime;

			// A) Guncellemeler (Update)
			camera.move(); // Kamerayi klavye girdilerine gore hareket ettir

			// B) Sahne Cizimi (Render)
			// Sahnedeki tum objeleri (su an bos) cizer
			renderEngine.renderScene(scene, delta);

			// C) Arayuz Cizimi (UI)
			uiText.beginUI();
			uiText.drawText("Gane Engine - Egitim #1", 20, 20, java.awt.Color.WHITE);
			uiText.drawText("Temel oyun dongusu ve bos pencere basariyla olusturuldu!", 20, 50, java.awt.Color.GREEN);
			uiText.drawText("Kamera Kontrolu: WASD", 20, 90, java.awt.Color.CYAN);
			uiText.endUI();

			// D) Ekrani Yenile
			renderEngine.update();
		}

		// Oyun kapatildiginda RAM/GPU hafizasini temizle
		uiText.cleanup();
		scene.delete();
		renderEngine.close();
	}

}
