package sunRenderer;

import org.lwjgl.util.vector.Vector3f;

import textures.Texture;

/**
 * Oyundaki Güneş nesnesini temsil eden sınıf.
 * Güneşin kaplamasını (texture), boyutunu, ışık yönünü ve dünya üzerindeki pozisyonunu tutar.
 */
public class Sun {

	// Güneşin kameraya olan varsayılan uzaklığı. Gökyüzü kutusunun (skybox) arkasında kalmamasına dikkat edilmelidir.
	private static final float SUN_DIS = 50;

	// Güneşin görünümünü belirleyen kaplama (texture)
	private final Texture texture;

	// Işığın geliş yönü (Varsayılan olarak yukarıdan aşağıya doğru)
	private Vector3f lightDirection = new Vector3f(0, -1, 0);
	// Güneşin ekrandaki boyutu
	private float scale;

	/**
	 * Yeni bir Güneş objesi oluşturur.
	 * 
	 * @param texture Güneşin 2B kaplaması
	 * @param scale Güneşin boyutu
	 */
	public Sun(Texture texture, float scale) {
		this.texture = texture;
		this.scale = scale;
	}

	/**
	 * Güneşin boyutunu günceller.
	 * @param scale Yeni boyut değeri
	 */
	public void setScale(float scale) {
		this.scale = scale;
	}

	/**
	 * Güneş ışığının yönünü belirler. Bu yön, aynı zamanda Güneşin
	 * gökyüzündeki konumunu da belirler.
	 * 
	 * @param dir Yeni ışık yönü vektörü
	 */
	public void setDirection(Vector3f dir) {
		lightDirection.set(dir);
		// Yön vektörü normalize edilerek uzunluğu 1 birim yapılır
		lightDirection.normalise();
	}

	/** @return Güneşin kaplaması (Texture) */
	public Texture getTexture() {
		return texture;
	}

	/** @return Güneş ışığının yön vektörü */
	public Vector3f getLightDirection() {
		return lightDirection;
	}

	/** @return Güneşin mevcut boyutu */
	public float getScale() {
		return scale;
	}

	/**
	 * Işığın yönüne göre Güneşin 3 boyutlu dünya (world) pozisyonunu hesaplar.
	 * Güneşin kameraya olan uzaklığı sabit (SUN_DIS) alınır.
	 * Gökyüzü sınırlarının (skybox) dışına çıkmamasına veya arkasında kalmamasına dikkat edilmelidir.
	 * 
	 * @param camPos Kameranın mevcut dünya pozisyonu.
	 * @return Güneşin 3 boyutlu dünya pozisyonu (Vector3f).
	 */
	public Vector3f getWorldPosition(Vector3f camPos) {
		Vector3f sunPos = new Vector3f(lightDirection);
		sunPos.negate(); // Işığın geldiği yöne doğru gitmek için vektörü ters çeviriyoruz
		sunPos.scale(SUN_DIS); // Vektörü Güneşin uzaklığı ile çarpıyoruz
		return Vector3f.add(camPos, sunPos, null); // Kameranın pozisyonuna ekleyerek nihai pozisyonu buluyoruz
	}

}
