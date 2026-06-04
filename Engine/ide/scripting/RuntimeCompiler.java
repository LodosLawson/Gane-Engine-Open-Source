package ide.scripting;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import scene.Component;

public class RuntimeCompiler {

	public static Component compileAndLoad(String className, String sourceCode) throws Exception {
		// 1. Dosya yollarini hazirla
		File sourceDir = new File(System.getProperty("user.dir"), "src/scripts");
		if (!sourceDir.exists()) {
			sourceDir.mkdirs();
		}
		
		File sourceFile = new File(sourceDir, className + ".java");
		
		// 2. Kodu dosyaya yaz
		try (FileWriter writer = new FileWriter(sourceFile)) {
			writer.write(sourceCode);
		}
		
		// 3. Compiler'i al
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null) {
			throw new RuntimeException("JavaCompiler bulunamadi! Uygulamaniz JDK ile calistirilmalidir, sadece JRE yeterli degildir.");
		}
		
		// 4. Classpath'i ayarla (Projeyi olusturan diger siniflari gorebilmesi icin)
		String classpath = System.getProperty("java.class.path");
		// Bin klasorunu de ekleyelim
		String binPath = new File(System.getProperty("user.dir"), "bin").getAbsolutePath();
		classpath += File.pathSeparator + binPath;
		
		Iterable<String> options = Arrays.asList("-classpath", classpath, "-d", binPath);
		
		// 5. Derle
		int result = compiler.run(null, null, null, "-classpath", classpath, "-d", binPath, sourceFile.getAbsolutePath());
		
		if (result != 0) {
			throw new RuntimeException("Derleme hatasi olustu! Konsolu kontrol edin.");
		}
		
		// 6. Sinifi yukle ve ornegini (instance) olustur
		// URLClassLoader kullanarak 'bin' klasorunden sinifi yukluyoruz
		URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{new File(binPath).toURI().toURL()});
		
		// Eger paket tanimlandiysa (ornegin scripts), paketi de className'e dahil etmeliyiz.
		// Biz simdilik kolaylik olmasi adina kodun icinde "package scripts;" varsa adini ona gore ayarlayacagiz.
		String fullClassName = className;
		if (sourceCode.contains("package scripts;")) {
			fullClassName = "scripts." + className;
		}
		
		Class<?> loadedClass = Class.forName(fullClassName, true, classLoader);
		
		// 7. Eger bu bir Component ise dondur
		if (Component.class.isAssignableFrom(loadedClass)) {
			return (Component) loadedClass.getDeclaredConstructor().newInstance();
		} else {
			throw new RuntimeException("Yazdiginiz sinif scene.Component sinifindan miras (extends) almalidir!");
		}
	}
}
