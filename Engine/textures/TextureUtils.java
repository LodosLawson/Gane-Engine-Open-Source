package textures;

import java.io.InputStream;
import java.nio.ByteBuffer;

import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

import de.matthiasmann.twl.utils.PNGDecoder;
import de.matthiasmann.twl.utils.PNGDecoder.Format;
import utils.MyFile;

/**
 * OpenGL'e doku (texture) ve CubeMap (Küp Kaplama) yüklemek için 
 * kullanılan düşük seviyeli (low-level) yardımcı metotları barındırır.
 */
public class TextureUtils {

	/**
	 * İçi boş (verisiz) bir CubeMap oluşturur. Bu genellikle çevre yansımalarını (Environment Map)
	 * dinamik olarak çizmek için (FBO ile) kullanılır.
	 * 
	 * @param size CubeMap yüzeylerinin genişliği/yüksekliği
	 * @return Oluşturulan CubeMap'in OpenGL kimliği
	 */
	public static int createEmptyCubeMap(int size) {
		int texID = GL11.glGenTextures();
		GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, texID);
		// Küpün 6 yüzü için de boş (null) bellek alanı tahsis et
		for (int i = 0; i < 6; i++) {
			GL11.glTexImage2D(GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL11.GL_RGBA8, size, size, 0, GL11.GL_RGBA,
					GL11.GL_UNSIGNED_BYTE, (ByteBuffer)null);
		}
		// Filtreleme ve kenar (wrap) ayarları
		GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
		// Yüzeylerin birleşim çizgilerini gizlemek için Clamp_to_Edge kullanıyoruz
		GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL12.GL_TEXTURE_WRAP_R, GL12.GL_CLAMP_TO_EDGE);
		GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, 0);
		return texID;
	}

	/**
	 * Diskten 6 adet doku dosyasını okuyarak bir CubeMap (Gökyüzü veya Yansıma için) yükler.
	 * 
	 * @param textureFiles Küpün 6 yüzünü (Sağ, Sol, Üst, Alt, Arka, Ön) temsil eden dosyalar
	 * @return Oluşturulan CubeMap'in OpenGL kimliği
	 */
	public static int loadCubeMap(MyFile[] textureFiles) {
		int texID = GL11.glGenTextures();
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, texID);
		GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
		
		// Her bir dosyayı decode et (çöz) ve CubeMap yüzeylerine sırayla ekle
		for (int i = 0; i < textureFiles.length; i++) {
			TextureData data = decodeTextureFile(textureFiles[i]);
			GL11.glTexImage2D(GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL11.GL_RGBA, data.getWidth(),
					data.getHeight(), 0, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, data.getBuffer());
		}
		// Temel filtreleme ve kenar ayarları
		GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
		GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, 0);
		return texID;
	}

	/**
	 * Bir PNG dosyasını TWL PNGDecoder kütüphanesini kullanarak okur ve piksel verilerine çevirir.
	 * 
	 * @param file PNG dosyası
	 * @return Genişlik, Yükseklik ve ByteBuffer içeren TextureData nesnesi
	 */
	protected static TextureData decodeTextureFile(MyFile file) {
		int width = 0;
		int height = 0;
		ByteBuffer buffer = null;
		try {
			InputStream in = file.getInputStream();
			if (in == null) {
				throw new IllegalStateException("Resource not found: " + file.getPath());
			}
			try {
				PNGDecoder decoder = new PNGDecoder(in);
				width = decoder.getWidth();
				height = decoder.getHeight();
				buffer = ByteBuffer.allocateDirect(4 * width * height);
				decoder.decode(buffer, width * 4, Format.BGRA);
				buffer.flip();
				in.close();
			} catch (Exception pngEx) {
				// PNGDecoder başarısız olursa (örn: 16-bit, grayscale veya farklı bir format ise),
				// Java'nın yerleşik ImageIO kütüphanesine geri dön (Fallback).
				in.close(); // Eski stream'i kapat
				
				// Yeni bir stream aç
				InputStream in2 = file.getInputStream();
				java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(in2);
				in2.close();
				
				if (image == null) {
					throw pngEx; // Eğer ImageIO da okuyamazsa orijinal hatayı fırlat
				}
				
				width = image.getWidth();
				height = image.getHeight();
				int[] pixels = new int[width * height];
				image.getRGB(0, 0, width, height, pixels, 0, width);
				
				buffer = org.lwjgl.BufferUtils.createByteBuffer(width * height * 4);
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						int pixel = pixels[y * width + x];
						buffer.put((byte) (pixel & 0xFF));         // Blue
						buffer.put((byte) ((pixel >> 8) & 0xFF));  // Green
						buffer.put((byte) ((pixel >> 16) & 0xFF)); // Red
						buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
					}
				}
				buffer.flip();
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Tried to load texture " + file.getName() + " , didn't work");
			System.exit(-1);
		}
		return new TextureData(buffer, width, height);
	}

	/**
	 * Önceden çözülmüş ham doku verisini (TextureData) OpenGL'e yükler
	 * ve TextureBuilder ayarlarını (Mipmap, Anisotropic vs.) uygular.
	 * 
	 * @param data Çözülmüş ham doku verisi
	 * @param builder Ayarları barındıran yapılandırıcı nesne
	 * @return GPU üzerindeki OpenGL doku kimliği (texID)
	 */
	protected static int loadTextureToOpenGL(TextureData data, TextureBuilder builder) {
		int texID = GL11.glGenTextures();
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texID);
		GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
		
		// Veriyi GPU'ya gönder
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, data.getWidth(), data.getHeight(), 0, GL12.GL_BGRA,
				GL11.GL_UNSIGNED_BYTE, data.getBuffer());
				
		// Builder içindeki ayarlara göre OpenGL yapılandırması
		if (builder.isMipmap()) {
			GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D); // Otomatik mipmap üret
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
			// Ekran kartı Anisotropic filtrelemeyi destekliyor mu kontrol et
			if (builder.isAnisotropic() && GLContext.getCapabilities().GL_EXT_texture_filter_anisotropic) {
				GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, 0);
				// Maksimum anisotropik kaliteyi (Örn: 4x) belirle
				GL11.glTexParameterf(GL11.GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT,
						4.0f);
			}
		} else if (builder.isNearest()) {
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		} else {
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		}
		
		if (builder.isClampEdges()) {
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
		} else {
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
		}
		
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		return texID;
	}

}
