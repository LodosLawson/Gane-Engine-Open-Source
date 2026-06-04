package tools;

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

import renderEngine.RenderEngine;
import scene.GameObject;
import scene.Scene;
import gane.AppSettings;
import utils.NativeLibraryLoader;
import guiRendering.OpenglYaziCizimi;

public class ObjectViewerApp {

	private static volatile String newObjPathToLoad = null;

	private static void openFileChooser() {
		SwingUtilities.invokeLater(() -> {
			JFileChooser fileChooser = new JFileChooser(System.getProperty("user.dir"));
			fileChooser.setDialogTitle("Select a 3D Model (.glb, .obj)");
			fileChooser.setFileFilter(new FileNameExtensionFilter("3D Models", "glb", "obj"));
			int userSelection = fileChooser.showOpenDialog(null);
			if (userSelection == JFileChooser.APPROVE_OPTION) {
				File fileToLoad = fileChooser.getSelectedFile();
				String absPath = fileToLoad.getAbsolutePath();
				String projectDir = System.getProperty("user.dir");
				
				if (absPath.startsWith(projectDir)) {
					String relativePath = absPath.substring(projectDir.length());
					if (relativePath.startsWith("\\") || relativePath.startsWith("/")) {
						relativePath = relativePath.substring(1);
					}
					newObjPathToLoad = relativePath.replace('\\', '/');
				} else {
					System.err.println("HATA: Lutfen proje klasoru ('" + projectDir + "') icinden bir dosya secin!");
				}
			}
		});
	}

	public static void main(String[] args) {
		// Native LWJGL kutuphanelerini yukle
		NativeLibraryLoader.loadNativeLibraries();

		// Oyun pencere ayarlarini baslat
		AppSettings.setup(1280, 720, false, "Gane Engine - 3D Object Viewer", null);

		// Render motorunu baslat
		RenderEngine renderEngine = RenderEngine.init();

		// Sahne ve Kamera ayarlari
		extra.Camera camera = new extra.Camera();
		camera.setMode(extra.Camera.CameraMode.FREE);
		camera.getPosition().set(0, 5, 20);
		Scene scene = new Scene(camera);

		// Gunes (Isik)
		textures.Texture sunTex = textures.Texture.newTexture(new utils.MyFile("res/sun.png")).create();
		sunRenderer.Sun sun = new sunRenderer.Sun(sunTex, 200.0f);
		scene.setSun(sun);

		// UI Sistemi
		OpenglYaziCizimi uiText = new OpenglYaziCizimi();
		uiText.init();

		// Yuklenecek Obje (Path'i buradan degistirebilirsiniz)
		String objPath = "res/DEFAULT_VEC_SHIP/fishing_boat_v.glb";
		GameObject testObj = new GameObject(objPath);
		scene.addEntity(testObj);

		// Obje Durum Degiskenleri
		float objScale = 1.0f;
		float pitch = 0f;
		float yaw = 0f;
		float roll = 0f;

		boolean enterPressed = false;
		boolean lKeyPressed = false;

		long lastTime = System.nanoTime();

		System.out.println("--- Gane Engine Object Viewer Baslatildi ---");
		System.out.println("Kamera Kontrolu: W, A, S, D, SPACE, SHIFT ve Mouse");

		while (!Display.isCloseRequested()) {
			long currentTime = System.nanoTime();
			float delta = (currentTime - lastTime) / 1000000000.0f;
			lastTime = currentTime;

			camera.move();

			// -- Obje Kontrolleri --

			// YENI OBJE YUKLE (L Tusu)
			if (Keyboard.isKeyDown(Keyboard.KEY_L)) {
				if (!lKeyPressed) {
					openFileChooser();
					lKeyPressed = true;
				}
			} else {
				lKeyPressed = false;
			}

			if (newObjPathToLoad != null) {
				scene.removeEntity(testObj);
				testObj.delete();
				
				objPath = newObjPathToLoad;
				testObj = new GameObject(objPath);
				scene.addEntity(testObj);
				
				objScale = 1.0f;
				pitch = 0f;
				yaw = 0f;
				roll = 0f;
				
				newObjPathToLoad = null;
			}

			// SCALE
			if (Keyboard.isKeyDown(Keyboard.KEY_ADD)) {
				objScale += 0.5f * delta;
			}
			if (Keyboard.isKeyDown(Keyboard.KEY_SUBTRACT)) {
				objScale = Math.max(0.01f, objScale - 0.5f * delta);
			}

			// PITCH (Yukari / Asagi Oklar)
			if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
				pitch -= 50f * delta;
			}
			if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
				pitch += 50f * delta;
			}

			// YAW (Sag / Sol Oklar)
			if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
				yaw -= 50f * delta;
			}
			if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
				yaw += 50f * delta;
			}

			// ROLL (U ve O tuslari)
			if (Keyboard.isKeyDown(Keyboard.KEY_U)) {
				roll -= 50f * delta;
			}
			if (Keyboard.isKeyDown(Keyboard.KEY_O)) {
				roll += 50f * delta;
			}

			// Degerleri objeye uygula
			testObj.setScale(objScale);
			testObj.getRotation().set(pitch, yaw, roll);

			// -- EXPORT (Enter Tusu) --
			if (Keyboard.isKeyDown(Keyboard.KEY_RETURN)) {
				if (!enterPressed) {
					System.out.println("\n========== EXPORTED OBJECT CODE ==========");
					System.out.println("scene.GameObject obj = new scene.GameObject(\"" + objPath.replace("\\", "\\\\") + "\");");
					System.out.println("obj.getPosition().set(0, 0, 0);");
					System.out.println("obj.getRotation().set(" + String.format("%.2ff, %.2ff, %.2ff", pitch, yaw, roll).replace(",", ".") + ");");
					System.out.println("obj.setScale(" + String.format("%.2ff", objScale).replace(",", ".") + ");");
					System.out.println("scene.addEntity(obj);");
					System.out.println("==========================================\n");
					enterPressed = true;
				}
			} else {
				enterPressed = false;
			}

			// Guncelleme ve Render
			testObj.update(delta);
			renderEngine.renderScene(scene, delta);
			
			// XYZ Eksenleri ve Terrain 0 noktasini (Grid) Ciz
			drawDebugGrid(camera);

			// UI Cizimi
			uiText.beginUI();
			uiText.drawText("Gane Engine - 3D Object Viewer", 20, 20, java.awt.Color.WHITE);
			uiText.drawText("Test Objesi: " + objPath, 20, 50, java.awt.Color.LIGHT_GRAY);
			uiText.drawText("Scale [Numpad +/-]: " + String.format("%.2f", objScale), 20, 90, java.awt.Color.YELLOW);
			uiText.drawText("Pitch (X Eksen) [Yukari/Asagi]: " + String.format("%.1f", pitch), 20, 120, java.awt.Color.CYAN);
			uiText.drawText("Yaw (Y Eksen) [Sag/Sol]: " + String.format("%.1f", yaw), 20, 150, java.awt.Color.GREEN);
			uiText.drawText("Roll (Z Eksen) [U/O]: " + String.format("%.1f", roll), 20, 180, java.awt.Color.MAGENTA);
			uiText.drawText("EXPORT KODU AL (Konsola yazdir) -> [ENTER]", 20, 220, java.awt.Color.RED);
			uiText.drawText("YENI OBJE YUKLE (Dosya Secici) -> [L]", 20, 260, java.awt.Color.ORANGE);
			uiText.endUI();

			renderEngine.update();
		}

		// Temizlik
		uiText.cleanup();
		scene.delete();
		renderEngine.close();
	}

	private static void drawDebugGrid(extra.Camera camera) {
		org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_LIGHTING);
		org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_TEXTURE_2D);
		org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
		org.lwjgl.opengl.GL11.glLineWidth(2.0f);

		org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_PROJECTION);
		org.lwjgl.opengl.GL11.glPushMatrix();
		java.nio.FloatBuffer projBuffer = org.lwjgl.BufferUtils.createFloatBuffer(16);
		camera.getProjectionViewMatrix().store(projBuffer);
		projBuffer.flip();
		org.lwjgl.opengl.GL11.glLoadMatrix(projBuffer);

		org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_MODELVIEW);
		org.lwjgl.opengl.GL11.glPushMatrix();
		org.lwjgl.opengl.GL11.glLoadIdentity();

		org.lwjgl.opengl.GL11.glBegin(org.lwjgl.opengl.GL11.GL_LINES);

		// Grid (Y=0 Terrain zeminini gosteren cizgili alan)
		org.lwjgl.opengl.GL11.glColor3f(0.2f, 0.2f, 0.2f);
		int gridSize = 50;
		for (int i = -gridSize; i <= gridSize; i += 2) {
			// Yatay cizgiler
			org.lwjgl.opengl.GL11.glVertex3f((float) i, 0, (float) -gridSize);
			org.lwjgl.opengl.GL11.glVertex3f((float) i, 0, (float) gridSize);
			// Dikey cizgiler
			org.lwjgl.opengl.GL11.glVertex3f((float) -gridSize, 0, (float) i);
			org.lwjgl.opengl.GL11.glVertex3f((float) gridSize, 0, (float) i);
		}

		// X Eksen (Kirmizi) - Saga Dogru
		org.lwjgl.opengl.GL11.glColor3f(1.0f, 0.0f, 0.0f);
		org.lwjgl.opengl.GL11.glVertex3f(0, 0.01f, 0);
		org.lwjgl.opengl.GL11.glVertex3f(20, 0.01f, 0);

		// Y Eksen (Yesil) - Yukari Dogru
		org.lwjgl.opengl.GL11.glColor3f(0.0f, 1.0f, 0.0f);
		org.lwjgl.opengl.GL11.glVertex3f(0, 0.01f, 0);
		org.lwjgl.opengl.GL11.glVertex3f(0, 20.01f, 0);

		// Z Eksen (Mavi) - Ileri Dogru
		org.lwjgl.opengl.GL11.glColor3f(0.0f, 0.5f, 1.0f);
		org.lwjgl.opengl.GL11.glVertex3f(0, 0.01f, 0);
		org.lwjgl.opengl.GL11.glVertex3f(0, 0.01f, 20);

		org.lwjgl.opengl.GL11.glEnd();

		org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_PROJECTION);
		org.lwjgl.opengl.GL11.glPopMatrix();
		org.lwjgl.opengl.GL11.glMatrixMode(org.lwjgl.opengl.GL11.GL_MODELVIEW);
		org.lwjgl.opengl.GL11.glPopMatrix();

		org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_TEXTURE_2D);
		org.lwjgl.opengl.GL11.glLineWidth(1.0f);
	}

}
