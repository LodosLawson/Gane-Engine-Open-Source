package textures;

import utils.MyFile;

/**
 * Dokuların (Texture) OpenGL'e yüklenirken hangi özelliklere sahip olacağını
 * Builder (Yapıcı) tasarım deseni ile belirlememizi sağlayan sınıf.
 * (Mipmapping, filtreleme türleri, kenar kenetleme vb. ayarlar)
 */
public class TextureBuilder {
	
	// Dokunun kenarlarını son piksellerle mi uzatacağını belirler (Clamp to Edge)
	private boolean clampEdges = false;
	// Mipmapping (Uzaklaştıkça daha düşük çözünürlüklü doku kullanma) aktif mi?
	private boolean mipmap = false;
	// Anisotropic filtreleme aktif mi? (Açılı bakışlarda doku kalitesini arttırır)
	private boolean anisotropic = true;
	// En yakın komşu (Nearest) filtreleme mi kullanılacak? (Piksel art stili için)
	private boolean nearest = false;
	
	// Yüklenecek dokunun dosya nesnesi
	private MyFile file;
	
	/**
	 * Belirtilen dosya için bir doku yapılandırıcısı oluşturur.
	 * @param textureFile Doku dosyası
	 */
	protected TextureBuilder(MyFile textureFile){
		this.file = textureFile;
	}
	
	/**
	 * Belirlenen ayarlarla dokuyu diskten okuyup GPU'ya yükler ve Texture nesnesi olarak döndürür.
	 * @return GPU'ya yüklenmiş Texture nesnesi
	 */
	public Texture create(){
		TextureData textureData = TextureUtils.decodeTextureFile(file);
		int textureId = TextureUtils.loadTextureToOpenGL(textureData, this);
		return new Texture(textureId, textureData.getWidth());
	}
	
	/**
	 * Dokunun kenar piksellerini uzatır.
	 * Özellikler gökyüzü (Skybox) gibi dokuların birleşim yerlerindeki çizgileri önlemek için kullanılır.
	 * @return TextureBuilder nesnesinin kendisi (Zincirleme kullanım için)
	 */
	public TextureBuilder clampEdges(){
		this.clampEdges = true;
		return this;
	}
	
	/**
	 * Standart Mipmap filtrelemesini aktifleştirir, ancak Anisotropic filtrelemeyi kapatır.
	 * @return TextureBuilder nesnesi
	 */
	public TextureBuilder normalMipMap(){
		this.mipmap = true;
		this.anisotropic = false;
		return this;
	}
	
	/**
	 * Nearest (En Yakın) filtreleme modunu kullanır.
	 * Piksel art (Pixel Art) oyunlar veya 8-bit görünümler için idealdir, pikseller bulanıklaşmaz.
	 * @return TextureBuilder nesnesi
	 */
	public TextureBuilder nearestFiltering(){
		this.mipmap = false;
		this.anisotropic = false;
		this.nearest = true;
		return this;
	}
	
	/**
	 * Hem Mipmap hem de Anisotropic filtrelemeyi açar (En yüksek kalite).
	 * @return TextureBuilder nesnesi
	 */
	public TextureBuilder anisotropic(){
		this.mipmap = true;
		this.anisotropic = true;
		return this;
	}
	
	protected boolean isClampEdges() {
		return clampEdges;
	}

	protected boolean isMipmap() {
		return mipmap;
	}

	protected boolean isAnisotropic() {
		return anisotropic;
	}

	protected boolean isNearest() {
		return nearest;
	}

}
