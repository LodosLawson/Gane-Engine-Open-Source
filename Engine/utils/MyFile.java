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
 * Önce Java projesinin kaynaklarına (ResourceAsStream), ardından fiziksel dosya sistemine (user.dir) bakar.
 */
public class MyFile {
	
	// Dosya yol ayracı (her platformda uyumlu olması için "/" kullanılmış)
	private static final String FILE_SEPARATOR = "/";

	// Tam dosya yolu
	private String path;
	// Dosyanın sonundaki isim
	private String name;

	/**
	 * Dosya yolundan yeni bir MyFile oluşturur.
	 * @param path Okunacak dosya yolu
	 */
	public MyFile(String path) {
		this.path = FILE_SEPARATOR + path;
		String[] dirs = path.split(FILE_SEPARATOR);
		this.name = dirs[dirs.length - 1];
	}

	/**
	 * Dosya yolunu parçalar halinde alarak birleştirir ve MyFile oluşturur.
	 * @param paths Klasör ve dosya isimleri dizisi
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
	 * Mevcut bir dizin içindeki bir dosyayı temsil edecek MyFile oluşturur.
	 * @param file Dizin (MyFile)
	 * @param subFile Dosya ismi
	 */
	public MyFile(MyFile file, String subFile) {
		this.path = file.path + FILE_SEPARATOR + subFile;
		this.name = subFile;
	}
	
	/**
	 * Mevcut bir dizin içindeki alt klasör/dosya hiyerarşisini MyFile olarak oluşturur.
	 * @param file Dizin (MyFile)
	 * @param subFiles Alt dosya yolları
	 */
	public MyFile(MyFile file, String... subFiles) {
		this.path = file.path;
		for (String part : subFiles) {
			this.path += (FILE_SEPARATOR + part);
		}
		String[] dirs = path.split(FILE_SEPARATOR);
		this.name = dirs[dirs.length - 1];
	}

	/** @return Dosyanın tam yolu */
	public String getPath() {
		return path;
	}
	
	@Override
	public String toString(){
		return getPath();
	}

	/**
	 * Dosyanın içeriğini okumak için bir InputStream döndürür.
	 * Önce projenin classpath'inde (Resource) arar, bulamazsa işletim sisteminin dosya yollarına bakar.
	 * 
	 * @return Dosya veri akışı (InputStream)
	 */
	public InputStream getInputStream() {
		InputStream in = MyFile.class.getResourceAsStream(path);
		if (in != null) {
			// Classpath'te bulunduysa döndür
			return in;
		}
		// Classpath'te yoksa, fiziksel çalışma dizininde ara
		String relativePath = path.startsWith(FILE_SEPARATOR) ? path.substring(1) : path;
		Path fsPath = Paths.get(System.getProperty("user.dir"), relativePath.replace(FILE_SEPARATOR, System.getProperty("file.separator")));
		if (Files.exists(fsPath)) {
			try {
				return Files.newInputStream(fsPath);
			} catch (IOException e) {
				// fall through to return null (Hata durumunda alttaki null döndürmeye geç)
			}
		}
		return null;
	}

	/**
	 * Dosyayı metin (text) olarak satır satır okumak için bir BufferedReader döndürür.
	 * 
	 * @return BufferedReader nesnesi
	 * @throws Exception Dosya okunamıyorsa hata fırlatır
	 */
	public BufferedReader getReader() throws Exception {
		try {
			InputStreamReader isr = new InputStreamReader(getInputStream());
			BufferedReader reader = new BufferedReader(isr);
			return reader;
		} catch (Exception e) {
			System.err.println("Couldn't get reader for " + path);
			throw e;
		}
	}

	/** @return Dosyanın sadece ismi ve uzantısı (Örn: "image.png") */
	public String getName() {
		return name;
	}

}
