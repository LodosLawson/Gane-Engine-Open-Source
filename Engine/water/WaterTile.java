package water;

/**
 * Dünyadaki belli bir alana yerleştirilen su düzlemi objesini temsil eder.
 * Her bir WaterTile (Su Fayansı), sabit boyutta bir dörtgendir.
 */
public class WaterTile {

	// Her bir su fayansının temel boyutu (Genişlik ve Derinlik)
	public static final float TILE_SIZE = 10;

	private float height; // Suyun Y eksenindeki yüksekliği
	private float x, z; // Suyun Dünya (World) üzerindeki konumu
	private float size; // Suyun boyutu

	// Her su tile'ının kendine ait dalgalanma hızı ve bükülme gücü
	private float waveSpeed = 0.0005f;
	private float waveStrength = 0.01f;
	private float moveFactor = 0f;

	/**
	 * Yeni bir su fayansı oluşturur.
	 * 
	 * @param centerX Merkez X koordinatı
	 * @param centerZ Merkez Z koordinatı
	 * @param height  Suyun yüksekliği (Deniz seviyesi)
	 */
	public WaterTile(float centerX, float centerZ, float height) {
		this(centerX, centerZ, height, TILE_SIZE);
	}

	/**
	 * Özel boyutta yeni bir su fayansı oluşturur.
	 * 
	 * @param centerX Merkez X koordinatı
	 * @param centerZ Merkez Z koordinatı
	 * @param height  Suyun yüksekliği (Deniz seviyesi)
	 * @param size    Suyun boyutu
	 */
	public WaterTile(float centerX, float centerZ, float height, float size) {
		this.x = centerX;
		this.z = centerZ;
		this.height = height;
		this.size = size;
	}

	/** @return Suyun yüksekliği */
	public float getHeight() {
		return height;
	}

	/** Suyun yüksekliğini ayarlar */
	public void setHeight(float height) {
		this.height = height;
	}

	/** @return Suyun merkez X konumu */
	public float getX() {
		return x;
	}

	/** Suyun merkez X konumunu ayarlar */
	public void setX(float x) {
		this.x = x;
	}

	/** @return Suyun merkez Z konumu */
	public float getZ() {
		return z;
	}

	/** Suyun merkez Z konumunu ayarlar */
	public void setZ(float z) {
		this.z = z;
	}

	/** @return Suyun boyutu */
	public float getSize() {
		return size;
	}

	/** Suyun boyutunu ayarlar */
	public void setSize(float size) {
		this.size = size;
	}

	/** @return Dalgalanma hızını döndürür */
	public float getWaveSpeed() {
		return waveSpeed;
	}

	/** Dalgalanma hızını ayarlar */
	public void setWaveSpeed(float waveSpeed) {
		this.waveSpeed = waveSpeed;
	}

	/** @return Dalgalanma gücünü döndürür */
	public float getWaveStrength() {
		return waveStrength;
	}

	/** Dalgalanma gücünü ayarlar */
	public void setWaveStrength(float waveStrength) {
		this.waveStrength = waveStrength;
	}

	/** @return Dalgaların hareket çarpanını döndürür */
	public float getMoveFactor() {
		return moveFactor;
	}

	/** Dalgaların hareket çarpanını ayarlar */
	public void setMoveFactor(float moveFactor) {
		this.moveFactor = moveFactor;
	}

	/** Dalgaların akışını günceller (Her karede çağrılır) */
	public void updateMoveFactor() {
		this.moveFactor = (this.moveFactor + this.waveSpeed) % 1f;
	}

}
