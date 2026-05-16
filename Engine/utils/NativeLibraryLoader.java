package utils;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Oyun motorunun ihtiyaç duyduğu yerel (Native) kütüphaneleri (.dll, .so, .dylib)
 * otomatik olarak bulan ve sisteme yükleyen yardımcı sınıf.
 */
public class NativeLibraryLoader {

	/**
	 * Proje dizinlerindeki yerel kütüphaneleri (LWJGL, OpenAL vb.) arar ve yükler.
	 * Oyun başlatılmadan önce ilk çağrılması gereken metottur.
	 */
	public static void loadNativeLibraries() {
		Path projectDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
		Path nativePath = projectDir.resolve("lib").resolve("natives");

		if (!Files.isDirectory(nativePath)) {
			nativePath = projectDir.resolve("lwjgl-2.9.3").resolve("native").resolve(getNativeFolder());
		}

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
				// Arama hatasını yoksay, orijinal yolu tut
			}
		}

		if (Files.isDirectory(nativePath)) {
			String path = nativePath.toAbsolutePath().toString();
			System.setProperty("org.lwjgl.librarypath", path);
			System.setProperty("java.library.path", path);
			resetLibraryPath();
			
			// Kütüphaneleri belleğe yükle
			String[] dlls = {"lwjgl64.dll", "OpenAL64.dll", "jinput-raw_64.dll", "jinput-dx8_64.dll"};
			for (String dll : dlls) {
				Path dllPath = Paths.get(path, dll);
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
			System.out.println("✓ Native library path set to: " + path);
		} else {
			System.err.println("LWJGL native folder not found: " + nativePath);
		}
	}

	/**
	 * İşletim sistemine göre native kütüphane klasörünün adını döndürür.
	 * @return "windows", "macosx", "linux" veya "solaris"
	 */
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

	/**
	 * JVM'nin dahili sys_paths önbelleğini sıfırlar; böylece yeni
	 * java.library.path değeri anında geçerli olur.
	 * Not: Java 21+'da bu alan gizlenmiş olabilir; bu durumda hata sessizce geçilir.
	 */
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
}
