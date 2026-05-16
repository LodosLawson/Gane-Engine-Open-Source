package water;

/**
 * Dünyadaki belli bir alana yerleştirilen su düzlemi objesini temsil eder.
 * Her bir WaterTile (Su Fayansı), sabit boyutta bir dörtgendir.
 */
public class WaterTile {
	
	// Her bir su fayansının temel boyutu (Genişlik ve Derinlik)
	public static final float TILE_SIZE = 10;
	
	private float height; // Suyun Y eksenindeki yüksekliği
	private float x, z;   // Suyun Dünya (World) üzerindeki konumu
	
	/**
	 * Yeni bir su fayansı oluşturur.
	 * 
	 * @param centerX Merkez X koordinatı
	 * @param centerZ Merkez Z koordinatı
	 * @param height Suyun yüksekliği (Deniz seviyesi)
	 */
	public WaterTile(float centerX, float centerZ, float height){
		this.x = centerX;
		this.z = centerZ;
		this.height = height;

	}

	/** @return Suyun yüksekliği */
	public float getHeight() {
		return height;
	}

	/** @return Suyun merkez X konumu */
	public float getX() {
		return x;
	}

	/** @return Suyun merkez Z konumu */
	public float getZ() {
		return z;
	}

}
