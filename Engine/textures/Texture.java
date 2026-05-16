package textures;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import utils.MyFile;

/**
 * OpenGL üzerinde yüklü olan dokuları (texture) temsil eden sınıf.
 * Doku kimliğini (texture ID) ve boyut bilgilerini tutar.
 */
public class Texture {

	// OpenGL tarafından atanan benzersiz doku kimliği
	public final int textureId;
	// Dokunun boyutu (genellikle genişlik)
	public final int size;
	// Dokunun tipi (Örn: GL_TEXTURE_2D, GL_TEXTURE_CUBE_MAP)
	private final int type;

	/**
	 * Varsayılan olarak 2 boyutlu (2D) bir doku nesnesi oluşturur.
	 * 
	 * @param textureId OpenGL doku kimliği
	 * @param size Dokunun boyutu
	 */
	protected Texture(int textureId, int size) {
		this.textureId = textureId;
		this.size = size;
		this.type = GL11.GL_TEXTURE_2D;
	}

	/**
	 * Belirtilen tipte özel bir doku nesnesi oluşturur.
	 * 
	 * @param textureId OpenGL doku kimliği
	 * @param type Dokunun tipi
	 * @param size Dokunun boyutu
	 */
	protected Texture(int textureId, int type, int size) {
		this.textureId = textureId;
		this.size = size;
		this.type = type;
	}

	/**
	 * Dokuyu belirtilen bir doku birimine (Texture Unit) bağlar.
	 * 
	 * @param unit Bağlanacak doku birimi numarası (Örn: 0, 1, 2)
	 */
	public void bindToUnit(int unit) {
		GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
		GL11.glBindTexture(type, textureId);
	}

	/**
	 * Dokuyu ekran kartı belleğinden (VRAM) siler.
	 */
	public void delete() {
		GL11.glDeleteTextures(textureId);
	}

	/**
	 * Yeni bir doku yüklemek için Builder (Yapıcı) sınıfını döndürür.
	 * 
	 * @param textureFile Yüklenecek doku dosyası
	 * @return Doku ayarlarını yapmak için TextureBuilder nesnesi
	 */
	public static TextureBuilder newTexture(MyFile textureFile) {
		return new TextureBuilder(textureFile);
	}

	/**
	 * Birden fazla görsel dosyasından bir CubeMap (Küp Kaplaması) oluşturur.
	 * Genellikle gökyüzü (Skybox) veya çevresel yansımalar için kullanılır.
	 * 
	 * @param textureFiles Küpün 6 yüzünü temsil eden 6 adet görsel dosyası
	 * @return Oluşturulan CubeMap dokusu
	 */
	public static Texture newCubeMap(MyFile[] textureFiles) {
		int cubeMapId = TextureUtils.loadCubeMap(textureFiles);
		// TODO needs to know size! (Boyut bilgisini tutmak ileride gerekebilir)
		return new Texture(cubeMapId, GL13.GL_TEXTURE_CUBE_MAP, 0);
	}
	
	/**
	 * Belirtilen boyutta içi boş bir CubeMap oluşturur.
	 * Bu genelde dinamik yansımalar (Environment Mapping) oluşturmak için
	 * Framebuffer'lara (FBO) bağlanmak üzere kullanılır.
	 * 
	 * @param size Küp yüzeyinin boyutu
	 * @return Boş CubeMap dokusu
	 */
	public static Texture newEmptyCubeMap(int size) {
		int cubeMapId = TextureUtils.createEmptyCubeMap(size);
		return new Texture(cubeMapId, GL13.GL_TEXTURE_CUBE_MAP, size);
	}

}
