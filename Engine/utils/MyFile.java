package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Projedeki dosyaları esnek ve taşınabilir bir şekilde okumak için oluşturulmuş yardımcı sınıf.
 * Önce Java projesinin derlenmiş kaynaklarına (Classpath/ResourceAsStream), ardından fiziksel 
 * dosya sistemine (user.dir) bakar. Bu sayede oyun hem JAR olarak paketlendiğinde hem de 
 * IDE içerisinde çalışırken dosya yollarında sorun yaşamaz.
 * 
 * <p><b>Kullanım Örneği:</b></p>
 * <pre>
 * {@code
 * // "res/textures/player.png" dosyasını okumak için
 * MyFile textureFile = new MyFile("res", "textures", "player.png");
 * 
 * // Veya tek bir string ile
 * MyFile modelFile = new MyFile("res/models/tree.obj");
 * 
 * // Dosyayı okumak için InputStream al
 * InputStream in = textureFile.getInputStream();
 * }
 * </pre>
 */
public class MyFile {
	
	/** Dosya yol ayracı (Her platformda uyumlu olması ve JAR içinde çalışması için "/" kullanılmıştır) */
	private static final String FILE_SEPARATOR = "/";

	/** Dosyanın projedeki veya sistemdeki tam yolu */
	private String path;
	
	/** Dosyanın sadece kendi ismi ve uzantısı (Örn: "model.obj") */
	private String name;

	/**
	 * Tek bir dosya yolundan yeni bir MyFile nesnesi oluşturur.
	 * 
	 * @param path Okunacak dosya yolu (Örn: "res/shaders/vertexShader.txt")
	 */
	public MyFile(String path) {
		this.path = FILE_SEPARATOR + path;
		String[] dirs = path.split(FILE_SEPARATOR);
		this.name = dirs[dirs.length - 1];
	}

	/**
	 * Klasör ve dosya isimlerini parça parça alarak birleştirir ve tek bir dosya yolu oluşturur.
	 * 
	 * @param paths Klasör ve dosya isimleri dizisi (Örn: "res", "textures", "diffuse.png")
	 */
	public MyFile(String... paths) {
		this.path = "";
		for (String part : paths) {
			this.path += (FILE_SEPARATOR + part);
		}
		String[] dirs = path.split(FILE_SEPARATOR);
		this.name = dirs[dirs.length - 1];
	}

	/**
	 * Mevcut bir dizin (MyFile) altındaki bir dosyayı temsil edecek yeni bir MyFile oluşturur.
	 * 
	 * @param file İçinde arama yapılacak üst klasör dizini
	 * @param subFile Klasörün içindeki dosyanın ismi
	 */
	public MyFile(MyFile file, String subFile) {
		this.path = file.path + FILE_SEPARATOR + subFile;
		this.name = subFile;
	}
	
	/**
	 * Mevcut bir dizin içindeki derinlemesine alt klasör/dosya hiyerarşisini MyFile olarak oluşturur.
	 * 
	 * @param file Üst klasör dizini (MyFile)
	 * @param subFiles Alt dosya yolları dizisi
	 */
	public MyFile(MyFile file, String... subFiles) {
		this.path = file.path;
		for (String part : subFiles) {
			this.path += (FILE_SEPARATOR + part);
		}
		String[] dirs = path.split(FILE_SEPARATOR);
		this.name = dirs[dirs.length - 1];
	}

	/** 
	 * Dosyanın tam yolunu döndürür.
	 * 
	 * @return Dosya yolu stringi.
	 */
	public String getPath() {
		return path;
	}
	
	@Override
	public String toString(){
		return getPath();
	}

	/**
	 * Dosyanın içeriğini bayt (byte) olarak okumak için bir InputStream döndürür.
	 * <p>Önce projenin classpath'inde (Resource) arar. Bu işlem JAR dosyalarından okuma yaparken çok önemlidir.
	 * Bulamazsa işletim sisteminin fiziksel dosya yollarına (user.dir) bakar.</p>
	 * 
	 * @return Dosya veri akışı (InputStream) veya dosya bulunamazsa null
	 */
	public InputStream getInputStream() {
		String cpPath = path.startsWith(FILE_SEPARATOR) ? path.replace(FILE_SEPARATOR, "/") : "/" + path.replace(FILE_SEPARATOR, "/");
		InputStream in = MyFile.class.getResourceAsStream(cpPath);
		if (in != null) {
			// Classpath'te bulunduysa döndür (JAR içi okumalar için)
			return in;
		}
		// Classpath'te yoksa, fiziksel çalışma dizininde (IDE veya kullanıcı dizini) ara
		String relativePath = path.startsWith(FILE_SEPARATOR) ? path.substring(1) : path;
		Path fsPath = Paths.get(System.getProperty("user.dir"), relativePath.replace(FILE_SEPARATOR, System.getProperty("file.separator")));
		if (Files.exists(fsPath)) {
			try {
				return Files.newInputStream(fsPath);
			} catch (IOException e) {
				// Hata durumunda alttaki null döndürme satırına geç (Fall through)
			}
		}
		return null;
	}

	/**
	 * Dosyayı metin (text) olarak satır satır okumak için bir BufferedReader döndürür.
	 * Genellikle .obj dosyaları veya .txt shader kodlarını okumak için kullanılır.
	 * 
	 * @return Metin okuyucu (BufferedReader) nesnesi
	 * @throws Exception Dosya fiziksel olarak yoksa veya okuma izni alınamazsa fırlatılır
	 */
	public BufferedReader getReader() throws Exception {
		try {
			InputStreamReader isr = new InputStreamReader(getInputStream());
			BufferedReader reader = new BufferedReader(isr);
			return reader;
		} catch (Exception e) {
			System.err.println(path + " dosyasi okunamadi (Reader olusturulamadi).");
			throw e;
		}
	}

	/** 
	 * Dosyanın sadece ismini ve uzantısını döndürür.
	 * 
	 * @return Dosya ismi (Örn: "image.png")
	 */
	public String getName() {
		return name;
	}

}
