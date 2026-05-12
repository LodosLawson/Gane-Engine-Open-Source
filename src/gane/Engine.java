package gane;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.lwjgl.opengl.Display;

import extra.Camera;
import renderEngine.RenderEngine;
import scene.Entity;
import scene.Scene;
import skybox.Skybox;
import textures.Texture;

public class Engine {

	private RenderEngine renderEngine;
	private Scene scene;
	private Camera camera;
	private boolean running;

	public Engine() {
		setupLwjglNativePath();
		loadNativeLibraries();
		init();
	}

	private void init() {
		renderEngine = RenderEngine.init();
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
		running = false;
	}

	public static void main(String[] args) {
		new Engine().run();
	}

	private static void setupLwjglNativePath() {
		Path projectDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
		Path nativePath = projectDir.resolve("lwjgl-2.9.3").resolve("native").resolve(getNativeFolder());

		if (!Files.isDirectory(nativePath)) {
			nativePath = projectDir.resolve("lib").resolve("lwjgl-2.9.3").resolve("native").resolve(getNativeFolder());
		}

		if (!Files.isDirectory(nativePath)) {
			try {
				nativePath = Files.walk(projectDir)
					.filter(path -> path.endsWith(getNativeFolder()) && Files.isDirectory(path))
					.filter(path -> Files.exists(path.resolve("lwjgl64.dll")))
					.findFirst()
					.orElse(nativePath);
			} catch (Exception e) {
				// ignore search failure, keep original path
			}
		}

		if (Files.isDirectory(nativePath)) {
			String path = nativePath.toAbsolutePath().toString();
			System.setProperty("org.lwjgl.librarypath", path);
			System.setProperty("java.library.path", path);
			resetLibraryPath();
			System.out.println("LWJGL native path set to: " + path);
		} else {
			System.err.println("LWJGL native folder not found: " + nativePath);
		}
	}

	private static String getNativeFolder() {
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("win")) {
			return "windows";
		}
		if (os.contains("mac")) {
			return "macosx";
		}
		if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
			return "linux";
		}
		if (os.contains("sunos") || os.contains("solaris")) {
			return "solaris";
		}
		return "windows";
	}

	private static void resetLibraryPath() {
		try {
			Field field = ClassLoader.class.getDeclaredField("sys_paths");
			field.setAccessible(true);
			field.set(null, null);
		} catch (NoSuchFieldException ignored) {
			// Java 21+ may not expose sys_paths; property setting is sufficient.
		} catch (ReflectiveOperationException e) {
			e.printStackTrace();
		}
	}

	private static void loadNativeLibraries() {
		Path projectDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
		Path nativePath = projectDir.resolve("lwjgl-2.9.3").resolve("native").resolve(getNativeFolder());

		if (!Files.isDirectory(nativePath)) {
			nativePath = projectDir.resolve("lib").resolve("lwjgl-2.9.3").resolve("native").resolve(getNativeFolder());
		}

		if (!Files.isDirectory(nativePath)) {
			try {
				nativePath = Files.walk(projectDir)
					.filter(path -> path.endsWith(getNativeFolder()) && Files.isDirectory(path))
					.filter(path -> Files.exists(path.resolve("lwjgl64.dll")))
					.findFirst()
					.orElse(nativePath);
			} catch (Exception e) {
				// ignore search failure, keep original path
			}
		}

		if (Files.isDirectory(nativePath)) {
			// Load all required DLLs
			String[] dlls = {"lwjgl64.dll", "OpenAL64.dll", "jinput-raw_64.dll", "jinput-dx8_64.dll"};
			for (String dll : dlls) {
				Path dllPath = nativePath.resolve(dll);
				if (Files.exists(dllPath)) {
					try {
						System.load(dllPath.toString());
						System.out.println("Loaded native library: " + dll);
					} catch (UnsatisfiedLinkError e) {
						System.err.println("Failed to load " + dll + ": " + e.getMessage());
					}
				} else {
					System.out.println("DLL not found: " + dllPath);
				}
			}
		} else {
			System.err.println("Native library folder not found: " + nativePath);
		}
	}
}
